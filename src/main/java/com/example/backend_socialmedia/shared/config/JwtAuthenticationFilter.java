package com.example.backend_socialmedia.shared.config;

import com.example.backend_socialmedia.shared.utils.JwtUtils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Filtro JWT para autenticación basada en tokens.
 *
 * Soporta dos fuentes de JWT:
 * 1. Authorization header: "Bearer TOKEN"
 * 2. Cookies httpOnly: access_token
 *
 * Responsabilidades:
 * - Extraer JWT de Authorization header o cookies
 * - Validar JWT
 * - Establecer autenticación en SecurityContext
 * - Manejar errores de token (expirado, inválido)
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extraer JWT desde Authorization header o cookie
            String jwt = extractJwtFromRequest(request);

            if (jwt == null) {
                // Sin JWT, continuar sin autenticación
                filterChain.doFilter(request, response);
                return;
            }

            // Procesar JWT
            authenticateWithJwt(request, jwt);

        } catch (ExpiredJwtException e) {
            log.warn("JWT expirado en {}: usuario debe refrescar token", request.getRequestURI());
            sendUnauthorized(response, "TOKEN_EXPIRED", "El token ha expirado");
            return;
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("JWT inválido en {}: {}", request.getRequestURI(), e.getMessage());
            sendUnauthorized(response, "INVALID_TOKEN", "Token de autenticación inválido");
            return;
        } catch (Exception e) {
            log.error("Error inesperado en JWT filter: {}", e.getMessage(), e);
            sendUnauthorized(response, "AUTH_ERROR", "Error de autenticación");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae JWT desde Authorization header o cookies
     * Prioridad:
     * 1. Authorization: Bearer <token>
     * 2. Cookie: access_token
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        // Intenta extraer desde Authorization header
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        // Intenta extraer desde cookies
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> ACCESS_TOKEN_COOKIE.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    /**
     * Autentica al usuario usando el JWT
     */
    private void authenticateWithJwt(HttpServletRequest request, String jwt) {
        // Validar JWT
        if (!jwtUtils.validateToken(jwt)) {
            log.debug("JWT no válido en request a {}", request.getRequestURI());
            return;
        }

        // Validar que sea un access token (no refresh token)
        String tokenType = jwtUtils.getTokenType(jwt);
        if (!"access".equals(tokenType)) {
            log.warn("Se envió token de tipo {} cuando se esperaba access token", tokenType);
            return;
        }

        // Extraer email y userId
        String userEmail = jwtUtils.getEmailFromToken(jwt);
        Long userId = jwtUtils.getUserIdFromToken(jwt);

        if (userEmail == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        // Cargar detalles del usuario
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            // Crear token de autenticación
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Establecer autenticación
            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("Usuario autenticado: email={}, userId={}", userEmail, userId);

        } catch (Exception e) {
            log.error("Error cargando usuario {} desde token: {}", userEmail, e.getMessage());
        }
    }

    /**
     * Envía respuesta de error 401 Unauthorized
     */
    private void sendUnauthorized(HttpServletResponse response, String errorCode, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"code\":\"%s\",\"message\":\"%s\"}"
                        .formatted(errorCode, message)
        );
    }
}
