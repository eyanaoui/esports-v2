package com.esports.services;

import com.esports.dao.SignatureDAO;
import com.esports.db.DatabaseConnection;
import com.esports.models.SignatureData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Service for managing signature-based authentication.
 * 
 * This service orchestrates signature authentication including:
 * - Signature registration with validation (minimum 50 pixels of content)
 * - Signature comparison using ImageComparator (threshold: 75)
 * - Rate limiting to prevent brute force (10 attempts/minute, 5-minute lockout)
 * - Attempt tracking in signature_auth_attempts table
 * - 2-second delay on failed attempts
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 8.6, 8.7, 8.8, 8.9
 */
public class SignatureAuthService {
    
    private final SignatureDAO signatureDAO;
    private final ImageComparator imageComparator;
    private final Connection connection;
    
    // Constants
    private static final int MINIMUM_CONTENT_PIXELS = 50;
    private static final double SIMILARITY_THRESHOLD = 75.0;
    private static final int RATE_LIMIT_ATTEMPTS = 10;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 1;
    private static final int LOCKOUT_DURATION_MINUTES = 5;
    private static final int FAILED_ATTEMPT_DELAY_SECONDS = 2;
    
    /**
     * Constructor with dependency injection.
     * 
     * @param signatureDAO DAO for signature storage/retrieval
     * @param imageComparator Service for image comparison
     */
    public SignatureAuthService(SignatureDAO signatureDAO, ImageComparator imageComparator) {
        this.signatureDAO = signatureDAO;
        this.imageComparator = imageComparator;
        this.connection = DatabaseConnection.getInstance().getConnection();
    }
    
    /**
     * Default constructor using default dependencies.
     */
    public SignatureAuthService() {
        this(new SignatureDAO(), new ImageComparator());
    }
    
    /**
     * Save a user's signature with minimum content validation.
     * 
     * Validates that the signature contains at least 50 pixels of drawn content
     * (non-transparent pixels) before storing. Converts BufferedImage to PNG
     * format and stores as binary data.
     * 
     * @param userId The user ID to associate the signature with
     * @param signatureImage The signature image captured from canvas
     * @return true if signature was saved successfully
     * @throws IllegalArgumentException if signature is null or has insufficient content
     * @throws RuntimeException if image conversion or storage fails
     * 
     * Requirement 2.3: Store signature in database
     * Requirement 2.4: Require minimum 50 pixels of content
     */
    public boolean saveSignature(int userId, BufferedImage signatureImage) {
        if (signatureImage == null) {
            throw new IllegalArgumentException("Signature image cannot be null");
        }
        
        // Validate minimum content
        if (!hasMinimumContent(signatureImage)) {
            throw new IllegalArgumentException(
                "Signature must contain at least " + MINIMUM_CONTENT_PIXELS + " pixels of content"
            );
        }
        
        try {
            // Convert BufferedImage to PNG byte array
            byte[] imageData = convertImageToBytes(signatureImage);
            
            // Store in database
            boolean saved = signatureDAO.saveSignature(userId, imageData);
            
            if (saved) {
                System.out.println("[SUCCESS] Signature saved for user ID: " + userId);
            }
            
            return saved;
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to convert signature image: " + e.getMessage());
            throw new RuntimeException("Failed to save signature: " + e.getMessage(), e);
        }
    }
    
