package com.swornhero.dynamicjoingreetings.message;

import com.swornhero.dynamicjoingreetings.config.DynamicJoinGreetingsConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MessageSelectorTest {
    private static final UUID PLAYER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @AfterEach
    void clearSelectionState() {
        MessageSelector.clear();
    }

    @Test
    void selectsOnlyMessageFromSingleEntryPool() {
        DynamicJoinGreetingsConfig.MessagePool pool =
                createPool("only");

        DynamicJoinGreetingsConfig.SelectionSettings settings =
                createSettings(
                        DynamicJoinGreetingsConfig.SelectionMode.RANDOM,
                        true
                );

        DynamicJoinGreetingsConfig.MessageEntry selected =
                MessageSelector.select(
                        "single",
                        pool,
                        settings,
                        PLAYER_ID
                );

        assertNotNull(selected);
        assertEquals("only", selected.id);
    }

    @Test
    void noRepeatAvoidsConsecutiveDuplicates() {
        DynamicJoinGreetingsConfig.MessagePool pool =
                createPool("first", "second", "third");

        DynamicJoinGreetingsConfig.SelectionSettings settings =
                createSettings(
                        DynamicJoinGreetingsConfig.SelectionMode.NO_REPEAT,
                        true
                );

        String previousId = null;

        for (int attempt = 0; attempt < 100; attempt++) {
            DynamicJoinGreetingsConfig.MessageEntry selected =
                    MessageSelector.select(
                            "no-repeat",
                            pool,
                            settings,
                            PLAYER_ID
                    );

            assertNotNull(selected);

            if (previousId != null) {
                assertNotEquals(previousId, selected.id);
            }

            previousId = selected.id;
        }
    }

    @Test
    void randomModeCanAvoidImmediateRepeats() {
        DynamicJoinGreetingsConfig.MessagePool pool =
                createPool("first", "second");

        DynamicJoinGreetingsConfig.SelectionSettings settings =
                createSettings(
                        DynamicJoinGreetingsConfig.SelectionMode.RANDOM,
                        true
                );

        String previousId = null;

        for (int attempt = 0; attempt < 50; attempt++) {
            DynamicJoinGreetingsConfig.MessageEntry selected =
                    MessageSelector.select(
                            "random-no-repeat",
                            pool,
                            settings,
                            PLAYER_ID
                    );

            assertNotNull(selected);

            if (previousId != null) {
                assertNotEquals(previousId, selected.id);
            }

            previousId = selected.id;
        }
    }

    @Test
    void shuffleBagUsesEveryMessageOncePerCycle() {
        DynamicJoinGreetingsConfig.MessagePool pool =
                createPool("first", "second", "third");

        DynamicJoinGreetingsConfig.SelectionSettings settings =
                createSettings(
                        DynamicJoinGreetingsConfig.SelectionMode.SHUFFLE_BAG,
                        true
                );

        Set<String> selectedIds = new HashSet<>();
        String lastId = null;

        for (int attempt = 0; attempt < 3; attempt++) {
            DynamicJoinGreetingsConfig.MessageEntry selected =
                    MessageSelector.select(
                            "shuffle",
                            pool,
                            settings,
                            PLAYER_ID
                    );

            assertNotNull(selected);
            selectedIds.add(selected.id);
            lastId = selected.id;
        }

        assertEquals(
                Set.of("first", "second", "third"),
                selectedIds
        );

        DynamicJoinGreetingsConfig.MessageEntry nextCycle =
                MessageSelector.select(
                        "shuffle",
                        pool,
                        settings,
                        PLAYER_ID
                );

        assertNotNull(nextCycle);
        assertNotEquals(lastId, nextCycle.id);
    }

    private static DynamicJoinGreetingsConfig.MessagePool createPool(
            String... messageIds
    ) {
        DynamicJoinGreetingsConfig.MessagePool pool =
                new DynamicJoinGreetingsConfig.MessagePool();

        pool.enabled = true;
        pool.audience =
                DynamicJoinGreetingsConfig.Audience.PLAYER;

        for (String messageId : messageIds) {
            pool.messages.add(
                    new DynamicJoinGreetingsConfig.MessageEntry(
                            messageId,
                            1.0,
                            List.of("Message " + messageId)
                    )
            );
        }

        return pool;
    }

    private static DynamicJoinGreetingsConfig.SelectionSettings
    createSettings(
            DynamicJoinGreetingsConfig.SelectionMode mode,
            boolean avoidImmediateRepeats
    ) {
        DynamicJoinGreetingsConfig.SelectionSettings settings =
                new DynamicJoinGreetingsConfig.SelectionSettings();

        settings.mode = mode;
        settings.avoidImmediateRepeats = avoidImmediateRepeats;
        settings.rememberPerPlayer = true;

        return settings;
    }
}