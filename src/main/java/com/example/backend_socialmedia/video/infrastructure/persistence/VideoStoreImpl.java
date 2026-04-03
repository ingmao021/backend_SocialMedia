package com.example.backend_socialmedia.video.infrastructure.persistence;

import com.example.backend_socialmedia.video.domain.Video;
import com.example.backend_socialmedia.video.domain.VideoRepository;
import com.example.backend_socialmedia.video.domain.VideoStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del repositorio de Video usando Spring Data JPA
 * Convierte entre la entidad JPA y el modelo de dominio
 */
@Component
public class VideoStoreImpl implements VideoRepository {

    private final VideoJpaRepository videoJpaRepository;

    public VideoStoreImpl(VideoJpaRepository videoJpaRepository) {
        this.videoJpaRepository = videoJpaRepository;
    }

    @Override
    public Video save(Video video) {
        VideoEntity entity = this.toPersistence(video);
        VideoEntity savedEntity = videoJpaRepository.save(entity);
        return this.toDomain(savedEntity);
    }

    @Override
    public Optional<Video> findById(Long id) {
        return videoJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<Video> findByGoogleJobId(String googleJobId) {
        return videoJpaRepository.findByGoogleJobId(googleJobId)
                .map(this::toDomain);
    }

    @Override
    public List<Video> findByUserId(Long userId) {
        return videoJpaRepository.findByUserId(userId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Video> findByUserIdAndStatus(Long userId, VideoStatus status) {
        VideoStatusEntity statusEntity = VideoStatusEntity.valueOf(status.name());
        return videoJpaRepository.findByUserIdAndStatus(userId, statusEntity)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        videoJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return videoJpaRepository.existsById(id);
    }

    @Override
    public List<Video> findByStatus(VideoStatus status) {
        VideoStatusEntity statusEntity = VideoStatusEntity.valueOf(status.name());
        return videoJpaRepository.findByStatus(statusEntity)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // Conversión de entidad a dominio
    private Video toDomain(VideoEntity entity) {
        return new Video(
                entity.getId(),
                entity.getUserId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getPrompt(),
                VideoStatus.valueOf(entity.getStatus().name()),
                entity.getVideoUrl(),
                entity.getGoogleJobId(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    // Conversión de dominio a entidad
    private VideoEntity toPersistence(Video video) {
        VideoEntity entity = new VideoEntity();
        if (video.getId() != null) {
            entity.setId(video.getId());
        }
        entity.setUserId(video.getUserId());
        entity.setTitle(video.getTitle());
        entity.setDescription(video.getDescription());
        entity.setPrompt(video.getPrompt());
        entity.setStatus(VideoStatusEntity.valueOf(video.getStatus().name()));
        entity.setVideoUrl(video.getVideoUrl());
        entity.setGoogleJobId(video.getGoogleJobId());
        entity.setErrorMessage(video.getErrorMessage());
        entity.setCreatedAt(video.getCreatedAt());
        entity.setUpdatedAt(video.getUpdatedAt());
        return entity;
    }
}

