package com.socialvideo.youtube.service;

import com.socialvideo.user.entity.User;
import com.socialvideo.youtube.entity.YouTubeOAuthState;
import com.socialvideo.youtube.exception.InvalidOAuthStateException;
import com.socialvideo.youtube.repository.YouTubeOAuthStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests del {@link OAuthStateService}.
 *
 * <p>El repositorio se mockea con Mockito; no hay BD ni Spring context.
 * El campo {@code ttlSeconds} (inyectado por {@code @Value} en producción)
 * se setea por reflexión, equivalente a lo que hace Spring al arrancar.</p>
 */
@ExtendWith(MockitoExtension.class)
class OAuthStateServiceTest {

    @Mock
    private YouTubeOAuthStateRepository stateRepository;

    @InjectMocks
    private OAuthStateService stateService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(stateService, "ttlSeconds", 600L);

        user = User.builder()
                .id(42L)
                .email("test@example.com")
                .name("Test User")
                .build();
    }

    @Test
    @DisplayName("generate() crea un state, lo persiste y lo devuelve")
    void generateCreatesAndPersistsState() {
        when(stateRepository.save(any(YouTubeOAuthState.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String state = stateService.generate(user);

        assertNotNull(state);
        assertFalse(state.isBlank());
        assertTrue(state.length() >= 40, "El state debe tener entropía suficiente");

        ArgumentCaptor<YouTubeOAuthState> captor =
                ArgumentCaptor.forClass(YouTubeOAuthState.class);
        verify(stateRepository).save(captor.capture());

        YouTubeOAuthState saved = captor.getValue();
        assertEquals(state, saved.getState());
        assertEquals(user, saved.getUser());
        assertNotNull(saved.getExpiresAt());
        assertTrue(saved.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Dos llamadas a generate() producen states distintos")
    void generateProducesUniqueStates() {
        when(stateRepository.save(any(YouTubeOAuthState.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String s1 = stateService.generate(user);
        String s2 = stateService.generate(user);

        assertNotEquals(s1, s2, "Cada generación debe producir un state único");
    }

    @Test
    @DisplayName("consume() válido devuelve el usuario y marca como consumido")
    void consumeValidStateReturnsUserAndMarksConsumed() {
        String state = "valid-state-token";
        YouTubeOAuthState entity = YouTubeOAuthState.builder()
                .state(state)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        when(stateRepository.findByState(state)).thenReturn(Optional.of(entity));
        when(stateRepository.save(any(YouTubeOAuthState.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User returned = stateService.consume(state);

        assertEquals(user, returned);
        assertNotNull(entity.getConsumedAt(), "El state debe quedar marcado como consumido");
        verify(stateRepository).save(entity);
    }

    @Test
    @DisplayName("consume() con state desconocido lanza InvalidOAuthStateException")
    void consumeUnknownStateThrows() {
        when(stateRepository.findByState("unknown")).thenReturn(Optional.empty());

        assertThrows(InvalidOAuthStateException.class,
                () -> stateService.consume("unknown"));
    }

    @Test
    @DisplayName("consume() con state ya consumido lanza InvalidOAuthStateException")
    void consumeAlreadyConsumedStateThrows() {
        String state = "already-used";
        YouTubeOAuthState entity = YouTubeOAuthState.builder()
                .state(state)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(300))
                .consumedAt(Instant.now().minusSeconds(30)) // ya consumido
                .build();

        when(stateRepository.findByState(state)).thenReturn(Optional.of(entity));

        assertThrows(InvalidOAuthStateException.class,
                () -> stateService.consume(state));

        // No debe re-guardar
        verify(stateRepository, never()).save(any());
    }

    @Test
    @DisplayName("consume() con state expirado lanza InvalidOAuthStateException")
    void consumeExpiredStateThrows() {
        String state = "expired-state";
        YouTubeOAuthState entity = YouTubeOAuthState.builder()
                .state(state)
                .user(user)
                .expiresAt(Instant.now().minusSeconds(60)) // expiró hace 60s
                .build();

        when(stateRepository.findByState(state)).thenReturn(Optional.of(entity));

        assertThrows(InvalidOAuthStateException.class,
                () -> stateService.consume(state));

        verify(stateRepository, never()).save(any());
    }

    @Test
    @DisplayName("consume() con state null o vacío lanza InvalidOAuthStateException")
    void consumeNullOrBlankStateThrows() {
        assertThrows(InvalidOAuthStateException.class, () -> stateService.consume(null));
        assertThrows(InvalidOAuthStateException.class, () -> stateService.consume(""));
        assertThrows(InvalidOAuthStateException.class, () -> stateService.consume("   "));
    }
}