    /**
     * Authenticate a user with their signature.
     * 
     * Compares the provided signature against the stored signature using
     * ImageComparator. Implements rate limiting (10 attempts/minute) and
     * lockout mechanism (5 minutes after rate limit). Logs all attempts
     * and adds 2-second delay on failed attempts.
     * 
     * @param userId The user ID attempting authentication
     * @param attemptImage The signature image to authenticate
     * @return AuthenticationResult containing success status, similarity score, and failure reason
     * @throws IllegalArgumentException if attemptImage is null
     * @throws RuntimeException if authentication process fails
     * 
     * Requirement 2.6: Compare signature to stored signature
     * Requirement 2.7: Calculate similarity score
     * Requirement 2.8: Authenticate if score >= 75
     * Requirement 2.9: Reject if score < 75
     * Requirement 8.6: Log authentication attempts
     * Requirement 8.7: Implement 2-second delay on failure
     * Requirement 8.8: Rate limit to 10 attempts per minute
     * Requirement 8.9: Lock out for 5 minutes after rate limit
     */
    public AuthenticationResult authenticateWithSignature(int userId, BufferedImage attemptImage) {
        if (attemptImage == null) {
            throw new IllegalArgumentException("Attempt image cannot be null");
        }
        
        try {
            // Check if user is locked out
            if (isLockedOut(userId)) {
                System.out.println("[RATE_LIMIT] User " + userId + " is locked out");
                return new AuthenticationResult(false, 0.0, FailureReason.LOCKED_OUT);
            }
            
            // Check rate limiting
            if (isRateLimited(userId)) {
                System.out.println("[RATE_LIMIT] User " + userId + " exceeded rate limit");
                // Lock out the user
                return new AuthenticationResult(false, 0.0, FailureReason.RATE_LIMITED);
            }
            
            // Retrieve stored signature
            SignatureData storedSignature = signatureDAO.getSignature(userId);
            if (storedSignature == null) {
                System.out.println("[ERROR] No signature found for user ID: " + userId);
                return new AuthenticationResult(false, 0.0, FailureReason.NO_SIGNATURE);
            }
            
            // Convert stored signature bytes to BufferedImage
            BufferedImage storedImage = convertBytesToImage(storedSignature.getSignatureData());
            
            // Calculate similarity score
            double similarityScore = imageComparator.calculateSimilarity(storedImage, attemptImage);
            
            // Determine authentication success
            boolean success = similarityScore >= SIMILARITY_THRESHOLD;
            
            // Log the attempt
            logAuthenticationAttempt(userId, similarityScore, success);
            
            if (success) {
                System.out.println("[SUCCESS] Signature authentication successful for user " + userId + 
                                 " (score: " + String.format("%.2f", similarityScore) + ")");
                return new AuthenticationResult(true, similarityScore, null);
            } else {
                System.out.println("[FAILURE] Signature authentication failed for user " + userId + 
                                 " (score: " + String.format("%.2f", similarityScore) + ")");
                
                // Implement 2-second delay on failed attempts
                try {
                    Thread.sleep(FAILED_ATTEMPT_DELAY_SECONDS * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                return new AuthenticationResult(false, similarityScore, FailureReason.LOW_SIMILARITY);
            }
            
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to process signature image: " + e.getMessage());
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if signature has minimum content (at least 50 non-transparent pixels).
     * 
     * Counts pixels that are not fully transparent (alpha > 0) to determine
     * if the signature has sufficient drawn content.
     * 
     * @param image The signature image to validate
     * @return true if image has at least 50 pixels of content
     * 
     * Requirement 2.4: Validate minimum 50 pixels of content
     */
    boolean hasMinimumContent(BufferedImage image) {
        if (image == null) {
            return false;
        }
        
        int contentPixels = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                
                // Count non-transparent pixels
                if (alpha > 0) {
                    contentPixels++;
                    
                    // Early exit if we've found enough content
                    if (contentPixels >= MINIMUM_CONTENT_PIXELS) {
                        return true;
                    }
                }
            }
        }
        
        return contentPixels >= MINIMUM_CONTENT_PIXELS;
    }
    
    /**
     * Check if user is currently locked out due to rate limiting.
     * 
     * A user is locked out if they have exceeded the rate limit within
     * the last 5 minutes.
     * 
     * @param userId The user ID to check
     * @return true if user is locked out
     * 
     * Requirement 8.9: Lock out for 5 minutes after rate limit
     */
    private boolean isLockedOut(int userId) {
        String sql = "SELECT COUNT(*) as attempt_count " +
                     "FROM signature_auth_attempts " +
                     "WHERE user_id = ? " +
                     "AND attempt_time >= DATE_SUB(NOW(), INTERVAL ? MINUTE)";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, LOCKOUT_DURATION_MINUTES);
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int attemptCount = rs.getInt("attempt_count");
                // User is locked out if they have more than rate limit attempts in lockout window
                return attemptCount > RATE_LIMIT_ATTEMPTS;
            }
            return false;
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to check lockout status: " + e.getMessage());
            // Fail safe: assume not locked out to avoid blocking legitimate users
            return false;
        }
    }
    
    /**
     * Check if user has exceeded rate limit (10 attempts per minute).
     * 
     * Counts authentication attempts in the last minute. If count exceeds
     * the limit, returns true to trigger lockout.
     * 
     * @param userId The user ID to check
     * @return true if rate limit exceeded
     * 
     * Requirement 8.8: Rate limit to 10 attempts per minute
     */
    private boolean isRateLimited(int userId) {
        String sql = "SELECT COUNT(*) as attempt_count " +
                     "FROM signature_auth_attempts " +
                     "WHERE user_id = ? " +
                     "AND attempt_time >= DATE_SUB(NOW(), INTERVAL ? MINUTE)";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, RATE_LIMIT_WINDOW_MINUTES);
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int attemptCount = rs.getInt("attempt_count");
                return attemptCount >= RATE_LIMIT_ATTEMPTS;
            }
            return false;
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to check rate limit: " + e.getMessage());
            // Fail safe: assume not rate limited to avoid blocking legitimate users
            return false;
        }
    }
    
    /**
     * Log an authentication attempt to the database.
     * 
     * Records the attempt with user ID, similarity score, success status,
     * and timestamp for rate limiting and audit purposes.
     * 
     * @param userId The user ID attempting authentication
     * @param similarityScore The calculated similarity score
     * @param success Whether authentication was successful
     * 
     * Requirement 8.6: Log authentication attempts
     */
    private void logAuthenticationAttempt(int userId, double similarityScore, boolean success) {
        String sql = "INSERT INTO signature_auth_attempts (user_id, similarity_score, success) " +
                     "VALUES (?, ?, ?)";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDouble(2, similarityScore);
            ps.setBoolean(3, success);
            
            ps.executeUpdate();
            System.out.println("[LOG] Logged authentication attempt for user " + userId);
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to log authentication attempt: " + e.getMessage());
            // Don't throw exception - logging failure shouldn't break authentication
        }
    }
    
    /**
     * Convert BufferedImage to PNG byte array.
     * 
     * @param image The image to convert
     * @return PNG image data as byte array
     * @throws IOException if conversion fails
     */
    private byte[] convertImageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
    
    /**
     * Convert PNG byte array to BufferedImage.
     * 
     * @param imageData The PNG image data
     * @return BufferedImage representation
     * @throws IOException if conversion fails
     */
    private BufferedImage convertBytesToImage(byte[] imageData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        return ImageIO.read(bais);
    }
    
    /**
     * Result of signature authentication attempt.
     */
    public static class AuthenticationResult {
        private final boolean success;
        private final double similarityScore;
        private final FailureReason failureReason;
        
        public AuthenticationResult(boolean success, double similarityScore, FailureReason failureReason) {
            this.success = success;
            this.similarityScore = similarityScore;
            this.failureReason = failureReason;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public double getSimilarityScore() {
            return similarityScore;
        }
        
        public FailureReason getFailureReason() {
            return failureReason;
        }
        
        @Override
        public String toString() {
            return "AuthenticationResult{" +
                    "success=" + success +
                    ", similarityScore=" + String.format("%.2f", similarityScore) +
                    ", failureReason=" + failureReason +
                    '}';
        }
    }
    
    /**
     * Reasons for authentication failure.
     */
    public enum FailureReason {
        LOW_SIMILARITY,
        NO_SIGNATURE,
        RATE_LIMITED,
        LOCKED_OUT
    }
}
