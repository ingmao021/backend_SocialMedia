ALTER TABLE users
ADD CONSTRAINT chk_users_auth_method
CHECK (password_hash IS NOT NULL OR google_id IS NOT NULL);
