package com.socialvideo.youtube.service;

import com.socialvideo.user.entity.User;
import com.socialvideo.youtube.entity.YouTubeOAuthState;
import com.socialvideo.youtube.exception.InvalidOAuthStateException;
import com.socialvideo.youtube.repository.YouTubeOAuthStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Gestiona los tokens anti-CSRF del flujo OAuth de YouTube.
 *
 * <p>Cuando un usuario autenticado inicia la conexión, este servicio genera
 * un {@code state} criptográfico aleatorio asociado a su {@code User},
 * que se pasa a Google en la URL de consentimiento. En el callback
 * público (sin JWT), el {@code state} es la única forma de identificar
 * de qué usuario se trata.</p>
 *
 * <p>Cumple los requisitos del RFC 6749 sección 10.12 sobre protección
 * anti-CSRF en flujos OAuth:</p>
 * <ul>
 *   <li>El {@code state} es <b>no adivinable</b> (36 bytes de
 *       {@link SecureRandom} → 48 caracteres Base64URL).</li>
 *   <li>Es <b>one-shot</b>: una vez consumido, no puede reutilizarse
 *       (anti-replay).</li>
 *   <li>Expira tras {@code app.youtube.oauth-state-ttl-seconds}
 *       (default: 600 = 10 minutos).</li>
 * </ul>
 *
 * <p>Los detalles del motivo por el que un state es inválido (no existe,
 * consumido o expirado) NO se propagan al cliente para evitar enumeración.
 * Internamente sí se loguean para debugging.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthStateService {

    /**
     * 36 bytes aleatorios codificados en Base64URL sin padding producen
     * exactamente 48 caracteres. Holgado dentro del límite VARCHAR(64)
     * de la columna {@code state}.
     */
    private static final int STATE_RANDOM_BYTES = 36;

    private final YouTubeOAuthStateRepository stateRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.youtube.oauth-state-ttl-seconds}")
    private long ttlSeconds;

    /**
     * Genera un nuevo {@code state} criptográfico, lo asocia al usuario
     * y lo persiste con su tiempo de expiración. El string devuelto debe
     * incluirse como parámetro {@code state} en la URL de autorización de
     * Google.
     *
     * @param user usuario autenticado que inicia la conexión
     * @return string del state (Base64URL sin padding, 48 caracteres)
     */
    @Transactional
    public String generate(User user) {
        byte[] randomBytes = new byte[STATE_RANDOM_BYTES];
        secureRandom.nextBytes(randomBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        Instant now = Instant.now();
        YouTubeOAuthState entity = YouTubeOAuthState.builder()
                .state(state)
                .user(user)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .build();

        stateRepository.save(entity);
        log.debug("OAuth state generado para userId={}, expira en {}s",
                user.getId(), ttlSeconds);
        return state;
    }

    /**
     * Valida y consume un {@code state} recibido en el callback OAuth.
     *
     * <p>Devuelve el {@link User} asociado si el state es válido. Marca
     * el state como consumido en la misma transacción (one-shot, anti-replay).</p>
     *
     * @param state el state recibido en {@code GET /api/youtube/oauth/callback}
     * @return usuario al que pertenece el state
     * @throws InvalidOAuthStateException si el state no existe, ya se usó, o expiró
     */
    @Transactional
    public User consume(String state) {
        if (state == null || state.isBlank()) {
            throw new InvalidOAuthStateException("State ausente");
        }

        YouTubeOAuthState entity = stateRepository.findByState(state)
                .orElseThrow(() -> {
                    log.warn("OAuth state desconocido recibido en callback");
                    return new InvalidOAuthStateException("State inválido");
                });

        if (entity.getConsumedAt() != null) {
            log.warn("Intento de reutilizar OAuth state ya consumido (userId={})",
                    entity.getUser().getId());
            throw new InvalidOAuthStateException("State inválido");
        }

        if (Instant.now().isAfter(entity.getExpiresAt())) {
            log.warn("OAuth state expirado (userId={}, expiró {}s atrás)",
                    entity.getUser().getId(),
                    Instant.now().getEpochSecond() - entity.getExpiresAt().getEpochSecond());
            throw new InvalidOAuthStateException("State inválido");
        }

        entity.setConsumedAt(Instant.now());
        stateRepository.save(entity);

        User user = entity.getUser();
        log.debug("OAuth state consumido correctamente (userId={})", user.getId());
        return user;
    }
}