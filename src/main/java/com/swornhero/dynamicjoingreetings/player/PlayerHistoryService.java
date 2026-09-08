package com.swornhero.dynamicjoingreetings.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public final class PlayerHistoryService {
    private static final Logger LOGGER =
            LoggerFactory.getLogger("dynamic-join-greetings/player-history");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static PlayerHistoryService instance;

    private final Path historyFile;
    private final Set<UUID> knownPlayers;

    private PlayerHistoryService(Path historyFile, Set<UUID> knownPlayers) {
        this.historyFile = historyFile;
        this.knownPlayers = knownPlayers;
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                instance = load(server)
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(server ->
                instance = null
        );
    }

    public static boolean recordJoin(UUID playerId) {
        if (instance == null) {
            throw new IllegalStateException(
                    "Player history has not been initialized"
            );
        }

        return instance.markSeen(playerId);
    }

    /**
     * Adds a player to the history.
     *
     * @return true if the UUID was not previously stored
     */
    public synchronized boolean markSeen(UUID playerId) {
        if (!knownPlayers.add(playerId)) {
            return false;
        }

        save();
        return true;
    }

    public synchronized boolean hasJoinedBefore(UUID playerId) {
        return knownPlayers.contains(playerId);
    }

    private static PlayerHistoryService load(MinecraftServer server) {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path dataDirectory =
                worldRoot.resolve("dynamic-join-greetings");

        Path historyFile =
                dataDirectory.resolve("players.json");

        try {
            Files.createDirectories(dataDirectory);

            if (Files.exists(historyFile)) {
                try {
                    HistoryData data = GSON.fromJson(
                            Files.readString(
                                    historyFile,
                                    StandardCharsets.UTF_8
                            ),
                            HistoryData.class
                    );

                    validateHistoryData(data);

                    Set<UUID> players =
                            new HashSet<>(data.players);

                    LOGGER.info(
                            "Loaded {} known player UUIDs",
                            players.size()
                    );

                    return new PlayerHistoryService(
                            historyFile,
                            players
                    );
                } catch (Exception exception) {
                    LOGGER.error(
                            "Player history at {} is damaged or unsupported. "
                                    + "It will be backed up and rebuilt.",
                            historyFile,
                            exception
                    );

                    backUpDamagedHistory(historyFile);
                }
            }

            Set<UUID> existingPlayers =
                    findExistingPlayers(
                            worldRoot.resolve("playerdata")
                    );

            PlayerHistoryService service =
                    new PlayerHistoryService(
                            historyFile,
                            existingPlayers
                    );

            service.save();

            LOGGER.info(
                    "Created player history with {} existing player UUIDs",
                    existingPlayers.size()
            );

            return service;
        } catch (IOException exception) {
            LOGGER.error(
                    "Unable to initialize persistent player history. "
                            + "The server will continue with temporary "
                            + "in-memory history.",
                    exception
            );

            Set<UUID> fallbackPlayers = new HashSet<>();

            try {
                fallbackPlayers.addAll(
                        findExistingPlayers(
                                worldRoot.resolve("playerdata")
                        )
                );
            } catch (IOException scanException) {
                LOGGER.error(
                        "Unable to scan existing Minecraft playerdata",
                        scanException
                );
            }

            return new PlayerHistoryService(
                    historyFile,
                    fallbackPlayers
            );
        }
    }

    private static void validateHistoryData(HistoryData data) {
        if (data == null) {
            throw new IllegalArgumentException(
                    "Player history cannot be empty"
            );
        }

        if (data.schemaVersion != 1) {
            throw new IllegalArgumentException(
                    "Unsupported player history schemaVersion: "
                            + data.schemaVersion
            );
        }

        if (data.players == null) {
            throw new IllegalArgumentException(
                    "Player history is missing the players collection"
            );
        }

        if (data.players.contains(null)) {
            throw new IllegalArgumentException(
                    "Player history contains a null UUID"
            );
        }
    }

    private static void backUpDamagedHistory(
            Path historyFile
    ) throws IOException {
        String backupName =
                historyFile.getFileName()
                        + ".corrupt-"
                        + System.currentTimeMillis()
                        + ".bak";

        Path backupFile =
                historyFile.resolveSibling(backupName);

        Files.move(
                historyFile,
                backupFile,
                StandardCopyOption.REPLACE_EXISTING
        );

        LOGGER.warn(
                "Backed up damaged player history to {}",
                backupFile
        );
    }

    private static Set<UUID> findExistingPlayers(Path playerDataDirectory)
            throws IOException {
        Set<UUID> players = new HashSet<>();

        if (!Files.isDirectory(playerDataDirectory)) {
            return players;
        }

        try (Stream<Path> files = Files.list(playerDataDirectory)) {
            files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".dat"))
                    .map(name -> name.substring(0, name.length() - 4))
                    .forEach(name -> {
                        try {
                            players.add(UUID.fromString(name));
                        } catch (IllegalArgumentException ignored) {
                            LOGGER.warn(
                                    "Ignoring invalid playerdata filename: {}",
                                    name
                            );
                        }
                    });
        }

        return players;
    }

    private synchronized void save() {
        HistoryData data = new HistoryData();
        data.players.addAll(knownPlayers);

        Path temporaryFile =
                historyFile.resolveSibling(historyFile.getFileName() + ".tmp");

        try {
            Files.writeString(
                    temporaryFile,
                    GSON.toJson(data),
                    StandardCharsets.UTF_8
            );

            try {
                Files.move(
                        temporaryFile,
                        historyFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(
                        temporaryFile,
                        historyFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (IOException exception) {
            LOGGER.error("Unable to save player history", exception);
        }
    }

    private static final class HistoryData {
        private int schemaVersion = 1;
        private Set<UUID> players = new HashSet<>();
    }
}