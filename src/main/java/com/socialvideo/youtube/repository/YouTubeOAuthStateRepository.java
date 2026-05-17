package com.socialvideo.youtube.repository;

import com.socialvideo.youtube.entity.YouTubeOAuthState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface YouTubeOAuthStateRepository extends JpaRepository<YouTubeOAuthState, String> {

    Optional<YouTubeOAuthState> findByState(String state);

    List<YouTubeOAuthState> findByExpiresAtBefore(OffsetDateTime cutoff);

    void deleteByExpiresAtBefore(OffsetDateTime cutoff);
}