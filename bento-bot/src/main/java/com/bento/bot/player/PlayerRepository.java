package com.bento.bot.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {

    Optional<PlayerEntity> findByIgn(String ign);

    /**
     * Rule 1 — Never joined: active, never seen, account older than N days.
     */
    @Query("""
            SELECT p FROM PlayerEntity p
            WHERE p.active = true
              AND p.lastSeen IS NULL
              AND p.createdAt < :cutoff
            """)
    List<PlayerEntity> findNoShows(@Param("cutoff") Instant cutoff);

    /**
     * Rule 2 — Gone cold: active, last seen more than N days ago.
     */
    @Query("""
            SELECT p FROM PlayerEntity p
            WHERE p.active = true
              AND p.lastSeen IS NOT NULL
              AND p.lastSeen < :cutoff
            """)
    List<PlayerEntity> findInactive(@Param("cutoff") Instant cutoff);
}
