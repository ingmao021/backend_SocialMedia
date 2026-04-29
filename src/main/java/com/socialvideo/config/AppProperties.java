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
