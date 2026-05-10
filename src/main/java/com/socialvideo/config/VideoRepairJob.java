package com.socialvideo.config;

import com.socialvideo.external.gcs.GcsService;
import com.socialvideo.video.entity.Video;
import com.socialvideo.video.entity.VideoStatus;
import com.socialvideo.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Repair job that runs at startup.
 * Finds videos stuck in PROCESSING or prematurely marked FAILED
 * and checks GCS to see if the video file actually exists.
 * If found, marks the video as COMPLETED with a valid signed URL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoRepairJob {

    private final VideoRepository videoRepository;
    private final GcsService gcsService;
    private final AppProperties appProperties;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void repairStuckVideos() {
        try {
            // Collect both PROCESSING and FAILED videos (FAILED may have files in GCS)
            List<Video> candidates = new ArrayList<>();
            candidates.addAll(videoRepository.findByStatus(VideoStatus.PROCESSING));
            candidates.addAll(videoRepository.findByStatus(VideoStatus.FAILED));

            if (candidates.isEmpty()) {
                log.info("[REPAIR] No PROCESSING or FAILED videos to repair");
                return;
            }

            log.info("[REPAIR] Found {} candidate videos (PROCESSING + FAILED). Checking GCS...", candidates.size());

            int repaired = 0;
            for (Video video : candidates) {
                try {
                    if (repairSingleVideo(video)) {
                        repaired++;
                    }
                } catch (Exception e) {
                    log.error("[REPAIR] Error repairing video {}: {}", video.getId(), e.getMessage());
                }
            }

            log.info("[REPAIR] Repair complete: {}/{} videos fixed", repaired, candidates.size());
        } catch (Exception e) {
            log.warn("[REPAIR] Could not run video repair: {}", e.getMessage());
        }
    }

    private boolean repairSingleVideo(Video video) {
        // Skip if already has a valid gcsUri and signedUrl
        if (video.getStatus() == VideoStatus.COMPLETED) {
            return false;
        }

        // Build the GCS prefix for this video: videos/{userId}/{videoId}/
        Long userId = video.getUser().getId();
        String prefix = String.format("videos/%d/%s/", userId, video.getId());

        log.info("[REPAIR] Searching GCS for video {} (status={}): prefix={}",
                video.getId(), video.getStatus(), prefix);

        String gcsUri = gcsService.findVideoFile(prefix);

        if (gcsUri == null) {
            log.warn("[REPAIR] No .mp4 file found in GCS for video {}. Skipping.", video.getId());
            return false;
        }

        log.info("[REPAIR] Found video file in GCS: {} → {}", video.getId(), gcsUri);

        // Update the video record
        video.setGcsUri(gcsUri);
        video.setStatus(VideoStatus.COMPLETED);
        video.setErrorMessage(null); // Clear the timeout error message

        try {
            var signedUrl = gcsService.generateSignedUrl(gcsUri);
            video.setSignedUrl(signedUrl.toString());
            video.setSignedUrlExpiresAt(Instant.now()
                    .plus(appProperties.getSignedUrl().getTtlDays(), ChronoUnit.DAYS));
        } catch (Exception e) {
            log.error("[REPAIR] Error generating signed URL for video {}: {}", video.getId(), e.getMessage());
        }

        videoRepository.save(video);
        log.info("[REPAIR] Video {} repaired → COMPLETED with signedUrl", video.getId());
        return true;
    }
}

