package com.swornhero.dynamicjoingreetings.config;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class DynamicJoinGreetingsConfig {
    private static final String DEFAULT_CONFIG_RESOURCE =
            "/default-dynamic-join-greetings.json";

    public int configVersion;
    public boolean enabled;
    public String serverName;
    public int delayTicks;

    public SelectionSettings selection;
    public MessagePool firstJoin;
    public MessagePool returningJoin;

    public static DynamicJoinGreetingsConfig createDefault() {
        try (InputStream stream = DynamicJoinGreetingsConfig.class
                .getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Bundled default configuration is missing: "
                                + DEFAULT_CONFIG_RESOURCE
                );
            }

            try (Reader reader = new InputStreamReader(
                    stream,
                    StandardCharsets.UTF_8
            )) {
                DynamicJoinGreetingsConfig config =
                        new Gson().fromJson(
                                reader,
                                DynamicJoinGreetingsConfig.class
                        );

                if (config == null) {
                    throw new IllegalStateException(
                            "Bundled default configuration is empty: "
                                    + DEFAULT_CONFIG_RESOURCE
                    );
                }

                return config;
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read bundled default configuration: "
                            + DEFAULT_CONFIG_RESOURCE,
                    exception
            );
        }
    }

    public enum SelectionMode {
        RANDOM,
        NO_REPEAT,
        SHUFFLE_BAG
    }

    public enum Audience {
        PLAYER,
        BROADCAST,
        BOTH
    }

    public static final class SelectionSettings {
        public SelectionMode mode;
        public boolean avoidImmediateRepeats;
        public boolean rememberPerPlayer;
    }

    public static final class MessagePool {
        public boolean enabled;
        public Audience audience;
        public List<MessageEntry> messages = new ArrayList<>();
    }

    public static final class MessageEntry {
        public String id;
        public double weight;
        public List<String> lines = new ArrayList<>();

        public MessageEntry() {
        }

        public MessageEntry(String id, double weight, List<String> lines) {
            this.id = id;
            this.weight = weight;
            this.lines = new ArrayList<>(lines);
        }
    }
}
