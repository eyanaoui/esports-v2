package com.esports.models;

import java.sql.Timestamp;

/**
 * Domain model representing a signature authentication attempt.
 * 
 * This model tracks authentication attempts for rate limiting and audit purposes.
 * Each attempt records the user ID, similarity score, success status, and timestamp.
 * 
 * Requirements: 5.1, 10.2, 10.4
 */
public class SignatureAuthAttempt {
    private int id;
    private int userId;
    private double similarityScore;
    private boolean success;
    private Timestamp attemptTime;

    // Constructors
    public SignatureAuthAttempt() {
    }

    public SignatureAuthAttempt(int userId, double similarityScore, boolean success) {
        this.userId = userId;
        this.similarityScore = similarityScore;
        this.success = success;
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

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Timestamp getAttemptTime() {
        return attemptTime;
    }

    public void setAttemptTime(Timestamp attemptTime) {
        this.attemptTime = attemptTime;
    }

    @Override
    public String toString() {
        return "SignatureAuthAttempt{" +
                "id=" + id +
                ", userId=" + userId +
                ", similarityScore=" + similarityScore +
                ", success=" + success +
                ", attemptTime=" + attemptTime +
                '}';
    }
}
