package com.socialvideo.youtube.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Token anti-CSRF del flujo OAuth de YouTube.
 *
 * <p>Cuando un usuario autenticado inicia la conexión con YouTube
 * ({@code POST /api/youtube/oauth/connect}), generamos un {@code state}
 * criptográfico aleatorio, lo asociamos a su {@code userId} y se lo
 * pasamos a Google. En el callback público
 * ({@code GET /api/youtube/oauth/callback}), Google nos devuelve el mismo
 * {@code state} y lo usamos para recuperar el {@code userId}, ya que el
 * callback no recibe JWT (es una redirección desde {@code accounts.google.com}).</p>
 *
 * <p>El {@code state} se consume una sola vez ({@code consumedAt}) y expira
 * tras {@code app.youtube.oauth-state-ttl-seconds}, según RFC 6749.</p>
 */
@Entity
@Table(name = "youtube_oauth_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YouTubeOAuthState {

    /** El propio state es la PK — string criptográfico único de 64 caracteres. */
    @Id
    @Column(name = "state", nullable = false, length = 64)
    private String state;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** Null si aún no se consumió. Marcado en el callback (one-shot anti-replay). */
    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    /** True si todavía es válido: no expirado y no consumido. */
    public boolean isUsable() {
        OffsetDateTime now = OffsetDateTime.now();
        return consumedAt == null && now.isBefore(expiresAt);
    }
}