package com.swornhero.dynamicjoingreetings.message;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageRendererTest {
    @Test
    void acceptsValidFormattedMessage() {
        assertDoesNotThrow(() ->
                MessageRenderer.validate(
                        "<gold><bold>Welcome, {player}!</bold></gold>"
                )
        );
    }

    @Test
    void rendersPlainText() {
        Component rendered = MessageRenderer.render(
                "Hello, {player}!",
                "SwornHero",
                "StoneHavenSMP"
        );

        assertEquals(
                "Hello, SwornHero!",
                rendered.getString()
        );
    }

    @Test
    void safelyInsertsPlaceholderValues() {
        String playerName = "<red>Injected Player</red>";
        String serverName =
                "<click:run_command:'/op @s'>Injected Server</click>";

        Component rendered = MessageRenderer.render(
                "Welcome {player} to {server}!",
                playerName,
                serverName
        );

        assertEquals(
                "Welcome " + playerName + " to " + serverName + "!",
                rendered.getString()
        );

        assertTrue(
                components(rendered)
                        .noneMatch(component ->
                                component.getStyle().getClickEvent() != null
                        ),
                "Placeholder values must not create click events"
        );

        assertTrue(
                components(rendered)
                        .noneMatch(component ->
                                component.getStyle().getColor() != null
                        ),
                "Placeholder values must not inject colors"
        );
    }

    @Test
    void replacesEveryPlaceholderOccurrence() {
        Component rendered = MessageRenderer.render(
                "{player} joined {server}. Welcome, {player}!",
                "SwornHero",
                "StoneHavenSMP"
        );

        assertEquals(
                "SwornHero joined StoneHavenSMP. Welcome, SwornHero!",
                rendered.getString()
        );
    }

    private static Stream<Component> components(
            Component component
    ) {
        return Stream.concat(
                Stream.of(component),
                component.getSiblings()
                        .stream()
                        .flatMap(MessageRendererTest::components)
        );
    }
}