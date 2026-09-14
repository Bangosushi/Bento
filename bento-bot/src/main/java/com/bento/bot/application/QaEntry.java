package com.bento.bot.application;

/**
 * One question/answer pair serialised into the {@code data} JSONB column.
 */
public record QaEntry(String question, String answer) {}
