package com.example.backend_socialmedia.video.domain;

public enum VideoStatus {
    PENDING,      // En espera de procesamiento
    PROCESSING,   // Siendo procesado por Google AI
    COMPLETED,    // Generado exitosamente
    ERROR         // Error durante la generación
}

