package com.example.backend_socialmedia.shared.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de escaneo de entidades JPA
 *
 * @EnableJpaRepositories está en BackendSocialMediaApplication para evitar
 * conflictos con la auto-configuración de Spring Boot
 */
@Configuration
@EntityScan(
        basePackages = {
                "com.example.backend_socialmedia.auth.infrastructure.persistence",
                "com.example.backend_socialmedia.shared.persistence",
                "com.example.backend_socialmedia.video.infrastructure.persistence",
                "com.example.backend_socialmedia.youtube.infrastructure.persistence"
        }
)
public class JpaConfig {
}







