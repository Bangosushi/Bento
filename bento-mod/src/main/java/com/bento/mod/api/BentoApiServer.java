package com.bento.mod.api;

import com.bento.mod.BentoMod;
import com.bento.mod.config.BentoModConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Lightweight REST API server embedded in the mod using the JDK's built-in HttpServer.
 * Zero external dependencies — Gson is bundled with Minecraft.
 *
 * Endpoints (all require X-Api-Key header):
 *   POST /api/whitelist/add     {"ign":"PlayerName"}
 *   POST /api/whitelist/remove  {"ign":"PlayerName"}
 *   POST /api/op/add            {"ign":"PlayerName"}
 *   POST /api/op/remove         {"ign":"PlayerName"}
 */
public class BentoApiServer {

    private static BentoModConfig config;

    public static void start(BentoModConfig cfg) {
        config = cfg;
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(cfg.httpPort()), 0);
            server.createContext("/api/whitelist/add",    ex -> handle(ex, "/whitelist add"));
            server.createContext("/api/whitelist/remove", ex -> handle(ex, "/whitelist remove"));
            server.createContext("/api/op/add",           ex -> handle(ex, "/op"));
            server.createContext("/api/op/remove",        ex -> handle(ex, "/deop"));
            server.setExecutor(Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "bento-api");
                t.setDaemon(true);
                return t;
            }));
            server.start();
            BentoMod.LOGGER.info("Bento API server listening on :{}", cfg.httpPort());
        } catch (IOException e) {
            BentoMod.LOGGER.error("Failed to start Bento API server: {}", e.getMessage(), e);
        }
    }

    private static void handle(HttpExchange exchange, String commandPrefix) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            respond(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }
        if (!isAuthorized(exchange)) {
            respond(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        String ign = parseIgn(exchange);
        if (ign == null || ign.isBlank()) {
            respond(exchange, 400, "{\"error\":\"Missing or empty 'ign' field\"}");
            return;
        }

        String command = commandPrefix + " " + ign;
        boolean ok = executeCommand(command);

        if (ok) {
            respond(exchange, 200, "{\"success\":true,\"command\":\"" + command + "\"}");
        } else {
            respond(exchange, 503, "{\"success\":false,\"error\":\"Server not ready\"}");
        }
    }

    private static boolean executeCommand(String command) {
        MinecraftServer server = BentoMod.SERVER;
        if (server == null) {
            BentoMod.LOGGER.warn("Cannot execute '{}' — server not ready", command);
            return false;
        }
        // Schedule on the server thread (required for command execution)
        server.execute(() -> {
            try {
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack(),
                        command
                        );
                BentoMod.LOGGER.info("Executed: /{}", command);
            } catch (Exception e) {
                BentoMod.LOGGER.error("Failed to execute '{}': {}", command, e.getMessage());
            }
        });
        return true;
    }

    private static boolean isAuthorized(HttpExchange exchange) {
        String key = exchange.getRequestHeaders().getFirst("X-Api-Key");
        return config.apiKey().equals(key);
    }

    private static String parseIgn(HttpExchange exchange) {
        try {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            JsonObject json = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            return json.get("ign").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
