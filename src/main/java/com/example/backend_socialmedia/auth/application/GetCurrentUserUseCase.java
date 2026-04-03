package com.example.backend_socialmedia.auth.application;

import com.example.backend_socialmedia.auth.domain.User;
import com.example.backend_socialmedia.auth.domain.UserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class GetCurrentUserUseCase {
    private final UserRepository userRepository;

    public GetCurrentUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(OAuth2User principal) {
        String googleId = principal.getAttribute("sub");
        return userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public User execute(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}
