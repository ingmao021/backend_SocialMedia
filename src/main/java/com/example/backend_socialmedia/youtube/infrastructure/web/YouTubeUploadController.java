package com.example.backend_socialmedia.youtube.infrastructure.web;

import com.example.backend_socialmedia.shared.config.CustomUserDetails;
import com.example.backend_socialmedia.shared.exception.UnauthorizedException;
import com.example.backend_socialmedia.shared.exception.VideoNotFoundException;
import com.example.backend_socialmedia.video.domain.Video;
import com.example.backend_socialmedia.video.domain.VideoRepository;
import com.example.backend_socialmedia.video.domain.VideoStatus;
import com.example.backend_socialmedia.youtube.application.DeleteYouTubeUploadUseCase;
import com.example.backend_socialmedia.youtube.application.GetYouTubeUploadStatusUseCase;
import com.example.backend_socialmedia.youtube.application.ListYouTubeUploadsUseCase;
import com.example.backend_socialmedia.youtube.application.PublishVideoToYouTubeUseCase;
import com.example.backend_socialmedia.youtube.domain.YouTubePublishRequest;
import com.example.backend_socialmedia.youtube.domain.YouTubeUploadResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/youtube")
public class YouTubeUploadController {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeUploadController.class);

    private final PublishVideoToYouTubeUseCase publishVideoToYouTubeUseCase;
    private final GetYouTubeUploadStatusUseCase getYouTubeUploadStatusUseCase;
    private final ListYouTubeUploadsUseCase listYouTubeUploadsUseCase;
    private final DeleteYouTubeUploadUseCase deleteYouTubeUploadUseCase;
    private final VideoRepository videoRepository;

    public YouTubeUploadController(PublishVideoToYouTubeUseCase publishVideoToYouTubeUseCase,
                                   GetYouTubeUploadStatusUseCase getYouTubeUploadStatusUseCase,
                                   ListYouTubeUploadsUseCase listYouTubeUploadsUseCase,
                                   DeleteYouTubeUploadUseCase deleteYouTubeUploadUseCase,
                                   VideoRepository videoRepository) {
        this.publishVideoToYouTubeUseCase = publishVideoToYouTubeUseCase;
        this.getYouTubeUploadStatusUseCase = getYouTubeUploadStatusUseCase;
        this.listYouTubeUploadsUseCase = listYouTubeUploadsUseCase;
        this.deleteYouTubeUploadUseCase = deleteYouTubeUploadUseCase;
        this.videoRepository = videoRepository;
    }

    @PostMapping("/publish")
    public ResponseEntity<YouTubeUploadResponse> publishVideo(@Valid @RequestBody YouTubePublishRequest request) {
        Long userId = getCurrentUserId();
        logger.info("Usuario={} solicita publicar video id={} en YouTube", userId, request.getVideoId());

        Video video = videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> new VideoNotFoundException(request.getVideoId()));

        if (video.getStatus() != VideoStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "El video no está listo para publicar. Estado actual: " + video.getStatus());
        }

        if (video.getVideoUrl() == null || video.getVideoUrl().isBlank()) {
            throw new IllegalArgumentException("URL del video no disponible para publicar");
        }

        YouTubeUploadResponse response = publishVideoToYouTubeUseCase.execute(userId, request, video.getVideoUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/uploads/{uploadId}")
    public ResponseEntity<YouTubeUploadResponse> getUploadStatus(@PathVariable Long uploadId) {
        Long userId = getCurrentUserId();
        logger.info("Usuario={} consulta estado de publicación id={}", userId, uploadId);
        return ResponseEntity.ok(getYouTubeUploadStatusUseCase.execute(uploadId, userId));
    }

    @GetMapping("/uploads")
    public ResponseEntity<List<YouTubeUploadResponse>> listUserUploads() {
        Long userId = getCurrentUserId();
        logger.info("Usuario={} lista publicaciones en YouTube", userId);
        return ResponseEntity.ok(listYouTubeUploadsUseCase.execute(userId));
    }

    @DeleteMapping("/uploads/{uploadId}")
    public ResponseEntity<Void> deleteUpload(@PathVariable Long uploadId) {
        Long userId = getCurrentUserId();
        logger.info("Usuario={} elimina publicación id={}", userId, uploadId);
        deleteYouTubeUploadUseCase.execute(uploadId, userId);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        return principal.getUserId();
    }
}
