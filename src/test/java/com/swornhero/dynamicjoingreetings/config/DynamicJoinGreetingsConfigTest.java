package com.swornhero.dynamicjoingreetings.config;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicJoinGreetingsConfigTest {
    @Test
    void defaultConfigurationContainsAdvertisedMessageCatalog() {
        DynamicJoinGreetingsConfig config =
                DynamicJoinGreetingsConfig.createDefault();

        assertEquals(10, config.firstJoin.messages.size());
        assertEquals(50, config.returningJoin.messages.size());
    }

    @Test
    void defaultMessageIdsAreUniqueWithinEachPool() {
        DynamicJoinGreetingsConfig config =
                DynamicJoinGreetingsConfig.createDefault();

        assertEquals(
                config.firstJoin.messages.size(),
                new HashSet<>(config.firstJoin.messages.stream()
                        .map(message -> message.id)
                        .toList()).size()
        );
        assertEquals(
                config.returningJoin.messages.size(),
                new HashSet<>(config.returningJoin.messages.stream()
                        .map(message -> message.id)
                        .toList()).size()
        );
    }
}
