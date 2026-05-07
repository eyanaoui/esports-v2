package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.SignatureData;
import com.esports.services.EncryptionService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

/**
 * Data Access Object for user signatures.
 * 
 * Manages CRUD operations for signature-based authentication data stored in the
 * user_signatures table. Signatures are stored as encrypted binary PNG images with SHA-256
 * hashes for integrity verification. Uses AES-256-GCM encryption for data at rest.
 * 
 * Requirements: 1.4, 2.1, 2.2, 2.4, 6.4
 */
public class SignatureDAO {

    private Connection con = DatabaseConnection.getInstance().getConnection();
    private EncryptionService encryptionService;

    /**
     * Constructor initializing the DAO with encryption service.
     * 
     * Requirement 2.2: Initialize encryption service for signature data protection
     */
    public SignatureDAO() {
        this.encryptionService = new EncryptionService();
    }

    /**
     * Constructor for testing purposes allowing injection of encryption service.
     * 
     * @param encryptionService The encryption service to use
     */
    SignatureDAO(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    /**
     * Calculate SHA-256 hash of signature data.
     * 
     * Hash is calculated on the unencrypted data for integrity verification.
     * This allows verification that the decrypted data matches the original.
     * 
     * @param data The binary signature data (unencrypted)
     * @return Hexadecimal string representation of the hash
     * @throws RuntimeException if SHA-256 algorithm is not available
     * 
     * Requirement 2.1: Store SHA-256 hash alongside signature data for integrity
     */
    private String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            
            // Convert bytes to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Store a user's signature with encryption and SHA-256 hash.
     * 
     * Creates a new signature record or updates existing signature for the user.
     * The signature data is encrypted using AES-256-GCM before storage.
     * The SHA-256 hash is calculated on the unencrypted data for integrity verification.
     * The unique constraint on user_id ensures one signature per user.
     * Uses ON DUPLICATE KEY UPDATE for upsert behavior.
     * 
     * @param userId The user ID to associate the signature with
     * @param signatureData The binary PNG image data (unencrypted)
     * @return true if signature was saved successfully, false otherwise
     * @throws IllegalArgumentException if parameters are invalid
     * @throws RuntimeException if database operation or encryption fails
     * 
     * Requirement 1.4: Store signature data with encryption
     * Requirement 2.1: Store SHA-256 hash alongside signature data
     * Requirement 2.2: Encrypt signature data before persisting to storage
     * Requirement 2.4: Use AES-256-GCM encryption for signature data at rest
     */
    public boolean saveSignature(int userId, byte[] signatureData) {
        if (signatureData == null || signatureData.length == 0) {
            throw new IllegalArgumentException("Signature data cannot be null or empty");
        }

        // Calculate hash on unencrypted data for integrity verification
        String signatureHash = calculateHash(signatureData);
        
        // Encrypt signature data before storage
        byte[] encryptedData = encryptionService.encrypt(signatureData);
        
        String sql = "INSERT INTO user_signatures (user_id, signature_data, signature_hash) " +
                     "VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "signature_data = VALUES(signature_data), " +
                     "signature_hash = VALUES(signature_hash)";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setBytes(2, encryptedData);
            ps.setString(3, signatureHash);
            
            int rowsAffected = ps.executeUpdate();
            System.out.println("[SUCCESS] Encrypted signature saved for user ID: " + userId);
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] saveSignature error: " + e.getMessage());
            throw new RuntimeException("Failed to save signature: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve a user's signature data with decryption.
     * 
     * Returns the complete signature record including decrypted binary data and hash.
     * The encrypted data is retrieved from the database and decrypted before returning.
     * 
     * @param userId The user ID to retrieve signature for
     * @return SignatureData object containing decrypted signature information, or null if not found
     * @throws RuntimeException if database operation or decryption fails
     * 
     * Requirement 1.4: Retrieve and decrypt stored signature data
     * Requirement 2.4: Decrypt signature data when retrieving for comparison
     */
    public SignatureData getSignature(int userId) {
        String sql = "SELECT * FROM user_signatures WHERE user_id = ?";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                SignatureData signature = new SignatureData();
                signature.setId(rs.getInt("id"));
                signature.setUserId(rs.getInt("user_id"));
                
                // Retrieve encrypted data and decrypt it
                byte[] encryptedData = rs.getBytes("signature_data");
                byte[] decryptedData = encryptionService.decrypt(encryptedData);
                signature.setSignatureData(decryptedData);
                
                signature.setSignatureHash(rs.getString("signature_hash"));
                signature.setCreatedAt(rs.getTimestamp("created_at"));
                signature.setUpdatedAt(rs.getTimestamp("updated_at"));
                return signature;
            }
            return null;
        } catch (SQLException e) {
            System.out.println("[ERROR] getSignature error: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve signature: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing user's signature with encryption.
     * 
     * Replaces the existing signature data with new encrypted data and recalculates the hash.
     * Uses ON DUPLICATE KEY UPDATE behavior through the saveSignature method.
     * The signature data is encrypted using AES-256-GCM before storage.
     * The SHA-256 hash is calculated on the unencrypted data for integrity verification.
     * 
     * @param userId The user ID whose signature to update
     * @param signatureData The new binary PNG image data (unencrypted)
     * @return true if signature was updated successfully, false if no signature exists
     * @throws IllegalArgumentException if parameters are invalid
     * @throws RuntimeException if database operation or encryption fails
     * 
     * Requirement 6.4: Replace existing signature with new encrypted signature
     * Requirement 2.2: Encrypt signature data before persisting to storage
     * Requirement 2.4: Use AES-256-GCM encryption for signature data at rest
     */
    public boolean updateSignature(int userId, byte[] signatureData) {
        if (signatureData == null || signatureData.length == 0) {
            throw new IllegalArgumentException("Signature data cannot be null or empty");
        }

        // Calculate hash on unencrypted data for integrity verification
        String signatureHash = calculateHash(signatureData);
        
        // Encrypt signature data before storage
        byte[] encryptedData = encryptionService.encrypt(signatureData);
        
        String sql = "UPDATE user_signatures SET signature_data = ?, signature_hash = ? WHERE user_id = ?";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setBytes(1, encryptedData);
            ps.setString(2, signatureHash);
            ps.setInt(3, userId);
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("[SUCCESS] Encrypted signature updated for user ID: " + userId);
            }
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] updateSignature error: " + e.getMessage());
            throw new RuntimeException("Failed to update signature: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a user's signature.
     * 
     * Permanently removes the signature record from the database.
     * Used when a user wants to remove their signature authentication method.
     * 
     * @param userId The user ID whose signature to delete
     * @return true if signature was deleted successfully, false if no signature existed
     * @throws RuntimeException if database operation fails
     * 
     * Requirement 6.4: Support signature deletion for account management
     */
    public boolean deleteSignature(int userId) {
        String sql = "DELETE FROM user_signatures WHERE user_id = ?";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("[SUCCESS] Signature deleted for user ID: " + userId);
            }
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] deleteSignature error: " + e.getMessage());
            throw new RuntimeException("Failed to delete signature: " + e.getMessage(), e);
        }
    }
}
