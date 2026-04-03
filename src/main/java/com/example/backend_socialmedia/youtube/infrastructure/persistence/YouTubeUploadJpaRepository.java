package com.example.backend_socialmedia.youtube.infrastructure.persistence;

import com.example.backend_socialmedia.youtube.domain.YouTubeUploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para YouTubeUploadEntity
 */
@Repository
public interface YouTubeUploadJpaRepository extends JpaRepository<YouTubeUploadEntity, Long> {

    /**
     * Obtiene todas las publicaciones de un usuario
     */
    List<YouTubeUploadEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Obtiene una publicación por su ID de video generado
     */
    Optional<YouTubeUploadEntity> findByVideoId(Long videoId);

    /**
     * Obtiene todas las publicaciones con un estado específico
     */
    List<YouTubeUploadEntity> findByStatus(YouTubeUploadStatus status);

    /**
     * Obtiene todas las publicaciones en proceso
     */
    List<YouTubeUploadEntity> findByStatusIn(List<YouTubeUploadStatus> statuses);

    /**
     * Verifica si existe una publicación para un video específico
     */
    boolean existsByVideoId(Long videoId);

    /**
     * Verifica si existe una publicación de un usuario para un video
     */
    boolean existsByUserIdAndVideoId(Long userId, Long videoId);
}

