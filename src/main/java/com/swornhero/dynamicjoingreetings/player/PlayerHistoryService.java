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
        Path dataDirectory = worldRoot.resolve("dynamic-join-greetings");
        Path historyFile = dataDirectory.resolve("players.json");

        try {
            Files.createDirectories(dataDirectory);

            if (Files.exists(historyFile)) {
                HistoryData data = GSON.fromJson(
                        Files.readString(historyFile, StandardCharsets.UTF_8),
                        HistoryData.class
                );

                Set<UUID> players =
                        data != null && data.players != null
                                ? data.players
                                : new HashSet<>();

                LOGGER.info(
                        "Loaded {} known player UUIDs",
                        players.size()
                );

                return new PlayerHistoryService(historyFile, players);
            }

            Set<UUID> existingPlayers =
                    findExistingPlayers(worldRoot.resolve("playerdata"));

            PlayerHistoryService service =
                    new PlayerHistoryService(historyFile, existingPlayers);

            service.save();

            LOGGER.info(
                    "Created player history with {} existing player UUIDs",
                    existingPlayers.size()
            );

            return service;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load player history from " + historyFile,
                    exception
            );
        }
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