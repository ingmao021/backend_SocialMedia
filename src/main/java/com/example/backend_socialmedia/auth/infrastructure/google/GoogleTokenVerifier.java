package com.example.backend_socialmedia.auth.infrastructure.google;

import com.example.backend_socialmedia.shared.exception.InvalidGoogleTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Component
public class GoogleTokenVerifier {

    private static final Logger log =
            LoggerFactory.getLogger(GoogleTokenVerifier.class);

    @Value("${google.token-verify-url}")
    private String googleVerifyUrl;

    public Map<String, Object> verify(String idToken) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(
                    googleVerifyUrl + idToken, Map.class);

            if (response == null || response.get("email") == null) {
                throw new InvalidGoogleTokenException(
                        "Token de Google inválido o expirado");
            }

            log.debug("Token de Google verificado para email={}",
                    response.get("email"));
            return response;

        } catch (InvalidGoogleTokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error verificando token de Google: {}", e.getMessage());
            throw new InvalidGoogleTokenException(
                    "Error al verificar token de Google: " + e.getMessage());
        }
    }
}