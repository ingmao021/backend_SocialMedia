-- Crear tabla de videos generados
CREATE TABLE generated_videos (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    video_url VARCHAR(2048),
    prompt TEXT NOT NULL,
    google_job_id VARCHAR(255),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT check_valid_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'ERROR'))
);

-- Crear índices para búsquedas frecuentes
CREATE INDEX idx_generated_videos_user_id ON generated_videos(user_id);
CREATE INDEX idx_generated_videos_status ON generated_videos(status);
CREATE INDEX idx_generated_videos_created_at ON generated_videos(created_at DESC);
CREATE INDEX idx_generated_videos_google_job_id ON generated_videos(google_job_id);

