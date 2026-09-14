CREATE TABLE players (
    id          BIGSERIAL    PRIMARY KEY,
    discord_id  BIGINT       NOT NULL UNIQUE,
    ign         VARCHAR(16)  NOT NULL,
    last_seen   TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_player_active_last_seen  ON players(active, last_seen);
CREATE INDEX idx_player_active_created_at ON players(active, created_at);
