package com.socialvideo.youtube.service;

import com.socialvideo.youtube.exception.EncryptionException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Servicio de cifrado simétrico para tokens sensibles (refresh tokens de OAuth).
 *
 * <p>Algoritmo: <b>AES-256-GCM</b> (cifrado autenticado, recomendado por NIST
 * SP 800-38D). Cada operación de cifrado genera un IV (vector de inicialización)
 * único de 96 bits con {@link SecureRandom}; reutilizar un IV con la misma clave
 * rompe la seguridad de GCM y debe evitarse siempre.</p>
 *
 * <p>La clave maestra se inyecta desde {@code app.crypto.token-encryption-key}
 * (Base64 de 32 bytes = 256 bits). Si la clave no está presente o tiene un
 * tamaño incorrecto, la aplicación falla al arrancar mediante {@link PostConstruct}.</p>
 *
 * <p><b>Garantías de seguridad:</b></p>
 * <ul>
 *   <li>Confidencialidad: solo quien tenga la clave puede leer el plaintext.</li>
 *   <li>Integridad: GCM detecta cualquier manipulación del ciphertext o del IV
 *       (tag de autenticación de 128 bits).</li>
 *   <li>No reutilización de IV: cada llamada a {@link #encrypt(String)} genera
 *       un IV criptográficamente aleatorio.</li>
 * </ul>
 *
 * <p><b>Lo que esta clase NO hace:</b> NUNCA loguea claves, IVs ni ciphertexts.
 * Los mensajes de error son genéricos para no facilitar criptoanálisis.</p>
 */
@Service
@Slf4j
public class TokenCipherService {

    /** AES-256 requiere clave de exactamente 32 bytes. */
    private static final int AES_KEY_BYTES = 32;

    /** GCM recomendado: IV de 12 bytes (96 bits). Estándar NIST. */
    private static final int GCM_IV_BYTES = 12;

    /** Tag de autenticación: 128 bits (máximo permitido por GCM). */
    private static final int GCM_TAG_BITS = 128;

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";

    private final String keyBase64;
    private final SecureRandom secureRandom;

    /** La clave decodificada se cachea tras {@link #validateKey()}. */
    private byte[] keyBytes;

    public TokenCipherService(@Value("${app.crypto.token-encryption-key}") String keyBase64) {
        this.keyBase64 = keyBase64;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Valida la clave al arrancar la app (fail-fast). Si la clave no es
     * Base64 válido o no decodifica a exactamente 32 bytes, lanza una
     * excepción que impide el inicio.
     */
    @PostConstruct
    void validateKey() {
        if (keyBase64 == null || keyBase64.isBlank()) {
            throw new IllegalStateException(
                    "app.crypto.token-encryption-key no está configurada. " +
                            "Genera una clave con: openssl rand -base64 32");
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(keyBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "app.crypto.token-encryption-key no es un Base64 válido", e);
        }

        if (decoded.length != AES_KEY_BYTES) {
            throw new IllegalStateException(
                    "app.crypto.token-encryption-key debe decodificar a " +
                            AES_KEY_BYTES + " bytes (256 bits) pero son " + decoded.length);
        }

        this.keyBytes = decoded;
        log.info("TokenCipherService inicializado correctamente (AES-256-GCM)");
    }

    /**
     * Cifra un texto plano y devuelve el ciphertext y el IV, ambos en Base64.
     *
     * @param plaintext texto plano a cifrar (no puede ser null)
     * @return objeto con {@code cipherBase64} e {@code ivBase64}, listos para persistir
     * @throws EncryptionException si ocurre cualquier fallo criptográfico
     */
    public CipherResult encrypt(String plaintext) {
        if (plaintext == null) {
            throw new EncryptionException("No se puede cifrar un valor nulo");
        }

        byte[] iv = new byte[GCM_IV_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(keyBytes, KEY_ALGORITHM),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return new CipherResult(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(iv)
            );
        } catch (GeneralSecurityException e) {
            // No revelamos detalles internos del error criptográfico
            throw new EncryptionException("Error al cifrar el token", e);
        }
    }

    /**
     * Descifra un ciphertext + IV (ambos en Base64) y devuelve el texto plano.
     *
     * <p>Si la verificación de integridad de GCM falla (tag incorrecto, ciphertext
     * manipulado, IV equivocado o clave distinta a la que cifró), se lanza
     * {@link EncryptionException}. El mensaje NO indica cuál de estos casos ocurrió.</p>
     *
     * @param cipherBase64 ciphertext en Base64
     * @param ivBase64     IV en Base64 (12 bytes decodificados)
     * @return texto plano original
     * @throws EncryptionException si el descifrado o la verificación de integridad fallan
     */
    public String decrypt(String cipherBase64, String ivBase64) {
        if (cipherBase64 == null || ivBase64 == null) {
            throw new EncryptionException("Ciphertext o IV nulos");
        }

        byte[] ciphertext;
        byte[] iv;
        try {
            ciphertext = Base64.getDecoder().decode(cipherBase64);
            iv = Base64.getDecoder().decode(ivBase64);
        } catch (IllegalArgumentException e) {
            throw new EncryptionException("Formato Base64 inválido", e);
        }

        if (iv.length != GCM_IV_BYTES) {
            throw new EncryptionException("Longitud de IV inválida");
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(keyBytes, KEY_ALGORITHM),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Error al descifrar el token", e);
        }
    }

    /**
     * Resultado de cifrar: el ciphertext y el IV, ambos en Base64, listos
     * para persistir tal cual en las columnas {@code refresh_token_cipher}
     * y {@code refresh_token_iv} de la tabla {@code youtube_connections}.
     */
    public record CipherResult(String cipherBase64, String ivBase64) {}
}