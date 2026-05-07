package com.esports.services;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.lifecycle.BeforeTry;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for EncryptionService.
 * 
 * Tests universal properties that should hold for all inputs.
 * 
 * **Validates: Requirements 7.1, 7.2**
 * **Property 1: OAuth Token Lifecycle Integrity (partial - encryption component)**
 */
@PropertyDefaults(tries = 100)
class EncryptionServicePropertyTest {
    
    private EncryptionService encryptionService;
    
    @BeforeTry
    void setUp() throws Exception {
        // Generate a test key for property tests
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256, new SecureRandom());
        SecretKey testKey = keyGen.generateKey();
        
        encryptionService = new EncryptionService(testKey);
    }
    
    /**
     * Property: Encryption/Decryption Round-Trip Integrity
     * 
     * For all non-empty strings, encrypting and then decrypting should return
     * the original string unchanged.
     */
    @Property
    @Label("Encryption round-trip preserves data")
    void encryptionRoundTripPreservesData(@ForAll @StringLength(min = 1, max = 1000) String plaintext) {
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(plaintext, decrypted, 
            "Decrypted text should match original plaintext");
    }
    
    /**
     * Property: Encryption Non-Determinism
     * 
     * For all non-empty strings, encrypting the same plaintext twice should
     * produce different ciphertexts (due to unique IVs).
     */
    @Property
    @Label("Encryption produces unique ciphertexts for same plaintext")
    void encryptionProducesUniqueCiphertexts(@ForAll @StringLength(min = 1, max = 500) String plaintext) {
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);
        
        assertNotEquals(encrypted1, encrypted2,
            "Same plaintext should produce different ciphertexts due to unique IVs");
        
        // But both should decrypt to the same plaintext
        assertEquals(plaintext, encryptionService.decrypt(encrypted1));
        assertEquals(plaintext, encryptionService.decrypt(encrypted2));
    }
    
    /**
     * Property: Encryption Output Length
     * 
     * For all non-empty strings, the encrypted output should be longer than
     * the minimum required (IV + authentication tag).
     */
    @Property
    @Label("Encrypted output has valid length")
    void encryptedOutputHasValidLength(@ForAll @StringLength(min = 1, max = 500) String plaintext) {
        String encrypted = encryptionService.encrypt(plaintext);
        
        assertNotNull(encrypted, "Encrypted output should not be null");
        assertFalse(encrypted.isEmpty(), "Encrypted output should not be empty");
        
        // Base64 encoded output should be valid
        byte[] encryptedBytes = java.util.Base64.getDecoder().decode(encrypted);
        
        // Should contain at least: IV (12 bytes) + GCM tag (16 bytes) = 28 bytes minimum
        assertTrue(encryptedBytes.length >= 28,
            "Encrypted output should contain at least IV + GCM tag");
    }
    
    /**
     * Property: Decryption Idempotence
     * 
     * For all valid ciphertexts, decrypting multiple times should always
     * produce the same result.
     */
    @Property
    @Label("Decryption is idempotent")
    void decryptionIsIdempotent(@ForAll @StringLength(min = 1, max = 500) String plaintext) {
        String encrypted = encryptionService.encrypt(plaintext);
        
        String decrypted1 = encryptionService.decrypt(encrypted);
        String decrypted2 = encryptionService.decrypt(encrypted);
        String decrypted3 = encryptionService.decrypt(encrypted);
        
        assertEquals(decrypted1, decrypted2, "Multiple decryptions should produce same result");
        assertEquals(decrypted2, decrypted3, "Multiple decryptions should produce same result");
    }
    
    /**
     * Property: Encryption Preserves Length Information
     * 
     * For all non-empty strings, longer plaintexts (in bytes) should generally produce
     * longer ciphertexts (accounting for fixed overhead).
     */
    @Property
    @Label("Longer plaintexts produce longer ciphertexts")
    void longerPlaintextsProduceLongerCiphertexts(
            @ForAll @StringLength(min = 1, max = 100) String shortText,
            @ForAll @StringLength(min = 200, max = 500) String longText) {
        
        // Compare based on byte length, not string length (UTF-8 encoding matters)
        byte[] shortBytes = shortText.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] longBytes = longText.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        // Only test if the byte lengths are actually different
        if (longBytes.length > shortBytes.length) {
            String encryptedShort = encryptionService.encrypt(shortText);
            String encryptedLong = encryptionService.encrypt(longText);
            
            byte[] encryptedShortBytes = java.util.Base64.getDecoder().decode(encryptedShort);
            byte[] encryptedLongBytes = java.util.Base64.getDecoder().decode(encryptedLong);
            
            assertTrue(encryptedLongBytes.length > encryptedShortBytes.length,
                "Longer plaintext should produce longer ciphertext");
        }
    }
    
    /**
     * Property: Encryption Handles Special Characters
     * 
     * For all strings containing special characters, encryption and decryption
     * should preserve them exactly.
     */
    @Property
    @Label("Encryption preserves special characters")
    void encryptionPreservesSpecialCharacters(
            @ForAll @StringLength(min = 1, max = 200) String text,
            @ForAll("specialCharacters") String specialChars) {
        
        String combined = text + specialChars;
        
        String encrypted = encryptionService.encrypt(combined);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(combined, decrypted,
            "Special characters should be preserved through encryption");
    }
    
    /**
     * Property: Encryption Handles Unicode
     * 
     * For all strings containing Unicode characters, encryption and decryption
     * should preserve them exactly.
     */
    @Property
    @Label("Encryption preserves Unicode characters")
    void encryptionPreservesUnicode(@ForAll @StringLength(min = 1, max = 200) String text) {
        // Add some ASCII characters to test
        String testString = text + " Unicode Test";
        
        String encrypted = encryptionService.encrypt(testString);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(testString, decrypted,
            "Unicode characters should be preserved through encryption");
    }
    
    /**
     * Property: Tampered Ciphertext Detection
     * 
     * For all valid ciphertexts, tampering with any byte should cause
     * decryption to fail (GCM authentication).
     */
    @Property
    @Label("Tampered ciphertext is detected")
    void tamperedCiphertextIsDetected(
            @ForAll @StringLength(min = 1, max = 200) String plaintext,
            @ForAll @IntRange(min = 0, max = 100) int tamperPosition) {
        
        String encrypted = encryptionService.encrypt(plaintext);
        byte[] encryptedBytes = java.util.Base64.getDecoder().decode(encrypted);
        
        // Only tamper if position is within bounds
        if (tamperPosition < encryptedBytes.length) {
            // Tamper with one byte
            encryptedBytes[tamperPosition] ^= 1;
            String tamperedEncrypted = java.util.Base64.getEncoder().encodeToString(encryptedBytes);
            
            // Decryption should fail
            assertThrows(RuntimeException.class, () -> {
                encryptionService.decrypt(tamperedEncrypted);
            }, "Tampered ciphertext should fail decryption");
        }
    }
    
    /**
     * Property: Multiple Sequential Operations
     * 
     * For all sequences of plaintexts, encrypting and decrypting them in
     * sequence should preserve all values correctly.
     */
    @Property
    @Label("Multiple sequential operations preserve all data")
    void multipleSequentialOperationsPreserveData(
            @ForAll("plaintextList") java.util.List<String> plaintexts) {
        
        // Encrypt all
        java.util.List<String> encrypted = new java.util.ArrayList<>();
        for (String plaintext : plaintexts) {
            encrypted.add(encryptionService.encrypt(plaintext));
        }
        
        // Decrypt all
        java.util.List<String> decrypted = new java.util.ArrayList<>();
        for (String ciphertext : encrypted) {
            decrypted.add(encryptionService.decrypt(ciphertext));
        }
        
        // All should match
        assertEquals(plaintexts, decrypted,
            "All plaintexts should be preserved through encryption/decryption");
    }
    
    /**
     * Property: OAuth Token Format Preservation
     * 
     * For all OAuth-like token strings, encryption and decryption should
     * preserve the exact format.
     */
    @Property
    @Label("OAuth token format is preserved")
    void oauthTokenFormatIsPreserved(@ForAll("oauthToken") String token) {
        String encrypted = encryptionService.encrypt(token);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(token, decrypted,
            "OAuth token format should be preserved exactly");
    }
    
    // Arbitraries (generators)
    
    @Provide
    Arbitrary<String> specialCharacters() {
        return Arbitraries.of(
            "!@#$%^&*()",
            "[]{}|;':\"",
            ",./<>?`~",
            "\n\t\r",
            "\\",
            "\"'`"
        );
    }
    
    @Provide
    Arbitrary<java.util.List<String>> plaintextList() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(1)
            .ofMaxLength(100)
            .list()
            .ofMinSize(1)
            .ofMaxSize(10);
    }
    
    @Provide
    Arbitrary<String> oauthToken() {
        // Generate OAuth-like tokens
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .numeric()
            .withChars('.', '-', '_')
            .ofMinLength(20)
            .ofMaxLength(200)
            .map(s -> "ya29." + s); // Google OAuth token prefix
    }
    
    // ========== Byte Array Property Tests ==========
    
    /**
     * Property: Byte Array Encryption/Decryption Round-Trip Integrity
     * 
     * For all non-empty byte arrays, encrypting and then decrypting should return
     * the original byte array unchanged.
     * 
     * **Validates: Requirements 2.2, 2.4**
     */
    @Property
    @Label("Byte array encryption round-trip preserves data")
    void byteArrayEncryptionRoundTripPreservesData(@ForAll("byteArrays") byte[] plaintext) {
        byte[] encrypted = encryptionService.encrypt(plaintext);
        byte[] decrypted = encryptionService.decrypt(encrypted);
        
        assertArrayEquals(plaintext, decrypted, 
            "Decrypted byte array should match original plaintext");
    }
    
    /**
     * Property: Byte Array Encryption Non-Determinism
     * 
     * For all non-empty byte arrays, encrypting the same plaintext twice should
     * produce different ciphertexts (due to unique IVs).
     * 
     * **Validates: Requirements 2.2**
     */
    @Property
    @Label("Byte array encryption produces unique ciphertexts for same plaintext")
    void byteArrayEncryptionProducesUniqueCiphertexts(@ForAll("byteArrays") byte[] plaintext) {
        byte[] encrypted1 = encryptionService.encrypt(plaintext);
        byte[] encrypted2 = encryptionService.encrypt(plaintext);
        
        assertFalse(java.util.Arrays.equals(encrypted1, encrypted2),
            "Same plaintext should produce different ciphertexts due to unique IVs");
        
        // But both should decrypt to the same plaintext
        assertArrayEquals(plaintext, encryptionService.decrypt(encrypted1));
        assertArrayEquals(plaintext, encryptionService.decrypt(encrypted2));
    }
    
    /**
     * Property: Byte Array Encryption Output Length
     * 
     * For all non-empty byte arrays, the encrypted output should be longer than
     * the minimum required (IV + authentication tag).
     * 
     * **Validates: Requirements 2.2**
     */
    @Property
    @Label("Byte array encrypted output has valid length")
    void byteArrayEncryptedOutputHasValidLength(@ForAll("byteArrays") byte[] plaintext) {
        byte[] encrypted = encryptionService.encrypt(plaintext);
        
        assertNotNull(encrypted, "Encrypted output should not be null");
        assertTrue(encrypted.length > 0, "Encrypted output should not be empty");
        
        // Should contain at least: IV (12 bytes) + GCM tag (16 bytes) = 28 bytes minimum
        assertTrue(encrypted.length >= 28,
            "Encrypted output should contain at least IV + GCM tag");
    }
    
    /**
     * Property: Byte Array Decryption Idempotence
     * 
     * For all valid ciphertexts, decrypting multiple times should always
     * produce the same result.
     * 
     * **Validates: Requirements 2.4**
     */
    @Property
    @Label("Byte array decryption is idempotent")
    void byteArrayDecryptionIsIdempotent(@ForAll("byteArrays") byte[] plaintext) {
        byte[] encrypted = encryptionService.encrypt(plaintext);
        
        byte[] decrypted1 = encryptionService.decrypt(encrypted);
        byte[] decrypted2 = encryptionService.decrypt(encrypted);
        byte[] decrypted3 = encryptionService.decrypt(encrypted);
        
        assertArrayEquals(decrypted1, decrypted2, "Multiple decryptions should produce same result");
        assertArrayEquals(decrypted2, decrypted3, "Multiple decryptions should produce same result");
    }
    
    /**
     * Property: Byte Array Encryption Preserves Length Information
     * 
     * For all non-empty byte arrays, longer plaintexts should generally produce
     * longer ciphertexts (accounting for fixed overhead).
     * 
     * **Validates: Requirements 2.2**
     */
    @Property
    @Label("Longer byte arrays produce longer ciphertexts")
    void longerByteArraysProduceLongerCiphertexts(
            @ForAll("smallByteArrays") byte[] shortData,
            @ForAll("largeByteArrays") byte[] longData) {
        
        byte[] encryptedShort = encryptionService.encrypt(shortData);
        byte[] encryptedLong = encryptionService.encrypt(longData);
        
        assertTrue(encryptedLong.length > encryptedShort.length,
            "Longer plaintext should produce longer ciphertext");
    }
    
    /**
     * Property: Byte Array Tampered Ciphertext Detection
     * 
     * For all valid ciphertexts, tampering with any byte should cause
     * decryption to fail (GCM authentication).
     * 
     * **Validates: Requirements 2.2, 2.5**
     */
    @Property
    @Label("Tampered byte array ciphertext is detected")
    void tamperedByteArrayCiphertextIsDetected(
            @ForAll("byteArrays") byte[] plaintext,
            @ForAll @IntRange(min = 0, max = 100) int tamperPosition) {
        
        byte[] encrypted = encryptionService.encrypt(plaintext);
        
        // Only tamper if position is within bounds
        if (tamperPosition < encrypted.length) {
            // Tamper with one byte
            encrypted[tamperPosition] ^= 1;
            
            // Decryption should fail
            byte[] finalEncrypted = encrypted;
            assertThrows(RuntimeException.class, () -> {
                encryptionService.decrypt(finalEncrypted);
            }, "Tampered ciphertext should fail decryption");
        }
    }
    
    /**
     * Property: Multiple Sequential Byte Array Operations
     * 
     * For all sequences of byte arrays, encrypting and decrypting them in
     * sequence should preserve all values correctly.
     * 
     * **Validates: Requirements 2.2, 2.4**
     */
    @Property
    @Label("Multiple sequential byte array operations preserve all data")
    void multipleSequentialByteArrayOperationsPreserveData(
            @ForAll("byteArrayList") java.util.List<byte[]> plaintexts) {
        
        // Encrypt all
        java.util.List<byte[]> encrypted = new java.util.ArrayList<>();
        for (byte[] plaintext : plaintexts) {
            encrypted.add(encryptionService.encrypt(plaintext));
        }
        
        // Decrypt all
        java.util.List<byte[]> decrypted = new java.util.ArrayList<>();
        for (byte[] ciphertext : encrypted) {
            decrypted.add(encryptionService.decrypt(ciphertext));
        }
        
        // All should match
        assertEquals(plaintexts.size(), decrypted.size(), "All plaintexts should be preserved");
        for (int i = 0; i < plaintexts.size(); i++) {
            assertArrayEquals(plaintexts.get(i), decrypted.get(i),
                "Plaintext at index " + i + " should be preserved through encryption/decryption");
        }
    }
    
    /**
     * Property: Signature Image Data Preservation
     * 
     * For all signature-like byte arrays (simulating PNG image data),
     * encryption and decryption should preserve the exact data.
     * 
     * **Validates: Requirements 2.2, 2.4, 2.5**
     */
    @Property
    @Label("Signature image data is preserved")
    void signatureImageDataIsPreserved(@ForAll("signatureImageData") byte[] imageData) {
        byte[] encrypted = encryptionService.encrypt(imageData);
        byte[] decrypted = encryptionService.decrypt(encrypted);
        
        assertArrayEquals(imageData, decrypted,
            "Signature image data should be preserved exactly");
    }
    
    // Arbitraries (generators) for byte arrays
    
    @Provide
    Arbitrary<byte[]> byteArrays() {
        return Arbitraries.bytes()
            .array(byte[].class)
            .ofMinSize(1)
            .ofMaxSize(1000);
    }
    
    @Provide
    Arbitrary<byte[]> smallByteArrays() {
        return Arbitraries.bytes()
            .array(byte[].class)
            .ofMinSize(1)
            .ofMaxSize(100);
    }
    
    @Provide
    Arbitrary<byte[]> largeByteArrays() {
        return Arbitraries.bytes()
            .array(byte[].class)
            .ofMinSize(200)
            .ofMaxSize(1000);
    }
    
    @Provide
    Arbitrary<java.util.List<byte[]>> byteArrayList() {
        return Arbitraries.bytes()
            .array(byte[].class)
            .ofMinSize(1)
            .ofMaxSize(100)
            .list()
            .ofMinSize(1)
            .ofMaxSize(10);
    }
    
    @Provide
    Arbitrary<byte[]> signatureImageData() {
        // Simulate PNG-like image data (larger byte arrays)
        return Arbitraries.bytes()
            .array(byte[].class)
            .ofMinSize(100)
            .ofMaxSize(10000);
    }
}
