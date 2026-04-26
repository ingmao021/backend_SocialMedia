package com.example.backend_socialmedia.youtube.application;

import com.example.backend_socialmedia.shared.OAuthTokenStore;
import com.example.backend_socialmedia.shared.exception.InvalidGoogleTokenException;
import com.example.backend_socialmedia.shared.exception.OAuthTokenNotFoundException;
import com.example.backend_socialmedia.youtube.domain.*;
import com.example.backend_socialmedia.youtube.infrastructure.google.YouTubeDataApiService;
import com.example.backend_socialmedia.youtube.infrastructure.google.YouTubePublishResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class PublishVideoToYouTubeUseCase {

    private static final Logger logger = LoggerFactory.getLogger(PublishVideoToYouTubeUseCase.class);

    private final YouTubeUploadRepository youtubeUploadRepository;
    private final YouTubeDataApiService youtubeDataApiService;
    private final OAuthTokenStore oAuthTokenStore;

    public PublishVideoToYouTubeUseCase(YouTubeUploadRepository youtubeUploadRepository,
                                        YouTubeDataApiService youtubeDataApiService,
                                        OAuthTokenStore oAuthTokenStore) {
        this.youtubeUploadRepository = youtubeUploadRepository;
        this.youtubeDataApiService = youtubeDataApiService;
        this.oAuthTokenStore = oAuthTokenStore;
    }

    public YouTubeUploadResponse execute(Long userId, YouTubePublishRequest request, String videoUrl) {
        logger.info("Publicando video id={} en YouTube para usuario={}", request.getVideoId(), userId);

        if (youtubeUploadRepository.findByVideoId(request.getVideoId()).isPresent()) {
            throw new IllegalArgumentException("Este video ya ha sido publicado en YouTube");
        }

        String accessToken = oAuthTokenStore.getAccessToken(userId)
                .orElseThrow(() -> new OAuthTokenNotFoundException(
                        "No se encontró token de acceso para usuario " + userId + ". Requiere re-autenticación con Google."));

        if (!youtubeDataApiService.validateAccessToken(accessToken)) {
            throw new InvalidGoogleTokenException("El token de acceso OAuth no es válido para YouTube");
        }

        YouTubeUpload upload = new YouTubeUpload(
                request.getVideoId(),
                userId,
                request.getTitle(),
                request.getDescription(),
                request.getVisibility()
        );
        upload.setStatus(YouTubeUploadStatus.PUBLISHING);
        upload = youtubeUploadRepository.save(upload);

        try {
            YouTubePublishResponse ytResponse = youtubeDataApiService.publishVideo(
                    accessToken,
                    videoUrl,
                    request.getTitle(),
                    request.getDescription(),
                    request.getVisibility().toString()
            );

            if ("PUBLISHED".equals(ytResponse.getStatus())) {
                upload.setStatus(YouTubeUploadStatus.PUBLISHED);
                upload.setYoutubeVideoId(ytResponse.getYoutubeVideoId());
                upload.setYoutubeUrl(ytResponse.getYoutubeUrl());
                upload.setPublishedAt(LocalDateTime.now());
                logger.info("Video publicado exitosamente en YouTube: id={}", ytResponse.getYoutubeVideoId());
            } else {
                upload.setStatus(YouTubeUploadStatus.FAILED);
                upload.setErrorMessage(ytResponse.getErrorMessage());
                logger.error("Fallo al publicar en YouTube: {}", ytResponse.getErrorMessage());
            }
        } catch (Exception e) {
            upload.setStatus(YouTubeUploadStatus.FAILED);
            upload.setErrorMessage(e.getMessage());
            logger.error("Excepción al publicar en YouTube: {}", e.getMessage(), e);
        }

        return new YouTubeUploadResponse(youtubeUploadRepository.save(upload));
    }
}
