package com.bento.bot.application;

public enum ApplicationStatus {
    PENDING, INTERVIEW, ACCEPTED, REJECTED, BANNED;

    public boolean isTerminal() {
        return this == ACCEPTED || this == REJECTED || this == BANNED;
    }
}
