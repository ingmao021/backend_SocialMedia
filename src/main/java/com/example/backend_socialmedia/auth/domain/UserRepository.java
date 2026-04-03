package com.example.backend_socialmedia.auth.domain;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    User save(User user);
}
