package com.example.backend_socialmedia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(
		basePackages = {
				"com.example.backend_socialmedia.auth.infrastructure.persistence",
				"com.example.backend_socialmedia.shared.persistence",
				"com.example.backend_socialmedia.video.infrastructure.persistence",
				"com.example.backend_socialmedia.youtube.infrastructure.persistence"
		}
)
public class BackendSocialMediaApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendSocialMediaApplication.class, args);
	}

}
