package com.example.backend_socialmedia.auth.infrastructure.google;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Component
public class GoogleTokenVerifier {
    private static final String GOOGLE_VERIFY_URL =
            "https://oauth2.googleapis.com/tokeninfo?id_token=";

    public Map<String, Object> verify(String idToken) {
        RestTemplate restTemplate = new RestTemplate();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(
                GOOGLE_VERIFY_URL + idToken, Map.class);

        if (response == null || response.get("email") == null) {
            throw new RuntimeException("Token de Google inválido o expirado");
        }
        return response;
    }
}
