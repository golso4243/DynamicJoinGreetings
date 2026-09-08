package com.swornhero.dynamicjoingreetings.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.swornhero.dynamicjoingreetings.config.ConfigManager;
import com.swornhero.dynamicjoingreetings.config.DynamicJoinGreetingsConfig;
import com.swornhero.dynamicjoingreetings.message.MessageRenderer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.level.ServerPlayer;
import com.swornhero.dynamicjoingreetings.message.MessageSelector;

public final class JoinGreetingsCommands {
    private JoinGreetingsCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("joingreetings")
                                        .requires(source ->
                                                source.getPermissionContext()
                                                        .permissionLevel()
                                                        .isEqualOrHigherThan(PermissionLevel.GAMEMASTERS)
                                        )
                                        .then(
                                                Commands.literal("preview")
                                                        .then(
                                                                Commands.literal("first")
                                                                        .executes(context ->
                                                                                preview(context, true)
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.literal("returning")
                                                                        .executes(context ->
                                                                                preview(context, false)
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("reload")
                                                        .executes(JoinGreetingsCommands::reload)
                                        )
                                        .then(
                                                Commands.literal("status")
                                                        .executes(JoinGreetingsCommands::status)
                                        )
                        )
        );
    }

    private static int reload(
            CommandContext<CommandSourceStack> context
    ) {
        boolean successful = ConfigManager.load();

        if (!successful) {
            context.getSource().sendFailure(
                    net.minecraft.network.chat.Component.literal(
                            "Dynamic Join Greetings could not reload. "
                                    + "The previous configuration remains active. "
                                    + "Check the server console for details."
                    )
            );

            return 0;
        }

        MessageSelector.clear();

        context.getSource().sendSuccess(
                () -> net.minecraft.network.chat.Component.literal(
                        "Dynamic Join Greetings configuration reloaded."
                ),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    private static int status(
            CommandContext<CommandSourceStack> context
    ) {
        DynamicJoinGreetingsConfig config = ConfigManager.get();

        sendStatusLine(
                context,
                "Dynamic Join Greetings Status"
        );

        sendStatusLine(
                context,
                "Enabled: " + config.enabled
        );

        sendStatusLine(
                context,
                "Server name: " + config.serverName
        );

        sendStatusLine(
                context,
                "Delay: " + config.delayTicks
                        + " ticks ("
                        + String.format("%.2f", config.delayTicks / 20.0)
                        + " seconds)"
        );

        sendStatusLine(
                context,
                "Selection mode: " + config.selection.mode
        );

        sendStatusLine(
                context,
                "Avoid immediate repeats: "
                        + config.selection.avoidImmediateRepeats
        );

        sendStatusLine(
                context,
                "Remember per player: "
                        + config.selection.rememberPerPlayer
        );

        sendStatusLine(
                context,
                "First join: "
                        + poolDescription(config.firstJoin)
        );

        sendStatusLine(
                context,
                "Returning join: "
                        + poolDescription(config.returningJoin)
        );

        sendStatusLine(
                context,
                "First join pool: enabled="
                        + config.firstJoin.enabled
                        + ", audience="
                        + config.firstJoin.audience
                        + ", messages="
                        + config.firstJoin.messages.size()
        );

        sendStatusLine(
                context,
                "Returning pool: enabled="
                        + config.returningJoin.enabled
                        + ", audience="
                        + config.returningJoin.audience
                        + ", messages="
                        + config.returningJoin.messages.size()
        );

        return Command.SINGLE_SUCCESS;
    }

    private static String poolDescription(
            DynamicJoinGreetingsConfig.MessagePool pool
    ) {
        int messageCount =
                pool.messages == null ? 0 : pool.messages.size();

        return "enabled=" + pool.enabled
                + ", audience=" + pool.audience
                + ", messages=" + messageCount;
    }

    private static void sendStatusLine(
            CommandContext<CommandSourceStack> context,
            String message
    ) {
        context.getSource().sendSuccess(
                () -> net.minecraft.network.chat.Component.literal(message),
                false
        );
    }

    private static int preview(
            CommandContext<CommandSourceStack> context,
            boolean firstTime
    ) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        DynamicJoinGreetingsConfig config = ConfigManager.get();

        DynamicJoinGreetingsConfig.MessagePool pool = firstTime
                ? config.firstJoin
                : config.returningJoin;

        if (pool.messages == null || pool.messages.isEmpty()) {
            player.sendMessage(Component.text(
                    "That message pool contains no messages.",
                    NamedTextColor.RED
            ));
            return 0;
        }

        String previewPoolName = firstTime
                ? "preview:firstJoin"
                : "preview:returningJoin";

        DynamicJoinGreetingsConfig.MessageEntry message =
                MessageSelector.select(
                        previewPoolName,
                        pool,
                        config.selection,
                        player.getUUID()
                );

        if (message == null) {
            player.sendMessage(Component.text(
                    "No message could be selected.",
                    NamedTextColor.RED
            ));

            return 0;
        }

        String playerName = player.getName().getString();

        for (String line : message.lines) {
            player.sendMessage(MessageRenderer.render(
                    line,
                    playerName,
                    config.serverName
            ));
        }

        return Command.SINGLE_SUCCESS;
    }
}