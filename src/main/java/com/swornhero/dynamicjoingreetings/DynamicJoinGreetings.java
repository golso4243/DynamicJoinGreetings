package com.swornhero.dynamicjoingreetings;
import com.swornhero.dynamicjoingreetings.config.ConfigManager;
import com.swornhero.dynamicjoingreetings.command.JoinGreetingsCommands;
import com.swornhero.dynamicjoingreetings.player.PlayerHistoryService;
import com.swornhero.dynamicjoingreetings.greeting.GreetingService;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DynamicJoinGreetings implements ModInitializer {
	public static final String MOD_ID = "dynamic-join-greetings";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ConfigManager.load();
		JoinGreetingsCommands.register();
		PlayerHistoryService.register();
		GreetingService.register();
		LOGGER.info("Dynamic Join Greetings initialized.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}