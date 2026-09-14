package com.bento.mod.config;

import com.bento.mod.BentoMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loaded from {@code <server>/config/bento.json} on startup.
 * A default file is created on first run — edit it and restart.
 */
public record BentoModConfig(
        String botUrl,      // e.g. "http://bento-bot:8080"
        String apiKey,      // must match the bot's API_KEY env var
        int    httpPort,    // port the embedded REST server listens on
        String serverType   // "SURVIVAL" or "CREATIVE"
) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final BentoModConfig DEFAULTS =
            new BentoModConfig("http://localhost:8080", "changeme", 7070, "SURVIVAL");

    public static BentoModConfig load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            write(path, DEFAULTS);
            BentoMod.LOGGER.warn("No bento.json found — created default config at {}. " +
                                 "Please update it and restart.", path);
            return DEFAULTS;
        }
        try {
            String json = Files.readString(path);
            return GSON.fromJson(json, BentoModConfig.class);
        } catch (IOException e) {
            BentoMod.LOGGER.error("Failed to read bento.json: {}. Using defaults.", e.getMessage());
            return DEFAULTS;
        }
    }

    private static void write(Path path, BentoModConfig config) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(config));
        } catch (IOException e) {
            BentoMod.LOGGER.error("Failed to write default bento.json: {}", e.getMessage());
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("bento.json");
    }
}
