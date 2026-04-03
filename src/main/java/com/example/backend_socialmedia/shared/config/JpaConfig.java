package com.example.backend_socialmedia.shared.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "com.example.backend_socialmedia.auth.infrastructure.persistence",
        "com.example.backend_socialmedia.shared.persistence",
        "com.example.backend_socialmedia.video.infrastructure.persistence",
        "com.example.backend_socialmedia.youtube.infrastructure.persistence"
})
@EntityScan(basePackages = {
        "com.example.backend_socialmedia.auth.infrastructure.persistence",
        "com.example.backend_socialmedia.shared.persistence",
        "com.example.backend_socialmedia.video.infrastructure.persistence",
        "com.example.backend_socialmedia.youtube.infrastructure.persistence"
})
public class JpaConfig {
}

