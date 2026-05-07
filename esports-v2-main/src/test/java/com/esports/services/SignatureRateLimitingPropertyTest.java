package com.esports.services;

import com.esports.dao.SignatureDAO;
import com.esports.db.DatabaseConnection;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for signature authentication rate limiting.
 * 
 * **Validates: Property 10: Signature Rate Limiting Effectiveness**
 * 
 * Property: For all users attempting signature authentication, if a user makes N 
 * authentication attempts within 1 minute where N > 10, then attempts 11 through N 
 * SHALL be rejected with rate limit error, and the user SHALL be locked out for 5 
 * minutes, during which all signature authentication attempts SHALL fail.
 */
class SignatureRateLimitingPropertyTest {
    
    private SignatureAuthService service;
    private SignatureDAO signatureDAO;
    private ImageComparator imageComparator;
    private Connection connection;
    
    // Test user ID range to avoid conflicts
    private static final int TEST_USER_BASE = 80000;
    
    @Property(tries = 10)
    @Label("Rate limiting enforces 10 attempts per minute threshold")
    void rateLimitingEnforcesThreshold(
        @ForAll @IntRange(min = 11, max = 20) int attemptCount,
        @ForAll @IntRange(min = 1, max = 100) int userOffset
    ) throws SQLException {
        // Setup
        int testUserId = TEST_USER_BASE + userOffset;
        setupTest(testUserId);
        
        try {
            // Create and save a signature
            BufferedImage signature = createTestSignature(100);
            service.saveSignature(testUserId, signature);
            
            // Create a different signature that will fail authentication
            BufferedImage wrongSignature = createDifferentSignature();
            
            // Make N attempts
            int successfulAttempts = 0;
            int rateLimitedAttempts = 0;
            
            for (int i = 0; i < attemptCount; i++) {
                SignatureAuthService.AuthenticationResult result = 
                    service.authenticateWithSignature(testUserId, wrongSignature);
                
                if (i < 10) {
                    // First 10 attempts should be processed (though they fail due to wrong signature)
                    assertNotEquals(SignatureAuthService.FailureReason.RATE_LIMITED, 
                                  result.getFailureReason(),
                                  "Attempt " + (i + 1) + " should not be rate limited");
                    successfulAttempts++;
                } else {
                    // Attempts 11+ should be rate limited
                    assertFalse(result.isSuccess(), 
                              "Attempt " + (i + 1) + " should fail");
                    assertTrue(
                        result.getFailureReason() == SignatureAuthService.FailureReason.RATE_LIMITED ||
                        result.getFailureReason() == SignatureAuthService.FailureReason.LOCKED_OUT,
                        "Attempt " + (i + 1) + " should be rate limited or locked out"
                    );
                    rateLimitedAttempts++;
                }
            }
            
            // Verify rate limiting was enforced
            assertEquals(10, successfulAttempts, 
                        "Exactly 10 attempts should be processed before rate limiting");
            assertEquals(attemptCount - 10, rateLimitedAttempts,
                        "All attempts after 10 should be rate limited");
            
        } finally {
            cleanupTest(testUserId);
        }
    }
    
    @Property(tries = 10)
    @Label("Lockout persists for subsequent attempts after rate limit")
    void lockoutPersistsAfterRateLimit(
        @ForAll @IntRange(min = 1, max = 100) int userOffset
    ) throws SQLException {
        // Setup
        int testUserId = TEST_USER_BASE + userOffset + 1000;
        setupTest(testUserId);
        
        try {
            // Create and save a signature
            BufferedImage signature = createTestSignature(100);
            service.saveSignature(testUserId, signature);
            
            // Manually insert 11 attempts to trigger lockout
            insertAttempts(testUserId, 11);
            
            // Try to authenticate - should be locked out
            SignatureAuthService.AuthenticationResult result = 
                service.authenticateWithSignature(testUserId, signature);
            
            assertFalse(result.isSuccess(), "Should be locked out");
            assertEquals(SignatureAuthService.FailureReason.LOCKED_OUT, 
                        result.getFailureReason(),
                        "Should fail with LOCKED_OUT reason");
            
        } finally {
            cleanupTest(testUserId);
        }
    }
    
