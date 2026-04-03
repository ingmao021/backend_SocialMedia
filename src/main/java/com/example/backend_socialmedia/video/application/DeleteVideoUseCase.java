package com.example.backend_socialmedia.video.application;

import com.example.backend_socialmedia.video.domain.Video;
import com.example.backend_socialmedia.video.domain.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case para eliminar un video
 */
@Service
public class DeleteVideoUseCase {

    private static final Logger logger = LoggerFactory.getLogger(DeleteVideoUseCase.class);

    private final VideoRepository videoRepository;

    public DeleteVideoUseCase(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    /**
     * Elimina un video
     * @param videoId ID del video a eliminar
     * @param userId ID del usuario (para validar que es el propietario)
     */
    public void execute(Long videoId, Long userId) {
        logger.info("Eliminando video: {} para usuario: {}", videoId, userId);

        Optional<Video> videoOpt = videoRepository.findById(videoId);

        if (videoOpt.isEmpty()) {
            logger.warn("Video no encontrado: {}", videoId);
            throw new RuntimeException("Video no encontrado");
        }

        Video video = videoOpt.get();

        // Validar que el usuario sea el propietario del video
        if (!video.getUserId().equals(userId)) {
            logger.warn("Usuario: {} intenta eliminar video de otro usuario", userId);
            throw new RuntimeException("No tienes permiso para eliminar este video");
        }

        videoRepository.deleteById(videoId);
        logger.info("Video eliminado: {}", videoId);
    }
}

