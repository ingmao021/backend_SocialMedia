package com.example.backend_socialmedia.video.application;

import com.example.backend_socialmedia.video.domain.Video;
import com.example.backend_socialmedia.video.domain.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case para obtener el estado/detalles de un video
 */
@Service
public class GetVideoUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetVideoUseCase.class);

    private final VideoRepository videoRepository;

    public GetVideoUseCase(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    /**
     * Obtiene un video por su ID
     * @param videoId ID del video
     * @param userId ID del usuario (para validar que es el propietario)
     * @return Video encontrado
     */
    public Video execute(Long videoId, Long userId) {
        logger.info("Obteniendo video: {} para usuario: {}", videoId, userId);

        Optional<Video> videoOpt = videoRepository.findById(videoId);

        if (videoOpt.isEmpty()) {
            logger.warn("Video no encontrado: {}", videoId);
            throw new RuntimeException("Video no encontrado");
        }

        Video video = videoOpt.get();

        // Validar que el usuario sea el propietario del video
        if (!video.getUserId().equals(userId)) {
            logger.warn("Usuario: {} intenta acceder a video de otro usuario", userId);
            throw new RuntimeException("No tienes permiso para acceder a este video");
        }

        return video;
    }
}

