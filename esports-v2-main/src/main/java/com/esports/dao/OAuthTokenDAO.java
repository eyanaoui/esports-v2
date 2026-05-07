package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.OAuthTokens;

import java.sql.*;

/**
 * Data Access Object for OAuth tokens.
 * 
 * Manages CRUD operations for OAuth 2.0 tokens stored in the oauth_tokens table.
 * All tokens are stored encrypted using AES-256-GCM encryption via EncryptionService.
 * 
 * Requirements: 7.1, 7.2, 7.10
 */
public class OAuthTokenDAO {

    private Connection con = DatabaseConnection.getInstance().getConnection();

    /**
     * Store encrypted OAuth tokens for a user.
     * 
     * Creates a new token record or updates existing tokens for the user.
     * The unique constraint on user_id ensures one token set per user.
     * 
     * @param userId The user ID to associate tokens with
     * @param encryptedAccessToken The encrypted access token
     * @param encryptedRefreshToken The encrypted refresh token
     * @param expiresAt The access token expiration timestamp
     * @return true if tokens were saved successfully, false otherwise
     * @throws RuntimeException if database operation fails
     * 
     * Requirement 7.1: Store OAuth access tokens encrypted in the database
     * Requirement 7.2: Store OAuth refresh tokens encrypted in the database
     */
    public boolean saveTokens(int userId, String encryptedAccessToken, 
                             String encryptedRefreshToken, Timestamp expiresAt) {
        if (encryptedAccessToken == null || encryptedAccessToken.isEmpty()) {
            throw new IllegalArgumentException("Encrypted access token cannot be null or empty");
        }
        if (encryptedRefreshToken == null || encryptedRefreshToken.isEmpty()) {
            throw new IllegalArgumentException("Encrypted refresh token cannot be null or empty");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiration timestamp cannot be null");
        }

        String sql = "INSERT INTO oauth_tokens (user_id, encrypted_access_token, encrypted_refresh_token, expires_at) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "encrypted_access_token = VALUES(encrypted_access_token), " +
                     "encrypted_refresh_token = VALUES(encrypted_refresh_token), " +
                     "expires_at = VALUES(expires_at)";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, encryptedAccessToken);
            ps.setString(3, encryptedRefreshToken);
            ps.setTimestamp(4, expiresAt);
            
            int rowsAffected = ps.executeUpdate();
            System.out.println("[SUCCESS] OAuth tokens saved for user ID: " + userId);
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] saveTokens error: " + e.getMessage());
            throw new RuntimeException("Failed to save OAuth tokens: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve OAuth tokens for a specific user.
     * 
     * Returns the encrypted tokens which must be decrypted by the caller
     * using EncryptionService before use.
     * 
     * @param userId The user ID to retrieve tokens for
     * @return OAuthTokens object containing encrypted tokens, or null if not found
     * @throws RuntimeException if database operation fails
     * 
     * Requirement 7.1: Retrieve stored OAuth tokens
     * Requirement 7.2: Retrieve stored refresh tokens
     */
    public OAuthTokens getTokensByUserId(int userId) {
        String sql = "SELECT * FROM oauth_tokens WHERE user_id = ?";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                OAuthTokens tokens = new OAuthTokens();
                tokens.setId(rs.getInt("id"));
                tokens.setUserId(rs.getInt("user_id"));
                tokens.setEncryptedAccessToken(rs.getString("encrypted_access_token"));
                tokens.setEncryptedRefreshToken(rs.getString("encrypted_refresh_token"));
                tokens.setExpiresAt(rs.getTimestamp("expires_at"));
                tokens.setCreatedAt(rs.getTimestamp("created_at"));
                tokens.setUpdatedAt(rs.getTimestamp("updated_at"));
                return tokens;
            }
            return null;
        } catch (SQLException e) {
            System.out.println("[ERROR] getTokensByUserId error: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve OAuth tokens: " + e.getMessage(), e);
        }
    }

    /**
     * Update the access token after refresh.
     * 
     * When an access token expires, it can be refreshed using the refresh token.
     * This method updates only the access token and expiration time, preserving
     * the refresh token.
     * 
     * @param userId The user ID whose token to update
     * @param encryptedAccessToken The new encrypted access token
     * @param expiresAt The new expiration timestamp
     * @return true if token was updated successfully, false otherwise
     * @throws RuntimeException if database operation fails
     * 
     * Requirement 7.10: Update access token when refreshed
     */
    public boolean updateAccessToken(int userId, String encryptedAccessToken, Timestamp expiresAt) {
        if (encryptedAccessToken == null || encryptedAccessToken.isEmpty()) {
            throw new IllegalArgumentException("Encrypted access token cannot be null or empty");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiration timestamp cannot be null");
        }

        String sql = "UPDATE oauth_tokens SET encrypted_access_token = ?, expires_at = ? WHERE user_id = ?";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, encryptedAccessToken);
            ps.setTimestamp(2, expiresAt);
            ps.setInt(3, userId);
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("[SUCCESS] Access token updated for user ID: " + userId);
            }
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] updateAccessToken error: " + e.getMessage());
            throw new RuntimeException("Failed to update access token: " + e.getMessage(), e);
        }
    }

    /**
     * Delete OAuth tokens for a user.
     * 
     * Used when a user disconnects their Google account or when tokens
     * need to be revoked. This permanently removes the token record.
     * 
     * @param userId The user ID whose tokens to delete
     * @return true if tokens were deleted successfully, false if no tokens existed
     * @throws RuntimeException if database operation fails
     * 
     * Requirement 7.10: Delete stored OAuth tokens when user disconnects
     */
    public boolean deleteTokens(int userId) {
        String sql = "DELETE FROM oauth_tokens WHERE user_id = ?";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("[SUCCESS] OAuth tokens deleted for user ID: " + userId);
            }
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] deleteTokens error: " + e.getMessage());
            throw new RuntimeException("Failed to delete OAuth tokens: " + e.getMessage(), e);
        }
    }
}
