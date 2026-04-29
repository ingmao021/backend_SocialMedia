package com.socialvideo.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GenerateVideoRequest(
        @NotBlank(message = "El prompt es obligatorio")
        @Size(max = 2000, message = "El prompt no puede exceder 2000 caracteres")
        String prompt,

        @NotNull(message = "La duración es obligatoria")
        @AllowedDuration
        Short durationSeconds
) {}
