package com.socialvideo.video.dto;

import com.socialvideo.video.entity.VideoStatus;

import java.time.Instant;
import java.util.UUID;

public record VideoResponse(
        UUID id,
        String prompt,
        short durationSeconds,
        VideoStatus status,
        String signedUrl,
        Instant signedUrlExpiresAt,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {}
