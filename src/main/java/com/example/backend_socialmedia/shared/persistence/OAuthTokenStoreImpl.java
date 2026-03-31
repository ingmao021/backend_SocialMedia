package com.example.backend_socialmedia.shared.persistence;

import com.example.backend_socialmedia.shared.OAuthTokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

@Component
public class OAuthTokenStoreImpl implements OAuthTokenStore {

    private static final Logger log =
            LoggerFactory.getLogger(OAuthTokenStoreImpl.class);

    private final OAuthTokenJpaRepository repo;

    public OAuthTokenStoreImpl(OAuthTokenJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
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

        log.debug("Token guardado para userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getRefreshToken(String userId) {
        return repo.findByUserId(userId)
                .map(OAuthTokenEntity::getRefreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getAccessToken(String userId) {
        return repo.findByUserId(userId)
                .map(OAuthTokenEntity::getAccessToken);
    }

    @Override
    @Transactional
    public void updateAccessToken(String userId,
                                  String newAccessToken,
                                  long expiresInSeconds) {
        repo.findByUserId(userId).ifPresentOrElse(
                entity -> {
                    entity.setAccessToken(newAccessToken);
                    entity.setExpiresAt(
                            Instant.now().plusSeconds(expiresInSeconds));
                    repo.save(entity);
                    log.debug("AccessToken actualizado para userId={}", userId);
                },
                () -> log.warn("No se encontró token para userId={} " +
                        "al intentar actualizar", userId)
        );
    }
}