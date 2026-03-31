package com.example.backend_socialmedia.auth.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAuthorizationRequestResolver
        implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public CustomAuthorizationRequestResolver(
            ClientRegistrationRepository repo) {
        this.defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        repo, "/oauth2/authorization"
                );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return customize(defaultResolver.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request,
                                              String clientRegistrationId) {
        return customize(
                defaultResolver.resolve(request, clientRegistrationId)
        );
    }

    private OAuth2AuthorizationRequest customize(
            OAuth2AuthorizationRequest req) {
        if (req == null) return null;

        Map<String, Object> extra =
                new HashMap<>(req.getAdditionalParameters());
        extra.put("access_type", "offline"); // ← genera refreshToken
        extra.put("prompt", "consent");      // ← fuerza que Google lo devuelva

        return OAuth2AuthorizationRequest.from(req)
                .additionalParameters(extra)
                .build();
    }
}