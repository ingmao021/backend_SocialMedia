package com.socialvideo.auth.dto;

import com.socialvideo.user.dto.UserResponse;

public record AuthResponse(
        String token,
        UserResponse user
) {}
