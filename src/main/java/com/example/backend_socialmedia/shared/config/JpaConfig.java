package com.example.backend_socialmedia.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuración de JPA
 * Utiliza EnableJpaRepositories de forma centralizada para evitar conflictos
 */
@Configuration
@EnableJpaRepositories(
        basePackages = {
                "com.example.backend_socialmedia.auth.infrastructure.persistence",
                "com.example.backend_socialmedia.shared.persistence",
                "com.example.backend_socialmedia.video.infrastructure.persistence",
                "com.example.backend_socialmedia.youtube.infrastructure.persistence"
        }
)
public class JpaConfig {
}







