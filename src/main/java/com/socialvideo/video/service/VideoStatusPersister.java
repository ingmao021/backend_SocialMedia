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
        String gcsUri = response.response().videos().get(0).gcsUri();
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
