package com.socialvideo.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Cors cors = new Cors();
    private Jwt jwt = new Jwt();
    private Google google = new Google();
    private Gcp gcp = new Gcp();
    private Vertex vertex = new Vertex();
    private SignedUrl signedUrl = new SignedUrl();
    private Video video = new Video();
    private Youtube youtube = new Youtube();
    private Crypto crypto = new Crypto();

    @Getter @Setter
    public static class Youtube {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String scopes = "...";
        private int uploadChunkSizeBytes = 10 * 1024 * 1024;
        private int uploadTimeoutMinutes = 30;
        private int oauthStateTtlSeconds = 600;
        private boolean verified = false;
    }

    @Getter @Setter
    public static class Crypto {
        private String tokenEncryptionKey;
    }

    @Getter @Setter
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";
    }

    @Getter @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000; // 24h
    }

    @Getter @Setter
    public static class Google {
        private String clientId;
    }

    @Getter @Setter
    public static class Gcp {
        private String projectId;
        private String location = "us-central1";
        private String bucket;
        private String saKeyJson;
    }

    @Getter @Setter
    public static class Vertex {
        private String veoModelId = "veo-3.1-lite-generate-preview";
        private int pollingIntervalSeconds = 15;
        private int jobTimeoutMinutes = 10;
    }

    @Getter @Setter
    public static class SignedUrl {
        private int ttlDays = 7;
        private int avatarTtlDays = 30;
    }

    @Getter @Setter
    public static class Video {
        private int maxCompleted = 2;
    }
}


