package com.example.backend_socialmedia.youtube.application;

import com.example.backend_socialmedia.youtube.domain.YouTubeUpload;
import com.example.backend_socialmedia.youtube.domain.YouTubeUploadRepository;
import com.example.backend_socialmedia.youtube.domain.YouTubeUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Caso de uso para listar todas las publicaciones de un usuario en YouTube
 */
@Service
@Transactional(readOnly = true)
public class ListYouTubeUploadsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ListYouTubeUploadsUseCase.class);

    private final YouTubeUploadRepository youtubeUploadRepository;

    public ListYouTubeUploadsUseCase(YouTubeUploadRepository youtubeUploadRepository) {
        this.youtubeUploadRepository = youtubeUploadRepository;
    }

    /**
     * Obtiene todas las publicaciones del usuario
     * @param userId ID del usuario
     * @return Lista de publicaciones
     */
    public List<YouTubeUploadResponse> execute(Long userId) {
        logger.info("Listando publicaciones en YouTube del usuario: {}", userId);

        List<YouTubeUpload> uploads = youtubeUploadRepository.findByUserId(userId);

        return uploads.stream()
                .map(YouTubeUploadResponse::new)
                .collect(Collectors.toList());
    }
}

