package com.example.backend_socialmedia.auth.infrastructure.google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Component
public class GoogleTokenVerifier {

    @Value("${google.token-verify-url}")
    private String googleVerifyUrl;

    public Map<String, Object> verify(String idToken) {
        RestTemplate restTemplate = new RestTemplate();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(
                googleVerifyUrl + idToken, Map.class);

        if (response == null || response.get("email") == null) {
            throw new RuntimeException("Token de Google inválido o expirado");
        }
        return response;
    }
}