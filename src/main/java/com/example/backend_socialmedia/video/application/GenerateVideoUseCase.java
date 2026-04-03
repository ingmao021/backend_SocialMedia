package com.example.backend_socialmedia.video.application;

import com.example.backend_socialmedia.video.domain.Video;
import com.example.backend_socialmedia.video.domain.VideoRepository;
import com.example.backend_socialmedia.video.domain.VideoStatus;
import com.example.backend_socialmedia.video.domain.GenerateVideoRequest;
import com.example.backend_socialmedia.video.infrastructure.google.GoogleGenerativeAiService;
import com.example.backend_socialmedia.video.infrastructure.google.GoogleVideoGenerationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * Use case para generar videos usando Google AI
 * Orquesta el proceso de generación de video
 */
@Service
public class GenerateVideoUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GenerateVideoUseCase.class);

    private final VideoRepository videoRepository;
    private final GoogleGenerativeAiService googleAiService;

    public GenerateVideoUseCase(VideoRepository videoRepository, GoogleGenerativeAiService googleAiService) {
        this.videoRepository = videoRepository;
        this.googleAiService = googleAiService;
    }

    /**
     * Ejecuta la generación de un video
     * @param userId ID del usuario que solicita la generación
     * @param request Datos de la solicitud (título, descripción, prompt)
     * @return Video creado
     */
    public Video execute(Long userId, GenerateVideoRequest request) {
        logger.info("Iniciando generación de video para usuario: {}", userId);

        // Validar que el API key sea válido
        if (!googleAiService.validateApiKey()) {
            throw new RuntimeException("Google Generative AI no está configurado correctamente");
        }

        // Crear el video con estado PENDING
        Video video = new Video();
        video.setUserId(userId);
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setPrompt(request.getPrompt());
        video.setStatus(VideoStatus.PENDING);
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());

        // Guardar el video
        video = videoRepository.save(video);
        logger.info("Video guardado con ID: {}", video.getId());

        // Llamar a Google Generative AI para iniciar la generación
        GoogleVideoGenerationResponse response = googleAiService.generateVideo(request.getPrompt());

        if ("ERROR".equals(response.getStatus())) {
            // Si hay error, actualizar el video
            video.setStatus(VideoStatus.ERROR);
            video.setErrorMessage(response.getErrorMessage());
            video.setUpdatedAt(LocalDateTime.now());
            video = videoRepository.save(video);
            logger.error("Error al generar video: {}", response.getErrorMessage());
            throw new RuntimeException("Error al generar video: " + response.getErrorMessage());
        }

        // Actualizar el video con el jobId y status de Google
        video.setStatus(VideoStatus.PROCESSING);
        video.setGoogleJobId(response.getJobId());
        video.setUpdatedAt(LocalDateTime.now());
        video = videoRepository.save(video);

        logger.info("Video en procesamiento con jobId: {}", response.getJobId());

        return video;
    }
}

