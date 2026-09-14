package com.bento.bot.minecraft.dto;

import java.time.Instant;

/**
 * Payload sent by the Fabric mod to {@code POST /api/event}.
 *
 * <pre>{@code
 * {
 *   "eventId":   "550e8400-e29b-41d4-a716-446655440000",
 *   "type":      "JOIN",
 *   "ign":       "PlayerName",
 *   "server":    "SURVIVAL",
 *   "timestamp": "2026-09-11T14:32:00Z"
 * }
 * }</pre>
 */
public record MinecraftEventDto(
        String  eventId,
        String  type,
        String  ign,
        String  server,
        Instant timestamp
) {}
