package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.OAuthTokens;
import com.esports.models.User;
import com.esports.models.UserRole;
import com.esports.services.EncryptionService;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OAuthTokenDAO.
 * 
 * Tests all CRUD operations for OAuth token management including:
 * - saveTokens() - Create/update encrypted tokens
 * - getTokensByUserId() - Retrieve tokens
 * - updateAccessToken() - Refresh access token
 * - deleteTokens() - Remove tokens on disconnect
 * 
 * **Validates: Requirements 7.1, 7.2, 7.10**
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OAuthTokenDAOTest {

    private static OAuthTokenDAO oauthTokenDAO;
    private static UserDAO userDAO;
    private static EncryptionService encryptionService;
    private static int testUserId;
    private static Connection connection;

    @BeforeAll
    static void setUp() {
        connection = DatabaseConnection.getInstance().getConnection();
        assertNotNull(connection, "Database connection should not be null");
        
        oauthTokenDAO = new OAuthTokenDAO();
        userDAO = new UserDAO();
        encryptionService = new EncryptionService();
        
        // Create a test user for OAuth token operations
        User testUser = new User();
        testUser.setFirstName("OAuth");
        testUser.setLastName("TestUser");
        testUser.setEmail("oauth.test@example.com");
        testUser.setPassword("testPassword123");
        testUser.setPhone("1234567890");
        testUser.setAddress("123 Test St");
        testUser.setCreatedAt(LocalDateTime.now());
        
        boolean userCreated = userDAO.add(testUser);
        assertTrue(userCreated, "Test user should be created successfully");
        
        // Retrieve the created user to get the ID
        User createdUser = userDAO.findByEmail("oauth.test@example.com");
        assertNotNull(createdUser, "Created user should be retrievable");
        testUserId = createdUser.getId();
        
        System.out.println("[TEST SETUP] Created test user with ID: " + testUserId);
    }

    @AfterAll
    static void tearDown() {
        // Clean up test data
        try {
            // Delete OAuth tokens first (due to foreign key)
            oauthTokenDAO.deleteTokens(testUserId);
            
            // Delete test user
            userDAO.delete(testUserId);
            
            System.out.println("[TEST CLEANUP] Deleted test user and tokens");
        } catch (Exception e) {
            System.out.println("[TEST CLEANUP WARNING] Error during cleanup: " + e.getMessage());
        }
    }

    @BeforeEach
    void cleanupTokens() {
        // Ensure no tokens exist before each test
        try {
            oauthTokenDAO.deleteTokens(testUserId);
        } catch (Exception e) {
            // Ignore if tokens don't exist
        }
    }

    /**
     * Test 1: Save OAuth tokens successfully
     * 
     * Validates Requirement 7.1: Store OAuth access tokens encrypted
     * Validates Requirement 7.2: Store OAuth refresh tokens encrypted
     */
    @Test
    @Order(1)
    void testSaveTokensSuccessfully() {
        // Arrange
        String accessToken = "test_access_token_12345";
        String refreshToken = "test_refresh_token_67890";
        String encryptedAccessToken = encryptionService.encrypt(accessToken);
        String encryptedRefreshToken = encryptionService.encrypt(refreshToken);
        Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusHours(1));

        // Act
        boolean result = oauthTokenDAO.saveTokens(testUserId, encryptedAccessToken, 
                                                  encryptedRefreshToken, expiresAt);

        // Assert
        assertTrue(result, "saveTokens should return true on success");
        
        // Verify tokens were saved
        OAuthTokens savedTokens = oauthTokenDAO.getTokensByUserId(testUserId);
        assertNotNull(savedTokens, "Saved tokens should be retrievable");
        assertEquals(testUserId, savedTokens.getUserId(), "User ID should match");
        assertEquals(encryptedAccessToken, savedTokens.getEncryptedAccessToken(), 
                    "Encrypted access token should match");
        assertEquals(encryptedRefreshToken, savedTokens.getEncryptedRefreshToken(), 
                    "Encrypted refresh token should match");
        // MySQL TIMESTAMP only stores seconds, not milliseconds
        assertEquals(expiresAt.getTime() / 1000, savedTokens.getExpiresAt().getTime() / 1000, 
                    "Expiration timestamp should match (seconds precision)");
        
        // Verify tokens can be decrypted
        String decryptedAccessToken = encryptionService.decrypt(savedTokens.getEncryptedAccessToken());
        String decryptedRefreshToken = encryptionService.decrypt(savedTokens.getEncryptedRefreshToken());
        assertEquals(accessToken, decryptedAccessToken, "Decrypted access token should match original");
        assertEquals(refreshToken, decryptedRefreshToken, "Decrypted refresh token should match original");
    }

    /**
     * Test 2: Update existing tokens (upsert behavior)
     * 
     * Validates that saveTokens updates existing tokens for the same user
     * due to unique constraint on user_id.
     */
    @Test
    @Order(2)
    void testSaveTokensUpdatesExistingTokens() {
        // Arrange - Save initial tokens
        String initialAccessToken = "initial_access_token";
        String initialRefreshToken = "initial_refresh_token";
        String encryptedInitialAccess = encryptionService.encrypt(initialAccessToken);
        String encryptedInitialRefresh = encryptionService.encrypt(initialRefreshToken);
        Timestamp initialExpiry = Timestamp.valueOf(LocalDateTime.now().plusHours(1));
        
        oauthTokenDAO.saveTokens(testUserId, encryptedInitialAccess, 
                                encryptedInitialRefresh, initialExpiry);

        // Act - Save new tokens for same user
        String newAccessToken = "new_access_token";
        String newRefreshToken = "new_refresh_token";
        String encryptedNewAccess = encryptionService.encrypt(newAccessToken);
        String encryptedNewRefresh = encryptionService.encrypt(newRefreshToken);
        Timestamp newExpiry = Timestamp.valueOf(LocalDateTime.now().plusHours(2));
        
        boolean result = oauthTokenDAO.saveTokens(testUserId, encryptedNewAccess, 
                                                  encryptedNewRefresh, newExpiry);

        // Assert
        assertTrue(result, "saveTokens should return true on update");
        
        // Verify new tokens replaced old tokens
        OAuthTokens updatedTokens = oauthTokenDAO.getTokensByUserId(testUserId);
        assertNotNull(updatedTokens, "Updated tokens should be retrievable");
        
        String decryptedAccessToken = encryptionService.decrypt(updatedTokens.getEncryptedAccessToken());
        String decryptedRefreshToken = encryptionService.decrypt(updatedTokens.getEncryptedRefreshToken());
        
        assertEquals(newAccessToken, decryptedAccessToken, "Access token should be updated");
        assertEquals(newRefreshToken, decryptedRefreshToken, "Refresh token should be updated");
        // MySQL TIMESTAMP only stores seconds, not milliseconds
        assertEquals(newExpiry.getTime() / 1000, updatedTokens.getExpiresAt().getTime() / 1000, 
                    "Expiration should be updated (seconds precision)");
    }

    /**
     * Test 3: Retrieve tokens by user ID
     * 
     * Validates Requirement 7.1: Retrieve stored OAuth tokens
     */
    @Test
    @Order(3)
    void testGetTokensByUserId() {
        // Arrange
        String accessToken = "retrieve_test_access";
        String refreshToken = "retrieve_test_refresh";
        String encryptedAccess = encryptionService.encrypt(accessToken);
        String encryptedRefresh = encryptionService.encrypt(refreshToken);
        Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusHours(1));
        
        oauthTokenDAO.saveTokens(testUserId, encryptedAccess, encryptedRefresh, expiresAt);

        // Act
        OAuthTokens retrievedTokens = oauthTokenDAO.getTokensByUserId(testUserId);

        // Assert
        assertNotNull(retrievedTokens, "Tokens should be retrieved");
        assertEquals(testUserId, retrievedTokens.getUserId(), "User ID should match");
        assertNotNull(retrievedTokens.getCreatedAt(), "Created timestamp should be set");
        assertNotNull(retrievedTokens.getUpdatedAt(), "Updated timestamp should be set");
        
        // Verify encryption integrity
        String decryptedAccess = encryptionService.decrypt(retrievedTokens.getEncryptedAccessToken());
        String decryptedRefresh = encryptionService.decrypt(retrievedTokens.getEncryptedRefreshToken());
        assertEquals(accessToken, decryptedAccess, "Access token should decrypt correctly");
        assertEquals(refreshToken, decryptedRefresh, "Refresh token should decrypt correctly");
    }

    /**
     * Test 4: Get tokens returns null for non-existent user
     */
    @Test
    @Order(4)
    void testGetTokensByUserIdReturnsNullForNonExistentUser() {
        // Act
        OAuthTokens tokens = oauthTokenDAO.getTokensByUserId(999999);

        // Assert
        assertNull(tokens, "Should return null for non-existent user");
    }

    /**
     * Test 5: Update access token after refresh
     * 
     * Validates Requirement 7.10: Update access token when refreshed
     */
    @Test
    @Order(5)
    void testUpdateAccessToken() {
        // Arrange - Save initial tokens
        String initialAccessToken = "initial_access";
        String refreshToken = "refresh_token_stays_same";
        String encryptedInitialAccess = encryptionService.encrypt(initialAccessToken);
        String encryptedRefresh = encryptionService.encrypt(refreshToken);
        Timestamp initialExpiry = Timestamp.valueOf(LocalDateTime.now().plusHours(1));
        
        oauthTokenDAO.saveTokens(testUserId, encryptedInitialAccess, 
                                encryptedRefresh, initialExpiry);

        // Act - Update only access token
        String newAccessToken = "refreshed_access_token";
        String encryptedNewAccess = encryptionService.encrypt(newAccessToken);
        Timestamp newExpiry = Timestamp.valueOf(LocalDateTime.now().plusHours(2));
        
        boolean result = oauthTokenDAO.updateAccessToken(testUserId, encryptedNewAccess, newExpiry);

        // Assert
        assertTrue(result, "updateAccessToken should return true on success");
        
        // Verify access token was updated but refresh token unchanged
        OAuthTokens updatedTokens = oauthTokenDAO.getTokensByUserId(testUserId);
        assertNotNull(updatedTokens, "Tokens should still exist");
        
        String decryptedAccess = encryptionService.decrypt(updatedTokens.getEncryptedAccessToken());
        String decryptedRefresh = encryptionService.decrypt(updatedTokens.getEncryptedRefreshToken());
        
        assertEquals(newAccessToken, decryptedAccess, "Access token should be updated");
        assertEquals(refreshToken, decryptedRefresh, "Refresh token should remain unchanged");
        // MySQL TIMESTAMP only stores seconds, not milliseconds
        assertEquals(newExpiry.getTime() / 1000, updatedTokens.getExpiresAt().getTime() / 1000, 
                    "Expiration should be updated (seconds precision)");
    }

    /**
     * Test 6: Update access token returns false for non-existent user
     */
    @Test
    @Order(6)
    void testUpdateAccessTokenReturnsFalseForNonExistentUser() {
        // Arrange
        String encryptedAccess = encryptionService.encrypt("some_token");
        Timestamp expiry = Timestamp.valueOf(LocalDateTime.now().plusHours(1));

        // Act
        boolean result = oauthTokenDAO.updateAccessToken(999999, encryptedAccess, expiry);

        // Assert
        assertFalse(result, "Should return false when no tokens exist for user");
    }

    /**
     * Test 7: Delete tokens successfully
     * 
     * Validates Requirement 7.10: Delete stored OAuth tokens when user disconnects
     */
    @Test
    @Order(7)
    void testDeleteTokensSuccessfully() {
        // Arrange - Save tokens first
        String encryptedAccess = encryptionService.encrypt("access_to_delete");
        String encryptedRefresh = encryptionService.encrypt("refresh_to_delete");
        Timestamp expiry = Timestamp.valueOf(LocalDateTime.now().plusHours(1));
        
        oauthTokenDAO.saveTokens(testUserId, encryptedAccess, encryptedRefresh, expiry);
        
        // Verify tokens exist
        assertNotNull(oauthTokenDAO.getTokensByUserId(testUserId), "Tokens should exist before deletion");

        // Act
        boolean result = oauthTokenDAO.deleteTokens(testUserId);

        // Assert
        assertTrue(result, "deleteTokens should return true on success");
        
        // Verify tokens were deleted
        OAuthTokens deletedTokens = oauthTokenDAO.getTokensByUserId(testUserId);
        assertNull(deletedTokens, "Tokens should not exist after deletion");
    }

    /**
     * Test 8: Delete tokens returns false for non-existent user
     */
    @Test
    @Order(8)
    void testDeleteTokensReturnsFalseForNonExistentUser() {
        // Act
        boolean result = oauthTokenDAO.deleteTokens(999999);

        // Assert
        assertFalse(result, "Should return false when no tokens exist to delete");
    }

    /**
     * Test 9: Save tokens with null access token throws exception
     */
    @Test
    @Order(9)
    void testSaveTokensWithNullAccessTokenThrowsException() {
        // Arrange
        String encryptedRefresh = encryptionService.encrypt("refresh_token");
        Timestamp expiry = Timestamp.valueOf(LocalDateTime.now().plusHours(1));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            oauthTokenDAO.saveTokens(testUserId, null, encryptedRefresh, expiry);
        });
        
        assertTrue(exception.getMessage().contains("access token"), 
                  "Exception message should mention access token");
    }

    /**
     * Test 10: Save tokens with empty access token throws exception
     */
    @Test
    @Order(10)
    void testSaveTokensWithEmptyAccessTokenThrowsException() {
        // Arrange
        String encryptedRefresh = encryptionService.encrypt("refresh_token");
        Timestamp expiry = Timestamp.valueOf(LocalDateTime.now().plusHours(1));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            oauthTokenDAO.saveTokens(testUserId, "", encryptedRefresh, expiry);
        });
        
        assertTrue(exception.getMessage().contains("access token"), 
                  "Exception message should mention access token");
    }

    /**
     * Test 11: Save tokens with null refresh token throws exception
     */
    @Test
    @Order(11)
    void testSaveTokensWithNullRefreshTokenThrowsException() {
        // Arrange
        String encryptedAccess = encryptionService.encrypt("access_token");
        Timestamp expiry = Timestamp.valueOf(LocalDateTime.now().plusHours(1));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            oauthTokenDAO.saveTokens(testUserId, encryptedAccess, null, expiry);
        });
        
        assertTrue(exception.getMessage().contains("refresh token"), 
                  "Exception message should mention refresh token");
    }

    /**
     * Test 12: Save tokens with null expiration throws exception
     */
    @Test
    @Order(12)
    void testSaveTokensWithNullExpirationThrowsException() {
        // Arrange
        String encryptedAccess = encryptionService.encrypt("access_token");
        String encryptedRefresh = encryptionService.encrypt("refresh_token");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            oauthTokenDAO.saveTokens(testUserId, encryptedAccess, encryptedRefresh, null);
        });
        
        assertTrue(exception.getMessage().contains("Expiration"), 
                  "Exception message should mention expiration");
    }

    /**
     * Test 13: Update access token with null token throws exception
     */
    @Test
    @Order(13)
    void testUpdateAccessTokenWithNullTokenThrowsException() {
        // Arrange
        Timestamp expiry = Timestamp.valueOf(LocalDateTime.now().plusHours(1));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            oauthTokenDAO.updateAccessToken(testUserId, null, expiry);
        });
        
        assertTrue(exception.getMessage().contains("access token"), 
                  "Exception message should mention access token");
    }

    /**
     * Test 14: Update access token with null expiration throws exception
     */
    @Test
    @Order(14)
    void testUpdateAccessTokenWithNullExpirationThrowsException() {
        // Arrange
        String encryptedAccess = encryptionService.encrypt("access_token");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            oauthTokenDAO.updateAccessToken(testUserId, encryptedAccess, null);
        });
        
        assertTrue(exception.getMessage().contains("Expiration"), 
                  "Exception message should mention expiration");
    }

    /**
     * Test 15: Verify tokens are properly encrypted in database
     * 
     * This test verifies that tokens stored in the database are encrypted
     * and not stored as plaintext.
     */
    @Test
    @Order(15)
    void testTokensAreEncryptedInDatabase() {
        // Arrange
        String plainAccessToken = "plaintext_access_token_12345";
        String plainRefreshToken = "plaintext_refresh_token_67890";
        String encryptedAccess = encryptionService.encrypt(plainAccessToken);
        String encryptedRefresh = encryptionService.encrypt(plainRefreshToken);
        Timestamp expiry = Timestamp.valueOf(LocalDateTime.now().plusHours(1));
        
        oauthTokenDAO.saveTokens(testUserId, encryptedAccess, encryptedRefresh, expiry);

        // Act - Retrieve tokens
        OAuthTokens tokens = oauthTokenDAO.getTokensByUserId(testUserId);

        // Assert - Verify stored values are encrypted (not plaintext)
        assertNotNull(tokens, "Tokens should be retrieved");
        assertNotEquals(plainAccessToken, tokens.getEncryptedAccessToken(), 
                       "Access token should be encrypted, not plaintext");
        assertNotEquals(plainRefreshToken, tokens.getEncryptedRefreshToken(), 
                       "Refresh token should be encrypted, not plaintext");
        
        // Verify encrypted values can be decrypted back to original
        String decryptedAccess = encryptionService.decrypt(tokens.getEncryptedAccessToken());
        String decryptedRefresh = encryptionService.decrypt(tokens.getEncryptedRefreshToken());
        assertEquals(plainAccessToken, decryptedAccess, "Decrypted access token should match original");
        assertEquals(plainRefreshToken, decryptedRefresh, "Decrypted refresh token should match original");
    }
}
