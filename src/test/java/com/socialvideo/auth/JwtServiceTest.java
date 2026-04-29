package com.socialvideo.auth;

import com.socialvideo.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // 64-char secret → 256-bit HMAC key
        String secret = "test-secret-key-that-is-at-least-32-bytes-long-for-hmac-sha256!!";
        jwtService = new JwtService(secret, 86_400_000L);
    }

    @Test
    void generate_returnsNonEmptyToken() {
        String token = jwtService.generate(42L, "user@test.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUserId_roundtrip() {
        Long userId = 99L;
        String token = jwtService.generate(userId, "a@b.com");

        Long extracted = jwtService.extractUserId(token);

        assertThat(extracted).isEqualTo(userId);
    }

    @Test
    void isValid_withMalformedToken_returnsFalse() {
        assertThat(jwtService.isValid("not.a.jwt")).isFalse();
        assertThat(jwtService.isValid("")).isFalse();
        assertThat(jwtService.isValid(null)).isFalse();
    }
}
