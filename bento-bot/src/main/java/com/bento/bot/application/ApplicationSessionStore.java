package com.bento.bot.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ApplicationSessionStore {

    private final ConcurrentHashMap<Long, ApplicationSession> sessions = new ConcurrentHashMap<>();

    public ApplicationSession create(long userId) {
        ApplicationSession session = new ApplicationSession(userId);
        sessions.put(userId, session);
        return session;
    }

    public Optional<ApplicationSession> get(long userId) {
        ApplicationSession session = sessions.get(userId);
        if (session == null) return Optional.empty();
        if (session.isExpired()) {
            sessions.remove(userId);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void remove(long userId) {
        sessions.remove(userId);
    }

    public boolean has(long userId) {
        return get(userId).isPresent();
    }

    @Scheduled(fixedRateString = "PT1H")
    public void evictExpired() {
        int before = sessions.size();
        sessions.entrySet().removeIf(e -> e.getValue().isExpired());
        int removed = before - sessions.size();
        if (removed > 0) log.info("Evicted {} expired application sessions", removed);
    }
}
