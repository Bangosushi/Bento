package com.bento.bot.player;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {

    private final PlayerRepository repository;

    /** Called after a player is fully accepted — creates their player record. */
    @Transactional
    public PlayerEntity create(long discordId, String ign) {
        PlayerEntity player = PlayerEntity.builder()
                .discordId(discordId)
                .ign(ign)
                .build();
        return repository.save(player);
    }

    /** Called by the Minecraft event endpoint on every JOIN event. */
    @Transactional
    public void updateLastSeen(String ign, Instant timestamp) {
        repository.findByIgn(ign).ifPresentOrElse(
                player -> {
                    player.setLastSeen(timestamp);
                    repository.save(player);
                },
                () -> log.warn("JOIN event received for unknown IGN: {}", ign)
        );
    }

    /** Marks a player as inactive (called by the inactivity scheduler). */
    @Transactional
    public void deactivate(long playerId) {
        repository.findById(playerId).ifPresent(player -> {
            player.setActive(false);
            repository.save(player);
        });
    }
}
