CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255),
    email      VARCHAR(255) UNIQUE NOT NULL,
    picture    TEXT,
    google_id  VARCHAR(255) UNIQUE
);