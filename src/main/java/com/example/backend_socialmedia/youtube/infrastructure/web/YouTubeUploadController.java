package com.example.backend_socialmedia.youtube.infrastructure.web;

import com.example.backend_socialmedia.youtube.application.*;
import com.example.backend_socialmedia.youtube.domain.YouTubePublishRequest;
import com.example.backend_socialmedia.youtube.domain.YouTubeUploadResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para operaciones de publicación en YouTube
 */
@RestController
@RequestMapping("/api/youtube")
public class YouTubeUploadController {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeUploadController.class);

    private final PublishVideoToYouTubeUseCase publishVideoToYouTubeUseCase;
    private final GetYouTubeUploadStatusUseCase getYouTubeUploadStatusUseCase;
    private final ListYouTubeUploadsUseCase listYouTubeUploadsUseCase;
    private final DeleteYouTubeUploadUseCase deleteYouTubeUploadUseCase;

    public YouTubeUploadController(
            PublishVideoToYouTubeUseCase publishVideoToYouTubeUseCase,
            GetYouTubeUploadStatusUseCase getYouTubeUploadStatusUseCase,
            ListYouTubeUploadsUseCase listYouTubeUploadsUseCase,
            DeleteYouTubeUploadUseCase deleteYouTubeUploadUseCase) {
        this.publishVideoToYouTubeUseCase = publishVideoToYouTubeUseCase;
        this.getYouTubeUploadStatusUseCase = getYouTubeUploadStatusUseCase;
        this.listYouTubeUploadsUseCase = listYouTubeUploadsUseCase;
        this.deleteYouTubeUploadUseCase = deleteYouTubeUploadUseCase;
    }

    /**
     * Publica un video generado en YouTube
     * POST /api/youtube/publish
     */
    @PostMapping("/publish")
    public ResponseEntity<YouTubeUploadResponse> publishVideo(
            @Valid @RequestBody YouTubePublishRequest request) {

        try {
            Long userId = getCurrentUserId();

            logger.info("Usuario {} solicita publicar video {} en YouTube", userId, request.getVideoId());

            // TODO: Obtener la URL del video generado desde la base de datos
            String videoUrl = "https://example.com/videos/generated_video.mp4";

            YouTubeUploadResponse response = publishVideoToYouTubeUseCase.execute(userId, request, videoUrl);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Validación fallida: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error al publicar video en YouTube", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene el estado de una publicación
     * GET /api/youtube/uploads/{uploadId}
     */
    @GetMapping("/uploads/{uploadId}")
    public ResponseEntity<YouTubeUploadResponse> getUploadStatus(
            @PathVariable Long uploadId) {

        try {
            Long userId = getCurrentUserId();

            logger.info("Usuario {} solicita estado de publicación: {}", userId, uploadId);

            YouTubeUploadResponse response = getYouTubeUploadStatusUseCase.execute(uploadId, userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error al obtener estado de publicación", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lista todas las publicaciones del usuario autenticado
     * GET /api/youtube/uploads
     */
    @GetMapping("/uploads")
    public ResponseEntity<List<YouTubeUploadResponse>> listUserUploads() {

        try {
            Long userId = getCurrentUserId();

            logger.info("Usuario {} solicita listar sus publicaciones en YouTube", userId);

            List<YouTubeUploadResponse> uploads = listYouTubeUploadsUseCase.execute(userId);

            return ResponseEntity.ok(uploads);

        } catch (Exception e) {
            logger.error("Error al listar publicaciones en YouTube", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Elimina una publicación de YouTube
     * DELETE /api/youtube/uploads/{uploadId}
     */
    @DeleteMapping("/uploads/{uploadId}")
    public ResponseEntity<Void> deleteUpload(
            @PathVariable Long uploadId) {

        try {
            Long userId = getCurrentUserId();

            logger.info("Usuario {} solicita eliminar publicación: {}", userId, uploadId);

            deleteYouTubeUploadUseCase.execute(uploadId, userId);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            logger.error("Error al eliminar publicación en YouTube", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene el ID del usuario autenticado
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Usuario no autenticado");
        }

        // El ID del usuario debería estar disponible en el principal o claims del JWT
        Object principal = authentication.getPrincipal();

        if (principal instanceof String) {
            // Si es un JWT, necesitaremos parsear el token
            // Por ahora, retornamos un error
            throw new RuntimeException("No se puede obtener el ID del usuario desde el token");
        }

        // En caso de ser un UserDetails
        return Long.parseLong(principal.toString());
    }
}

