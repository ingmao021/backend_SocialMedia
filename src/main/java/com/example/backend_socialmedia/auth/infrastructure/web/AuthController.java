package com.example.backend_socialmedia.auth.infrastructure.web;

import com.example.backend_socialmedia.auth.application.GoogleAuthUseCase;
import com.example.backend_socialmedia.auth.application.GetCurrentUserUseCase;
import com.example.backend_socialmedia.auth.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final GoogleAuthUseCase googleAuthUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    public AuthController(GoogleAuthUseCase googleAuthUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase) {
        this.googleAuthUseCase = googleAuthUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    // Devuelve la URL para que el frontend abra el login de Google
    @Value("${app.backend-url}")
    private String backendUrl;

    @GetMapping("/google-url")
    public ResponseEntity<Map<String, String>> getGoogleLoginUrl() {
        return ResponseEntity.ok(Map.of(
                "url", backendUrl + "/oauth2/authorization/google"
        ));
    }

    // Después del redirect de Google, devuelve el usuario autenticado
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }
        User user = getCurrentUserUseCase.execute(principal);
        return ResponseEntity.ok(Map.of(
                "id",      user.getId(),
                "name",    user.getName(),
                "email",   user.getEmail(),
                "picture", user.getPicture()
        ));
    }

    // Health check
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
                "status",  "ok",
                "message", "Backend conectado correctamente"
        ));
    }
}
