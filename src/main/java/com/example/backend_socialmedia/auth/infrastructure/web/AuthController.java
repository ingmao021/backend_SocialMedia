package com.example.backend_socialmedia.auth.infrastructure.web;

import com.example.backend_socialmedia.auth.application.GoogleAuthUseCase;
import com.example.backend_socialmedia.auth.application.GetCurrentUserUseCase;
import com.example.backend_socialmedia.auth.domain.User;
import com.example.backend_socialmedia.shared.utils.JwtUtils;
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
    private final JwtUtils jwtUtils;

    public AuthController(GoogleAuthUseCase googleAuthUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase,
                          JwtUtils jwtUtils) {
        this.googleAuthUseCase = googleAuthUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.jwtUtils = jwtUtils;
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
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }

        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Token inválido"));
        }

        Long userId = jwtUtils.getUserIdFromToken(token);
        User user = getCurrentUserUseCase.execute(userId);

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

    // Debug: Verificar token sin extraer usuario
    @GetMapping("/debug/token")
    public ResponseEntity<Map<String, Object>> debugToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "No autenticado",
                    "detail", "No se proporciona header Authorization con formato Bearer"
            ));
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtils.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of(
                        "error", "Token inválido",
                        "detail", "El token no pasó validación"
                ));
            }

            Long userId = jwtUtils.getUserIdFromToken(token);
            String email = jwtUtils.getEmailFromToken(token);

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "userId", userId,
                    "email", email,
                    "message", "Token válido, pero puede que el usuario no esté en la BD"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Error al procesar token",
                    "detail", e.getMessage()
            ));
        }
    }
}
