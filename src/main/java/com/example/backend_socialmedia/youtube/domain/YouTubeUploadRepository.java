package com.example.backend_socialmedia.youtube.domain;

import java.util.Optional;

/**
 * Interfaz del repositorio para operaciones CRUD de YouTubeUpload
 */
public interface YouTubeUploadRepository {

    /**
     * Guarda o actualiza una publicación de YouTube
     */
    YouTubeUpload save(YouTubeUpload youtubeUpload);

    /**
     * Obtiene una publicación por su ID
     */
    Optional<YouTubeUpload> findById(Long id);

    /**
     * Obtiene las publicaciones de un usuario
     */
    java.util.List<YouTubeUpload> findByUserId(Long userId);

    /**
     * Obtiene una publicación por su ID de video de Google
     */
    Optional<YouTubeUpload> findByVideoId(Long videoId);

    /**
     * Obtiene publicaciones con un estado específico
     */
    java.util.List<YouTubeUpload> findByStatus(YouTubeUploadStatus status);

    /**
     * Elimina una publicación
     */
    void delete(Long id);

    /**
     * Verifica si existe una publicación
     */
    boolean existsById(Long id);
}

