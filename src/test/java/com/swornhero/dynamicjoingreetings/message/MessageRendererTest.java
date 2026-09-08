package com.swornhero.dynamicjoingreetings.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageRendererTest {
    @Test
    void acceptsValidMiniMessage() {
        assertDoesNotThrow(() ->
                MessageRenderer.validate(
                        "<gold><bold>Welcome, {player}!</bold></gold>"
                )
        );
    }

    @Test
    void rejectsUnclosedMiniMessageTags() {
        assertThrows(RuntimeException.class, () ->
                MessageRenderer.validate(
                        "<gold><bold>Broken message"
                )
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

        String plainText = extractText(rendered);

        assertEquals(
                "Welcome " + playerName + " to " + serverName + "!",
                plainText
        );

        assertTrue(
                components(rendered)
                        .noneMatch(component ->
                                component.style().clickEvent() != null
                        ),
                "Placeholder values must not create click events"
        );

        assertTrue(
                components(rendered)
                        .noneMatch(component ->
                                component.style().color() != null
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

        String plainText = extractText(rendered);

        assertEquals(
                "SwornHero joined StoneHavenSMP. Welcome, SwornHero!",
                plainText
        );
    }

    private static Stream<Component> components(
            Component component
    ) {
        return Stream.concat(
                Stream.of(component),
                component.children()
                        .stream()
                        .flatMap(MessageRendererTest::components)
        );
    }

    private static String extractText(Component component) {
        StringBuilder output = new StringBuilder();
        appendText(component, output);
        return output.toString();
    }

    private static void appendText(
            Component component,
            StringBuilder output
    ) {
        if (component instanceof TextComponent textComponent) {
            output.append(textComponent.content());
        }

        for (Component child : component.children()) {
            appendText(child, output);
        }
    }
}