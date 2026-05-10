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
import java.util.List;

/**
 * One-time repair job that runs at startup.
 * Finds videos stuck in PROCESSING state (due to the previous polling bug)
 * and checks GCS to see if the video file actually exists.
 * If found, marks the video as COMPLETED with a valid signed URL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoRepairJob {

    private final VideoRepository videoRepository;
    private final GcsService gcsService;
    private final com.socialvideo.config.AppProperties appProperties;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void repairStuckVideos() {
        try {
            List<Video> stuckVideos = videoRepository.findByStatus(VideoStatus.PROCESSING);

            if (stuckVideos.isEmpty()) {
                log.info("[REPAIR] No stuck PROCESSING videos found");
                return;
            }

            log.info("[REPAIR] Found {} videos stuck in PROCESSING state. Checking GCS...", stuckVideos.size());

            int repaired = 0;
            for (Video video : stuckVideos) {
                try {
                    if (repairSingleVideo(video)) {
                        repaired++;
                    }
                } catch (Exception e) {
                    log.error("[REPAIR] Error repairing video {}: {}", video.getId(), e.getMessage());
                }
            }

            log.info("[REPAIR] Repair complete: {}/{} videos fixed", repaired, stuckVideos.size());
        } catch (Exception e) {
            log.warn("[REPAIR] Could not run video repair: {}", e.getMessage());
        }
    }

    private boolean repairSingleVideo(Video video) {
        // Build the GCS prefix for this video: videos/{userId}/{videoId}/
        Long userId = video.getUser().getId();
        String prefix = String.format("videos/%d/%s/", userId, video.getId());

        log.info("[REPAIR] Searching GCS for video {}: prefix={}", video.getId(), prefix);

        String gcsUri = gcsService.findVideoFile(prefix);

        if (gcsUri == null) {
            log.warn("[REPAIR] No .mp4 file found in GCS for video {}. Skipping.", video.getId());
            return false;
        }

        log.info("[REPAIR] Found video file in GCS: {} → {}", video.getId(), gcsUri);

        // Update the video record
        video.setGcsUri(gcsUri);
        video.setStatus(VideoStatus.COMPLETED);

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
