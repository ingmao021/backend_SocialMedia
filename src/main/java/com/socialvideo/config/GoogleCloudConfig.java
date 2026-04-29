package com.socialvideo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class GoogleCloudConfig {

    private final AppProperties appProperties;

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        String json = appProperties.getGcp().getSaKeyJson();
        return GoogleCredentials.fromStream(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
        ).createScoped("https://www.googleapis.com/auth/cloud-platform");
    }

    @Bean
    public Storage googleCloudStorage(GoogleCredentials credentials) {
        return StorageOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(appProperties.getGcp().getProjectId())
                .build()
                .getService();
    }
}
