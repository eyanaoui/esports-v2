package com.esports.models;

import java.sql.Timestamp;

/**
 * Domain model representing an audit log entry.
 * 
 * This model provides a comprehensive audit trail for all signature-related events
 * including authentication attempts, signature registrations, and signature updates.
 * 
 * Requirements: 5.1, 6.5, 10.1, 10.2, 10.3, 10.4, 10.5
 */
public class AuditLogEntry {
    private int id;
    private int userId;
    private String eventType;
    private String accountIdentifier;
    private Double similarityScore;
    private String deviceInfo;
    private Timestamp timestamp;

    /**
     * Event types for audit logging
     */
    public static class EventType {
        public static final String REGISTRATION = "REGISTRATION";
        public static final String UPDATE = "UPDATE";
        public static final String AUTH_SUCCESS = "AUTH_SUCCESS";
        public static final String AUTH_FAILURE = "AUTH_FAILURE";
        public static final String RATE_LIMITED = "RATE_LIMITED";
        public static final String LOCKED_OUT = "LOCKED_OUT";
    }

    // Constructors
    public AuditLogEntry() {
    }

    public AuditLogEntry(int userId, String eventType, String accountIdentifier) {
        this.userId = userId;
        this.eventType = eventType;
        this.accountIdentifier = accountIdentifier;
    }

    public AuditLogEntry(int userId, String eventType, String accountIdentifier, Double similarityScore, String deviceInfo) {
        this.userId = userId;
        this.eventType = eventType;
        this.accountIdentifier = accountIdentifier;
        this.similarityScore = similarityScore;
        this.deviceInfo = deviceInfo;
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAccountIdentifier() {
        return accountIdentifier;
    }

    public void setAccountIdentifier(String accountIdentifier) {
        this.accountIdentifier = accountIdentifier;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AuditLogEntry{" +
                "id=" + id +
                ", userId=" + userId +
                ", eventType='" + eventType + '\'' +
                ", accountIdentifier='" + accountIdentifier + '\'' +
                ", similarityScore=" + similarityScore +
                ", deviceInfo='" + deviceInfo + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
