package com.example.backend_socialmedia.video.application;

import com.example.backend_socialmedia.video.domain.Video;
import com.example.backend_socialmedia.video.domain.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case para listar videos de un usuario
 */
@Service
public class ListVideosUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ListVideosUseCase.class);

    private final VideoRepository videoRepository;

    public ListVideosUseCase(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    /**
     * Lista todos los videos de un usuario
     * @param userId ID del usuario
     * @return Lista de videos del usuario
     */
    public List<Video> execute(Long userId) {
        logger.info("Listando videos para usuario: {}", userId);
        return videoRepository.findByUserId(userId);
    }
}


