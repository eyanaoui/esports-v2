package com.esports.services;

import com.esports.dao.SignatureDAO;
import com.esports.db.DatabaseConnection;
import com.esports.models.SignatureData;
import com.esports.models.User;
import com.esports.models.UserRole;
import org.junit.jupiter.api.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SignatureAuthService.
 * 
 * Tests signature registration, authentication, rate limiting, lockout,
 * and attempt logging functionality.
 * 
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 8.6, 8.7, 8.8, 8.9**
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SignatureAuthServiceTest {
    
    private SignatureAuthService service;
    private SignatureDAO signatureDAO;
    private ImageComparator imageComparator;
    private Connection connection;
    
    // Test user IDs - use high IDs that are unlikely to conflict
    private static final int TEST_USER_ID = 999991;
    private static final int TEST_USER_ID_2 = 999992;
    
    @BeforeEach
    void setUp() throws SQLException {
        signatureDAO = new SignatureDAO();
        imageComparator = new ImageComparator();
        service = new SignatureAuthService(signatureDAO, imageComparator);
        connection = DatabaseConnection.getInstance().getConnection();
        
        // Clean up test data first
        cleanupTestData();
        
        // Create minimal test users directly with SQL
        createTestUsersDirectly();
    }
    
    @AfterEach
    void tearDown() throws SQLException {
        cleanupTestData();
        deleteTestUsers();
    }
    
    private void createTestUsersDirectly() throws SQLException {
        // Insert minimal test users directly, bypassing constraints
        String sql = "INSERT IGNORE INTO user (id, first_name, last_name, email, password, roles) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            // Test user 1
            ps.setInt(1, TEST_USER_ID);
            ps.setString(2, "Test");
            ps.setString(3, "User" + TEST_USER_ID);
            ps.setString(4, "test" + TEST_USER_ID + "@test.com");
            ps.setString(5, "$2a$10$dummyhash");
            ps.setString(6, "[\"USER\"]"); // JSON array
            ps.executeUpdate();
            
            // Test user 2
            ps.setInt(1, TEST_USER_ID_2);
            ps.setString(2, "Test");
            ps.setString(3, "User" + TEST_USER_ID_2);
            ps.setString(4, "test" + TEST_USER_ID_2 + "@test.com");
            ps.setString(5, "$2a$10$dummyhash");
            ps.setString(6, "[\"USER\"]");
            ps.executeUpdate();
        } catch (SQLException e) {
            // Ignore if users already exist
            System.out.println("[INFO] Test users may already exist: " + e.getMessage());
        }
    }
    
    private void deleteTestUsers() throws SQLException {
        String deleteUsers = "DELETE FROM user WHERE id IN (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(deleteUsers)) {
            ps.setInt(1, TEST_USER_ID);
            ps.setInt(2, TEST_USER_ID_2);
            ps.executeUpdate();
        }
    }
    
    private void cleanupTestData() throws SQLException {
        // Delete test signatures
        String deleteSigs = "DELETE FROM user_signatures WHERE user_id IN (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(deleteSigs)) {
            ps.setInt(1, TEST_USER_ID);
            ps.setInt(2, TEST_USER_ID_2);
            ps.executeUpdate();
        }
        
        // Delete test attempts
        String deleteAttempts = "DELETE FROM signature_auth_attempts WHERE user_id IN (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(deleteAttempts)) {
            ps.setInt(1, TEST_USER_ID);
            ps.setInt(2, TEST_USER_ID_2);
            ps.executeUpdate();
        }
    }
    
    // ========== Signature Registration Tests ==========
    
    @Test
    @Order(1)
    void testSaveSignature_ValidSignature_Success() {
        // Create signature with sufficient content
        BufferedImage signature = createSignatureWithContent(100);
        
        boolean result = service.saveSignature(TEST_USER_ID, signature);
        
        assertTrue(result, "Should save valid signature");
        
        // Verify signature was stored
        SignatureData stored = signatureDAO.getSignature(TEST_USER_ID);
        assertNotNull(stored, "Signature should be stored in database");
        assertEquals(TEST_USER_ID, stored.getUserId());
    }
    
    @Test
    @Order(2)
    void testSaveSignature_NullImage_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.saveSignature(TEST_USER_ID, null);
        });
        
        assertTrue(exception.getMessage().contains("cannot be null"));
    }
    
    @Test
    @Order(3)
    void testSaveSignature_InsufficientContent_ThrowsException() {
        // Create signature with only 30 pixels (below minimum of 50)
        BufferedImage signature = createSignatureWithContent(30);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.saveSignature(TEST_USER_ID, signature);
        });
        
        assertTrue(exception.getMessage().contains("at least 50 pixels"));
    }
    
    @Test
    @Order(4)
    void testSaveSignature_ExactlyMinimumContent_Success() {
        // Create signature with exactly 50 pixels
        BufferedImage signature = createSignatureWithContent(50);
        
        boolean result = service.saveSignature(TEST_USER_ID, signature);
        
        assertTrue(result, "Should save signature with exactly 50 pixels");
    }
    
    @Test
    @Order(5)
    void testSaveSignature_UpdateExisting_Success() {
        // Save initial signature
        BufferedImage signature1 = createSignatureWithContent(100);
        service.saveSignature(TEST_USER_ID, signature1);
        
        // Update with new signature
        BufferedImage signature2 = createSignatureWithContent(150);
        boolean result = service.saveSignature(TEST_USER_ID, signature2);
        
        assertTrue(result, "Should update existing signature");
        
        // Verify only one signature exists
        SignatureData stored = signatureDAO.getSignature(TEST_USER_ID);
        assertNotNull(stored);
    }
    
    // ========== Minimum Content Validation Tests ==========
    
    @Test
    @Order(6)
    void testHasMinimumContent_EmptyImage_ReturnsFalse() {
        BufferedImage emptyImage = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        
        boolean result = service.hasMinimumContent(emptyImage);
        
        assertFalse(result, "Empty image should not have minimum content");
    }
    
    @Test
    @Order(7)
    void testHasMinimumContent_NullImage_ReturnsFalse() {
        boolean result = service.hasMinimumContent(null);
        
        assertFalse(result, "Null image should not have minimum content");
    }
    
    @Test
    @Order(8)
    void testHasMinimumContent_SufficientContent_ReturnsTrue() {
        BufferedImage signature = createSignatureWithContent(100);
        
        boolean result = service.hasMinimumContent(signature);
        
        assertTrue(result, "Image with 100 pixels should have minimum content");
    }
    
    // ========== Authentication Tests ==========
    
    @Test
    @Order(9)
    void testAuthenticateWithSignature_MatchingSignature_Success() {
        // Save a signature
        BufferedImage originalSignature = createSignatureWithContent(100);
        service.saveSignature(TEST_USER_ID, originalSignature);
        
        // Authenticate with identical signature
        SignatureAuthService.AuthenticationResult result = 
            service.authenticateWithSignature(TEST_USER_ID, originalSignature);
        
        assertTrue(result.isSuccess(), "Identical signature should authenticate");
        assertTrue(result.getSimilarityScore() >= 75.0, "Score should be >= 75");
        assertNull(result.getFailureReason());
    }
    
    @Test
    @Order(10)
    void testAuthenticateWithSignature_NoStoredSignature_Fails() {
        BufferedImage attemptSignature = createSignatureWithContent(100);
        
        SignatureAuthService.AuthenticationResult result = 
            service.authenticateWithSignature(TEST_USER_ID_2, attemptSignature);
        
        assertFalse(result.isSuccess(), "Should fail when no signature stored");
        assertEquals(SignatureAuthService.FailureReason.NO_SIGNATURE, result.getFailureReason());
    }
    
    @Test
    @Order(11)
    void testAuthenticateWithSignature_DifferentSignature_Fails() {
        // Save a signature
        BufferedImage originalSignature = createSignatureWithContent(100);
        service.saveSignature(TEST_USER_ID, originalSignature);
        
        // Authenticate with completely different signature
        BufferedImage differentSignature = createDifferentSignature();
        SignatureAuthService.AuthenticationResult result = 
            service.authenticateWithSignature(TEST_USER_ID, differentSignature);
        
        assertFalse(result.isSuccess(), "Different signature should fail");
        assertEquals(SignatureAuthService.FailureReason.LOW_SIMILARITY, result.getFailureReason());
        assertTrue(result.getSimilarityScore() < 75.0, "Score should be < 75");
    }
    
    @Test
    @Order(12)
    void testAuthenticateWithSignature_NullAttempt_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.authenticateWithSignature(TEST_USER_ID, null);
        });
        
        assertTrue(exception.getMessage().contains("cannot be null"));
    }
    
    // ========== Rate Limiting Tests ==========
    
    @Test
    @Order(13)
    void testAuthenticateWithSignature_RateLimitEnforced_FailsAfter10Attempts() throws SQLException {
        // Save a signature
        BufferedImage signature = createSignatureWithContent(100);
        service.saveSignature(TEST_USER_ID, signature);
        
        // Create a different signature that will fail
        BufferedImage wrongSignature = createDifferentSignature();
        
        // Make 10 failed attempts (should succeed)
        for (int i = 0; i < 10; i++) {
            SignatureAuthService.AuthenticationResult result = 
                service.authenticateWithSignature(TEST_USER_ID, wrongSignature);
            assertFalse(result.isSuccess(), "Attempt " + (i + 1) + " should fail due to wrong signature");
        }
        
        // 11th attempt should be rate limited
        SignatureAuthService.AuthenticationResult result = 
            service.authenticateWithSignature(TEST_USER_ID, wrongSignature);
        
        assertFalse(result.isSuccess(), "11th attempt should be rate limited");
        assertEquals(SignatureAuthService.FailureReason.RATE_LIMITED, result.getFailureReason());
    }
    
    @Test
    @Order(14)
    void testAuthenticateWithSignature_LockedOut_FailsImmediately() throws SQLException {
        // Save a signature
        BufferedImage signature = createSignatureWithContent(100);
        service.saveSignature(TEST_USER_ID_2, signature);
        
        // Manually insert 11 attempts to trigger lockout
        String sql = "INSERT INTO signature_auth_attempts (user_id, similarity_score, success, attempt_time) " +
                     "VALUES (?, ?, ?, NOW())";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < 11; i++) {
                ps.setInt(1, TEST_USER_ID_2);
                ps.setDouble(2, 50.0);
                ps.setBoolean(3, false);
                ps.executeUpdate();
            }
        }
        
        // Next attempt should be locked out
        SignatureAuthService.AuthenticationResult result = 
            service.authenticateWithSignature(TEST_USER_ID_2, signature);
        
        assertFalse(result.isSuccess(), "Should be locked out");
        assertEquals(SignatureAuthService.FailureReason.LOCKED_OUT, result.getFailureReason());
    }
    
    // ========== Attempt Logging Tests ==========
    
    @Test
    @Order(15)
    void testAuthenticateWithSignature_LogsSuccessfulAttempt() throws SQLException {
        // Save a signature
        BufferedImage signature = createSignatureWithContent(100);
        service.saveSignature(TEST_USER_ID, signature);
        
        // Authenticate
        service.authenticateWithSignature(TEST_USER_ID, signature);
        
        // Verify attempt was logged
        String sql = "SELECT COUNT(*) as count FROM signature_auth_attempts WHERE user_id = ? AND success = true";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, TEST_USER_ID);
            var rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getInt("count") > 0, "Successful attempt should be logged");
        }
    }
    
    @Test
    @Order(16)
    void testAuthenticateWithSignature_LogsFailedAttempt() throws SQLException {
        // Save a signature
        BufferedImage signature = createSignatureWithContent(100);
        service.saveSignature(TEST_USER_ID, signature);
        
        // Authenticate with wrong signature
        BufferedImage wrongSignature = createDifferentSignature();
        service.authenticateWithSignature(TEST_USER_ID, wrongSignature);
        
        // Verify attempt was logged
        String sql = "SELECT COUNT(*) as count FROM signature_auth_attempts WHERE user_id = ? AND success = false";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, TEST_USER_ID);
            var rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getInt("count") > 0, "Failed attempt should be logged");
        }
    }
    
    @Test
    @Order(17)
    void testAuthenticateWithSignature_LogsSimilarityScore() throws SQLException {
        // Save a signature
        BufferedImage signature = createSignatureWithContent(100);
        service.saveSignature(TEST_USER_ID, signature);
        
        // Authenticate
        SignatureAuthService.AuthenticationResult result = 
            service.authenticateWithSignature(TEST_USER_ID, signature);
        
        // Verify similarity score was logged
        String sql = "SELECT similarity_score FROM signature_auth_attempts WHERE user_id = ? ORDER BY attempt_time DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, TEST_USER_ID);
            var rs = ps.executeQuery();
            assertTrue(rs.next());
            double loggedScore = rs.getDouble("similarity_score");
            assertEquals(result.getSimilarityScore(), loggedScore, 0.01, "Logged score should match result");
        }
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Create a signature image with specified number of content pixels.
     */
    private BufferedImage createSignatureWithContent(int pixelCount) {
        BufferedImage image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Fill with transparent background
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, 400, 200);
        
        // Draw content pixels
        g2d.setComposite(AlphaComposite.Src);
        g2d.setColor(Color.BLACK);
        
        int pixelsDrawn = 0;
        for (int y = 0; y < 200 && pixelsDrawn < pixelCount; y++) {
            for (int x = 0; x < 400 && pixelsDrawn < pixelCount; x++) {
                g2d.fillRect(x, y, 1, 1);
                pixelsDrawn++;
            }
        }
        
        g2d.dispose();
        return image;
    }
    
    /**
     * Create a signature that looks different from the standard test signature.
     */
    private BufferedImage createDifferentSignature() {
        BufferedImage image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Fill with transparent background
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, 400, 200);
        
        // Draw a completely different pattern (circles instead of pixels)
        g2d.setComposite(AlphaComposite.Src);
        g2d.setColor(Color.WHITE); // Use white instead of black
        g2d.setStroke(new BasicStroke(5));
        
        // Draw multiple circles in different positions
        for (int i = 0; i < 10; i++) {
            g2d.drawOval(50 + i * 30, 50 + i * 10, 20, 20);
        }
        
        g2d.dispose();
        return image;
    }
}
