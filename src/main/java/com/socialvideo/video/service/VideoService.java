package com.socialvideo.video.service;

import com.socialvideo.config.AppProperties;
import com.socialvideo.exception.QuotaExceededException;
import com.socialvideo.exception.ResourceNotFoundException;
import com.socialvideo.external.gcs.GcsService;
import com.socialvideo.external.vertex.VertexAiClient;
import com.socialvideo.user.entity.User;
import com.socialvideo.user.repository.UserRepository;
import com.socialvideo.video.dto.GenerateVideoRequest;
import com.socialvideo.video.dto.VideoResponse;
import com.socialvideo.video.dto.VideoStatusResponse;
import com.socialvideo.video.entity.Video;
import com.socialvideo.video.entity.VideoStatus;
import com.socialvideo.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final VertexAiClient vertexAiClient;
    private final GcsService gcsService;
    private final AppProperties appProperties;

    @Transactional
    public VideoResponse generate(Long userId, GenerateVideoRequest request) {
        // Check quota
        int completedCount = videoRepository.countByUserIdAndStatus(userId, VideoStatus.COMPLETED);
        int maxCompleted = appProperties.getVideo().getMaxCompleted();
        if (completedCount >= maxCompleted) {
            throw new QuotaExceededException(
                    String.format("Has alcanzado el límite de %d videos completados", maxCompleted));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Create video record in PROCESSING state
        Video video = Video.builder()
                .user(user)
                .prompt(request.prompt())
                .durationSeconds(request.durationSeconds())
                .status(VideoStatus.PROCESSING)
                .build();
        video = videoRepository.save(video);

        // Call Vertex AI
        String operationName = vertexAiClient.predictLongRunning(
                request.prompt(),
                request.durationSeconds(),
                userId,
                video.getId()
        );

        video.setVertexOperationName(operationName);
        video = videoRepository.save(video);

        return toVideoResponse(video);
    }

    @Transactional
    public VideoStatusResponse getStatus(Long userId, UUID videoId) {
        Video video = videoRepository.findByIdAndUserId(videoId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Video no encontrado"));

        // Regenerate signed URL if expired or close to expiring
        refreshSignedUrlIfNeeded(video);

        return new VideoStatusResponse(video.getStatus(), video.getSignedUrl());
    }

    @Transactional
    public VideoResponse getVideo(Long userId, UUID videoId) {
        Video video = videoRepository.findByIdAndUserId(videoId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Video no encontrado"));

        refreshSignedUrlIfNeeded(video);

        return toVideoResponse(video);
    }

    @Transactional
    public void deleteVideo(Long userId, UUID videoId) {
        Video video = videoRepository.findByIdAndUserId(videoId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Video no encontrado"));
        videoRepository.delete(video);
    }

    @Transactional
    public Page<VideoResponse> listByUser(Long userId, int page, int size) {
        size = Math.min(size, 50); // Cap at 50
        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videos = videoRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        // Refresh signed URLs for completed videos
        videos.getContent().forEach(this::refreshSignedUrlIfNeeded);

        return videos.map(this::toVideoResponse);
    }

    /**
     * Regenerates signed URL if it's expired or will expire within 1 hour.
     */
    private void refreshSignedUrlIfNeeded(Video video) {
        if (video.getStatus() != VideoStatus.COMPLETED || video.getGcsUri() == null) {
            return;
        }

        if (video.getSignedUrl() == null || video.getSignedUrlExpiresAt() == null
                || video.getSignedUrlExpiresAt().isBefore(Instant.now().plus(1, ChronoUnit.HOURS))) {
            try {
                var signedUrl = gcsService.generateSignedUrl(video.getGcsUri());
                video.setSignedUrl(signedUrl.toString());
                video.setSignedUrlExpiresAt(Instant.now()
                        .plus(appProperties.getSignedUrl().getTtlDays(), ChronoUnit.DAYS));
                videoRepository.save(video);
            } catch (Exception e) {
                log.error("Error regenerating signed URL for video {}", video.getId(), e);
            }
        }
    }

    private VideoResponse toVideoResponse(Video video) {
        return new VideoResponse(
                video.getId(),
                video.getPrompt(),
                video.getDurationSeconds(),
                video.getStatus(),
                video.getSignedUrl(),
                video.getSignedUrlExpiresAt(),
                video.getErrorMessage(),
                video.getCreatedAt(),
                video.getUpdatedAt()
        );
    }
}
