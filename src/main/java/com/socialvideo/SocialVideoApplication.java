package com.socialvideo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@EnableScheduling
@Slf4j
public class SocialVideoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialVideoApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("=================================================");
        log.info("APP READY on port {}", 
            System.getenv().getOrDefault("PORT", "8080"));
        log.info("=================================================");
    }
}