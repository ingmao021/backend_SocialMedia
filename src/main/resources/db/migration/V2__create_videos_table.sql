CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE videos (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    prompt                  TEXT NOT NULL,
    duration_seconds        SMALLINT NOT NULL CHECK (duration_seconds IN (4, 6, 8)),
    status                  VARCHAR(20) NOT NULL CHECK (status IN ('PROCESSING','COMPLETED','FAILED')),
    vertex_operation_name   TEXT,
    gcs_uri                 TEXT,
    signed_url              TEXT,
    signed_url_expires_at   TIMESTAMPTZ,
    error_message           TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_videos_user_id      ON videos(user_id);
CREATE INDEX idx_videos_user_status  ON videos(user_id, status);
CREATE INDEX idx_videos_status       ON videos(status);
CREATE INDEX idx_videos_created_at   ON videos(created_at DESC);
