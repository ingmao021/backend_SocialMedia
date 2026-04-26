package com.example.backend_socialmedia.video.application;

import com.example.backend_socialmedia.video.domain.Video;
import com.example.backend_socialmedia.video.domain.VideoGenerationPort;
import com.example.backend_socialmedia.video.domain.VideoRepository;
import com.example.backend_socialmedia.video.domain.VideoStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class VideoStatusPollingService {

    private static final Logger logger = LoggerFactory.getLogger(VideoStatusPollingService.class);

    @Value("${video-generation.max-processing-hours:2}")
    private int maxProcessingHours;

    private final VideoRepository videoRepository;
    private final VideoGenerationPort videoGenerationPort;

    public VideoStatusPollingService(VideoRepository videoRepository,
                                     VideoGenerationPort videoGenerationPort) {
        this.videoRepository = videoRepository;
        this.videoGenerationPort = videoGenerationPort;
    }

    @Scheduled(fixedDelayString = "${video-generation.polling-interval-seconds:30}",
               timeUnit = TimeUnit.SECONDS)
    public void pollVideoStatus() {
        List<Video> processingVideos = videoRepository.findByStatus(VideoStatus.PROCESSING);

        if (processingVideos.isEmpty()) {
            logger.debug("No hay videos en procesamiento");
            return;
        }

        logger.info("Polling: {} video(s) en procesamiento", processingVideos.size());

        for (Video video : processingVideos) {
            if (hasExceededTimeout(video)) {
                markAsTimedOut(video);
                continue;
            }
            updateVideoStatus(video);
        }
    }

    private boolean hasExceededTimeout(Video video) {
        return video.getCreatedAt() != null &&
               video.getCreatedAt().isBefore(LocalDateTime.now().minusHours(maxProcessingHours));
    }

    private void markAsTimedOut(Video video) {
        logger.warn("Video id={} superó el timeout de {}h — marcando como ERROR", video.getId(), maxProcessingHours);
        video.setStatus(VideoStatus.ERROR);
        video.setErrorMessage("Timeout: el video no se generó en el tiempo límite de " + maxProcessingHours + "h");
        video.setUpdatedAt(LocalDateTime.now());
        videoRepository.save(video);
    }

    private void updateVideoStatus(Video video) {
        try {
            if (video.getGoogleJobId() == null) {
                logger.warn("Video id={} no tiene googleJobId — marcando ERROR", video.getId());
                video.setStatus(VideoStatus.ERROR);
                video.setErrorMessage("Job ID de Google no disponible");
                video.setUpdatedAt(LocalDateTime.now());
                videoRepository.save(video);
                return;
            }

            VideoGenerationPort.JobStatusResult result = videoGenerationPort.getJobStatus(video.getGoogleJobId());

            switch (result.status()) {
                case "COMPLETED" -> {
                    video.setStatus(VideoStatus.COMPLETED);
                    video.setVideoUrl(result.videoUrl());
                    video.setUpdatedAt(LocalDateTime.now());
                    videoRepository.save(video);
                    logger.info("Video id={} COMPLETADO. URL={}", video.getId(), result.videoUrl());
                }
                case "ERROR" -> {
                    video.setStatus(VideoStatus.ERROR);
                    video.setErrorMessage(result.errorMessage());
                    video.setUpdatedAt(LocalDateTime.now());
                    videoRepository.save(video);
                    logger.error("Video id={} FALLIDO: {}", video.getId(), result.errorMessage());
                }
                default -> logger.debug("Video id={} sigue en PROCESSING", video.getId());
            }

        } catch (Exception e) {
            logger.error("Error al actualizar estado del video id={}: {}", video.getId(), e.getMessage(), e);
        }
    }
}
