package com.example.backend_socialmedia.shared.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OAuthTokenJpaRepository
        extends JpaRepository<OAuthTokenEntity, String> {
    Optional<OAuthTokenEntity> findByUserId(String userId);
}