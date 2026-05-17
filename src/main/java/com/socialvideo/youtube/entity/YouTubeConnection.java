package com.socialvideo.youtube.entity;

import com.socialvideo.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Conexión OAuth de un usuario con su cuenta de YouTube.
 *
 * <p>Modelo 1:1 con la tabla {@code youtube_connections}. Una sola conexión
 * por usuario (constraint UNIQUE en {@code user_id}). El refresh_token se
 * guarda cifrado con AES-256-GCM; el access_token NO se persiste, se
 * obtiene en memoria desde el refresh cuando se necesita.</p>
 */
@Entity
@Table(name = "youtube_connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YouTubeConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** FK a {@code users}. UNIQUE: una conexión por usuario. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** ID único del usuario en Google (claim {@code sub} del ID token). */
    @Column(name = "google_sub", nullable = false, length = 255)
    private String googleSub;

    /** ID del canal de YouTube por defecto de esta cuenta. */
    @Column(name = "youtube_channel_id", nullable = false, length = 255)
    private String youtubeChannelId;

    /** Nombre del canal — se muestra al usuario en el frontend. */
    @Column(name = "youtube_channel_title", nullable = false, length = 255)
    private String youtubeChannelTitle;

    /** Refresh token cifrado (Base64 del ciphertext AES-GCM). */
    @Column(name = "refresh_token_cipher", nullable = false, columnDefinition = "TEXT")
    private String refreshTokenCipher;

    /** IV (vector de inicialización) del cifrado AES-GCM, en Base64. */
    @Column(name = "refresh_token_iv", nullable = false, length = 64)
    private String refreshTokenIv;

    /** Scopes concedidos realmente por el usuario, separados por espacio. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String scopes;

    @CreationTimestamp
    @Column(name = "connected_at", nullable = false, updatable = false)
    private Instant connectedAt;

    /** Última vez que se renovó el access_token con este refresh_token. */
    @Column(name = "last_refreshed_at")
    private Instant lastRefreshedAt;

    /** Marcado en soft-delete cuando el usuario desconecta o YouTube revoca. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** True si la conexión sigue activa (no fue revocada). */
    public boolean isActive() {
        return revokedAt == null;
    }
}