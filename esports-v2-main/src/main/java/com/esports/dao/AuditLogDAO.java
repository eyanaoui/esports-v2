package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.AuditLogEntry;
import com.esports.models.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for audit log operations.
 * 
 * Manages audit trail for signature authentication events including authentication attempts,
 * signature registrations, and signature updates. Provides comprehensive logging and querying
 * capabilities with filtering support and automatic 90-day retention policy enforcement.
 * 
 * Requirements: 5.1, 6.5, 10.1, 10.2, 10.3, 10.5
 */
public class AuditLogDAO {

    private Connection con = DatabaseConnection.getInstance().getConnection();
    private UserDAO userDAO;

    /**
     * Constructor initializing the DAO with database connection.
     * 
     * Requirement 10.1: Initialize audit logging capability
     */
    public AuditLogDAO() {
        this.userDAO = new UserDAO();
    }

    /**
     * Constructor for testing purposes allowing injection of UserDAO.
     * 
     * @param userDAO The UserDAO instance to use
     */
    AuditLogDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Log an authentication attempt (success or failure).
     * 
     * Records authentication events with similarity score and device information.
     * Creates an audit log entry with event type AUTH_SUCCESS or AUTH_FAILURE.
     * Retrieves account identifier (email) from the user record.
     * 
     * @param userId The user ID attempting authentication
     * @param score The similarity score calculated during authentication
     * @param success Whether the authentication succeeded
     * @param deviceInfo Device/browser information (can be null)
     * @throws RuntimeException if database operation fails
     * 
     * Requirement 5.1: Log all authentication attempts with timestamp and result
     * Requirement 10.1: Create audit log entry for authentication events
     * Requirement 10.2: Record account identifier, timestamp, result, and device information
     */
    public void logAuthenticationAttempt(int userId, double score, boolean success, String deviceInfo) {
        // Retrieve user to get account identifier
        User user = userDAO.findByEmail(getUserEmail(userId));
        String accountIdentifier = (user != null) ? user.getEmail() : "unknown";
        
        String eventType = success ? AuditLogEntry.EventType.AUTH_SUCCESS : AuditLogEntry.EventType.AUTH_FAILURE;
        
        String sql = "INSERT INTO audit_log (user_id, event_type, account_identifier, similarity_score, device_info) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, eventType);
            ps.setString(3, accountIdentifier);
            ps.setDouble(4, score);
            ps.setString(5, deviceInfo);
            
            ps.executeUpdate();
            System.out.println("[SUCCESS] Authentication attempt logged for user ID: " + userId + " (success: " + success + ")");
        } catch (SQLException e) {
            System.out.println("[ERROR] logAuthenticationAttempt error: " + e.getMessage());
            throw new RuntimeException("Failed to log authentication attempt: " + e.getMessage(), e);
        }
    }

    /**
     * Log a signature registration event.
     * 
     * Records when a user registers a new signature during account creation.
     * Creates an audit log entry with event type REGISTRATION.
     * Retrieves account identifier (email) from the user record.
     * 
     * @param userId The user ID registering a signature
     * @throws RuntimeException if database operation fails
     * 
     * Requirement 6.5: Log signature registration events
     * Requirement 10.1: Create audit log entry for registration events
     * Requirement 10.3: Record signature registration with timestamp and account identifier
     */
    public void logSignatureRegistration(int userId) {
        // Retrieve user to get account identifier
        User user = userDAO.findByEmail(getUserEmail(userId));
        String accountIdentifier = (user != null) ? user.getEmail() : "unknown";
        
        String sql = "INSERT INTO audit_log (user_id, event_type, account_identifier) " +
                     "VALUES (?, ?, ?)";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, AuditLogEntry.EventType.REGISTRATION);
            ps.setString(3, accountIdentifier);
            
            ps.executeUpdate();
            System.out.println("[SUCCESS] Signature registration logged for user ID: " + userId);
        } catch (SQLException e) {
            System.out.println("[ERROR] logSignatureRegistration error: " + e.getMessage());
            throw new RuntimeException("Failed to log signature registration: " + e.getMessage(), e);
        }
    }

    /**
     * Log a signature update event.
     * 
     * Records when a user updates their existing signature.
     * Creates an audit log entry with event type UPDATE.
     * Retrieves account identifier (email) from the user record.
     * 
     * @param userId The user ID updating their signature
     * @throws RuntimeException if database operation fails
     * 
     * Requirement 6.5: Log signature update events
     * Requirement 10.1: Create audit log entry for update events
     * Requirement 10.3: Record signature update with timestamp and account identifier
     */
    public void logSignatureUpdate(int userId) {
        // Retrieve user to get account identifier
        User user = userDAO.findByEmail(getUserEmail(userId));
        String accountIdentifier = (user != null) ? user.getEmail() : "unknown";
        
        String sql = "INSERT INTO audit_log (user_id, event_type, account_identifier) " +
                     "VALUES (?, ?, ?)";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, AuditLogEntry.EventType.UPDATE);
            ps.setString(3, accountIdentifier);
            
            ps.executeUpdate();
            System.out.println("[SUCCESS] Signature update logged for user ID: " + userId);
        } catch (SQLException e) {
            System.out.println("[ERROR] logSignatureUpdate error: " + e.getMessage());
            throw new RuntimeException("Failed to log signature update: " + e.getMessage(), e);
        }
    }

    /**
     * Query audit logs with filtering support.
     * 
     * Retrieves audit log entries matching the specified filter criteria.
     * Supports filtering by user ID, event type, and date range.
     * Returns results ordered by timestamp descending (most recent first).
     * 
     * @param query The query object containing filter criteria
     * @return List of audit log entries matching the filter criteria
     * @throws RuntimeException if database operation fails
     * 
     * Requirement 10.5: Provide filtered and searchable access to audit entries
     */
    public List<AuditLogEntry> queryAuditLogs(AuditLogQuery query) {
        List<AuditLogEntry> results = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM audit_log WHERE 1=1");
        List<Object> parameters = new ArrayList<>();
        
        // Apply filters
        if (query.getUserId() != null) {
            sql.append(" AND user_id = ?");
            parameters.add(query.getUserId());
        }
        
        if (query.getEventType() != null && !query.getEventType().isEmpty()) {
            sql.append(" AND event_type = ?");
            parameters.add(query.getEventType());
        }
        
        if (query.getStartDate() != null) {
            sql.append(" AND timestamp >= ?");
            parameters.add(Timestamp.valueOf(query.getStartDate()));
        }
        
        if (query.getEndDate() != null) {
            sql.append(" AND timestamp <= ?");
            parameters.add(Timestamp.valueOf(query.getEndDate()));
        }
        
        // Order by timestamp descending (most recent first)
        sql.append(" ORDER BY timestamp DESC");
        
        // Apply limit if specified
        if (query.getLimit() != null && query.getLimit() > 0) {
            sql.append(" LIMIT ?");
            parameters.add(query.getLimit());
        }
        
        try {
            PreparedStatement ps = con.prepareStatement(sql.toString());
            
            // Set parameters
            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param instanceof Integer) {
                    ps.setInt(i + 1, (Integer) param);
                } else if (param instanceof String) {
                    ps.setString(i + 1, (String) param);
                } else if (param instanceof Timestamp) {
                    ps.setTimestamp(i + 1, (Timestamp) param);
                }
            }
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                AuditLogEntry entry = new AuditLogEntry();
                entry.setId(rs.getInt("id"));
                entry.setUserId(rs.getInt("user_id"));
                entry.setEventType(rs.getString("event_type"));
                entry.setAccountIdentifier(rs.getString("account_identifier"));
                
                // Handle nullable fields
                double score = rs.getDouble("similarity_score");
                if (!rs.wasNull()) {
                    entry.setSimilarityScore(score);
                }
                
                entry.setDeviceInfo(rs.getString("device_info"));
                entry.setTimestamp(rs.getTimestamp("timestamp"));
                
                results.add(entry);
            }
            
            System.out.println("[SUCCESS] Retrieved " + results.size() + " audit log entries");
            return results;
        } catch (SQLException e) {
            System.out.println("[ERROR] queryAuditLogs error: " + e.getMessage());
            throw new RuntimeException("Failed to query audit logs: " + e.getMessage(), e);
        }
    }

    /**
     * Enforce 90-day retention policy by deleting old audit log entries.
     * 
     * Removes audit log entries older than 90 days from the current date.
     * This method should be called periodically (e.g., daily) to maintain the retention policy.
     * 
     * @return Number of audit log entries deleted
     * @throws RuntimeException if database operation fails
     * 
     * Requirement 10.4: Retain audit logs for a minimum of 90 days
     */
    public int enforceRetentionPolicy() {
        String sql = "DELETE FROM audit_log WHERE timestamp < DATE_SUB(NOW(), INTERVAL 90 DAY)";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            int rowsDeleted = ps.executeUpdate();
            
            if (rowsDeleted > 0) {
                System.out.println("[SUCCESS] Retention policy enforced: " + rowsDeleted + " old audit log entries deleted");
            }
            return rowsDeleted;
        } catch (SQLException e) {
            System.out.println("[ERROR] enforceRetentionPolicy error: " + e.getMessage());
            throw new RuntimeException("Failed to enforce retention policy: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to retrieve user email by user ID.
     * 
     * @param userId The user ID
     * @return The user's email address, or "unknown" if not found
     */
    private String getUserEmail(int userId) {
        try {
            String sql = "SELECT email FROM user WHERE id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getString("email");
            }
            return "unknown";
        } catch (SQLException e) {
            System.out.println("[WARNING] Could not retrieve email for user ID: " + userId);
            return "unknown";
        }
    }

    /**
     * Helper class for building audit log queries with filters.
     * 
     * Provides a fluent API for constructing queries with optional filters
     * for user ID, event type, date range, and result limit.
     */
    public static class AuditLogQuery {
        private Integer userId;
        private String eventType;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer limit;

        public AuditLogQuery() {
        }

        public Integer getUserId() {
            return userId;
        }

        public AuditLogQuery setUserId(Integer userId) {
            this.userId = userId;
            return this;
        }

        public String getEventType() {
            return eventType;
        }

        public AuditLogQuery setEventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public LocalDateTime getStartDate() {
            return startDate;
        }

        public AuditLogQuery setStartDate(LocalDateTime startDate) {
            this.startDate = startDate;
            return this;
        }

        public LocalDateTime getEndDate() {
            return endDate;
        }

        public AuditLogQuery setEndDate(LocalDateTime endDate) {
            this.endDate = endDate;
            return this;
        }

        public Integer getLimit() {
            return limit;
        }

        public AuditLogQuery setLimit(Integer limit) {
            this.limit = limit;
            return this;
        }
    }
}
