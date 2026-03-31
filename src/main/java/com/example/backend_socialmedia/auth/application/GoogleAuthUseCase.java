package com.example.backend_socialmedia.auth.application;

import com.example.backend_socialmedia.auth.domain.User;
import com.example.backend_socialmedia.auth.domain.UserRepository;
import com.example.backend_socialmedia.shared.OAuthTokenStore;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthUseCase {

    private final UserRepository userRepository;
    private final OAuthTokenStore tokenStore;  // ← nuevo

    public GoogleAuthUseCase(UserRepository userRepository,
                             OAuthTokenStore tokenStore) {
        this.userRepository = userRepository;
        this.tokenStore = tokenStore;
    }

    // Firma original que ya tienes — sin cambios para no romper nada
    public User execute(OAuth2User oAuth2User) {
        String googleId = oAuth2User.getAttribute("sub");
        String email    = oAuth2User.getAttribute("email");
        String name     = oAuth2User.getAttribute("name");
        String picture  = oAuth2User.getAttribute("picture");

        return userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.save(
                        new User(null, name, email, picture, googleId)
                ));
    }

    // Firma nueva que llama el OAuth2SuccessHandler con los tokens
    public User executeWithTokens(OAuth2User oAuth2User,
                                  String accessToken,
                                  String refreshToken,
                                  long expiresInSeconds) {
        User user = execute(oAuth2User); // reutiliza la lógica existente

        // Guarda los tokens ligados al userId
        tokenStore.save(String.valueOf(user.getId()), accessToken, refreshToken, expiresInSeconds);

        if (refreshToken == null || refreshToken.isBlank()) {
            System.err.println("[WARN] refreshToken no recibido para userId="
                    + user.getId()
                    + " — revoca el acceso en myaccount.google.com/permissions y vuelve a loguearte");
        }

        return user;
    }
}