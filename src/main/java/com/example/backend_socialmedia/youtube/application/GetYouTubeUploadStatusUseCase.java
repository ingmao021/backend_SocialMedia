package com.example.backend_socialmedia.youtube.application;

import com.example.backend_socialmedia.youtube.domain.YouTubeUpload;
import com.example.backend_socialmedia.youtube.domain.YouTubeUploadRepository;
import com.example.backend_socialmedia.youtube.domain.YouTubeUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Caso de uso para obtener el estado de una publicación en YouTube
 */
@Service
@Transactional(readOnly = true)
public class GetYouTubeUploadStatusUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetYouTubeUploadStatusUseCase.class);

    private final YouTubeUploadRepository youtubeUploadRepository;

    public GetYouTubeUploadStatusUseCase(YouTubeUploadRepository youtubeUploadRepository) {
        this.youtubeUploadRepository = youtubeUploadRepository;
    }

    /**
     * Obtiene el estado de una publicación
     * @param uploadId ID de la publicación
     * @param userId ID del usuario (para validación de permisos)
     * @return Respuesta con detalles de la publicación
     */
    public YouTubeUploadResponse execute(Long uploadId, Long userId) {
        logger.info("Obteniendo estado de publicación: {}", uploadId);

        Optional<YouTubeUpload> upload = youtubeUploadRepository.findById(uploadId);

        if (upload.isEmpty()) {
            throw new RuntimeException("Publicación no encontrada");
        }

        YouTubeUpload youtubeUpload = upload.get();

        // Validar que el usuario sea el propietario
        if (!youtubeUpload.getUserId().equals(userId)) {
            throw new RuntimeException("No tienes permiso para ver esta publicación");
        }

        return new YouTubeUploadResponse(youtubeUpload);
    }
}

