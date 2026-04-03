package com.example.backend_socialmedia.youtube.application;

import com.example.backend_socialmedia.shared.OAuthTokenStore;
import com.example.backend_socialmedia.youtube.domain.*;
import com.example.backend_socialmedia.youtube.infrastructure.google.YouTubeDataApiService;
import com.example.backend_socialmedia.youtube.infrastructure.google.YouTubePublishResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Caso de uso para publicar un video en YouTube
 * Orquesta la obtención del token OAuth, validación y llamada a la API de YouTube
 */
@Service
@Transactional
public class PublishVideoToYouTubeUseCase {

    private static final Logger logger = LoggerFactory.getLogger(PublishVideoToYouTubeUseCase.class);

    private final YouTubeUploadRepository youtubeUploadRepository;
    private final YouTubeDataApiService youtubeDataApiService;
    private final OAuthTokenStore oAuthTokenStore;

    public PublishVideoToYouTubeUseCase(
            YouTubeUploadRepository youtubeUploadRepository,
            YouTubeDataApiService youtubeDataApiService,
            OAuthTokenStore oAuthTokenStore) {
        this.youtubeUploadRepository = youtubeUploadRepository;
        this.youtubeDataApiService = youtubeDataApiService;
        this.oAuthTokenStore = oAuthTokenStore;
    }

    /**
     * Ejecuta el caso de uso de publicación a YouTube
     * @param userId ID del usuario
     * @param request DTO con datos de la publicación
     * @param videoUrl URL del video generado
     * @return Respuesta con detalles de la publicación
     */
    public YouTubeUploadResponse execute(Long userId, YouTubePublishRequest request, String videoUrl) {
        try {
            logger.info("Iniciando publicación de video a YouTube para usuario: {}", userId);

            // 1. Validar que el video no esté ya publicado
            Optional<YouTubeUpload> existingUpload = youtubeUploadRepository.findByVideoId(request.getVideoId());
            if (existingUpload.isPresent()) {
                throw new IllegalArgumentException("Este video ya ha sido publicado en YouTube");
            }

            // 2. Obtener el access token del usuario
            Optional<String> accessTokenOpt = oAuthTokenStore.getAccessToken(userId);
            if (accessTokenOpt.isEmpty()) {
                throw new RuntimeException("No se encontró token de acceso para el usuario. Se requiere autenticación con Google.");
            }

            String accessToken = accessTokenOpt.get();

            // 3. Validar el token
            if (!youtubeDataApiService.validateAccessToken(accessToken)) {
                throw new RuntimeException("El token de acceso no es válido para YouTube");
            }

            // 4. Crear registro de publicación
            YouTubeUpload youtubeUpload = new YouTubeUpload(
                    request.getVideoId(),
                    userId,
                    request.getTitle(),
                    request.getDescription(),
                    request.getVisibility()
            );
            youtubeUpload.setStatus(YouTubeUploadStatus.PUBLISHING);
            youtubeUpload = youtubeUploadRepository.save(youtubeUpload);

            // 5. Publicar en YouTube
            YouTubePublishResponse youtubeResponse = youtubeDataApiService.publishVideo(
                    accessToken,
                    videoUrl,
                    request.getTitle(),
                    request.getDescription(),
                    request.getVisibility().toString()
            );

            // 6. Actualizar registro con respuesta de YouTube
            if ("PUBLISHED".equals(youtubeResponse.getStatus())) {
                youtubeUpload.setStatus(YouTubeUploadStatus.PUBLISHED);
                youtubeUpload.setYoutubeVideoId(youtubeResponse.getYoutubeVideoId());
                youtubeUpload.setYoutubeUrl(youtubeResponse.getYoutubeUrl());
                youtubeUpload.setPublishedAt(LocalDateTime.now());
            } else {
                youtubeUpload.setStatus(YouTubeUploadStatus.FAILED);
                youtubeUpload.setErrorMessage(youtubeResponse.getErrorMessage());
            }

            youtubeUpload = youtubeUploadRepository.save(youtubeUpload);

            logger.info("Video publicado exitosamente en YouTube con ID: {}", youtubeResponse.getYoutubeVideoId());

            return new YouTubeUploadResponse(youtubeUpload);

        } catch (Exception e) {
            logger.error("Error al publicar video en YouTube", e);
            
            // Crear registro con error
            YouTubeUpload failedUpload = new YouTubeUpload(
                    request.getVideoId(),
                    userId,
                    request.getTitle(),
                    request.getDescription(),
                    request.getVisibility()
            );
            failedUpload.setErrorMessage(e.getMessage());
            youtubeUploadRepository.save(failedUpload);

            throw new RuntimeException("Error al publicar video en YouTube: " + e.getMessage());
        }
    }
}

