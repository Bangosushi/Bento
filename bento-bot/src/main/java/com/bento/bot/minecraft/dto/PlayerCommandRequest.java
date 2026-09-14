package com.bento.bot.minecraft.dto;

/** Sent to {@code POST /api/whitelist/add} etc. on the Fabric mod. */
public record PlayerCommandRequest(String ign) {}
