package com.example.backend_socialmedia.auth.application;

import com.example.backend_socialmedia.auth.domain.User;
import com.example.backend_socialmedia.auth.domain.UserRepository;
import com.example.backend_socialmedia.shared.OAuthTokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(GoogleAuthUseCase.class);

    private final UserRepository userRepository;
    private final OAuthTokenStore tokenStore;

    public GoogleAuthUseCase(UserRepository userRepository,
                             OAuthTokenStore tokenStore) {
        this.userRepository = userRepository;
        this.tokenStore = tokenStore;
    }

    public User execute(OAuth2User oAuth2User) {
        String googleId = oAuth2User.getAttribute("sub");
        String email    = oAuth2User.getAttribute("email");
        String name     = oAuth2User.getAttribute("name");
        String picture  = oAuth2User.getAttribute("picture");

        return userRepository.findByGoogleId(googleId)
                .orElseGet(() -> {
                    log.info("Nuevo usuario registrado: email={}", email);
                    return userRepository.save(
                            new User(null, name, email, picture, googleId)
                    );
                });
    }

    public User executeWithTokens(OAuth2User oAuth2User,
                                  String accessToken,
                                  String refreshToken,
                                  long expiresInSeconds) {
        User user = execute(oAuth2User);

        tokenStore.save(
                String.valueOf(user.getId()),
                accessToken,
                refreshToken,
                expiresInSeconds
        );

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("refreshToken no recibido para userId={}. " +
                    "Revoca el acceso en myaccount.google.com/permissions " +
                    "y vuelve a loguearte", user.getId());
        } else {
            log.info("Tokens guardados correctamente para userId={}",
                    user.getId());
        }

        return user;
    }
}