package com.swornhero.dynamicjoingreetings.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.swornhero.dynamicjoingreetings.DynamicJoinGreetings;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("dynamic-join-greetings.json");

    private static DynamicJoinGreetingsConfig activeConfig =
            DynamicJoinGreetingsConfig.createDefault();

    private ConfigManager() {
    }

    public static synchronized void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            if (Files.notExists(CONFIG_PATH)) {
                DynamicJoinGreetingsConfig defaultConfig =
                        DynamicJoinGreetingsConfig.createDefault();

                write(defaultConfig);
                activeConfig = defaultConfig;

                DynamicJoinGreetings.LOGGER.info(
                        "Created default configuration at {}.",
                        CONFIG_PATH
                );
                return;
            }

            DynamicJoinGreetingsConfig loadedConfig;

            try (Reader reader = Files.newBufferedReader(
                    CONFIG_PATH,
                    StandardCharsets.UTF_8
            )) {
                loadedConfig = GSON.fromJson(
                        reader,
                        DynamicJoinGreetingsConfig.class
                );
            }

            validate(loadedConfig);
            activeConfig = loadedConfig;

            DynamicJoinGreetings.LOGGER.info(
                    "Loaded configuration from {}.",
                    CONFIG_PATH
            );
        } catch (Exception exception) {
            DynamicJoinGreetings.LOGGER.error(
                    "Could not load configuration from {}. "
                            + "The previous configuration will remain active.",
                    CONFIG_PATH,
                    exception
            );
        }
    }

    public static DynamicJoinGreetingsConfig get() {
        return activeConfig;
    }

    private static void write(DynamicJoinGreetingsConfig config)
            throws Exception {
        Path temporaryPath = CONFIG_PATH.resolveSibling(
                CONFIG_PATH.getFileName() + ".tmp"
        );

        try (Writer writer = Files.newBufferedWriter(
                temporaryPath,
                StandardCharsets.UTF_8
        )) {
            GSON.toJson(config, writer);
        }

        try {
            Files.move(
                    temporaryPath,
                    CONFIG_PATH,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(
                    temporaryPath,
                    CONFIG_PATH,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private static void validate(DynamicJoinGreetingsConfig config) {
        if (config == null) {
            throw new IllegalArgumentException(
                    "The configuration cannot be empty."
            );
        }

        if (config.configVersion != 1) {
            throw new IllegalArgumentException(
                    "Unsupported configVersion: " + config.configVersion
            );
        }

        if (config.serverName == null || config.serverName.isBlank()) {
            throw new IllegalArgumentException(
                    "serverName cannot be empty."
            );
        }

        if (config.delayTicks < 0 || config.delayTicks > 1200) {
            throw new IllegalArgumentException(
                    "delayTicks must be between 0 and 1200."
            );
        }

        if (config.selection == null || config.selection.mode == null) {
            throw new IllegalArgumentException(
                    "selection and selection.mode are required."
            );
        }

        validatePool("firstJoin", config.firstJoin);
        validatePool("returningJoin", config.returningJoin);
    }

    private static void validatePool(
            String poolName,
            DynamicJoinGreetingsConfig.MessagePool pool
    ) {
        if (pool == null) {
            throw new IllegalArgumentException(
                    poolName + " cannot be missing."
            );
        }

        if (pool.audience == null) {
            throw new IllegalArgumentException(
                    poolName + ".audience cannot be missing."
            );
        }

        if (pool.messages == null) {
            throw new IllegalArgumentException(
                    poolName + ".messages cannot be missing."
            );
        }

        if (pool.enabled && pool.messages.isEmpty()) {
            throw new IllegalArgumentException(
                    poolName + " is enabled but has no messages."
            );
        }

        Set<String> messageIds = new HashSet<>();

        for (DynamicJoinGreetingsConfig.MessageEntry message : pool.messages) {
            if (message == null) {
                throw new IllegalArgumentException(
                        poolName + " contains an empty message entry."
                );
            }

            if (message.id == null || message.id.isBlank()) {
                throw new IllegalArgumentException(
                        poolName + " contains a message without an ID."
                );
            }

            if (!messageIds.add(message.id)) {
                throw new IllegalArgumentException(
                        poolName + " contains duplicate message ID: "
                                + message.id
                );
            }

            if (!Double.isFinite(message.weight) || message.weight <= 0) {
                throw new IllegalArgumentException(
                        poolName + "." + message.id
                                + ".weight must be greater than zero."
                );
            }

            if (message.lines == null || message.lines.isEmpty()) {
                throw new IllegalArgumentException(
                        poolName + "." + message.id
                                + " must contain at least one line."
                );
            }

            for (String line : message.lines) {
                if (line == null) {
                    throw new IllegalArgumentException(
                            poolName + "." + message.id
                                    + " contains a null line."
                    );
                }
            }
        }
    }
}