package com.example.backend_socialmedia.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    // URL del frontend a donde redirigir tras login exitoso
    private static final String FRONTEND_REDIRECT_URL = "http://localhost:5173/chat";

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        response.sendRedirect(FRONTEND_REDIRECT_URL);
    }
}