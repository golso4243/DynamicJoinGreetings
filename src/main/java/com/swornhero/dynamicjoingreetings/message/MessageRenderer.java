package com.swornhero.dynamicjoingreetings.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class MessageRenderer {
    private static final MiniMessage MINI_MESSAGE =
            MiniMessage.miniMessage();

    private static final MiniMessage STRICT_MINI_MESSAGE =
            MiniMessage.builder()
                    .strict(true)
                    .build();

    private MessageRenderer() {
    }

    public static Component render(
            String template,
            String playerName,
            String serverName
    ) {
        return deserialize(
                MINI_MESSAGE,
                template,
                playerName,
                serverName
        );
    }

    public static void validate(String template) {
        deserialize(
                STRICT_MINI_MESSAGE,
                template,
                "Player",
                "Minecraft Server"
        );
    }

    private static Component deserialize(
            MiniMessage miniMessage,
            String template,
            String playerName,
            String serverName
    ) {
        String preparedTemplate = template
                .replace("{player}", "<djg_player>")
                .replace("{server}", "<djg_server>");

        TagResolver placeholders = TagResolver.builder()
                .resolver(Placeholder.component(
                        "djg_player",
                        Component.text(playerName)
                ))
                .resolver(Placeholder.component(
                        "djg_server",
                        Component.text(serverName)
                ))
                .build();

        return miniMessage.deserialize(
                preparedTemplate,
                placeholders
        );
    }
}