package com.esports.models;

import java.sql.Timestamp;

/**
 * Domain model representing user signature data for signature-based authentication.
 * 
 * This model stores signature images as binary data (PNG format) along with
 * SHA-256 hash for integrity verification. Signatures are captured from a
 * drawing canvas and stored in the user_signatures table.
 * 
 * Requirements: 8.1, 8.2, 8.5
 */
public class SignatureData {
    private int id;
    private int userId;
    private byte[] signatureData;
    private String signatureHash;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors
    public SignatureData() {
    }

    public SignatureData(int userId, byte[] signatureData, String signatureHash) {
        this.userId = userId;
        this.signatureData = signatureData;
        this.signatureHash = signatureHash;
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

    public byte[] getSignatureData() {
        return signatureData;
    }

    public void setSignatureData(byte[] signatureData) {
        this.signatureData = signatureData;
    }

    public String getSignatureHash() {
        return signatureHash;
    }

    public void setSignatureHash(String signatureHash) {
        this.signatureHash = signatureHash;
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
        return "SignatureData{" +
                "id=" + id +
                ", userId=" + userId +
                ", signatureHash='" + signatureHash + '\'' +
                ", dataSize=" + (signatureData != null ? signatureData.length : 0) + " bytes" +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
