package com.example.backend_socialmedia.video.infrastructure.web;

import com.example.backend_socialmedia.video.application.GenerateVideoUseCase;
import com.example.backend_socialmedia.video.application.GetVideoUseCase;
import com.example.backend_socialmedia.video.application.ListVideosUseCase;
import com.example.backend_socialmedia.video.application.DeleteVideoUseCase;
import com.example.backend_socialmedia.video.domain.GenerateVideoRequest;
import com.example.backend_socialmedia.video.domain.Video;
import com.example.backend_socialmedia.video.domain.VideoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión de videos generados por IA
 */
@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = {"${app.frontend-url}"})
public class VideosController {

    private static final Logger logger = LoggerFactory.getLogger(VideosController.class);

    private final GenerateVideoUseCase generateVideoUseCase;
    private final GetVideoUseCase getVideoUseCase;
    private final ListVideosUseCase listVideosUseCase;
    private final DeleteVideoUseCase deleteVideoUseCase;

    public VideosController(GenerateVideoUseCase generateVideoUseCase,
                           GetVideoUseCase getVideoUseCase,
                           ListVideosUseCase listVideosUseCase,
                           DeleteVideoUseCase deleteVideoUseCase) {
        this.generateVideoUseCase = generateVideoUseCase;
        this.getVideoUseCase = getVideoUseCase;
        this.listVideosUseCase = listVideosUseCase;
        this.deleteVideoUseCase = deleteVideoUseCase;
    }

    /**
     * POST /api/videos/generate - Genera un nuevo video
     */
    @PostMapping("/generate")
    public ResponseEntity<VideoResponse> generateVideo(@RequestBody GenerateVideoRequest request) {
        logger.info("Solicitud para generar video");

        try {
            Long userId = getUserIdFromAuth();
            Video video = generateVideoUseCase.execute(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new VideoResponse(video));
        } catch (Exception e) {
            logger.error("Error al generar video", e);
            throw e;
        }
    }

    /**
     * GET /api/videos/{id} - Obtiene los detalles de un video
     */
    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> getVideo(@PathVariable Long id) {
        logger.info("Obteniendo video: {}", id);

        try {
            Long userId = getUserIdFromAuth();
            Video video = getVideoUseCase.execute(id, userId);
            return ResponseEntity.ok(new VideoResponse(video));
        } catch (Exception e) {
            logger.error("Error al obtener video", e);
            throw e;
        }
    }

    /**
     * GET /api/videos - Lista todos los videos del usuario autenticado
     */
    @GetMapping
    public ResponseEntity<List<VideoResponse>> listVideos() {
        logger.info("Listando videos del usuario");

        try {
            Long userId = getUserIdFromAuth();
            List<Video> videos = listVideosUseCase.execute(userId);
            List<VideoResponse> responses = videos.stream()
                    .map(VideoResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Error al listar videos", e);
            throw e;
        }
    }

    /**
     * DELETE /api/videos/{id} - Elimina un video
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long id) {
        logger.info("Eliminando video: {}", id);

        try {
            Long userId = getUserIdFromAuth();
            deleteVideoUseCase.execute(id, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error al eliminar video", e);
            throw e;
        }
    }

    /**
     * Obtiene el ID del usuario desde el contexto de seguridad
     */
    private Long getUserIdFromAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Usuario no autenticado");
        }

        // El nombre del principal debería ser el userId (convertido a String)
        try {
            String userId = auth.getName();
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            logger.error("No se puede convertir userId a Long: {}", auth.getName());
            throw new RuntimeException("ID de usuario inválido");
        }
    }
}

