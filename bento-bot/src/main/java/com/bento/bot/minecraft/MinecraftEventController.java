package com.bento.bot.minecraft;

import com.bento.bot.minecraft.dto.MinecraftEventDto;
import com.bento.bot.player.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives game events from the Bento Fabric mod.
 * Authentication is handled upstream by {@link com.bento.bot.config.ApiKeyFilter}.
 */
@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
@Slf4j
public class MinecraftEventController {

    private final PlayerService playerService;

    @PostMapping
    public ResponseEntity<Void> handleEvent(@RequestBody MinecraftEventDto event) {
        log.debug("Event: type={} ign={} server={} ts={}", event.type(), event.ign(), event.server(), event.timestamp());

        if ("JOIN".equals(event.type())) {
            playerService.updateLastSeen(event.ign(), event.timestamp());
        }

        return ResponseEntity.ok().build();
    }
}
