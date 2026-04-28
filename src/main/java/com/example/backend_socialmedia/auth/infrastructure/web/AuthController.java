package com.example.backend_socialmedia.auth.infrastructure.web;

import com.example.backend_socialmedia.auth.application.GetCurrentUserUseCase;
import com.example.backend_socialmedia.auth.domain.User;
import com.example.backend_socialmedia.shared.config.CustomUserDetails;
import com.example.backend_socialmedia.shared.exception.UnauthorizedException;
import com.example.backend_socialmedia.shared.utils.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador de autenticación.
 *
 * Endpoints:
 * - GET /api/auth/status: Verificar que el backend está vivo
 * - GET /api/auth/google-url: Obtener URL de inicio de OAuth2 con Google
 * - GET /api/auth/me: Obtener usuario autenticado actual
 * - POST /api/auth/refresh: Refrescar JWT access token
 * - POST /api/auth/logout: Cerrar sesión
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final JwtUtils jwtUtils;

    @Value("${app.backend-url}")
    private String backendUrl;

    @Value("${app.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${app.cookie-same-site:Strict}")
    private String cookieSameSite;

    public AuthController(GetCurrentUserUseCase getCurrentUserUseCase, JwtUtils jwtUtils) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Obtiene la URL de inicio de OAuth2 con Google
     * @return URL para redirigir al usuario a Google OAuth
     */
    @GetMapping("/google-url")
    public ResponseEntity<Map<String, String>> getGoogleLoginUrl() {
        String googleAuthUrl = backendUrl + "/oauth2/authorization/google";
        log.debug("Retornando Google OAuth URL");
        return ResponseEntity.ok(Map.of("url", googleAuthUrl));
    }

    /**
     * Obtiene la información del usuario autenticado actual
     * @return Datos del usuario (id, email, name, picture)
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails principal)) {
            log.warn("Intento de acceso /me sin autenticación");
            throw new UnauthorizedException("Usuario no autenticado");
        }

        User user = getCurrentUserUseCase.execute(principal.getUserId());

        log.debug("Retornando datos de usuario: email={}", user.getEmail());
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "picture", user.getPicture()
        ));
    }

    /**
     * Verifica el estado del backend
     * @return Estado OK si el servidor está vivo
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Backend conectado correctamente"
        ));
    }

    /**
     * Refresca el access token utilizando el refresh token
     * El refresh token debe estar en la cookie 'refresh_token'
     *
     * @param response Para enviar el nuevo access token en cookie
     * @return Confirmación de refresh exitoso
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshAccessToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Intento de refresh sin refresh token");
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "MISSING_REFRESH_TOKEN",
                    "message", "Refresh token no encontrado o expirado"
            ));
        }

        try {
            // Validar que sea un refresh token
            if (!jwtUtils.validateToken(refreshToken)) {
                log.warn("Refresh token inválido o expirado");
                clearAuthCookies(response);
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "error", "INVALID_REFRESH_TOKEN",
                        "message", "Refresh token ha expirado"
                ));
            }

            String tokenType = jwtUtils.getTokenType(refreshToken);
            if (!"refresh".equals(tokenType)) {
                log.warn("Token enviado como refresh no es de tipo refresh: type={}", tokenType);
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "error", "WRONG_TOKEN_TYPE",
                        "message", "Token de tipo incorrecto"
                ));
            }

            // Generar nuevo access token
            Long userId = jwtUtils.getUserIdFromToken(refreshToken);
            String email = jwtUtils.getEmailFromToken(refreshToken);
            String newAccessToken = jwtUtils.generateAccessToken(userId, email);

            // Guardar nuevo access token en cookie
            Cookie accessTokenCookie = createSecureCookie(
                    "access_token",
                    newAccessToken,
                    (int) (jwtUtils.getAccessTokenExpirationMs() / 1000)
            );
            response.addCookie(accessTokenCookie);

            log.info("Access token refrescado para userId={}", userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Token refrescado exitosamente"
            ));

        } catch (Exception e) {
            log.error("Error refrescando token: {}", e.getMessage(), e);
            clearAuthCookies(response);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "REFRESH_ERROR",
                    "message", "Error interno al refrescar token"
            ));
        }
    }

    /**
     * Cierra la sesión del usuario (logout)
     * @param response Para limpiar las cookies de autenticación
     * @return Confirmación de logout exitoso
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        clearAuthCookies(response);
        log.info("Usuario desconectado");
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sesión cerrada correctamente"
        ));
    }

    /**
     * DEBUG ONLY: Endpoint para verificar validez de token (SOLO DESARROLLO)
     * En producción, eliminar este endpoint o protegerlo contra abuso
     *
     * @param authHeader Authorization header con formato "Bearer TOKEN"
     * @return Información del token (válido/inválido, claims)
     */
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
                return ResponseEntity.status(401).body(Map.of(
                        "valid", false,
                        "error", "Token inválido o expirado"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "userId", jwtUtils.getUserIdFromToken(token),
                    "email", jwtUtils.getEmailFromToken(token),
                    "type", jwtUtils.getTokenType(token)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "valid", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Crea una cookie segura (httpOnly, Secure, SameSite)
     */
    private Cookie createSecureCookie(String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", cookieSameSite);
        return cookie;
    }

    /**
     * Limpia las cookies de autenticación (logout)
     */
    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessTokenCookie = new Cookie("access_token", "");
        accessTokenCookie.setMaxAge(0);
        accessTokenCookie.setPath("/");
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refresh_token", "");
        refreshTokenCookie.setMaxAge(0);
        refreshTokenCookie.setPath("/");
        response.addCookie(refreshTokenCookie);
    }
}
