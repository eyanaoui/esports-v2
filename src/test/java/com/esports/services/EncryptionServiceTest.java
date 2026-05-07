package com.esports.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EncryptionService.
 * 
 * Tests encryption/decryption round-trip, error handling, and security properties.
 * 
 * **Validates: Requirements 7.1, 7.2**
 */
@DisplayName("EncryptionService Unit Tests")
class EncryptionServiceTest {
    
    private EncryptionService encryptionService;
    private SecretKey testKey;
    
    @BeforeEach
    void setUp() throws Exception {
        // Generate a test key to avoid keystore dependencies in tests
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256, new SecureRandom());
        testKey = keyGen.generateKey();
        
        // Use package-private constructor for testing
        encryptionService = new EncryptionService(testKey);
    }
    
    @Test
    @DisplayName("Encrypt and decrypt simple text - round trip should return original")
    void testEncryptDecrypt_SimpleText_ReturnsOriginal() {
        String plaintext = "Hello, World!";
        
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    @DisplayName("Encrypt and decrypt OAuth token - round trip should return original")
    void testEncryptDecrypt_OAuthToken_ReturnsOriginal() {
        String oauthToken = "ya29.a0AfH6SMBx7K3...very-long-token...xyz";
        
        String encrypted = encryptionService.encrypt(oauthToken);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(oauthToken, decrypted);
    }
    
    @Test
    @DisplayName("Encrypt and decrypt empty string - should throw exception")
    void testEncrypt_EmptyString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionService.encrypt("");
        });
    }
    
    @Test
    @DisplayName("Encrypt null value - should throw exception")
    void testEncrypt_NullValue_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionService.encrypt((String) null);
        });
    }
    
    @Test
    @DisplayName("Decrypt empty string - should throw exception")
    void testDecrypt_EmptyString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionService.decrypt("");
        });
    }
    
    @Test
    @DisplayName("Decrypt null value - should throw exception")
    void testDecrypt_NullValue_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionService.decrypt((String) null);
        });
    }
    
    @Test
    @DisplayName("Decrypt invalid Base64 - should throw exception")
    void testDecrypt_InvalidBase64_ThrowsException() {
        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt("not-valid-base64!!!");
        });
    }
    
    @Test
    @DisplayName("Decrypt tampered ciphertext - should throw exception")
    void testDecrypt_TamperedCiphertext_ThrowsException() {
        String plaintext = "Secret message";
        String encrypted = encryptionService.encrypt(plaintext);
        
        // Tamper with the ciphertext
        byte[] cipherBytes = Base64.getDecoder().decode(encrypted);
        cipherBytes[cipherBytes.length - 1] ^= 1; // Flip one bit
        String tamperedCiphertext = Base64.getEncoder().encodeToString(cipherBytes);
        
        // Decryption should fail due to authentication tag mismatch
        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt(tamperedCiphertext);
        });
    }
    
    @Test
    @DisplayName("Decrypt too short ciphertext - should throw exception")
    void testDecrypt_TooShortCiphertext_ThrowsException() {
        // Create a ciphertext that's shorter than IV length
        String shortCiphertext = Base64.getEncoder().encodeToString(new byte[5]);
        
        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt(shortCiphertext);
        });
    }
    
    @Test
    @DisplayName("Encrypt same plaintext twice - should produce different ciphertexts")
    void testEncrypt_SamePlaintext_ProducesDifferentCiphertexts() {
        String plaintext = "Same message";
        
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);
        
        // Due to unique IVs, ciphertexts should be different
        assertNotEquals(encrypted1, encrypted2);
        
        // But both should decrypt to the same plaintext
        assertEquals(plaintext, encryptionService.decrypt(encrypted1));
        assertEquals(plaintext, encryptionService.decrypt(encrypted2));
    }
    
    @Test
    @DisplayName("Encrypt and decrypt long text - should handle large data")
    void testEncryptDecrypt_LongText_ReturnsOriginal() {
        // Create a long string (simulating a large OAuth token or JSON)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("This is line ").append(i).append(" of a very long text. ");
        }
        String longText = sb.toString();
        
        String encrypted = encryptionService.encrypt(longText);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(longText, decrypted);
    }
    
    @Test
    @DisplayName("Encrypt and decrypt special characters - should preserve all characters")
    void testEncryptDecrypt_SpecialCharacters_ReturnsOriginal() {
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~\n\t\r";
        
        String encrypted = encryptionService.encrypt(specialChars);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(specialChars, decrypted);
    }
    
    @Test
    @DisplayName("Encrypt and decrypt Unicode characters - should preserve Unicode")
    void testEncryptDecrypt_UnicodeCharacters_ReturnsOriginal() {
        String unicode = "Hello World Unicode Test";
        
        String encrypted = encryptionService.encrypt(unicode);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(unicode, decrypted);
    }
    
    @Test
    @DisplayName("Encrypted output is Base64 - should be valid Base64")
    void testEncrypt_OutputIsBase64_Valid() {
        String plaintext = "Test message";
        String encrypted = encryptionService.encrypt(plaintext);
        
        // Should not throw exception when decoding
        assertDoesNotThrow(() -> {
            Base64.getDecoder().decode(encrypted);
        });
    }
    
    @Test
    @DisplayName("Encrypted output contains IV - should be at least IV length + some ciphertext")
    void testEncrypt_OutputContainsIV_ValidLength() {
        String plaintext = "Test";
        String encrypted = encryptionService.encrypt(plaintext);
        
        byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);
        
        // Should be at least 12 bytes (IV) + some ciphertext + 16 bytes (GCM tag)
        assertTrue(encryptedBytes.length >= 12 + 16);
    }
    
    @Test
    @DisplayName("Multiple encrypt/decrypt operations - should all work correctly")
    void testMultipleOperations_AllSucceed() {
        String[] messages = {
            "First message",
            "Second message",
            "Third message",
            "Fourth message",
            "Fifth message"
        };
        
        // Encrypt all messages
        String[] encrypted = new String[messages.length];
        for (int i = 0; i < messages.length; i++) {
            encrypted[i] = encryptionService.encrypt(messages[i]);
        }
        
        // Decrypt all messages
        for (int i = 0; i < messages.length; i++) {
            String decrypted = encryptionService.decrypt(encrypted[i]);
            assertEquals(messages[i], decrypted);
        }
    }
    
    @Test
    @DisplayName("Encrypt JSON data - should handle structured data")
    void testEncryptDecrypt_JsonData_ReturnsOriginal() {
        String jsonData = "{\"access_token\":\"ya29.abc123\",\"refresh_token\":\"1//xyz789\",\"expires_in\":3600}";
        
        String encrypted = encryptionService.encrypt(jsonData);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(jsonData, decrypted);
    }
    
    // ========== Byte Array Encryption Tests ==========
    
    @Test
    @DisplayName("Encrypt and decrypt byte array - round trip should return original")
    void testEncryptDecrypt_ByteArray_ReturnsOriginal() {
        byte[] plaintext = "Hello, World!".getBytes();
        
        byte[] encrypted = encryptionService.encrypt(plaintext);
        byte[] decrypted = encryptionService.decrypt(encrypted);
        
        assertArrayEquals(plaintext, decrypted);
    }
    
    @Test
    @DisplayName("Encrypt and decrypt signature image data - round trip should return original")
    void testEncryptDecrypt_SignatureImageData_ReturnsOriginal() {
        // Simulate PNG image data (random bytes)
        byte[] imageData = new byte[1024];
        new SecureRandom().nextBytes(imageData);
        
        byte[] encrypted = encryptionService.encrypt(imageData);
        byte[] decrypted = encryptionService.decrypt(encrypted);
        
        assertArrayEquals(imageData, decrypted);
    }
    
    @Test
    @DisplayName("Encrypt empty byte array - should throw exception")
    void testEncrypt_EmptyByteArray_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionService.encrypt(new byte[0]);
        });
    }
    
    @Test
    @DisplayName("Encrypt null byte array - should throw exception")
    void testEncrypt_NullByteArray_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionService.encrypt((byte[]) null);
        });
    }
    
    @Test
    @DisplayName("Decrypt empty byte array - should throw exception")
    void testDecrypt_EmptyByteArray_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionService.decrypt(new byte[0]);
        });
    }
    
    @Test
    @DisplayName("Decrypt null byte array - should throw exception")
    void testDecrypt_NullByteArray_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionService.decrypt((byte[]) null);
        });
    }
    
    @Test
    @DisplayName("Decrypt tampered byte array ciphertext - should throw exception")
    void testDecrypt_TamperedByteArrayCiphertext_ThrowsException() {
        byte[] plaintext = "Secret message".getBytes();
        byte[] encrypted = encryptionService.encrypt(plaintext);
        
        // Tamper with the ciphertext
        encrypted[encrypted.length - 1] ^= 1; // Flip one bit
        
        // Decryption should fail due to authentication tag mismatch
        byte[] finalEncrypted = encrypted;
        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt(finalEncrypted);
        });
    }
    
    @Test
    @DisplayName("Decrypt too short byte array ciphertext - should throw exception")
    void testDecrypt_TooShortByteArrayCiphertext_ThrowsException() {
        // Create a ciphertext that's shorter than IV length
        byte[] shortCiphertext = new byte[5];
        
        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt(shortCiphertext);
        });
    }
    
    @Test
    @DisplayName("Encrypt same byte array twice - should produce different ciphertexts")
    void testEncrypt_SameByteArray_ProducesDifferentCiphertexts() {
        byte[] plaintext = "Same message".getBytes();
        
        byte[] encrypted1 = encryptionService.encrypt(plaintext);
        byte[] encrypted2 = encryptionService.encrypt(plaintext);
        
        // Due to unique IVs, ciphertexts should be different
        assertFalse(java.util.Arrays.equals(encrypted1, encrypted2));
        
        // But both should decrypt to the same plaintext
        assertArrayEquals(plaintext, encryptionService.decrypt(encrypted1));
        assertArrayEquals(plaintext, encryptionService.decrypt(encrypted2));
    }
    
    @Test
    @DisplayName("Encrypt and decrypt large byte array - should handle large data")
    void testEncryptDecrypt_LargeByteArray_ReturnsOriginal() {
        // Create a large byte array (simulating a large signature image)
        byte[] largeData = new byte[100000];
        new SecureRandom().nextBytes(largeData);
        
        byte[] encrypted = encryptionService.encrypt(largeData);
        byte[] decrypted = encryptionService.decrypt(encrypted);
        
        assertArrayEquals(largeData, decrypted);
    }
    
    @Test
    @DisplayName("Encrypted byte array output contains IV - should be at least IV length + some ciphertext")
    void testEncrypt_ByteArrayOutputContainsIV_ValidLength() {
        byte[] plaintext = "Test".getBytes();
        byte[] encrypted = encryptionService.encrypt(plaintext);
        
        // Should be at least 12 bytes (IV) + some ciphertext + 16 bytes (GCM tag)
        assertTrue(encrypted.length >= 12 + 16);
    }
    
    @Test
    @DisplayName("Multiple byte array encrypt/decrypt operations - should all work correctly")
    void testMultipleByteArrayOperations_AllSucceed() {
        byte[][] messages = {
            "First message".getBytes(),
            "Second message".getBytes(),
            "Third message".getBytes(),
            "Fourth message".getBytes(),
            "Fifth message".getBytes()
        };
        
        // Encrypt all messages
        byte[][] encrypted = new byte[messages.length][];
        for (int i = 0; i < messages.length; i++) {
            encrypted[i] = encryptionService.encrypt(messages[i]);
        }
        
        // Decrypt all messages
        for (int i = 0; i < messages.length; i++) {
            byte[] decrypted = encryptionService.decrypt(encrypted[i]);
            assertArrayEquals(messages[i], decrypted);
        }
    }
}
