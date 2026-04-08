package com.example.backend_socialmedia.video.application;

import com.example.backend_socialmedia.video.domain.Video;
import com.example.backend_socialmedia.video.domain.VideoRepository;
import com.example.backend_socialmedia.video.domain.VideoStatus;
import com.example.backend_socialmedia.video.infrastructure.google.GoogleGenerativeAiService;
import com.example.backend_socialmedia.video.infrastructure.google.GoogleVideoGenerationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio que monitorea y actualiza el estado de videos en generación
 * Consulta periódicamente a Google AI para obtener actualizaciones
 */
@Service
public class VideoStatusPollingService {

    private static final Logger logger = LoggerFactory.getLogger(VideoStatusPollingService.class);

    @Value("${video-generation.polling-interval-seconds:30}")
    private int pollingIntervalSeconds;

    @Value("${video-generation.signed-url-hours:12}")
    private int signedUrlExpirationHours;

    private final VideoRepository videoRepository;
    private final GoogleGenerativeAiService googleAiService;

    public VideoStatusPollingService(VideoRepository videoRepository,
                                     GoogleGenerativeAiService googleAiService) {
        this.videoRepository = videoRepository;
        this.googleAiService = googleAiService;
    }

    /**
     * Ejecuta el polling de estado de videos periódicamente
     * Se ejecuta cada X segundos configurados
     */
    @Scheduled(fixedDelayString = "${video-generation.polling-interval-seconds:30}", timeUnit = java.util.concurrent.TimeUnit.SECONDS)
    public void pollVideoStatus() {
        logger.debug("Iniciando polling de estado de videos");

        try {
            // Obtener todos los videos en estado PROCESSING
            List<Video> processingVideos = videoRepository.findByStatus(VideoStatus.PROCESSING);

            if (processingVideos.isEmpty()) {
                logger.debug("No hay videos en procesamiento");
                return;
            }

            logger.info("Consultando estado de {} videos en procesamiento", processingVideos.size());

            for (Video video : processingVideos) {
                updateVideoStatus(video);
            }

        } catch (Exception e) {
            logger.error("Error en el polling de estado de videos", e);
        }
    }

    /**
     * Actualiza el estado de un video individual
     */
    private void updateVideoStatus(Video video) {
        try {
            if (video.getGoogleJobId() == null) {
                logger.warn("Video {} no tiene googleJobId", video.getId());
                return;
            }

            logger.debug("Consultando estado de jobId: {}", video.getGoogleJobId());

            GoogleVideoGenerationResponse response = googleAiService.getJobStatus(video.getGoogleJobId());

            if ("COMPLETED".equals(response.getStatus())) {
                logger.info("Video {} completado", video.getId());
                video.setStatus(VideoStatus.COMPLETED);
                
                if (response.getVideoUrl() != null && response.getVideoUrl().startsWith("gs://")) {
                    String signedUrl = googleAiService.generateSignedUrl(
                        response.getVideoUrl(), 
                        signedUrlExpirationHours
                    );
                    logger.info("Signed URL generada para video {}: {}", video.getId(), signedUrl);
                    video.setVideoUrl(signedUrl);
                } else {
                    video.setVideoUrl(response.getVideoUrl());
                }
                
                video.setUpdatedAt(LocalDateTime.now());
                videoRepository.save(video);

            } else if ("ERROR".equals(response.getStatus())) {
                logger.error("Video {} falló: {}", video.getId(), response.getErrorMessage());
                video.setStatus(VideoStatus.ERROR);
                video.setErrorMessage(response.getErrorMessage());
                video.setUpdatedAt(LocalDateTime.now());
                videoRepository.save(video);

            } else {
                logger.debug("Video {} aún en procesamiento", video.getId());
            }

        } catch (Exception e) {
            logger.error("Error al actualizar estado del video: {}", video.getId(), e);
        }
    }
}

