package com.socialvideo.youtube.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
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
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /** FK a {@code users.id}. UNIQUE: una conexión por usuario. */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** ID único del usuario en Google (claim {@code sub} del ID token). */
    @Column(name = "google_sub", nullable = false, length = 255)
    private String googleSub;

    /** ID del canal de YouTube por defecto de esta cuenta. */
    @Column(name = "youtube_channel_id", nullable = false, length = 255)
    private String youtubeChannelId;

    /** Nombre del canal — se muestra al usuario en el dropdown del frontend. */
    @Column(name = "youtube_channel_title", nullable = false, length = 255)
    private String youtubeChannelTitle;

    /** Refresh token cifrado (Base64 del ciphertext AES-GCM). */
    @Column(name = "refresh_token_cipher", nullable = false, columnDefinition = "text")
    private String refreshTokenCipher;

    /** IV (vector de inicialización) del cifrado AES-GCM, en Base64. */
    @Column(name = "refresh_token_iv", nullable = false, length = 64)
    private String refreshTokenIv;

    /** Scopes concedidos realmente por el usuario, separados por espacio. */
    @Column(name = "scopes", nullable = false, columnDefinition = "text")
    private String scopes;

    @Column(name = "connected_at", nullable = false)
    private OffsetDateTime connectedAt;

    /** Última vez que se renovó el access_token con este refresh_token. */
    @Column(name = "last_refreshed_at")
    private OffsetDateTime lastRefreshedAt;

    /** Marcado en soft-delete cuando el usuario desconecta o YouTube revoca. */
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    /**
     * Conveniencia: si {@code revokedAt} es null, la conexión está activa.
     */
    public boolean isActive() {
        return revokedAt == null;
    }
}