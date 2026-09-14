package com.bento.bot.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory state for a user who is currently filling out their application via DM.
 * Sessions expire after 24 hours of inactivity to avoid memory leaks.
 */
public class ApplicationSession {

    private final long discordUserId;
    private final List<String> answers = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private final Instant createdAt = Instant.now();

    public ApplicationSession(long discordUserId) {
        this.discordUserId = discordUserId;
    }

    /** Records the current answer and advances to the next question. */
    public void addAnswer(String answer) {
        answers.add(answer);
        currentQuestionIndex++;
    }

    /** True when the user has answered every question. */
    public boolean isComplete(int totalQuestions) {
        return currentQuestionIndex >= totalQuestions;
    }

    /** The first answer is always the IGN. */
    public String getIgn() {
        return answers.isEmpty() ? null : answers.get(0);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(createdAt.plus(24, ChronoUnit.HOURS));
    }

    public long getDiscordUserId() { return discordUserId; }
    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public List<String> getAnswers() { return Collections.unmodifiableList(answers); }
}
