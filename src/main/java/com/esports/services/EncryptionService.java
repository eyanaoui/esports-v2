package com.esports.services;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.Base64;

/**
 * Encryption service for securing sensitive data using AES-256-GCM encryption.
 * 
 * This service provides encryption and decryption capabilities for OAuth tokens
 * and other sensitive data. It uses AES-256-GCM (Galois/Counter Mode) for
 * authenticated encryption with unique IVs for each operation.
 * 
 * Security features:
 * - AES-256-GCM authenticated encryption
 * - Unique IV (Initialization Vector) for each encryption operation
 * - Key stored in Java KeyStore (JCEKS format)
 * - IV prepended to ciphertext for decryption
 */
public class EncryptionService {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // 128 bits authentication tag
    private static final int AES_KEY_SIZE = 256; // 256 bits
    private static final String KEYSTORE_TYPE = "JCEKS";
    private static final String KEY_ALIAS = "esports-encryption-key";
    private static final String KEYSTORE_FILENAME = "esports-keystore.jceks";
    
    private SecretKey secretKey;
    private SecureRandom secureRandom;
    
    /**
     * Initialize the encryption service.
     * Loads or generates the encryption key from the keystore.
     */
    public EncryptionService() {
        try {
            this.secureRandom = new SecureRandom();
            this.secretKey = loadOrGenerateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize EncryptionService", e);
        }
    }
    
    /**
     * Encrypt plaintext using AES-256-GCM.
     * 
     * @param plaintext The text to encrypt
     * @return Base64-encoded string containing IV + ciphertext
     * @throws IllegalArgumentException if plaintext is null or empty
     * @throws RuntimeException if encryption fails
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext cannot be null or empty");
        }
        
        try {
            // Generate unique IV for this encryption operation
            byte[] iv = generateIV();
            
            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            // Encrypt the plaintext
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
            
            // Prepend IV to ciphertext (IV is not secret, needed for decryption)
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            
            // Return Base64-encoded result
            return Base64.getEncoder().encodeToString(combined);
            
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypt ciphertext using AES-256-GCM.
     * 
     * @param ciphertext Base64-encoded string containing IV + ciphertext
     * @return Decrypted plaintext
     * @throws IllegalArgumentException if ciphertext is null or empty
     * @throws RuntimeException if decryption fails
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            throw new IllegalArgumentException("Ciphertext cannot be null or empty");
        }
        
        try {
            // Decode Base64
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            
            // Extract IV from the beginning
            if (combined.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid ciphertext: too short");
            }
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            // Decrypt the data
            byte[] plaintext = cipher.doFinal(encryptedData);
            
            return new String(plaintext, "UTF-8");
            
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    /**
     * Generate a secure random IV for GCM mode.
     * 
     * @return 12-byte (96-bit) random IV
     */
    private byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }
    
    /**
     * Load encryption key from keystore or generate a new one if it doesn't exist.
     * 
     * @return The secret key for AES encryption
     * @throws Exception if key loading/generation fails
     */
    private SecretKey loadOrGenerateKey() throws Exception {
        Path keystorePath = getKeystorePath();
        char[] keystorePassword = getKeystorePassword();
        
        KeyStore keyStore;
        
        if (Files.exists(keystorePath)) {
            // Load existing keystore
            keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            try (InputStream is = Files.newInputStream(keystorePath)) {
                keyStore.load(is, keystorePassword);
            }
            
            // Check if key exists
            if (keyStore.containsAlias(KEY_ALIAS)) {
                KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) 
                    keyStore.getEntry(KEY_ALIAS, new KeyStore.PasswordProtection(keystorePassword));
                return entry.getSecretKey();
            }
        } else {
            // Create new keystore
            keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            keyStore.load(null, keystorePassword);
        }
        
        // Generate new key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE, secureRandom);
        SecretKey newKey = keyGen.generateKey();
        
        // Store key in keystore
        KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(newKey);
        keyStore.setEntry(KEY_ALIAS, entry, new KeyStore.PasswordProtection(keystorePassword));
        
        // Save keystore to file
        Files.createDirectories(keystorePath.getParent());
        try (OutputStream os = Files.newOutputStream(keystorePath)) {
            keyStore.store(os, keystorePassword);
        }
        
        return newKey;
    }
    
    /**
     * Get the path to the keystore file.
     * Stored in user's home directory under .esports/
     * 
     * @return Path to keystore file
     */
    private Path getKeystorePath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".esports", KEYSTORE_FILENAME);
    }
    
    /**
     * Get the keystore password.
     * In production, this should be retrieved from a secure configuration source.
     * 
     * @return Keystore password as char array
     */
    private char[] getKeystorePassword() {
        // TODO: In production, retrieve from secure configuration
        // For now, using a default password
        return "esports-keystore-password-2024".toCharArray();
    }
    
    /**
     * Encrypt binary data using AES-256-GCM.
     * 
     * @param plaintext The binary data to encrypt
     * @return Byte array containing IV + ciphertext
     * @throws IllegalArgumentException if plaintext is null or empty
     * @throws RuntimeException if encryption fails
     */
    public byte[] encrypt(byte[] plaintext) {
        if (plaintext == null || plaintext.length == 0) {
            throw new IllegalArgumentException("Plaintext cannot be null or empty");
        }
        
        try {
            // Generate unique IV for this encryption operation
            byte[] iv = generateIV();
            
            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            // Encrypt the plaintext
            byte[] ciphertext = cipher.doFinal(plaintext);
            
            // Prepend IV to ciphertext (IV is not secret, needed for decryption)
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            
            return combined;
            
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypt binary data using AES-256-GCM.
     * 
     * @param ciphertext Byte array containing IV + ciphertext
     * @return Decrypted binary data
     * @throws IllegalArgumentException if ciphertext is null or empty
     * @throws RuntimeException if decryption fails
     */
    public byte[] decrypt(byte[] ciphertext) {
        if (ciphertext == null || ciphertext.length == 0) {
            throw new IllegalArgumentException("Ciphertext cannot be null or empty");
        }
        
        try {
            // Extract IV from the beginning
            if (ciphertext.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid ciphertext: too short");
            }
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[ciphertext.length - GCM_IV_LENGTH];
            System.arraycopy(ciphertext, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            // Decrypt the data
            byte[] plaintext = cipher.doFinal(encryptedData);
            
            return plaintext;
            
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    /**
     * For testing purposes: create an EncryptionService with a specific key.
     * This allows testing without relying on the keystore.
     * 
     * @param key The secret key to use
     */
    EncryptionService(SecretKey key) {
        this.secureRandom = new SecureRandom();
        this.secretKey = key;
    }
}
