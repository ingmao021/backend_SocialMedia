package com.socialvideo.user.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String name,
        String avatarUrl,
        boolean hasPassword,
        boolean hasGoogle,
        int videosGenerated,
        int videosLimit,
        Instant createdAt
) {}
