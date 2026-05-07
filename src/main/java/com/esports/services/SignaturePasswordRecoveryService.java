package com.esports.services;

import com.esports.dao.SignatureDAO;
import com.esports.dao.UserDAO;
import com.esports.models.SignatureData;
import com.esports.models.User;
import org.mindrot.jbcrypt.BCrypt;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Service for password recovery using signature verification.
 * 
 * Flow:
 * 1. User provides email and draws signature
 * 2. System retrieves stored signature for that email
 * 3. System compares signatures using ImageComparator
 * 4. If similarity >= 75%, allow password reset
 */
public class SignaturePasswordRecoveryService {
    
    private final UserDAO userDAO;
    private final SignatureDAO signatureDAO;
    private final ImageComparator imageComparator;
    
    private static final double SIMILARITY_THRESHOLD = 75.0;
    
    public SignaturePasswordRecoveryService() {
        this.userDAO = new UserDAO();
        this.signatureDAO = new SignatureDAO();
        this.imageComparator = new ImageComparator();
    }
    
    /**
     * Verify signature and allow password reset if signature matches.
     * 
     * @param email User's email address
     * @param signatureImage Signature image drawn by user
     * @param newPassword New password to set
     * @return RecoveryResult with success status and message
     */
    public RecoveryResult recoverPassword(String email, BufferedImage signatureImage, String newPassword) {
        // Validate inputs
        if (email == null || email.isEmpty()) {
            return new RecoveryResult(false, "Email is required");
        }
        
        if (signatureImage == null) {
            return new RecoveryResult(false, "Signature is required");
        }
        
        if (newPassword == null || newPassword.length() < 4) {
            return new RecoveryResult(false, "Password must be at least 4 characters");
        }
        
        if (newPassword.length() > 100) {
            return new RecoveryResult(false, "Password must not exceed 100 characters");
        }
        
        if (newPassword.contains(" ")) {
            return new RecoveryResult(false, "Password cannot contain spaces");
        }
        
        try {
            // Find user by email
            User user = userDAO.findByEmail(email);
            if (user == null) {
                // Don't reveal if email exists (security)
                return new RecoveryResult(false, "Invalid email or signature");
            }
            
            // Check if user is blocked
            if (user.getIsBlocked() != null && user.getIsBlocked()) {
                return new RecoveryResult(false, "Account is blocked. Please contact support.");
            }
            
            // Retrieve stored signature
            SignatureData storedSignature = signatureDAO.getSignature(user.getId());
            if (storedSignature == null) {
                return new RecoveryResult(false, "No signature found for this account");
            }
            
            // Convert stored signature bytes to BufferedImage
            BufferedImage storedImage;
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(storedSignature.getSignatureData());
                storedImage = ImageIO.read(bais);
                if (storedImage == null) {
                    System.err.println("[Password Recovery] Failed to decode stored signature image");
                    return new RecoveryResult(false, "Error loading stored signature");
                }
            } catch (IOException e) {
                System.err.println("[Password Recovery] Error reading stored signature: " + e.getMessage());
                return new RecoveryResult(false, "Error loading stored signature");
            }
            
            // Compare signatures
            double similarity = imageComparator.calculateSimilarity(signatureImage, storedImage);
            
            System.out.println("[Password Recovery] Signature similarity: " + similarity + "% for user: " + email);
            
            // Check if similarity meets threshold
            if (similarity < SIMILARITY_THRESHOLD) {
                return new RecoveryResult(false, "Signature does not match. Please try again.");
            }
            
            // Signature matches - update password
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            user.setPassword(hashedPassword);
            
            boolean updated = userDAO.update(user);
            
            if (!updated) {
                return new RecoveryResult(false, "Failed to update password. Please try again.");
            }
            
            System.out.println("[Password Recovery] Password reset successfully for user: " + email);
            return new RecoveryResult(true, "Password reset successfully! You can now login with your new password.");
            
        } catch (Exception e) {
            System.err.println("[Password Recovery] Error: " + e.getMessage());
            e.printStackTrace();
            return new RecoveryResult(false, "An error occurred. Please try again.");
        }
    }
    
    /**
     * Verify if a user has a signature registered (for UI validation).
     * 
     * @param email User's email address
     * @return true if user exists and has a signature
     */
    public boolean hasSignature(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        
        try {
            User user = userDAO.findByEmail(email);
            if (user == null) {
                return false;
            }
            
            SignatureData signature = signatureDAO.getSignature(user.getId());
            return signature != null;
            
        } catch (Exception e) {
            System.err.println("[Password Recovery] Error checking signature: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Result object for password recovery operation.
     */
    public static class RecoveryResult {
        private final boolean success;
        private final String message;
        
        public RecoveryResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
