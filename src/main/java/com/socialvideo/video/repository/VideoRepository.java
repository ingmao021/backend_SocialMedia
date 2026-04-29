package com.socialvideo.video.repository;

import com.socialvideo.video.entity.Video;
import com.socialvideo.video.entity.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {

    int countByUserIdAndStatus(Long userId, VideoStatus status);

    List<Video> findByStatus(VideoStatus status);

    Optional<Video> findByIdAndUserId(UUID id, Long userId);

    Page<Video> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
