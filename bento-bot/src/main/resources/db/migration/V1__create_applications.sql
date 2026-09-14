CREATE TABLE applications (
    id               BIGSERIAL    PRIMARY KEY,
    discord_id       BIGINT       NOT NULL,
    ign              VARCHAR(16)  NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    data             JSONB        NOT NULL,
    submitted_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    reviewed_at      TIMESTAMPTZ,
    reason           TEXT,
    embed_message_id BIGINT,
    thread_id        BIGINT,
    CONSTRAINT chk_status CHECK (status IN ('PENDING','INTERVIEW','ACCEPTED','REJECTED','BANNED'))
);

CREATE INDEX idx_app_discord_id ON applications(discord_id);
CREATE INDEX idx_app_status     ON applications(status);
