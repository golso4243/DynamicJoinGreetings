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

        DynamicJoinGreetingsConfig.MessageEntry message =
                pool.messages.getFirst();

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