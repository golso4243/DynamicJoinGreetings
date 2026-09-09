package com.swornhero.dynamicjoingreetings.greeting;

import com.swornhero.dynamicjoingreetings.config.ConfigManager;
import com.swornhero.dynamicjoingreetings.config.DynamicJoinGreetingsConfig;
import com.swornhero.dynamicjoingreetings.message.MessageRenderer;
import com.swornhero.dynamicjoingreetings.message.MessageSelector;
import com.swornhero.dynamicjoingreetings.player.PlayerHistoryService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class GreetingService {
    private static final Logger LOGGER =
            LoggerFactory.getLogger("dynamic-join-greetings/greetings");

    private static final List<PendingGreeting> PENDING_GREETINGS =
            new ArrayList<>();

    private GreetingService() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> {
                    ServerPlayer player = handler.player;
                    UUID playerId = player.getUUID();

                    boolean firstJoin =
                            PlayerHistoryService.recordJoin(playerId);

                    LOGGER.info(
                            "{} joined as a {} player",
                            player.getName().getString(),
                            firstJoin ? "first-time" : "returning"
                    );

                    scheduleGreeting(server, playerId, firstJoin, false);
                }
        );

        ServerTickEvents.END_SERVER_TICK.register(
                GreetingService::processPendingGreetings
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PENDING_GREETINGS.clear();
            MessageSelector.clear();
        });
    }

    public static boolean simulateJoin(
            MinecraftServer server,
            UUID playerId,
            boolean firstJoin
    ) {
        return scheduleGreeting(
                server,
                playerId,
                firstJoin,
                true
        );
    }

    private static boolean scheduleGreeting(
            MinecraftServer server,
            UUID playerId,
            boolean firstJoin,
            boolean simulation
    ) {
        DynamicJoinGreetingsConfig config = ConfigManager.get();

        if (!config.enabled) {
            return false;
        }

        DynamicJoinGreetingsConfig.MessagePool pool =
                firstJoin
                        ? config.firstJoin
                        : config.returningJoin;

        if (!pool.enabled || pool.messages.isEmpty()) {
            return false;
        }

        PENDING_GREETINGS.removeIf(greeting ->
                greeting.playerId.equals(playerId)
                        && greeting.simulation == simulation
        );

        if (config.delayTicks == 0) {
            sendGreeting(server, playerId, firstJoin, simulation);
            return true;
        }

        PENDING_GREETINGS.add(new PendingGreeting(
                playerId,
                firstJoin,
                config.delayTicks,
                simulation
        ));

        return true;
    }

    private static void processPendingGreetings(
            MinecraftServer server
    ) {
        Iterator<PendingGreeting> iterator =
                PENDING_GREETINGS.iterator();

        while (iterator.hasNext()) {
            PendingGreeting greeting = iterator.next();
            greeting.ticksRemaining--;

            if (greeting.ticksRemaining <= 0) {
                iterator.remove();

                sendGreeting(
                        server,
                        greeting.playerId,
                        greeting.firstJoin,
                        greeting.simulation
                );
            }
        }
    }

    private static void sendGreeting(
            MinecraftServer server,
            UUID playerId,
            boolean firstJoin,
            boolean simulation
    ) {
        ServerPlayer joiningPlayer =
                server.getPlayerList().getPlayer(playerId);

        if (joiningPlayer == null) {
            return;
        }

        DynamicJoinGreetingsConfig config = ConfigManager.get();

        if (!config.enabled) {
            return;
        }

        DynamicJoinGreetingsConfig.MessagePool pool =
                firstJoin
                        ? config.firstJoin
                        : config.returningJoin;

        if (!pool.enabled || pool.messages.isEmpty()) {
            return;
        }

        DynamicJoinGreetingsConfig.MessageEntry message =
                MessageSelector.select(
                        simulation
                                ? firstJoin
                                        ? "simulation:firstJoin"
                                        : "simulation:returningJoin"
                                : firstJoin
                                        ? "firstJoin"
                                        : "returningJoin",
                        pool,
                        config.selection,
                        playerId
                );

        if (message == null) {
            return;
        }

        String playerName =
                joiningPlayer.getName().getString();

        for (String line : message.lines) {
            Component rendered = MessageRenderer.render(
                    line,
                    playerName,
                    config.serverName
            );

            deliver(
                    server,
                    joiningPlayer,
                    pool.audience,
                    rendered
            );
        }

        LOGGER.info(
                "{} greeting '{}' to {}",
                simulation ? "Simulated" : "Sent",
                message.id,
                playerName
        );
    }

    private static void deliver(
            MinecraftServer server,
            ServerPlayer joiningPlayer,
            DynamicJoinGreetingsConfig.Audience audience,
            Component message
    ) {
        switch (audience) {
            case PLAYER -> joiningPlayer.sendSystemMessage(message);

            case BROADCAST -> server.getPlayerList()
                    .getPlayers()
                    .stream()
                    .filter(player ->
                            !player.getUUID().equals(
                                    joiningPlayer.getUUID()
                            )
                    )
                    .forEach(player ->
                            player.sendSystemMessage(message)
                    );

            case BOTH -> server.getPlayerList()
                    .getPlayers()
                    .forEach(player ->
                            player.sendSystemMessage(message)
                    );
        }
    }

    private static final class PendingGreeting {
        private final UUID playerId;
        private final boolean firstJoin;
        private final boolean simulation;
        private int ticksRemaining;

        private PendingGreeting(
                UUID playerId,
                boolean firstJoin,
                int ticksRemaining,
                boolean simulation
        ) {
            this.playerId = playerId;
            this.firstJoin = firstJoin;
            this.ticksRemaining = ticksRemaining;
            this.simulation = simulation;
        }
    }
}