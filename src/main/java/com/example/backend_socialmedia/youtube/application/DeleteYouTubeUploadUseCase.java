package com.example.backend_socialmedia.youtube.application;

import com.example.backend_socialmedia.shared.OAuthTokenStore;
import com.example.backend_socialmedia.youtube.domain.YouTubeUpload;
import com.example.backend_socialmedia.youtube.domain.YouTubeUploadRepository;
import com.example.backend_socialmedia.youtube.domain.YouTubeUploadStatus;
import com.example.backend_socialmedia.youtube.infrastructure.google.YouTubeDataApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Caso de uso para eliminar una publicación de YouTube
 */
@Service
@Transactional
public class DeleteYouTubeUploadUseCase {

    private static final Logger logger = LoggerFactory.getLogger(DeleteYouTubeUploadUseCase.class);

    private final YouTubeUploadRepository youtubeUploadRepository;
    private final YouTubeDataApiService youtubeDataApiService;
    private final OAuthTokenStore oAuthTokenStore;

    public DeleteYouTubeUploadUseCase(
            YouTubeUploadRepository youtubeUploadRepository,
            YouTubeDataApiService youtubeDataApiService,
            OAuthTokenStore oAuthTokenStore) {
        this.youtubeUploadRepository = youtubeUploadRepository;
        this.youtubeDataApiService = youtubeDataApiService;
        this.oAuthTokenStore = oAuthTokenStore;
    }

    /**
     * Elimina una publicación de YouTube
     * @param uploadId ID de la publicación a eliminar
     * @param userId ID del usuario (para validación de permisos)
     */
    public void execute(Long uploadId, Long userId) {
        logger.info("Eliminando publicación en YouTube: {}", uploadId);

        Optional<YouTubeUpload> upload = youtubeUploadRepository.findById(uploadId);

        if (upload.isEmpty()) {
            throw new RuntimeException("Publicación no encontrada");
        }

        YouTubeUpload youtubeUpload = upload.get();

        // Validar que el usuario sea el propietario
        if (!youtubeUpload.getUserId().equals(userId)) {
            throw new RuntimeException("No tienes permiso para eliminar esta publicación");
        }

        // Si está publicada, intentar eliminarla de YouTube
        if (youtubeUpload.getYoutubeVideoId() != null) {
            try {
                String accessToken = oAuthTokenStore.getAccessToken(userId);
                if (accessToken != null && !accessToken.isEmpty()) {
                    boolean deleted = youtubeDataApiService.deleteVideo(accessToken, youtubeUpload.getYoutubeVideoId());
                    if (!deleted) {
                        logger.warn("No se pudo eliminar el video de YouTube: {}", youtubeUpload.getYoutubeVideoId());
                    }
                }
            } catch (Exception e) {
                logger.error("Error al eliminar video de YouTube", e);
            }
        }

        // Marcar como eliminado
        youtubeUpload.setStatus(YouTubeUploadStatus.DELETED);
        youtubeUploadRepository.save(youtubeUpload);

        logger.info("Publicación eliminada exitosamente: {}", uploadId);
    }
}

