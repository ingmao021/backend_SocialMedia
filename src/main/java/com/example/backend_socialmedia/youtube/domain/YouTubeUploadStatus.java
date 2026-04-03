package com.example.backend_socialmedia.youtube.domain;

/**
 * Enum que representa los posibles estados de una publicación en YouTube
 */
public enum YouTubeUploadStatus {
    PENDING,      // En espera de publicar
    PUBLISHING,   // En proceso de publicación
    PUBLISHED,    // Publicado exitosamente
    FAILED,       // Error durante la publicación
    DELETED       // Eliminado de YouTube
}

