package com.bento.mod.http;

import com.bento.mod.BentoMod;
import com.bento.mod.config.BentoModConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Fire-and-forget HTTP client for sending events to the BentoBot.
 * Uses the JDK 11 {@link HttpClient} — no extra dependencies.
 */
public class BotHttpClient {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static void sendJoinEvent(BentoModConfig config, String ign) {
        String body = buildJoinPayload(ign, config.serverType());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.botUrl() + "/api/event"))
                .header("Content-Type", "application/json")
                .header("X-Api-Key", config.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(5))
                .build();

        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
              .whenComplete((resp, ex) -> {
                  if (ex != null) {
                      BentoMod.LOGGER.warn("Failed to send JOIN event for {}: {}", ign, ex.getMessage());
                  } else if (resp.statusCode() != 200) {
                      BentoMod.LOGGER.warn("Bot returned {} for JOIN event (IGN: {})", resp.statusCode(), ign);
                  }
              });
    }

    private static String buildJoinPayload(String ign, String serverType) {
        // Hand-built JSON — Gson is available but this avoids the dependency on this class
        return String.format(
                "{\"eventId\":\"%s\",\"type\":\"JOIN\",\"ign\":\"%s\",\"server\":\"%s\",\"timestamp\":\"%s\"}",
                UUID.randomUUID(),
                ign.replace("\"", "\\\""),   // sanitise IGN just in case
                serverType,
                Instant.now()
        );
    }
}
