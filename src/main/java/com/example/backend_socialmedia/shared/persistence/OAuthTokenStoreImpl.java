package com.example.backend_socialmedia.shared.persistence;

import com.example.backend_socialmedia.shared.OAuthTokenStore;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Optional;

@Component
public class OAuthTokenStoreImpl implements OAuthTokenStore {

    private final OAuthTokenJpaRepository repo;

    public OAuthTokenStoreImpl(OAuthTokenJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public void save(String userId, String accessToken,
                     String refreshToken, long expiresInSeconds) {
        OAuthTokenEntity entity = repo.findByUserId(userId)
                .orElse(new OAuthTokenEntity());

        entity.setUserId(userId);
        entity.setAccessToken(accessToken);

        if (refreshToken != null && !refreshToken.isBlank()) {
            entity.setRefreshToken(refreshToken);
        }

        entity.setExpiresAt(Instant.now().plusSeconds(expiresInSeconds));
        entity.setProvider("google");
        repo.save(entity);
    }

    @Override
    public Optional<String> getRefreshToken(String userId) {
        return repo.findByUserId(userId)
                .map(OAuthTokenEntity::getRefreshToken);
    }

    @Override
    public Optional<String> getAccessToken(String userId) {
        return repo.findByUserId(userId)
                .map(OAuthTokenEntity::getAccessToken);
    }

    @Override
    public void updateAccessToken(String userId,
                                  String newAccessToken,
                                  long expiresInSeconds) {
        repo.findByUserId(userId).ifPresent(entity -> {
            entity.setAccessToken(newAccessToken);
            entity.setExpiresAt(Instant.now().plusSeconds(expiresInSeconds));
            repo.save(entity);
        });
    }
}