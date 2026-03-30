package com.example.backend_socialmedia.auth.application;

import com.example.backend_socialmedia.auth.domain.User;
import com.example.backend_socialmedia.auth.domain.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthUseCase {
    private final UserRepository userRepository;

    public GoogleAuthUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(OAuth2User oAuth2User) {
        String googleId = oAuth2User.getAttribute("sub");
        String email    = oAuth2User.getAttribute("email");
        String name     = oAuth2User.getAttribute("name");
        String picture  = oAuth2User.getAttribute("picture");

        // Si el usuario ya existe lo devuelve, si no lo crea
        return userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.save(
                        new User(null, name, email, picture, googleId)
                ));
    }
}
