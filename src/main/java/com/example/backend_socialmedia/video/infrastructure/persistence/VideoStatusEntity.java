package com.example.backend_socialmedia.video.infrastructure.persistence;

/**
 * Enum que representa los estados de un video en la capa de persistencia
 */
public enum VideoStatusEntity {
    PENDING,      // En espera de procesamiento
    PROCESSING,   // Siendo procesado por Google AI
    COMPLETED,    // Generado exitosamente
    ERROR         // Error durante la generación
}

