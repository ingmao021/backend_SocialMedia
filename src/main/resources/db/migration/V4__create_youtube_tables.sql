
CREATE TABLE youtube_connections (
                                     id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     user_id                 BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                                     google_sub              VARCHAR(255) NOT NULL,
                                     youtube_channel_id      VARCHAR(255) NOT NULL,
                                     youtube_channel_title   VARCHAR(255) NOT NULL,
                                     refresh_token_cipher    TEXT NOT NULL,
                                     refresh_token_iv        VARCHAR(64) NOT NULL,
                                     scopes                  TEXT NOT NULL,
                                     connected_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     last_refreshed_at       TIMESTAMPTZ,
                                     revoked_at              TIMESTAMPTZ
);

CREATE INDEX idx_youtube_connections_user_id    ON youtube_connections(user_id);
CREATE INDEX idx_youtube_connections_google_sub ON youtube_connections(google_sub);


CREATE TABLE youtube_export_jobs (
                                     id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                     video_id            UUID NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
                                     status              VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','UPLOADING','COMPLETED','FAILED')),
                                     title               VARCHAR(100) NOT NULL,
                                     description         TEXT,
                                     tags                TEXT,
                                     privacy_status      VARCHAR(20) NOT NULL CHECK (privacy_status IN ('PRIVATE','UNLISTED','PUBLIC')),
                                     youtube_video_id    VARCHAR(32),
                                     youtube_video_url   TEXT,
                                     bytes_uploaded      BIGINT NOT NULL DEFAULT 0,
                                     bytes_total         BIGINT,
                                     error_code          VARCHAR(64),
                                     error_message       TEXT,
                                     attempt_count       INTEGER NOT NULL DEFAULT 0,
                                     created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     started_at          TIMESTAMPTZ,
                                     completed_at        TIMESTAMPTZ,
                                     updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_youtube_export_jobs_user_id      ON youtube_export_jobs(user_id);
CREATE INDEX idx_youtube_export_jobs_video_id     ON youtube_export_jobs(video_id);
CREATE INDEX idx_youtube_export_jobs_status       ON youtube_export_jobs(status);
CREATE INDEX idx_youtube_export_jobs_user_created ON youtube_export_jobs(user_id, created_at DESC);


CREATE TABLE youtube_oauth_states (
                                      state           VARCHAR(64) PRIMARY KEY,
                                      user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                      created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      expires_at      TIMESTAMPTZ NOT NULL,
                                      consumed_at     TIMESTAMPTZ
);

CREATE INDEX idx_youtube_oauth_states_user_id    ON youtube_oauth_states(user_id);
CREATE INDEX idx_youtube_oauth_states_expires_at ON youtube_oauth_states(expires_at);