    @Property(tries = 10)
    @Label("Rate limiting is per-user and does not affect other users")
    void rateLimitingIsPerUser(
        @ForAll @IntRange(min = 1, max = 50) int user1Offset,
        @ForAll @IntRange(min = 51, max = 100) int user2Offset
    ) throws SQLException {
        // Setup two different users
        int testUserId1 = TEST_USER_BASE + user1Offset + 2000;
        int testUserId2 = TEST_USER_BASE + user2Offset + 2000;
        
        setupTest(testUserId1);
        setupTest(testUserId2);
        
        try {
            // Create signatures for both users
            BufferedImage signature1 = createTestSignature(100);
            BufferedImage signature2 = createTestSignature(150);
            service.saveSignature(testUserId1, signature1);
            service.saveSignature(testUserId2, signature2);
            
            // Rate limit user 1 by making 11 attempts
            BufferedImage wrongSignature = createDifferentSignature();
            for (int i = 0; i < 11; i++) {
                service.authenticateWithSignature(testUserId1, wrongSignature);
            }
            
            // Verify user 1 is rate limited
            SignatureAuthService.AuthenticationResult result1 = 
                service.authenticateWithSignature(testUserId1, wrongSignature);
            assertTrue(
                result1.getFailureReason() == SignatureAuthService.FailureReason.RATE_LIMITED ||
                result1.getFailureReason() == SignatureAuthService.FailureReason.LOCKED_OUT,
                "User 1 should be rate limited"
            );
            
            // Verify user 2 is NOT rate limited
            SignatureAuthService.AuthenticationResult result2 = 
                service.authenticateWithSignature(testUserId2, signature2);
            assertNotEquals(SignatureAuthService.FailureReason.RATE_LIMITED, 
                          result2.getFailureReason(),
                          "User 2 should not be rate limited");
            assertNotEquals(SignatureAuthService.FailureReason.LOCKED_OUT, 
                          result2.getFailureReason(),
                          "User 2 should not be locked out");
            
        } finally {
            cleanupTest(testUserId1);
            cleanupTest(testUserId2);
        }
    }
    
    @Property(tries = 10)
    @Label("Successful authentication does not bypass rate limiting")
    void successfulAuthenticationCountsTowardRateLimit(
        @ForAll @IntRange(min = 1, max = 100) int userOffset
    ) throws SQLException {
        // Setup
        int testUserId = TEST_USER_BASE + userOffset + 3000;
        setupTest(testUserId);
        
        try {
            // Create and save a signature
            BufferedImage signature = createTestSignature(100);
            service.saveSignature(testUserId, signature);
            
            // Make 10 successful authentication attempts
            for (int i = 0; i < 10; i++) {
                SignatureAuthService.AuthenticationResult result = 
                    service.authenticateWithSignature(testUserId, signature);
                assertTrue(result.isSuccess() || 
                          result.getFailureReason() != SignatureAuthService.FailureReason.RATE_LIMITED,
                          "First 10 attempts should not be rate limited");
            }
            
            // 11th attempt should be rate limited even if it would succeed
            SignatureAuthService.AuthenticationResult result = 
                service.authenticateWithSignature(testUserId, signature);
            
            assertFalse(result.isSuccess(), "11th attempt should fail");
            assertTrue(
                result.getFailureReason() == SignatureAuthService.FailureReason.RATE_LIMITED ||
                result.getFailureReason() == SignatureAuthService.FailureReason.LOCKED_OUT,
                "11th attempt should be rate limited or locked out"
            );
            
        } finally {
            cleanupTest(testUserId);
        }
    }
    
    // ========== Helper Methods ==========
    
    private void setupTest(int userId) throws SQLException {
        signatureDAO = new SignatureDAO();
        imageComparator = new ImageComparator();
        service = new SignatureAuthService(signatureDAO, imageComparator);
        connection = DatabaseConnection.getInstance().getConnection();
        
        cleanupTest(userId);
        
        // Create test user
        createTestUser(userId);
    }
    
    private void createTestUser(int userId) throws SQLException {
        String sql = "INSERT IGNORE INTO user (id, first_name, last_name, email, password, roles) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, "Test");
            ps.setString(3, "User" + userId);
            ps.setString(4, "test" + userId + "@test.com");
            ps.setString(5, "$2a$10$dummyhash");
            ps.setString(6, "[\"USER\"]");
            ps.executeUpdate();
        } catch (SQLException e) {
            // Ignore if user already exists
        }
    }
    
    private void cleanupTest(int userId) throws SQLException {
        if (connection == null) {
            connection = DatabaseConnection.getInstance().getConnection();
        }
        
        // Delete test signatures
        String deleteSigs = "DELETE FROM user_signatures WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(deleteSigs)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        
        // Delete test attempts
        String deleteAttempts = "DELETE FROM signature_auth_attempts WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(deleteAttempts)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        
        // Delete test user
        String deleteUser = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(deleteUser)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
    
    private void insertAttempts(int userId, int count) throws SQLException {
        String sql = "INSERT INTO signature_auth_attempts (user_id, similarity_score, success, attempt_time) " +
                     "VALUES (?, ?, ?, NOW())";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                ps.setInt(1, userId);
                ps.setDouble(2, 50.0);
                ps.setBoolean(3, false);
                ps.executeUpdate();
            }
        }
    }
    
    private BufferedImage createTestSignature(int pixelCount) {
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
    
    private BufferedImage createDifferentSignature() {
        BufferedImage image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Fill with transparent background
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, 400, 200);
        
        // Draw a different pattern (diagonal line)
        g2d.setComposite(AlphaComposite.Src);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(0, 0, 400, 200);
        
        g2d.dispose();
        return image;
    }
}
