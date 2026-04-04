CREATE TABLE oauth_tokens (
    id            VARCHAR(36)   PRIMARY KEY,
    user_id       BIGINT        NOT NULL UNIQUE,
    access_token  VARCHAR(2048) NOT NULL,
    refresh_token VARCHAR(512),
    expires_at    TIMESTAMP     NOT NULL,
    provider      VARCHAR(50)   NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);