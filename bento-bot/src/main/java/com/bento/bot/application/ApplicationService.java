package com.bento.bot.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository repository;
    private final ObjectMapper objectMapper;

    /** True if the user already has a PENDING or INTERVIEW application. */
    @Transactional(readOnly = true)
    public boolean hasActiveApplication(long discordId) {
        return repository.existsByDiscordIdAndStatusIn(
                discordId,
                Set.of(ApplicationStatus.PENDING, ApplicationStatus.INTERVIEW)
        );
    }

    /** Persists a completed application. Returns the saved entity. */
    @Transactional
    public ApplicationEntity save(long discordId, String ign, List<QaEntry> qaEntries) {
        ApplicationEntity app = ApplicationEntity.builder()
                .discordId(discordId)
                .ign(ign)
                .status(ApplicationStatus.PENDING)
                .data(serialize(qaEntries))
                .submittedAt(Instant.now())
                .build();
        return repository.save(app);
    }

    /**
     * Moves a PENDING application to INTERVIEW.
     * Called when a moderator clicks "Accept" on the review embed.
     */
    @Transactional
    public ApplicationEntity startInterview(long applicationId) {
        ApplicationEntity app = getOrThrow(applicationId);
        app.setStatus(ApplicationStatus.INTERVIEW);
        app.setReviewedAt(Instant.now());
        return repository.save(app);
    }

    /** Updates status for REJECT / BAN actions (from dropdown or slash command). */
    @Transactional
    public ApplicationEntity updateStatus(long applicationId, ApplicationStatus status, String reason) {
        ApplicationEntity app = getOrThrow(applicationId);
        app.setStatus(status);
        app.setReason(reason);
        app.setReviewedAt(Instant.now());
        return repository.save(app);
    }

    /** Full acceptance — called from /accept slash command. */
    @Transactional
    public ApplicationEntity accept(long applicationId) {
        return updateStatus(applicationId, ApplicationStatus.ACCEPTED, null);
    }

    @Transactional
    public void updateEmbedMessageId(long applicationId, long messageId) {
        repository.findById(applicationId).ifPresent(app -> {
            app.setEmbedMessageId(messageId);
            repository.save(app);
        });
    }

    @Transactional
    public void updateThreadId(long applicationId, long threadId) {
        repository.findById(applicationId).ifPresent(app -> {
            app.setThreadId(threadId);
            repository.save(app);
        });
    }

    @Transactional(readOnly = true)
    public Optional<ApplicationEntity> findById(long id) {
        return repository.findById(id);
    }

    // ── JSON helpers ──────────────────────────────────────────

    public List<QaEntry> parseQaEntries(String data) {
        try {
            return objectMapper.readValue(data, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse QA data: {}", e.getMessage());
            return List.of();
        }
    }

    private String serialize(List<QaEntry> entries) {
        try {
            return objectMapper.writeValueAsString(entries);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize QA entries", e);
        }
    }

    private ApplicationEntity getOrThrow(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
    }
}
