package com.socialvideo.auth.service;

import com.socialvideo.auth.dto.AuthResponse;
import com.socialvideo.auth.dto.LoginRequest;
import com.socialvideo.auth.dto.RegisterRequest;
import com.socialvideo.auth.service.GoogleIdTokenService.GooglePayload;
import com.socialvideo.exception.EmailAlreadyRegisteredException;
import com.socialvideo.exception.InvalidCredentialsException;
import com.socialvideo.user.entity.User;
import com.socialvideo.user.repository.UserRepository;
import com.socialvideo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdTokenService googleIdTokenService;
    private final UserService userService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException("Ya existe una cuenta con ese email");
        }

        User user = userRepository.save(User.builder()
                .email(request.email())
                .name(request.name())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build());

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales inválidas"));

        if (user.getPasswordHash() == null ||
                !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Credenciales inválidas");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse googleLogin(String idToken) {
        GooglePayload p = googleIdTokenService.verify(idToken);

        User user = userRepository.findByGoogleId(p.sub())
                .or(() -> userRepository.findByEmail(p.email()).map(existing -> {
                    // Vinculación automática de cuentas
                    existing.setGoogleId(p.sub());
                    if (existing.getAvatarUrl() == null) {
                        existing.setAvatarUrl(p.picture());
                    }
                    return userRepository.save(existing);
                }))
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(p.email())
                        .name(p.name() != null ? p.name() : p.email())
                        .googleId(p.sub())
                        .avatarUrl(p.picture())
                        .build()));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String jwt = jwtService.generate(user.getId(), user.getEmail());
        return new AuthResponse(jwt, userService.toUserResponse(user));
    }
}
