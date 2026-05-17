package com.socialvideo.youtube.repository;

import com.socialvideo.youtube.entity.YouTubeExportJob;
import com.socialvideo.youtube.entity.YouTubeExportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface YouTubeExportJobRepository extends JpaRepository<YouTubeExportJob, UUID> {

    Optional<YouTubeExportJob> findByIdAndUserId(UUID id, Long userId);

    Page<YouTubeExportJob> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<YouTubeExportJob> findByStatusIn(List<YouTubeExportStatus> statuses);

    int countByUserIdAndStatus(Long userId, YouTubeExportStatus status);
}