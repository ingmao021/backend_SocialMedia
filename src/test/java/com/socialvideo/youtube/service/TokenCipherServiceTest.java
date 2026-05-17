package com.socialvideo.youtube.service;

import com.socialvideo.youtube.exception.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del {@link TokenCipherService}.
 *
 * <p>Cubre los casos críticos:</p>
 * <ul>
 *   <li>Round-trip básico (encrypt → decrypt devuelve el plaintext original).</li>
 *   <li>Cada cifrado produce un IV distinto (no determinismo de IV).</li>
 *   <li>Cifrados distintos del MISMO plaintext son distintos entre sí.</li>
 *   <li>Manipulación del ciphertext es detectada (integridad GCM).</li>
 *   <li>Manipulación del IV es detectada.</li>
 *   <li>IV de tamaño incorrecto se rechaza.</li>
 *   <li>Clave maestra inválida en arranque hace fallar la inicialización.</li>
 * </ul>
 *
 * <p>No usa Spring: instancia el service directamente. El método package-private
 * {@code validateKey()} se invoca manualmente para reproducir el comportamiento
 * de {@code @PostConstruct}.</p>
 */
class TokenCipherServiceTest {

    /** Clave AES-256 random, generada una vez para todos los tests de esta clase. */
    private static final String VALID_KEY = generateRandomAesKey();

    private TokenCipherService cipher;

    @BeforeEach
    void setUp() {
        cipher = new TokenCipherService(VALID_KEY);
        cipher.validateKey();
    }

    @Test
    @DisplayName("encrypt + decrypt devuelve el plaintext original")
    void roundTripPreservesPlaintext() {
        String plaintext = "1//0gXyZAbc-DefGhi_jklmNoPqRsTuVwXyZ12345";

        TokenCipherService.CipherResult enc = cipher.encrypt(plaintext);
        String decrypted = cipher.decrypt(enc.cipherBase64(), enc.ivBase64());

        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("Cifrar dos veces el mismo plaintext produce ciphertexts distintos (IV único)")
    void encryptingSamePlaintextTwiceProducesDifferentCiphertexts() {
        String plaintext = "refresh_token_de_ejemplo";

        TokenCipherService.CipherResult enc1 = cipher.encrypt(plaintext);
        TokenCipherService.CipherResult enc2 = cipher.encrypt(plaintext);

        assertNotEquals(enc1.cipherBase64(), enc2.cipherBase64(),
                "Dos cifrados del mismo plaintext deben diferir (IV aleatorio)");
        assertNotEquals(enc1.ivBase64(), enc2.ivBase64(),
                "Cada cifrado debe generar un IV distinto");
    }

    @Test
    @DisplayName("Descifrar plaintext vacío funciona correctamente")
    void encryptsAndDecryptsEmptyString() {
        TokenCipherService.CipherResult enc = cipher.encrypt("");
        String decrypted = cipher.decrypt(enc.cipherBase64(), enc.ivBase64());

        assertEquals("", decrypted);
    }

    @Test
    @DisplayName("Manipular el ciphertext lanza EncryptionException (integridad GCM)")
    void tamperedCiphertextIsRejected() {
        TokenCipherService.CipherResult enc = cipher.encrypt("payload sensible");

        // Alteramos un carácter del ciphertext en Base64
        char[] chars = enc.cipherBase64().toCharArray();
        chars[0] = (chars[0] == 'A') ? 'B' : 'A';
        String tampered = new String(chars);

        assertThrows(EncryptionException.class,
                () -> cipher.decrypt(tampered, enc.ivBase64()));
    }

    @Test
    @DisplayName("Manipular el IV lanza EncryptionException")
    void tamperedIvIsRejected() {
        TokenCipherService.CipherResult enc = cipher.encrypt("payload sensible");

        // Generamos un IV distinto, mismo tamaño
        byte[] otherIv = new byte[12];
        new SecureRandom().nextBytes(otherIv);
        String tamperedIv = Base64.getEncoder().encodeToString(otherIv);

        assertThrows(EncryptionException.class,
                () -> cipher.decrypt(enc.cipherBase64(), tamperedIv));
    }

    @Test
    @DisplayName("Descifrar con IV de tamaño incorrecto lanza EncryptionException")
    void invalidIvLengthIsRejected() {
        TokenCipherService.CipherResult enc = cipher.encrypt("payload");

        byte[] shortIv = new byte[8]; // GCM espera 12, no 8
        String invalidIv = Base64.getEncoder().encodeToString(shortIv);

        EncryptionException ex = assertThrows(EncryptionException.class,
                () -> cipher.decrypt(enc.cipherBase64(), invalidIv));
        assertTrue(ex.getMessage().toLowerCase().contains("iv"));
    }

    @Test
    @DisplayName("encrypt rechaza plaintext null")
    void encryptRejectsNull() {
        assertThrows(EncryptionException.class, () -> cipher.encrypt(null));
    }

    @Test
    @DisplayName("decrypt rechaza ciphertext o IV null")
    void decryptRejectsNull() {
        assertThrows(EncryptionException.class, () -> cipher.decrypt(null, "iv"));
        assertThrows(EncryptionException.class, () -> cipher.decrypt("cipher", null));
    }

    @Test
    @DisplayName("Clave maestra ausente hace fallar validateKey()")
    void missingKeyFailsAtStartup() {
        TokenCipherService bad = new TokenCipherService(null);
        assertThrows(IllegalStateException.class, bad::validateKey);
    }

    @Test
    @DisplayName("Clave maestra de tamaño incorrecto hace fallar validateKey()")
    void wrongSizeKeyFailsAtStartup() {
        // 16 bytes (AES-128) en vez de 32 (AES-256)
        byte[] shortKey = new byte[16];
        new SecureRandom().nextBytes(shortKey);
        String shortKeyBase64 = Base64.getEncoder().encodeToString(shortKey);

        TokenCipherService bad = new TokenCipherService(shortKeyBase64);
        IllegalStateException ex = assertThrows(IllegalStateException.class, bad::validateKey);
        assertTrue(ex.getMessage().contains("32 bytes"));
    }

    @Test
    @DisplayName("Clave maestra con Base64 inválido hace fallar validateKey()")
    void invalidBase64KeyFailsAtStartup() {
        TokenCipherService bad = new TokenCipherService("esto-no-es-base64-valido!!!");
        assertThrows(IllegalStateException.class, bad::validateKey);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static String generateRandomAesKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}