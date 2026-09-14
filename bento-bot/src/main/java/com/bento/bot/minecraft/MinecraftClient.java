package com.bento.bot.minecraft;

import com.bento.bot.config.BentoProperties;
import com.bento.bot.minecraft.dto.PlayerCommandRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class MinecraftClient {

    public enum Server { SURVIVAL, CREATIVE }

    private final RestClient survivalClient;
    private final RestClient creativeClient;

    public MinecraftClient(BentoProperties props) {
        this.survivalClient = build(props.minecraft().survival().url(), props.security().apiKey());
        this.creativeClient = build(props.minecraft().creative().url(), props.security().apiKey());
    }

    public void whitelistAdd(String ign, Server server) {
        post("/api/whitelist/add", ign, server);
    }

    public void whitelistRemove(String ign, Server server) {
        post("/api/whitelist/remove", ign, server);
    }

    public void op(String ign, Server server) {
        post("/api/op/add", ign, server);
    }

    public void deop(String ign, Server server) {
        post("/api/op/remove", ign, server);
    }

    // ── Private ───────────────────────────────────────────────

    private void post(String path, String ign, Server server) {
        try {
            client(server).post()
                    .uri(path)
                    .body(new PlayerCommandRequest(ign))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("[{}] {} {}", server, path, ign);
        } catch (Exception e) {
            // Log and continue — Minecraft server might be down; bot keeps running
            log.error("Minecraft {} call to {} failed for IGN '{}': {}", server, path, ign, e.getMessage());
        }
    }

    private RestClient client(Server server) {
        return server == Server.SURVIVAL ? survivalClient : creativeClient;
    }

    private static RestClient build(String baseUrl, String apiKey) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Api-Key",    apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
