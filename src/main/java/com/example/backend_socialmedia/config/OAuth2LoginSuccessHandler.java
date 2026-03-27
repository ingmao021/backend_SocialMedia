package com.example.backend_socialmedia.config;

import com.example.backend_socialmedia.service.CustomOAuth2UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    // Cambiamos UserService por tu servicio real
    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // Redirigimos al frontend (puerto 5173) tras el éxito del login
        String targetUrl = "http://localhost:5173/chat";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}