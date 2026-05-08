package com.socialvideo.video.service;

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
        com.fasterxml.jackson.databind.JsonNode responseNode = response.response();
        String gcsUri = null;
        
        com.fasterxml.jackson.databind.JsonNode uriNode = responseNode.findValue("uri");
        if (uriNode == null) {
            uriNode = responseNode.findValue("gcsUri");
        }
        
        if (uriNode != null) {
            gcsUri = uriNode.asText();
        } else {
            log.error("No se encontró 'uri' o 'gcsUri' en la respuesta: {}", responseNode);
            markAsFailed(video, "Vertex AI no devolvió la URL del video generado");
            return;
        }
        
        // Vertex AI Veo 3.1 a veces devuelve el directorio en lugar del archivo exacto.
        // Si no termina en .mp4, le agregamos el nombre del archivo generado (sample_0.mp4)
        if (gcsUri != null && !gcsUri.endsWith(".mp4")) {
            if (!gcsUri.endsWith("/")) {
                gcsUri += "/";
            }
            gcsUri += "sample_0.mp4";
        }
        
        video.setGcsUri(gcsUri);

        try {
            var signedUrl = gcsService.generateSignedUrl(gcsUri);
            video.setSignedUrl(signedUrl.toString());
            video.setSignedUrlExpiresAt(Instant.now()
                    .plus(appProperties.getSignedUrl().getTtlDays(), ChronoUnit.DAYS));
        } catch (Exception e) {
            log.error("Error generating signed URL for video {}", video.getId(), e);
        }

        video.setStatus(VideoStatus.COMPLETED);
        videoRepository.save(video);
        log.info("Video {} COMPLETED: gcsUri={}", video.getId(), gcsUri);
    }

    @Transactional
    public void markAsFailed(Video video, String errorMessage) {
        video.setStatus(VideoStatus.FAILED);
        video.setErrorMessage(errorMessage);
        videoRepository.save(video);
        log.info("Video {} FAILED: {}", video.getId(), errorMessage);
    }
}
