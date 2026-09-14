package com.bento.bot.player;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "players")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "discord_id", nullable = false, unique = true)
    private Long discordId;

    @Column(name = "ign", nullable = false)
    private String ign;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.active = true;
    }
}
