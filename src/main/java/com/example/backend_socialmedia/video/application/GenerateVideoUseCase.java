package com.example.backend_socialmedia.video.application;

import com.example.backend_socialmedia.shared.exception.VideoGenerationException;
import com.example.backend_socialmedia.video.domain.GenerateVideoRequest;
import com.example.backend_socialmedia.video.domain.Video;
import com.example.backend_socialmedia.video.domain.VideoGenerationPort;
import com.example.backend_socialmedia.video.domain.VideoRepository;
import com.example.backend_socialmedia.video.domain.VideoStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(noRollbackFor = VideoGenerationException.class)
public class GenerateVideoUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GenerateVideoUseCase.class);

    private final VideoRepository videoRepository;
    private final VideoGenerationPort videoGenerationPort;

    public GenerateVideoUseCase(VideoRepository videoRepository,
                                VideoGenerationPort videoGenerationPort) {
        this.videoRepository = videoRepository;
        this.videoGenerationPort = videoGenerationPort;
    }

    public Video execute(Long userId, GenerateVideoRequest request) {
        logger.info("Iniciando generación de video para usuario={}", userId);

        if (!videoGenerationPort.isConfigured()) {
            throw new VideoGenerationException("El servicio de generación de video no está configurado correctamente");
        }

        Video video = new Video();
        video.setUserId(userId);
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setPrompt(request.getPrompt());
        video.setStatus(VideoStatus.PENDING);
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());
        video = videoRepository.save(video);

        logger.info("Video creado con id={}, llamando a Veo 2...", video.getId());

        try {
            VideoGenerationPort.GenerationResult result = videoGenerationPort.startGeneration(
                    new VideoGenerationPort.GenerationRequest(request.getPrompt(), 5, "16:9")
            );
            video.setStatus(VideoStatus.PROCESSING);
            video.setGoogleJobId(result.jobId());
            video.setUpdatedAt(LocalDateTime.now());
            logger.info("Video id={} en PROCESSING, jobId={}", video.getId(), result.jobId());
        } catch (VideoGenerationException e) {
            video.setStatus(VideoStatus.ERROR);
            video.setErrorMessage(e.getMessage());
            video.setUpdatedAt(LocalDateTime.now());
            videoRepository.save(video);
            logger.error("Error al iniciar generación para video id={}: {}", video.getId(), e.getMessage());
            throw e;
        }

        return videoRepository.save(video);
    }
}
