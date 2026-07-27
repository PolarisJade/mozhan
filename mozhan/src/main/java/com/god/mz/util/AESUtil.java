package com.god.mz.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility for diary content.
 * Each encryption produces a base64-encoded string containing:
 * [12-byte random IV][ciphertext + 16-byte GCM authentication tag]
 */
@Component
public class AESUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;      // bytes
    private static final int GCM_TAG_LENGTH = 128;     // bits
    private static final int AES_KEY_LENGTH = 32;      // 256 bits

    private final javax.crypto.SecretKey secretKey;
    private final SecureRandom secureRandom;

    public AESUtil(@Value("${diary.aes-key}") String base64Key) throws NoSuchAlgorithmException {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            keyBytes = base64Key.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length != AES_KEY_LENGTH) {
            keyBytes = MessageDigest.getInstance("SHA-256").digest(keyBytes);
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypt plaintext with AES-256-GCM.
     *
     * @param plainText the content to encrypt
     * @return base64-encoded ciphertext (IV prepended)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext for storage
            byte[] combined = new byte[GCM_IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherText, 0, combined, GCM_IV_LENGTH, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES-256-GCM encryption failed", e);
        }
    }

    /**
     * Decrypt ciphertext encrypted with AES-256-GCM.
     *
     * @param encryptedText base64-encoded ciphertext with prepended IV
     * @return decrypted plaintext
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);
            if (combined.length < GCM_IV_LENGTH + 16) {
                throw new IllegalArgumentException("Invalid encrypted data: too short");
            }

            // Split IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES-256-GCM decryption failed", e);
        }
    }
}
