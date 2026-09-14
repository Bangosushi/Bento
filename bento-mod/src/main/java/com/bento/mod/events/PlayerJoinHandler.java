package com.bento.mod.events;

import com.bento.mod.BentoMod;
import com.bento.mod.config.BentoModConfig;
import com.bento.mod.http.BotHttpClient;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class PlayerJoinHandler {

    public static void register(BentoModConfig config) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String ign = handler.player.getName().getString();
            // Fire-and-forget async — never block the server thread
            BotHttpClient.sendJoinEvent(config, ign);
            BentoMod.LOGGER.debug("JOIN event fired for {}", ign);
        });
    }
}
