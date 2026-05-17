package com.socialvideo.youtube.repository;

import com.socialvideo.youtube.entity.YouTubeConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface YouTubeConnectionRepository extends JpaRepository<YouTubeConnection, UUID> {

    Optional<YouTubeConnection> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);
}