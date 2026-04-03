package com.example.backend_socialmedia.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;

/**
 * Propiedades de configuración para generación de videos
 */
@Configuration
@ConfigurationProperties(prefix = "video-generation")
@Validated
public class VideoGenerationProperties {

    @Positive(message = "video-generation.polling-interval-seconds debe ser mayor a 0")
    private int pollingIntervalSeconds = 30;

    public int getPollingIntervalSeconds() { return pollingIntervalSeconds; }
    public void setPollingIntervalSeconds(int pollingIntervalSeconds) {
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }
}

