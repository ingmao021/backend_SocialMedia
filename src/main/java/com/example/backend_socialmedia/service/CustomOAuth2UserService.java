package com.example.backend_socialmedia.service;

import com.example.backend_socialmedia.entity.User;
import com.example.backend_socialmedia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import static com.example.backend_socialmedia.entity.User.*;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws org.springframework.security.oauth2.core.OAuth2AuthenticationException {
        // 1. Cargamos los datos del usuario desde Google
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. Extraemos los atributos necesarios
        String googleId = oAuth2User.getAttribute("sub");
        String email    = oAuth2User.getAttribute("email");
        String name     = oAuth2User.getAttribute("name");
        String picture  = oAuth2User.getAttribute("picture");

        // 3. Lógica de Persistencia: Busca el usuario por Google ID, si no existe lo crea
        userRepository.findByGoogleId(googleId).orElseGet(() -> {
            User newUser = User.builder()
                    .googleId(googleId)
                    .email(email)
                    .name(name)
                    .picture(picture)
                    .build();

            System.out.println("🚀 Registrando nuevo usuario de Google: " + email);
            return userRepository.save(newUser);
        });

        return oAuth2User;
    }
}