package com.example.backend_socialmedia.video.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de Spring Data para VideoEntity
 * Proporciona operaciones CRUD y consultas específicas
 */
@Repository
public interface VideoJpaRepository extends JpaRepository<VideoEntity, Long> {

    /**
     * Encontrar un video por su ID de trabajo en Google
     */
    Optional<VideoEntity> findByGoogleJobId(String googleJobId);

    /**
     * Listar todos los videos de un usuario específico
     */
    List<VideoEntity> findByUserId(Long userId);

    /**
     * Listar videos de un usuario filtrados por estado
     */
    List<VideoEntity> findByUserIdAndStatus(Long userId, VideoStatusEntity status);

    /**
     * Listar todos los videos con un estado específico
     */
    List<VideoEntity> findByStatus(VideoStatusEntity status);
}

