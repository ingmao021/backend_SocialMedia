package com.socialvideo.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.socialvideo.config.AppProperties;
import com.socialvideo.external.gcs.GcsService;
import com.socialvideo.external.vertex.dto.OperationResponse;
import com.socialvideo.video.entity.Video;
import com.socialvideo.video.entity.VideoStatus;
import com.socialvideo.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoStatusPersister {

    private final VideoRepository videoRepository;
    private final GcsService gcsService;
    private final AppProperties appProperties;

    @Transactional
    public void markAsCompleted(Video video, OperationResponse response) {
        // Re-read entity inside this transaction to avoid stale/detached state
        video = videoRepository.findById(video.getId()).orElse(video);

        String gcsUri = null;

        // 1. Try to extract URI from the Vertex AI response JSON
        JsonNode responseNode = response.response();
        if (responseNode != null && !responseNode.isNull() && !responseNode.isEmpty()) {
            JsonNode uriNode = responseNode.findValue("uri");
            if (uriNode == null) {
                uriNode = responseNode.findValue("gcsUri");
            }
            if (uriNode != null && !uriNode.asText().isBlank()) {
                gcsUri = uriNode.asText();
                log.info("Video {} URI extracted from Vertex AI response: {}", video.getId(), gcsUri);
            }
        }

        // 2. If response didn't contain URI, search GCS for the actual .mp4 file
        //    using the known prefix pattern: videos/{userId}/{videoId}/
        if (gcsUri == null || gcsUri.isBlank()) {
            log.warn("Video {} Vertex AI response had no URI field. Searching GCS for the file...", video.getId());
            Long userId = video.getUser().getId();
            String prefix = String.format("videos/%d/%s/", userId, video.getId());
            gcsUri = gcsService.findVideoFile(prefix);

            if (gcsUri == null) {
                log.error("Video {} operation completed but no .mp4 found in GCS at prefix: {}", video.getId(), prefix);
                markAsFailed(video, "Video generado pero archivo no encontrado en GCS");
                return;
            }
            log.info("Video {} found in GCS via prefix search: {}", video.getId(), gcsUri);
        }

        // 3. Normalize URI — Veo sometimes returns directory instead of file
        if (!gcsUri.endsWith(".mp4")) {
            if (!gcsUri.endsWith("/")) {
                gcsUri += "/";
            }
            gcsUri += "sample_0.mp4";
            log.info("Video {} URI normalized to include filename: {}", video.getId(), gcsUri);
        }

        video.setGcsUri(gcsUri);

        // 4. Generate signed URL for frontend playback
        try {
            var signedUrl = gcsService.generateSignedUrl(gcsUri);
            video.setSignedUrl(signedUrl.toString());
            video.setSignedUrlExpiresAt(Instant.now()
                    .plus(appProperties.getSignedUrl().getTtlDays(), ChronoUnit.DAYS));
        } catch (Exception e) {
            log.error("Error generating signed URL for video {}: {}", video.getId(), e.getMessage(), e);
            // ✅ NO marcar COMPLETED si no tenemos signedUrl válido
            return;
        }

        video.setStatus(VideoStatus.COMPLETED);
        video.setErrorMessage(null); // Clear any previous error
        videoRepository.save(video);
        log.info("Video {} COMPLETED: gcsUri={}, signedUrl={}", video.getId(), gcsUri,
                video.getSignedUrl() != null ? "present" : "MISSING");
    }

    @Transactional
    public void markAsFailed(Video video, String errorMessage) {
        // Re-read entity inside this transaction to avoid stale/detached state
        video = videoRepository.findById(video.getId()).orElse(video);

        video.setStatus(VideoStatus.FAILED);
        video.setErrorMessage(errorMessage);
        videoRepository.save(video);
        log.info("Video {} FAILED: {}", video.getId(), errorMessage);
    }
}
