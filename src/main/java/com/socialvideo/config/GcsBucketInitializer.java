package com.socialvideo.config;

import com.socialvideo.external.gcs.GcsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Configures CORS on the GCS bucket at application startup.
 * This is idempotent — safe to run every time the app starts.
 * Required so the frontend browser can load videos directly via Signed URLs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GcsBucketInitializer {

    private final GcsService gcsService;

    @EventListener(ApplicationReadyEvent.class)
    public void configureBucketCors() {
        try {
            gcsService.configureBucketCors();
        } catch (Exception e) {
            log.warn("Could not configure CORS on GCS bucket: {}. " +
                    "This is OK if running locally without GCS credentials.", e.getMessage());
        }
    }
}
