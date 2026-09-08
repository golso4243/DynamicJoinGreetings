package com.swornhero.dynamicjoingreetings.config;

import java.util.ArrayList;
import java.util.List;

public final class DynamicJoinGreetingsConfig {
    public int configVersion;
    public boolean enabled;
    public String serverName;
    public int delayTicks;

    public SelectionSettings selection;
    public MessagePool firstJoin;
    public MessagePool returningJoin;

    public static DynamicJoinGreetingsConfig createDefault() {
        DynamicJoinGreetingsConfig config = new DynamicJoinGreetingsConfig();

        config.configVersion = 1;
        config.enabled = true;
        config.serverName = "Minecraft Server";
        config.delayTicks = 40;

        config.selection = new SelectionSettings();
        config.selection.mode = SelectionMode.SHUFFLE_BAG;
        config.selection.avoidImmediateRepeats = true;
        config.selection.rememberPerPlayer = true;

        config.firstJoin = new MessagePool();
        config.firstJoin.enabled = true;
        config.firstJoin.audience = Audience.PLAYER;
        config.firstJoin.messages.add(new MessageEntry(
                "first_welcome",
                1.0,
                List.of(
                        "<gold><bold>Welcome to {server}, {player}!</bold></gold>",
                        "<gray>Your first adventure begins here.</gray>"
                )
        ));
        config.firstJoin.messages.add(new MessageEntry(
                "first_new_chapter",
                1.0,
                List.of(
                        "<yellow>Welcome, {player}!</yellow>",
                        "<gray>A new chapter awaits you on {server}.</gray>"
                )
        ));

        config.returningJoin = new MessagePool();
        config.returningJoin.enabled = true;
        config.returningJoin.audience = Audience.PLAYER;
        config.returningJoin.messages.add(new MessageEntry(
                "return_welcome_back",
                1.0,
                List.of(
                        "<gold>Welcome back, <yellow>{player}</yellow>!</gold>"
                )
        ));
        config.returningJoin.messages.add(new MessageEntry(
                "return_adventure",
                1.0,
                List.of(
                        "<yellow>Another adventure awaits, {player}!</yellow>"
                )
        ));
        config.returningJoin.messages.add(new MessageEntry(
                "return_home",
                1.0,
                List.of(
                        "<gold>Welcome home, {player}.</gold>",
                        "<gray>It is good to see you again on {server}.</gray>"
                )
        ));

        return config;
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