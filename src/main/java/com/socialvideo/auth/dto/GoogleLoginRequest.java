package com.socialvideo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "El idToken es obligatorio")
        String idToken
) {}
