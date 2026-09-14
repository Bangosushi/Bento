package com.bento.mod;

import com.bento.mod.api.BentoApiServer;
import com.bento.mod.config.BentoModConfig;
import com.bento.mod.events.PlayerJoinHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BentoMod implements ModInitializer {

    public static final String MOD_ID = "bento-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Shared server reference — set on start, cleared on stop. */
    public static volatile MinecraftServer SERVER;

    @Override
    public void onInitialize() {
        LOGGER.info("Bento Mod initialising...");

        BentoModConfig config = BentoModConfig.load();

        // Embedded REST API for the bot to call
        BentoApiServer.start(config);

        // Fires JOIN events back to the bot
        PlayerJoinHandler.register(config);

        // Keep the server reference up to date
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            SERVER = server;
            LOGGER.info("Bento Mod ready — server type: {}", config.serverType());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> SERVER = null);
    }
}
