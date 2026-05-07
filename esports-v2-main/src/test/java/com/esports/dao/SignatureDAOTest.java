package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.SignatureData;
import com.esports.models.User;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SignatureDAO.
 * 
 * Tests all CRUD operations for signature data management including:
 * - saveSignature() - Create/update signature with binary data and hash
 * - getSignature() - Retrieve signature data
 * - updateSignature() - Update existing signature
 * - deleteSignature() - Remove signature
 * 
 * **Validates: Requirements 8.1, 8.2, 8.5**
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SignatureDAOTest {

    private static SignatureDAO signatureDAO;
    private static UserDAO userDAO;
    private static int testUserId;
    private static Connection connection;

    @BeforeAll
    static void setUp() {
        connection = DatabaseConnection.getInstance().getConnection();
        assertNotNull(connection, "Database connection should not be null");
        
        signatureDAO = new SignatureDAO();
        userDAO = new UserDAO();
        
        // Create a test user for signature operations
        User testUser = new User();
        testUser.setFirstName("Signature");
        testUser.setLastName("TestUser");
        testUser.setEmail("signature.test@example.com");
        testUser.setPassword("testPassword123");
        testUser.setPhone("1234567890");
        testUser.setAddress("123 Test St");
        testUser.setCreatedAt(LocalDateTime.now());
        
        boolean userCreated = userDAO.add(testUser);
        assertTrue(userCreated, "Test user should be created successfully");
        
        // Retrieve the created user to get the ID
        User createdUser = userDAO.findByEmail("signature.test@example.com");
        assertNotNull(createdUser, "Created user should be retrievable");
        testUserId = createdUser.getId();
        
        System.out.println("[TEST SETUP] Created test user with ID: " + testUserId);
    }

    @AfterAll
    static void tearDown() {
        // Clean up test data
        try {
            // Delete signature first (due to foreign key)
            signatureDAO.deleteSignature(testUserId);
            
            // Delete test user
            userDAO.delete(testUserId);
            
            System.out.println("[TEST CLEANUP] Deleted test user and signature");
        } catch (Exception e) {
            System.out.println("[TEST CLEANUP WARNING] Error during cleanup: " + e.getMessage());
        }
    }

    @BeforeEach
    void cleanupSignatures() {
        // Ensure no signature exists before each test
        try {
            signatureDAO.deleteSignature(testUserId);
        } catch (Exception e) {
            // Ignore if signature doesn't exist
        }
    }

    /**
     * Helper method to create test signature data
     */
    private byte[] createTestSignatureData(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Helper method to calculate expected SHA-256 hash
     */
    private String calculateExpectedHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate hash", e);
        }
    }

    /**
     * Test 1: Save signature successfully
     * 
     * Validates Requirement 8.1: Store signature images as binary data
     * Validates Requirement 8.2: Store SHA-256 hash alongside signature data
     */
    @Test
    @Order(1)
    void testSaveSignatureSuccessfully() {
        // Arrange
        byte[] signatureData = createTestSignatureData("Test signature image data");

        // Act
        boolean result = signatureDAO.saveSignature(testUserId, signatureData);

        // Assert
        assertTrue(result, "saveSignature should return true on success");
        
        // Verify signature was saved
        SignatureData savedSignature = signatureDAO.getSignature(testUserId);
        assertNotNull(savedSignature, "Saved signature should be retrievable");
        assertEquals(testUserId, savedSignature.getUserId(), "User ID should match");
        assertArrayEquals(signatureData, savedSignature.getSignatureData(), 
                         "Signature data should match");
        
        // Verify hash was calculated correctly
        String expectedHash = calculateExpectedHash(signatureData);
        assertEquals(expectedHash, savedSignature.getSignatureHash(), 
                    "SHA-256 hash should be calculated correctly");
        
        // Verify timestamps are set
        assertNotNull(savedSignature.getCreatedAt(), "Created timestamp should be set");
        assertNotNull(savedSignature.getUpdatedAt(), "Updated timestamp should be set");
    }

    /**
     * Test 2: Update existing signature (upsert behavior)
     * 
     * Validates that saveSignature updates existing signature for the same user
     * due to unique constraint on user_id.
     */
    @Test
    @Order(2)
    void testSaveSignatureUpdatesExistingSignature() {
        // Arrange - Save initial signature
        byte[] initialData = createTestSignatureData("Initial signature data");
        signatureDAO.saveSignature(testUserId, initialData);

        // Act - Save new signature for same user
        byte[] newData = createTestSignatureData("Updated signature data");
        boolean result = signatureDAO.saveSignature(testUserId, newData);

        // Assert
        assertTrue(result, "saveSignature should return true on update");
        
        // Verify new signature replaced old signature
        SignatureData updatedSignature = signatureDAO.getSignature(testUserId);
        assertNotNull(updatedSignature, "Updated signature should be retrievable");
        assertArrayEquals(newData, updatedSignature.getSignatureData(), 
                         "Signature data should be updated");
        
        // Verify hash was recalculated
        String expectedHash = calculateExpectedHash(newData);
        assertEquals(expectedHash, updatedSignature.getSignatureHash(), 
                    "Hash should be recalculated for new data");
    }

    /**
     * Test 3: Retrieve signature by user ID
     * 
     * Validates Requirement 8.1: Retrieve stored signature data
     * Validates Requirement 8.2: Retrieve stored signature hash
     */
    @Test
    @Order(3)
    void testGetSignature() {
        // Arrange
        byte[] signatureData = createTestSignatureData("Retrieve test signature");
        signatureDAO.saveSignature(testUserId, signatureData);

        // Act
        SignatureData retrievedSignature = signatureDAO.getSignature(testUserId);

        // Assert
        assertNotNull(retrievedSignature, "Signature should be retrieved");
        assertEquals(testUserId, retrievedSignature.getUserId(), "User ID should match");
        assertArrayEquals(signatureData, retrievedSignature.getSignatureData(), 
                         "Signature data should match");
        
        // Verify hash integrity
        String expectedHash = calculateExpectedHash(signatureData);
        assertEquals(expectedHash, retrievedSignature.getSignatureHash(), 
                    "Hash should match calculated value");
        
        assertNotNull(retrievedSignature.getCreatedAt(), "Created timestamp should be set");
        assertNotNull(retrievedSignature.getUpdatedAt(), "Updated timestamp should be set");
    }

    /**
     * Test 4: Get signature returns null for non-existent user
     */
    @Test
    @Order(4)
    void testGetSignatureReturnsNullForNonExistentUser() {
        // Act
        SignatureData signature = signatureDAO.getSignature(999999);

        // Assert
        assertNull(signature, "Should return null for non-existent user");
    }

    /**
     * Test 5: Update signature successfully
     * 
     * Validates Requirement 8.1: Update stored signature data
     * Validates Requirement 8.2: Update SHA-256 hash when signature changes
     */
    @Test
    @Order(5)
    void testUpdateSignature() {
        // Arrange - Save initial signature
        byte[] initialData = createTestSignatureData("Initial signature");
        signatureDAO.saveSignature(testUserId, initialData);

        // Act - Update signature
        byte[] newData = createTestSignatureData("Updated signature with new data");
        boolean result = signatureDAO.updateSignature(testUserId, newData);

        // Assert
        assertTrue(result, "updateSignature should return true on success");
        
        // Verify signature was updated
        SignatureData updatedSignature = signatureDAO.getSignature(testUserId);
        assertNotNull(updatedSignature, "Signature should still exist");
        assertArrayEquals(newData, updatedSignature.getSignatureData(), 
                         "Signature data should be updated");
        
        // Verify hash was recalculated
        String expectedHash = calculateExpectedHash(newData);
        assertEquals(expectedHash, updatedSignature.getSignatureHash(), 
                    "Hash should be recalculated for updated data");
    }

    /**
     * Test 6: Update signature returns false for non-existent user
     */
    @Test
    @Order(6)
    void testUpdateSignatureReturnsFalseForNonExistentUser() {
        // Arrange
        byte[] signatureData = createTestSignatureData("Some signature data");

        // Act
        boolean result = signatureDAO.updateSignature(999999, signatureData);

        // Assert
        assertFalse(result, "Should return false when no signature exists for user");
    }

    /**
     * Test 7: Delete signature successfully
     * 
     * Validates Requirement 8.1: Delete stored signature data
     */
    @Test
    @Order(7)
    void testDeleteSignatureSuccessfully() {
        // Arrange - Save signature first
        byte[] signatureData = createTestSignatureData("Signature to delete");
        signatureDAO.saveSignature(testUserId, signatureData);
        
        // Verify signature exists
        assertNotNull(signatureDAO.getSignature(testUserId), 
                     "Signature should exist before deletion");

        // Act
        boolean result = signatureDAO.deleteSignature(testUserId);

        // Assert
        assertTrue(result, "deleteSignature should return true on success");
        
        // Verify signature was deleted
        SignatureData deletedSignature = signatureDAO.getSignature(testUserId);
        assertNull(deletedSignature, "Signature should not exist after deletion");
    }

    /**
     * Test 8: Delete signature returns false for non-existent user
     */
    @Test
    @Order(8)
    void testDeleteSignatureReturnsFalseForNonExistentUser() {
        // Act
        boolean result = signatureDAO.deleteSignature(999999);

        // Assert
        assertFalse(result, "Should return false when no signature exists to delete");
    }

    /**
     * Test 9: Save signature with null data throws exception
     */
    @Test
    @Order(9)
    void testSaveSignatureWithNullDataThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            signatureDAO.saveSignature(testUserId, null);
        });
        
        assertTrue(exception.getMessage().contains("Signature data"), 
                  "Exception message should mention signature data");
    }

    /**
     * Test 10: Save signature with empty data throws exception
     */
    @Test
    @Order(10)
    void testSaveSignatureWithEmptyDataThrowsException() {
        // Arrange
        byte[] emptyData = new byte[0];

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            signatureDAO.saveSignature(testUserId, emptyData);
        });
        
        assertTrue(exception.getMessage().contains("Signature data"), 
                  "Exception message should mention signature data");
    }

    /**
     * Test 11: Update signature with null data throws exception
     */
    @Test
    @Order(11)
    void testUpdateSignatureWithNullDataThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            signatureDAO.updateSignature(testUserId, null);
        });
        
        assertTrue(exception.getMessage().contains("Signature data"), 
                  "Exception message should mention signature data");
    }

    /**
     * Test 12: Update signature with empty data throws exception
     */
    @Test
    @Order(12)
    void testUpdateSignatureWithEmptyDataThrowsException() {
        // Arrange
        byte[] emptyData = new byte[0];

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            signatureDAO.updateSignature(testUserId, emptyData);
        });
        
        assertTrue(exception.getMessage().contains("Signature data"), 
                  "Exception message should mention signature data");
    }

    /**
     * Test 13: Verify SHA-256 hash is correctly calculated
     * 
     * This test verifies that the hash stored in the database matches
     * the expected SHA-256 hash of the signature data.
     */
    @Test
    @Order(13)
    void testHashCalculationIsCorrect() {
        // Arrange
        byte[] signatureData = createTestSignatureData("Test data for hash verification");
        String expectedHash = calculateExpectedHash(signatureData);

        // Act
        signatureDAO.saveSignature(testUserId, signatureData);
        SignatureData savedSignature = signatureDAO.getSignature(testUserId);

        // Assert
        assertNotNull(savedSignature, "Signature should be saved");
        assertEquals(expectedHash, savedSignature.getSignatureHash(), 
                    "Stored hash should match expected SHA-256 hash");
        assertEquals(64, savedSignature.getSignatureHash().length(), 
                    "SHA-256 hash should be 64 characters (256 bits in hex)");
    }

    /**
     * Test 14: Verify hash changes when signature data changes
     * 
     * Validates Requirement 8.2: Hash is recalculated when signature changes
     */
    @Test
    @Order(14)
    void testHashChangesWhenDataChanges() {
        // Arrange - Save initial signature
        byte[] initialData = createTestSignatureData("Initial data");
        signatureDAO.saveSignature(testUserId, initialData);
        SignatureData initialSignature = signatureDAO.getSignature(testUserId);
        String initialHash = initialSignature.getSignatureHash();

        // Act - Update with different data
        byte[] newData = createTestSignatureData("Different data");
        signatureDAO.updateSignature(testUserId, newData);
        SignatureData updatedSignature = signatureDAO.getSignature(testUserId);
        String newHash = updatedSignature.getSignatureHash();

        // Assert
        assertNotEquals(initialHash, newHash, 
                       "Hash should change when signature data changes");
        
        // Verify new hash is correct for new data
        String expectedNewHash = calculateExpectedHash(newData);
        assertEquals(expectedNewHash, newHash, 
                    "New hash should match expected hash for new data");
    }

    /**
     * Test 15: Verify binary data integrity
     * 
     * This test verifies that binary data is stored and retrieved correctly
     * without corruption, including special bytes and null bytes.
     */
    @Test
    @Order(15)
    void testBinaryDataIntegrity() {
        // Arrange - Create binary data with various byte values
        byte[] binaryData = new byte[256];
        for (int i = 0; i < 256; i++) {
            binaryData[i] = (byte) i;
        }

        // Act
        signatureDAO.saveSignature(testUserId, binaryData);
        SignatureData retrievedSignature = signatureDAO.getSignature(testUserId);

        // Assert
        assertNotNull(retrievedSignature, "Signature should be retrieved");
        assertArrayEquals(binaryData, retrievedSignature.getSignatureData(), 
                         "Binary data should be stored and retrieved without corruption");
        assertEquals(256, retrievedSignature.getSignatureData().length, 
                    "Data length should be preserved");
    }

    /**
     * Test 16: Verify large signature data handling
     * 
     * Tests that the DAO can handle larger signature images
     * (simulating realistic PNG image sizes).
     */
    @Test
    @Order(16)
    void testLargeSignatureData() {
        // Arrange - Create larger signature data (50KB, typical for PNG signature)
        byte[] largeData = new byte[50 * 1024];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        // Act
        boolean saveResult = signatureDAO.saveSignature(testUserId, largeData);
        SignatureData retrievedSignature = signatureDAO.getSignature(testUserId);

        // Assert
        assertTrue(saveResult, "Should successfully save large signature data");
        assertNotNull(retrievedSignature, "Should retrieve large signature data");
        assertArrayEquals(largeData, retrievedSignature.getSignatureData(), 
                         "Large signature data should be stored correctly");
        
        // Verify hash is correct for large data
        String expectedHash = calculateExpectedHash(largeData);
        assertEquals(expectedHash, retrievedSignature.getSignatureHash(), 
                    "Hash should be correct for large data");
    }

    /**
     * Test 17: Verify unique constraint on user_id
     * 
     * Tests that only one signature can exist per user (enforced by database).
     */
    @Test
    @Order(17)
    void testUniqueConstraintOnUserId() {
        // Arrange
        byte[] firstSignature = createTestSignatureData("First signature");
        byte[] secondSignature = createTestSignatureData("Second signature");

        // Act - Save first signature
        signatureDAO.saveSignature(testUserId, firstSignature);
        
        // Save second signature (should replace first due to unique constraint)
        signatureDAO.saveSignature(testUserId, secondSignature);
        
        SignatureData finalSignature = signatureDAO.getSignature(testUserId);

        // Assert - Only the second signature should exist
        assertNotNull(finalSignature, "Signature should exist");
        assertArrayEquals(secondSignature, finalSignature.getSignatureData(), 
                         "Second signature should replace first");
        
        String expectedHash = calculateExpectedHash(secondSignature);
        assertEquals(expectedHash, finalSignature.getSignatureHash(), 
                    "Hash should match second signature");
    }
}
