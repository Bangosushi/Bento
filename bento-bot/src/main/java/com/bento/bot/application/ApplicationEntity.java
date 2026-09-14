package com.bento.bot.application;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "applications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "discord_id", nullable = false)
    private Long discordId;

    @Column(name = "ign", nullable = false)
    private String ign;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    /** JSON array of {@link QaEntry} objects. Stored as JSONB in Postgres. */
    @Column(name = "data", nullable = false, columnDefinition = "jsonb")
    private String data;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reason")
    private String reason;

    /** Message ID of the review embed in #cat-pplications. */
    @Column(name = "embed_message_id")
    private Long embedMessageId;

    /** ID of the private interview thread. */
    @Column(name = "thread_id")
    private Long threadId;
}
