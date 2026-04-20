package com.seichou.logos.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ApiKeyCryptoService {

    private static final String CIPHER_PREFIX = "enc:v1:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyCryptoService(@Value("${application.security.crypto.secret-key}") String secret) {
        this.keySpec = new SecretKeySpec(deriveKey(secret), "AES");
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(CIPHER_PREFIX);
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        String normalized = plainText.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(normalized.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);

            return CIPHER_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt API key", e);
        }
    }

    public String decryptIfNeeded(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        if (!isEncrypted(stored)) {
            return stored;
        }

        try {
            byte[] payload = Base64.getDecoder().decode(stored.substring(CIPHER_PREFIX.length()));
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[payload.length - GCM_IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(payload, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt API key", e);
        }
    }

    private byte[] deriveKey(String secret) {
        String normalized = (secret == null || secret.isBlank())
                ? "dev-only-unsafe-secret-change-me"
                : secret;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive encryption key", e);
        }
    }
}

