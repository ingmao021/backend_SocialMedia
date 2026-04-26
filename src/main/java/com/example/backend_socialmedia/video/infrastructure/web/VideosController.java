package com.example.backend_socialmedia.video.infrastructure.web;

import com.example.backend_socialmedia.video.application.GenerateVideoUseCase;
import com.example.backend_socialmedia.video.application.GetVideoUseCase;
import com.example.backend_socialmedia.video.application.ListVideosUseCase;
import com.example.backend_socialmedia.video.application.DeleteVideoUseCase;
import com.example.backend_socialmedia.video.domain.GenerateVideoRequest;
import com.example.backend_socialmedia.video.domain.Video;
import com.example.backend_socialmedia.video.domain.VideoResponse;
import com.example.backend_socialmedia.shared.config.CustomUserDetails;
import com.example.backend_socialmedia.shared.exception.UnauthorizedException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
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

    @PostMapping("/generate")
    public ResponseEntity<VideoResponse> generateVideo(@Valid @RequestBody GenerateVideoRequest request) {
        logger.info("Solicitud de generación de video para usuario={}", getCurrentUserId());
        Video video = generateVideoUseCase.execute(getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new VideoResponse(video));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> getVideo(@PathVariable Long id) {
        logger.info("Obteniendo video id={} para usuario={}", id, getCurrentUserId());
        Video video = getVideoUseCase.execute(id, getCurrentUserId());
        return ResponseEntity.ok(new VideoResponse(video));
    }

    @GetMapping
    public ResponseEntity<List<VideoResponse>> listVideos() {
        logger.info("Listando videos para usuario={}", getCurrentUserId());
        List<VideoResponse> responses = listVideosUseCase.execute(getCurrentUserId())
                .stream()
                .map(VideoResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long id) {
        logger.info("Eliminando video id={} para usuario={}", id, getCurrentUserId());
        deleteVideoUseCase.execute(id, getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        return principal.getUserId();
    }
}
