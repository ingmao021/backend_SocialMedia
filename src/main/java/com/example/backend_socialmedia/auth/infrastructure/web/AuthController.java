package com.example.backend_socialmedia.auth.infrastructure.web;

import com.example.backend_socialmedia.auth.application.GetCurrentUserUseCase;
import com.example.backend_socialmedia.auth.domain.User;
import com.example.backend_socialmedia.shared.config.CustomUserDetails;
import com.example.backend_socialmedia.shared.exception.UnauthorizedException;
import com.example.backend_socialmedia.shared.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final JwtUtils jwtUtils;

    @Value("${app.backend-url}")
    private String backendUrl;

    public AuthController(GetCurrentUserUseCase getCurrentUserUseCase, JwtUtils jwtUtils) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.jwtUtils = jwtUtils;
    }

    @GetMapping("/google-url")
    public ResponseEntity<Map<String, String>> getGoogleLoginUrl() {
        return ResponseEntity.ok(Map.of("url", backendUrl + "/oauth2/authorization/google"));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        User user = getCurrentUserUseCase.execute(principal.getUserId());
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "picture", user.getPicture()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Backend conectado correctamente"
        ));
    }

    @GetMapping("/debug/token")
    public ResponseEntity<Map<String, Object>> debugToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of(
                    "valid", false,
                    "error", "No se proporcionó Authorization header con formato Bearer"
            ));
        }
        String token = authHeader.substring(7);
        try {
            if (!jwtUtils.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of("valid", false, "error", "Token inválido o expirado"));
            }
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "userId", jwtUtils.getUserIdFromToken(token),
                    "email", jwtUtils.getEmailFromToken(token)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("valid", false, "error", e.getMessage()));
        }
    }
}
