package com.socialvideo.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.socialvideo.exception.InvalidGoogleTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleIdTokenService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenService(@Value("${app.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GooglePayload verify(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new InvalidGoogleTokenException("Token de Google inválido o expirado");
            }
            Payload p = token.getPayload();
            return new GooglePayload(
                    p.getSubject(),
                    p.getEmail(),
                    (String) p.get("name"),
                    (String) p.get("picture")
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidGoogleTokenException("Error verificando token de Google");
        }
    }

    public record GooglePayload(String sub, String email, String name, String picture) {}
}
