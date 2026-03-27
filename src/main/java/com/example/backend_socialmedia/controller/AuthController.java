package com.example.backend_socialmedia.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/google-url")
    public ResponseEntity<Map<String, String>> getGoogleLoginUrl() {
        String googleOAuthUrl = "http://localhost:8080/oauth2/authorization/google";
        return ResponseEntity.ok(Map.of("url", googleOAuthUrl));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Backend conectado correctamente"
        ));
    }
}