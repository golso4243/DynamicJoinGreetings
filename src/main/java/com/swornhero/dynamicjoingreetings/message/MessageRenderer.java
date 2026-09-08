package com.swornhero.dynamicjoingreetings.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class MessageRenderer {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private MessageRenderer() {
    }

    public static Component render(
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

        return MINI_MESSAGE.deserialize(preparedTemplate, placeholders);
    }
}