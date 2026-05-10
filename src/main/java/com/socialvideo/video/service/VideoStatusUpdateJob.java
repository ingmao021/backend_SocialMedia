package com.socialvideo.video.service;

import com.socialvideo.config.AppProperties;
import com.socialvideo.external.vertex.VertexAiClient;
import com.socialvideo.external.vertex.dto.OperationResponse;
import com.socialvideo.video.entity.Video;
import com.socialvideo.video.entity.VideoStatus;
import com.socialvideo.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoStatusUpdateJob {

    private final VideoRepository videoRepository;
    private final VertexAiClient vertexAiClient;
    private final VideoStatusPersister persister;
    private final AppProperties appProperties;

    @Scheduled(fixedRateString = "${app.vertex.polling-interval-seconds:15}000")
    public void pollProcessingVideos() {
        List<Video> processingVideos = videoRepository.findByStatus(VideoStatus.PROCESSING);

        if (processingVideos.isEmpty()) {
            return;
        }

        log.info("Polling {} videos in PROCESSING state", processingVideos.size());

        for (Video video : processingVideos) {
            try {
                pollSingleVideo(video);
            } catch (Exception e) {
                log.error("Error polling video {}: {}", video.getId(), e.getMessage(), e);
            }
        }
    }

    private void pollSingleVideo(Video video) {
        if (video.getVertexOperationName() == null) {
            log.warn("Video {} has no operation name, skipping", video.getId());
            return;
        }

        // Check for timeout
        int timeoutMinutes = appProperties.getVertex().getJobTimeoutMinutes();
        if (video.getCreatedAt().plus(timeoutMinutes, ChronoUnit.MINUTES).isBefore(Instant.now())) {
            log.warn("Video {} timed out after {} minutes", video.getId(), timeoutMinutes);
            persister.markAsFailed(video,
                    "La generación excedió el tiempo máximo de " + timeoutMinutes + " minutos");
            return;
        }

        // HTTP call to Vertex AI — outside any transaction
        log.info("Polling Vertex AI for video {}: operation={}", video.getId(), video.getVertexOperationName());
        OperationResponse response = vertexAiClient.getOperation(video.getVertexOperationName());
        log.info("Vertex AI response for video {}: done={}, hasResponse={}, hasError={}",
                video.getId(), response.done(),
                response.response() != null,
                response.error() != null);

        if (!Boolean.TRUE.equals(response.done())) {
            log.info("Video {} still processing in Vertex AI", video.getId());
            return;
        }

        // Operation completed — delegate DB write to transactional persister
        if (response.error() != null) {
            persister.markAsFailed(video, response.error().message());
            return;
        }

        if (response.response() != null && !response.response().isEmpty()) {
            log.info("Video {} done with response data, marking as COMPLETED. Response: {}",
                    video.getId(), response.response().toString());
            persister.markAsCompleted(video, response);
        } else {
            log.error("Video {} done but no response data. Full response: {}", video.getId(), response);
            persister.markAsFailed(video, "Vertex AI completó pero no devolvió datos en 'response'");
        }
    }
}
