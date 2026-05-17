package com.socialvideo.youtube.job;

import com.socialvideo.youtube.repository.YouTubeOAuthStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Job programado que elimina los {@code oauth_states} expirados.
 *
 * <p>Sin esta limpieza, la tabla {@code youtube_oauth_states} crecería
 * indefinidamente con entradas que ya no pueden usarse (consumidas o
 * caducadas). Como su TTL por defecto es de 10 minutos, el volumen real
 * es bajo, pero la limpieza periódica mantiene la tabla pequeña y
 * predecible.</p>
 *
 * <p>Se ejecuta <b>al inicio de cada hora</b> ({@code 0 0 * * * *}).
 * El borrado es bulk (un único {@code DELETE FROM ... WHERE expires_at < ?}),
 * eficiente incluso si hubiera miles de filas.</p>
 *
 * <p>Coherente con el patrón {@code VideoRepairJob} ya existente
 * en el proyecto.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YouTubeOAuthStateCleanupJob {

    private final YouTubeOAuthStateRepository stateRepository;

    /**
     * Borra todos los states cuyo {@code expires_at} ya pasó.
     *
     * <p>El cron {@code "0 0 * * * *"} se ejecuta en el minuto 0 de cada
     * hora. Para sobrescribirlo en pruebas o staging se puede mover a
     * {@code application.yml} si en el futuro se necesita configurabilidad.</p>
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredStates() {
        Instant now = Instant.now();

        // Conteo solo para logging informativo
        List<?> expired = stateRepository.findByExpiresAtBefore(now);
        int count = expired.size();

        if (count == 0) {
            log.debug("OAuth state cleanup: nada que borrar");
            return;
        }

        stateRepository.deleteByExpiresAtBefore(now);
        log.info("OAuth state cleanup: {} states expirados eliminados", count);
    }
}