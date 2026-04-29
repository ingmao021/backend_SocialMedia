package com.socialvideo.auth;

import com.socialvideo.auth.dto.AuthResponse;
import com.socialvideo.auth.dto.LoginRequest;
import com.socialvideo.auth.dto.RegisterRequest;
import com.socialvideo.auth.service.AuthService;
import com.socialvideo.auth.service.GoogleIdTokenService;
import com.socialvideo.auth.service.JwtService;
import com.socialvideo.exception.EmailAlreadyRegisteredException;
import com.socialvideo.exception.InvalidCredentialsException;
import com.socialvideo.user.entity.User;
import com.socialvideo.user.repository.UserRepository;
import com.socialvideo.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private GoogleIdTokenService googleIdTokenService;
    @Mock private UserService userService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .passwordHash("$2a$10$hashedPassword")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void register_withNewEmail_returnsAuthResponse() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "password123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generate(1L, "test@example.com")).thenReturn("jwt-token");
        when(userService.toUserResponse(any(User.class))).thenReturn(
                new com.socialvideo.user.dto.UserResponse(
                        1L, "test@example.com", "Test User", null,
                        true, false, 0, 2, testUser.getCreatedAt()
                )
        );

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("test@example.com");
        assertThat(response.user().name()).isEqualTo("Test User");
    }

    @Test
    void register_withExistingEmail_throwsEmailAlreadyRegistered() {
        RegisterRequest request = new RegisterRequest("Test", "existing@example.com", "password");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessageContaining("Ya existe una cuenta con ese email");
    }

    @Test
    void login_withValidCredentials_returnsAuthResponse() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtService.generate(1L, "test@example.com")).thenReturn("jwt-token");
        when(userService.toUserResponse(any(User.class))).thenReturn(
                new com.socialvideo.user.dto.UserResponse(
                        1L, "test@example.com", "Test User", null,
                        true, false, 0, 2, testUser.getCreatedAt()
                )
        );

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("test@example.com");
    }

    @Test
    void login_withInvalidCredentials_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Credenciales inválidas");
    }
}
