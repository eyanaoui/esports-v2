package com.esports.dao;

import com.esports.models.AuditLogEntry;
import com.esports.models.User;
import com.esports.models.UserRole;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuditLogDAO.
 * 
 * Tests audit logging functionality including authentication attempts,
 * signature registration/update logging, query filtering, and retention policy enforcement.
 * 
 * Requirements: 5.1, 6.5, 10.1, 10.2, 10.3, 10.5
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditLogDAOTest {

    private static AuditLogDAO auditLogDAO;
    private static UserDAO userDAO;
    private static int testUserId;

    @BeforeAll
    static void setUp() {
        auditLogDAO = new AuditLogDAO();
        userDAO = new UserDAO();
        
        // Create audit_log table if it doesn't exist
        createAuditLogTable();
        
        // Create a test user
        User testUser = new User();
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("auditlog.test@example.com");
        testUser.setPassword("password123");
        testUser.setRoles(List.of(UserRole.USER));
        
        userDAO.add(testUser);
        
        // Retrieve the created user to get the ID
        User createdUser = userDAO.findByEmail("auditlog.test@example.com");
        testUserId = createdUser.getId();
        
        System.out.println("[TEST SETUP] Created test user with ID: " + testUserId);
    }
    
    /**
     * Create the audit_log table for testing if it doesn't exist.
     */
    private static void createAuditLogTable() {
        try {
            java.sql.Connection con = com.esports.db.DatabaseConnection.getInstance().getConnection();
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS audit_log (
                    id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
                    user_id INT NOT NULL COMMENT 'Foreign key to user associated with event',
                    event_type VARCHAR(50) NOT NULL COMMENT 'Type of event: REGISTRATION, UPDATE, AUTH_SUCCESS, AUTH_FAILURE, RATE_LIMITED, LOCKED_OUT',
                    account_identifier VARCHAR(255) NOT NULL COMMENT 'Email or username used for authentication',
                    similarity_score DOUBLE COMMENT 'Similarity score for authentication events (NULL for registration/update)',
                    device_info TEXT COMMENT 'Device/browser information',
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'When event occurred',
                    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Audit trail for signature authentication events'
                """;
            
            java.sql.Statement stmt = con.createStatement();
            stmt.execute(createTableSQL);
            
            // Create indexes
            String createIndexes = """
                CREATE INDEX IF NOT EXISTS idx_audit_user_timestamp ON audit_log(user_id, timestamp);
                CREATE INDEX IF NOT EXISTS idx_audit_event_type ON audit_log(event_type);
                CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_log(timestamp);
                """;
            
            for (String indexSQL : createIndexes.split(";")) {
                if (!indexSQL.trim().isEmpty()) {
                    try {
                        stmt.execute(indexSQL.trim());
                    } catch (Exception e) {
                        // Index might already exist, ignore
                    }
                }
            }
            
            System.out.println("[TEST SETUP] audit_log table created/verified");
        } catch (Exception e) {
            System.out.println("[TEST SETUP ERROR] Failed to create audit_log table: " + e.getMessage());
        }
    }

    @AfterAll
    static void tearDown() {
        // Clean up test data
        userDAO.delete(testUserId);
        System.out.println("[TEST CLEANUP] Deleted test user and audit logs");
    }

    @Test
    @Order(1)
    void testLogAuthenticationAttemptSuccess() {
        // Test logging a successful authentication attempt
        assertDoesNotThrow(() -> {
            auditLogDAO.logAuthenticationAttempt(testUserId, 85.5, true, "Chrome/Windows");
        });
        
        // Verify the log entry was created
        AuditLogDAO.AuditLogQuery query = new AuditLogDAO.AuditLogQuery()
                .setUserId(testUserId)
                .setEventType(AuditLogEntry.EventType.AUTH_SUCCESS)
                .setLimit(1);
        
        List<AuditLogEntry> results = auditLogDAO.queryAuditLogs(query);
        
        assertFalse(results.isEmpty(), "Should have at least one AUTH_SUCCESS entry");
        AuditLogEntry entry = results.get(0);
        assertEquals(testUserId, entry.getUserId());
        assertEquals(AuditLogEntry.EventType.AUTH_SUCCESS, entry.getEventType());
        assertEquals(85.5, entry.getSimilarityScore());
        assertEquals("Chrome/Windows", entry.getDeviceInfo());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    @Order(2)
    void testLogAuthenticationAttemptFailure() {
        // Test logging a failed authentication attempt
        assertDoesNotThrow(() -> {
            auditLogDAO.logAuthenticationAttempt(testUserId, 45.2, false, "Firefox/Linux");
        });
        
        // Verify the log entry was created
        AuditLogDAO.AuditLogQuery query = new AuditLogDAO.AuditLogQuery()
                .setUserId(testUserId)
                .setEventType(AuditLogEntry.EventType.AUTH_FAILURE)
                .setLimit(1);
        
        List<AuditLogEntry> results = auditLogDAO.queryAuditLogs(query);
        
        assertFalse(results.isEmpty(), "Should have at least one AUTH_FAILURE entry");
        AuditLogEntry entry = results.get(0);
        assertEquals(testUserId, entry.getUserId());
        assertEquals(AuditLogEntry.EventType.AUTH_FAILURE, entry.getEventType());
        assertEquals(45.2, entry.getSimilarityScore());
        assertEquals("Firefox/Linux", entry.getDeviceInfo());
    }

    @Test
    @Order(3)
    void testLogSignatureRegistration() {
        // Test logging a signature registration
        assertDoesNotThrow(() -> {
            auditLogDAO.logSignatureRegistration(testUserId);
        });
        
        // Verify the log entry was created
        AuditLogDAO.AuditLogQuery query = new AuditLogDAO.AuditLogQuery()
                .setUserId(testUserId)
                .setEventType(AuditLogEntry.EventType.REGISTRATION)
                .setLimit(1);
        
        List<AuditLogEntry> results = auditLogDAO.queryAuditLogs(query);
        
        assertFalse(results.isEmpty(), "Should have at least one REGISTRATION entry");
        AuditLogEntry entry = results.get(0);
        assertEquals(testUserId, entry.getUserId());
        assertEquals(AuditLogEntry.EventType.REGISTRATION, entry.getEventType());
        assertNull(entry.getSimilarityScore(), "Registration should not have similarity score");
    }

    @Test
    @Order(4)
    void testLogSignatureUpdate() {
        // Test logging a signature update
        assertDoesNotThrow(() -> {
            auditLogDAO.logSignatureUpdate(testUserId);
        });
        
        // Verify the log entry was created
        AuditLogDAO.AuditLogQuery query = new AuditLogDAO.AuditLogQuery()
                .setUserId(testUserId)
                .setEventType(AuditLogEntry.EventType.UPDATE)
                .setLimit(1);
        
        List<AuditLogEntry> results = auditLogDAO.queryAuditLogs(query);
        
        assertFalse(results.isEmpty(), "Should have at least one UPDATE entry");
        AuditLogEntry entry = results.get(0);
        assertEquals(testUserId, entry.getUserId());
        assertEquals(AuditLogEntry.EventType.UPDATE, entry.getEventType());
        assertNull(entry.getSimilarityScore(), "Update should not have similarity score");
    }

    @Test
    @Order(5)
    void testQueryAuditLogsWithUserIdFilter() {
        // Query all logs for the test user
        AuditLogDAO.AuditLogQuery query = new AuditLogDAO.AuditLogQuery()
                .setUserId(testUserId);
        
        List<AuditLogEntry> results = auditLogDAO.queryAuditLogs(query);
        
        assertFalse(results.isEmpty(), "Should have audit log entries for test user");
        
        // Verify all results are for the test user
        for (AuditLogEntry entry : results) {
            assertEquals(testUserId, entry.getUserId());
        }
    }

    @Test
    @Order(6)
    void testQueryAuditLogsWithEventTypeFilter() {
        // Query only authentication success events
        AuditLogDAO.AuditLogQuery query = new AuditLogDAO.AuditLogQuery()
                .setUserId(testUserId)
                .setEventType(AuditLogEntry.EventType.AUTH_SUCCESS);
        
        List<AuditLogEntry> results = auditLogDAO.queryAuditLogs(query);
        
        assertFalse(results.isEmpty(), "Should have AUTH_SUCCESS entries");
        
        // Verify all results are AUTH_SUCCESS
        for (AuditLogEntry entry : results) {
            assertEquals(AuditLogEntry.EventType.AUTH_SUCCESS, entry.getEventType());
        }
    }

    @Test
    @Order(7)
    void testQueryAuditLogsWithDateRangeFilter() {
        // Query logs from the last hour
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);
        
        AuditLogDAO.AuditLogQuery query = new AuditLogDAO.AuditLogQuery()
                .setUserId(testUserId)
                .setStartDate(startDate)
                .setEndDate(endDate);
        
        List<AuditLogEntry> results = auditLogDAO.queryAuditLogs(query);
        
        assertFalse(results.isEmpty(), "Should have entries within date range");
        
        // Verify all results are within the date range
        for (AuditLogEntry entry : results) {
            assertTrue(entry.getTimestamp().toLocalDateTime().isAfter(startDate) ||
                      entry.getTimestamp().toLocalDateTime().isEqual(startDate));
            assertTrue(entry.getTimestamp().toLocalDateTime().isBefore(endDate) ||
                      entry.getTimestamp().toLocalDateTime().isEqual(endDate));
        }
    }

    @Test
    @Order(8)
    void testQueryAuditLogsWithLimit() {
        // Query with a limit of 2 entries
        AuditLogDAO.AuditLogQuery query = new AuditLogDAO.AuditLogQuery()
                .setUserId(testUserId)
                .setLimit(2);
        
        List<AuditLogEntry> results = auditLogDAO.queryAuditLogs(query);
        
        assertTrue(results.size() <= 2, "Should return at most 2 entries");
    }

    @Test
    @Order(9)
    void testQueryAuditLogsWithMultipleFilters() {
        // Query with multiple filters
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        
        AuditLogDAO.AuditLogQuery query = new AuditLogDAO.AuditLogQuery()
                .setUserId(testUserId)
                .setEventType(AuditLogEntry.EventType.AUTH_SUCCESS)
                .setStartDate(startDate)
                .setLimit(10);
        
        List<AuditLogEntry> results = auditLogDAO.queryAuditLogs(query);
        
        // Verify all results match all filters
        for (AuditLogEntry entry : results) {
            assertEquals(testUserId, entry.getUserId());
            assertEquals(AuditLogEntry.EventType.AUTH_SUCCESS, entry.getEventType());
            assertTrue(entry.getTimestamp().toLocalDateTime().isAfter(startDate) ||
                      entry.getTimestamp().toLocalDateTime().isEqual(startDate));
        }
    }

    @Test
    @Order(10)
    void testEnforceRetentionPolicy() {
        // Test retention policy enforcement
        // This should not delete any recent entries
        int deletedCount = auditLogDAO.enforceRetentionPolicy();
        
        // Verify the method executes without error
        assertTrue(deletedCount >= 0, "Deleted count should be non-negative");
        
        // Verify recent entries are still present
        AuditLogDAO.AuditLogQuery query = new AuditLogDAO.AuditLogQuery()
                .setUserId(testUserId);
        
        List<AuditLogEntry> results = auditLogDAO.queryAuditLogs(query);
        
        assertFalse(results.isEmpty(), "Recent entries should not be deleted");
    }

    @Test
    @Order(11)
    void testLogAuthenticationWithNullDeviceInfo() {
        // Test logging with null device info
        assertDoesNotThrow(() -> {
            auditLogDAO.logAuthenticationAttempt(testUserId, 75.0, true, null);
        });
        
        // Verify the log entry was created
        AuditLogDAO.AuditLogQuery query = new AuditLogDAO.AuditLogQuery()
                .setUserId(testUserId)
                .setLimit(1);
        
        List<AuditLogEntry> results = auditLogDAO.queryAuditLogs(query);
        
        assertFalse(results.isEmpty(), "Should have audit log entry");
        AuditLogEntry entry = results.get(0);
        assertNull(entry.getDeviceInfo(), "Device info should be null");
    }

    @Test
    @Order(12)
    void testQueryAuditLogsOrderedByTimestamp() {
        // Query all logs for the test user
        AuditLogDAO.AuditLogQuery query = new AuditLogDAO.AuditLogQuery()
                .setUserId(testUserId);
        
        List<AuditLogEntry> results = auditLogDAO.queryAuditLogs(query);
        
        // Verify results are ordered by timestamp descending (most recent first)
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(
                results.get(i).getTimestamp().compareTo(results.get(i + 1).getTimestamp()) >= 0,
                "Results should be ordered by timestamp descending"
            );
        }
    }
}
