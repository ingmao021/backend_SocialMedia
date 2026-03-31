package com.example.backend_socialmedia.shared;

import java.util.Optional;

public interface OAuthTokenStore {
    void save(String userId, String accessToken,
              String refreshToken, long expiresInSeconds);
    Optional<String> getRefreshToken(String userId);
    Optional<String> getAccessToken(String userId);
    void updateAccessToken(String userId,
                           String newAccessToken, long expiresInSeconds);
}