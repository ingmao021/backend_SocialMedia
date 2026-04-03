-- Crear tabla de publicaciones en YouTube
CREATE TABLE youtube_uploads (
    id BIGSERIAL PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    youtube_video_id VARCHAR(255),
    youtube_url VARCHAR(2048),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    visibility VARCHAR(50) NOT NULL DEFAULT 'PRIVATE',
    published_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES generated_videos(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT check_valid_status CHECK (status IN ('PENDING', 'PUBLISHING', 'PUBLISHED', 'FAILED', 'DELETED')),
    CONSTRAINT check_valid_visibility CHECK (visibility IN ('PRIVATE', 'UNLISTED', 'PUBLIC'))
);

-- Crear índices para búsquedas frecuentes
CREATE INDEX idx_youtube_uploads_user_id ON youtube_uploads(user_id);
CREATE INDEX idx_youtube_uploads_video_id ON youtube_uploads(video_id);
CREATE INDEX idx_youtube_uploads_status ON youtube_uploads(status);
CREATE INDEX idx_youtube_uploads_youtube_video_id ON youtube_uploads(youtube_video_id);
CREATE INDEX idx_youtube_uploads_created_at ON youtube_uploads(created_at DESC);

