package com.socialvideo.youtube.dto;

import com.socialvideo.youtube.entity.YouTubePrivacyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo del request para {@code POST /api/videos/{videoId}/youtube/export}.
 *
 * <p>Los límites de tamaño corresponden a las restricciones de la
 * YouTube Data API v3:</p>
 * <ul>
 *   <li>{@code title}: máximo 100 caracteres.</li>
 *   <li>{@code description}: máximo 5000 caracteres.</li>
 *   <li>{@code tags}: máximo 500 caracteres totales (suma de todas las tags y comas).</li>
 * </ul>
 *
 * <p>El {@code privacyStatus} se valida automáticamente por Jackson al
 * deserializar el enum: si el cliente manda un valor inválido,
 * el {@link com.socialvideo.exception.GlobalExceptionHandler} responde
 * con HTTP 400.</p>
 */
public record YouTubeExportRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 100, message = "El título no puede exceder 100 caracteres")
        String title,

        @Size(max = 5000, message = "La descripción no puede exceder 5000 caracteres")
        String description,

        @Size(max = 500, message = "Las etiquetas no pueden exceder 500 caracteres en total")
        String tags,

        @NotNull(message = "El estado de privacidad es obligatorio")
        YouTubePrivacyStatus privacyStatus
) {}