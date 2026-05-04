package com.socialvideo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Configuration
public class GoogleCloudConfig {

    @Bean
    @Lazy    // ← AGREGA ESTO
    public GoogleCredentials googleCredentials(AppProperties props) throws IOException {
        String json = props.getGcp().getSaKeyJson();
        return GoogleCredentials
            .fromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
            .createScoped("https://www.googleapis.com/auth/cloud-platform");
    }

    @Bean
    @Lazy    // ← AGREGA ESTO
    public Storage storage(GoogleCredentials googleCredentials) {
        return StorageOptions.newBuilder()
            .setCredentials(googleCredentials)
            .build()
            .getService();
    }
}