package com.example.backend_socialmedia.youtube.infrastructure.persistence;

import com.example.backend_socialmedia.youtube.domain.YouTubeUpload;
import com.example.backend_socialmedia.youtube.domain.YouTubeUploadRepository;
import com.example.backend_socialmedia.youtube.domain.YouTubeUploadStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del repositorio para YouTubeUpload
 * Mapea entre entidades JPA y entidades de dominio
 */
@Repository
public class YouTubeUploadRepositoryImpl implements YouTubeUploadRepository {

    private final YouTubeUploadJpaRepository jpaRepository;

    public YouTubeUploadRepositoryImpl(YouTubeUploadJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public YouTubeUpload save(YouTubeUpload youtubeUpload) {
        YouTubeUploadEntity entity = toDomainEntity(youtubeUpload);
        YouTubeUploadEntity saved = jpaRepository.save(entity);
        return toYouTubeDomain(saved);
    }

    @Override
    public Optional<YouTubeUpload> findById(Long id) {
        return jpaRepository.findById(id)
                .map(this::toYouTubeDomain);
    }

    @Override
    public List<YouTubeUpload> findByUserId(Long userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toYouTubeDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<YouTubeUpload> findByVideoId(Long videoId) {
        return jpaRepository.findByVideoId(videoId)
                .map(this::toYouTubeDomain);
    }

    @Override
    public List<YouTubeUpload> findByStatus(YouTubeUploadStatus status) {
        return jpaRepository.findByStatus(status)
                .stream()
                .map(this::toYouTubeDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    /**
     * Convierte de entidad JPA a entidad de dominio
     */
    private YouTubeUpload toYouTubeDomain(YouTubeUploadEntity entity) {
        YouTubeUpload domain = new YouTubeUpload();
        domain.setId(entity.getId());
        domain.setVideoId(entity.getVideoId());
        domain.setUserId(entity.getUserId());
        domain.setStatus(entity.getStatus());
        domain.setYoutubeVideoId(entity.getYoutubeVideoId());
        domain.setYoutubeUrl(entity.getYoutubeUrl());
        domain.setTitle(entity.getTitle());
        domain.setDescription(entity.getDescription());
        domain.setVisibility(entity.getVisibility());
        domain.setPublishedAt(entity.getPublishedAt());
        domain.setErrorMessage(entity.getErrorMessage());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    /**
     * Convierte de entidad de dominio a entidad JPA
     */
    private YouTubeUploadEntity toDomainEntity(YouTubeUpload domain) {
        return YouTubeUploadEntity.builder()
                .id(domain.getId())
                .videoId(domain.getVideoId())
                .userId(domain.getUserId())
                .status(domain.getStatus())
                .youtubeVideoId(domain.getYoutubeVideoId())
                .youtubeUrl(domain.getYoutubeUrl())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .visibility(domain.getVisibility())
                .publishedAt(domain.getPublishedAt())
                .errorMessage(domain.getErrorMessage())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

