package com.esports.models;

import java.sql.Timestamp;

/**
 * Domain model representing OAuth 2.0 tokens for Google authentication.
 * 
 * This model stores encrypted access and refresh tokens along with expiration
 * information. Tokens are encrypted using AES-256-GCM before storage and
 * decrypted when retrieved.
 * 
 * Requirements: 7.1, 7.2
 */
public class OAuthTokens {
    private int id;
    private int userId;
    private String encryptedAccessToken;
    private String encryptedRefreshToken;
    private Timestamp expiresAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors
    public OAuthTokens() {
    }

    public OAuthTokens(int userId, String encryptedAccessToken, String encryptedRefreshToken, Timestamp expiresAt) {
        this.userId = userId;
        this.encryptedAccessToken = encryptedAccessToken;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEncryptedAccessToken() {
        return encryptedAccessToken;
    }

    public void setEncryptedAccessToken(String encryptedAccessToken) {
        this.encryptedAccessToken = encryptedAccessToken;
    }

    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }

    public void setEncryptedRefreshToken(String encryptedRefreshToken) {
        this.encryptedRefreshToken = encryptedRefreshToken;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "OAuthTokens{" +
                "id=" + id +
                ", userId=" + userId +
                ", expiresAt=" + expiresAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
