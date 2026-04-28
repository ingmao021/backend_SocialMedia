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

    /**
     * Endpoint alternativo para obtener token JWT sin redirigir al frontend
     * Usado por el frontend después de completar OAuth2 con Google
     * Query Params: none (el usuario ya está autenticado en sesión)
     *
     * Respuesta: { "token": "JWT", "user": {...} }
     */
    @GetMapping("/callback/token")
    public ResponseEntity<Map<String, Object>> getCallbackToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Validar que el usuario esté autenticado
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails principal)) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "No autenticado. Debe completar OAuth2 primero."
            ));
        }

        try {
            User user = getCurrentUserUseCase.execute(principal.getUserId());
            String jwt = jwtUtils.generateToken(user.getId(), user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "token", jwt,
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "name", user.getName(),
                            "picture", user.getPicture()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Error generando token: " + e.getMessage()
            ));
        }
    }
}
