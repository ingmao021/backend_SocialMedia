package com.example.backend_socialmedia.shared.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "google")
@Validated
public class GoogleAiProperties {

    private String generativeAiApiKey;

    private String tokenVerifyUrl;

    @Positive(message = "google.video-api-timeout-seconds debe ser mayor a 0")
    private int videoApiTimeoutSeconds = 300;

    public String getGenerativeAiApiKey() { return generativeAiApiKey; }
    public void setGenerativeAiApiKey(String generativeAiApiKey) {
        this.generativeAiApiKey = generativeAiApiKey;
    }

    public String getTokenVerifyUrl() { return tokenVerifyUrl; }
    public void setTokenVerifyUrl(String tokenVerifyUrl) {
        this.tokenVerifyUrl = tokenVerifyUrl;
    }

    public int getVideoApiTimeoutSeconds() { return videoApiTimeoutSeconds; }
    public void setVideoApiTimeoutSeconds(int videoApiTimeoutSeconds) {
        this.videoApiTimeoutSeconds = videoApiTimeoutSeconds;
    }
}
