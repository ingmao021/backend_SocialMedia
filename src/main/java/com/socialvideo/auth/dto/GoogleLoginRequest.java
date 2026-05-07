package com.socialvideo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "El token es obligatorio")
        @com.fasterxml.jackson.annotation.JsonProperty("token")
        String idToken
) {}
