package com.example.backend_socialmedia.shared;

import java.util.Optional;

public interface OAuthTokenStore {
    void save(Long userId, String accessToken,
              String refreshToken, long expiresInSeconds);
    Optional<String> getRefreshToken(Long userId);
    Optional<String> getAccessToken(Long userId);
    void updateAccessToken(Long userId,
                           String newAccessToken, long expiresInSeconds);
}