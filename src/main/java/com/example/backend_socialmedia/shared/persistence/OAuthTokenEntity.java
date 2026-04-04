package com.example.backend_socialmedia.shared.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "oauth_tokens")
public class OAuthTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 2048)
    private String accessToken;

    @Column(length = 512)
    private String refreshToken;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private String provider;

    public String getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}