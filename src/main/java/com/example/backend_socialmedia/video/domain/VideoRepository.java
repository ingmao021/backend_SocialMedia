package com.example.backend_socialmedia.video.domain;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz del repositorio de videos
 * Define los contratos para persistencia de videos
 */
public interface VideoRepository {

    /**
     * Guarda un nuevo video o actualiza uno existente
     */
    Video save(Video video);

    /**
     * Obtiene un video por su ID
     */
    Optional<Video> findById(Long id);

    /**
     * Obtiene un video por su ID de trabajo en Google
     */
    Optional<Video> findByGoogleJobId(String googleJobId);

    /**
     * Lista todos los videos de un usuario específico
     */
    List<Video> findByUserId(Long userId);

    /**
     * Lista videos de un usuario filtrados por estado
     */
    List<Video> findByUserIdAndStatus(Long userId, VideoStatus status);

    /**
     * Elimina un video por su ID
     */
    void deleteById(Long id);

    /**
     * Verifica si un video existe
     */
    boolean existsById(Long id);

    /**
     * Obtiene todos los videos con estado específico (para procesamiento)
     */
    List<Video> findByStatus(VideoStatus status);
}

