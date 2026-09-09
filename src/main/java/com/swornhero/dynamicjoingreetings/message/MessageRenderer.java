package com.swornhero.dynamicjoingreetings.message;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public final class MessageRenderer {
    private static final NodeParser MESSAGE_PARSER =
            NodeParser.builder()
                    .simplifiedTextFormat()
                    .build();

    private MessageRenderer() {
    }

    public static Component render(
            String template,
            String playerName,
            String serverName
    ) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(serverName, "serverName");

        String preparedTemplate = prepareTemplate(
                template,
                playerName,
                serverName
        );

        return MESSAGE_PARSER.parseComponent(
                preparedTemplate,
                ParserContext.of()
        );
    }

    public static void validate(String template) {
        Objects.requireNonNull(template, "template");

        String preparedTemplate = prepareTemplate(
                template,
                "Player",
                "Minecraft Server"
        );

        /*
         * Parse the node structure without converting it into a native
         * Component. Component conversion requires an active Fabric launcher,
         * which is available in-game but not during ordinary unit tests.
         */
        MESSAGE_PARSER.parseNode(preparedTemplate);
    }

    private static String prepareTemplate(
            String template,
            String playerName,
            String serverName
    ) {
        return template
                .replace("{player}", escapeLiteral(playerName))
                .replace("{server}", escapeLiteral(serverName));
    }

    private static String escapeLiteral(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("<", "\\<");
    }
}