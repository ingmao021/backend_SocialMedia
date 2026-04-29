package com.socialvideo.video.controller;

import com.socialvideo.security.CurrentUser;
import com.socialvideo.video.dto.GenerateVideoRequest;
import com.socialvideo.video.dto.VideoResponse;
import com.socialvideo.video.dto.VideoStatusResponse;
import com.socialvideo.video.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping("/generate")
    public ResponseEntity<VideoResponse> generate(
            @CurrentUser Long userId,
            @Valid @RequestBody GenerateVideoRequest request) {
        VideoResponse response = videoService.generate(userId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{videoId}/status")
    public ResponseEntity<VideoStatusResponse> getStatus(
            @CurrentUser Long userId,
            @PathVariable UUID videoId) {
        return ResponseEntity.ok(videoService.getStatus(userId, videoId));
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<VideoResponse> getVideo(
            @CurrentUser Long userId,
            @PathVariable UUID videoId) {
        return ResponseEntity.ok(videoService.getVideo(userId, videoId));
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> deleteVideo(
            @CurrentUser Long userId,
            @PathVariable UUID videoId) {
        videoService.deleteVideo(userId, videoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<VideoResponse>> listVideos(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(videoService.listByUser(userId, page, size));
    }
}
