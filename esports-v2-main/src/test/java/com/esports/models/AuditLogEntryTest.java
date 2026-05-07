package com.esports.models;

import org.junit.jupiter.api.Test;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuditLogEntry model class.
 * Validates basic model functionality including constructors, getters, setters, and event types.
 */
class AuditLogEntryTest {

    @Test
    void testDefaultConstructor() {
        AuditLogEntry entry = new AuditLogEntry();
        assertNotNull(entry);
        assertEquals(0, entry.getId());
        assertEquals(0, entry.getUserId());
        assertNull(entry.getEventType());
        assertNull(entry.getAccountIdentifier());
        assertNull(entry.getSimilarityScore());
        assertNull(entry.getDeviceInfo());
        assertNull(entry.getTimestamp());
    }

    @Test
    void testThreeParameterConstructor() {
        AuditLogEntry entry = new AuditLogEntry(123, "REGISTRATION", "user@example.com");
        
        assertEquals(123, entry.getUserId());
        assertEquals("REGISTRATION", entry.getEventType());
        assertEquals("user@example.com", entry.getAccountIdentifier());
        assertNull(entry.getSimilarityScore());
        assertNull(entry.getDeviceInfo());
    }

    @Test
    void testFiveParameterConstructor() {
        AuditLogEntry entry = new AuditLogEntry(456, "AUTH_SUCCESS", "john@example.com", 85.5, "Chrome/Windows");
        
        assertEquals(456, entry.getUserId());
        assertEquals("AUTH_SUCCESS", entry.getEventType());
        assertEquals("john@example.com", entry.getAccountIdentifier());
        assertEquals(85.5, entry.getSimilarityScore());
        assertEquals("Chrome/Windows", entry.getDeviceInfo());
    }

    @Test
    void testSettersAndGetters() {
        AuditLogEntry entry = new AuditLogEntry();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        
        entry.setId(1);
        entry.setUserId(789);
        entry.setEventType("AUTH_FAILURE");
        entry.setAccountIdentifier("test@example.com");
        entry.setSimilarityScore(65.0);
        entry.setDeviceInfo("Firefox/Linux");
        entry.setTimestamp(now);
        
        assertEquals(1, entry.getId());
        assertEquals(789, entry.getUserId());
        assertEquals("AUTH_FAILURE", entry.getEventType());
        assertEquals("test@example.com", entry.getAccountIdentifier());
        assertEquals(65.0, entry.getSimilarityScore());
        assertEquals("Firefox/Linux", entry.getDeviceInfo());
        assertEquals(now, entry.getTimestamp());
    }

    @Test
    void testEventTypeConstants() {
        assertEquals("REGISTRATION", AuditLogEntry.EventType.REGISTRATION);
        assertEquals("UPDATE", AuditLogEntry.EventType.UPDATE);
        assertEquals("AUTH_SUCCESS", AuditLogEntry.EventType.AUTH_SUCCESS);
        assertEquals("AUTH_FAILURE", AuditLogEntry.EventType.AUTH_FAILURE);
        assertEquals("RATE_LIMITED", AuditLogEntry.EventType.RATE_LIMITED);
        assertEquals("LOCKED_OUT", AuditLogEntry.EventType.LOCKED_OUT);
    }

    @Test
    void testToString() {
        AuditLogEntry entry = new AuditLogEntry(999, "UPDATE", "admin@example.com", null, "Safari/Mac");
        entry.setId(10);
        
        String result = entry.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("id=10"));
        assertTrue(result.contains("userId=999"));
        assertTrue(result.contains("eventType='UPDATE'"));
        assertTrue(result.contains("accountIdentifier='admin@example.com'"));
        assertTrue(result.contains("deviceInfo='Safari/Mac'"));
    }

    @Test
    void testNullSimilarityScore() {
        // For registration and update events, similarity score should be null
        AuditLogEntry entry = new AuditLogEntry(100, "REGISTRATION", "new@example.com");
        
        assertNull(entry.getSimilarityScore());
        
        // Can explicitly set to null
        entry.setSimilarityScore(null);
        assertNull(entry.getSimilarityScore());
    }

    @Test
    void testAllEventTypes() {
        String[] eventTypes = {
            AuditLogEntry.EventType.REGISTRATION,
            AuditLogEntry.EventType.UPDATE,
            AuditLogEntry.EventType.AUTH_SUCCESS,
            AuditLogEntry.EventType.AUTH_FAILURE,
            AuditLogEntry.EventType.RATE_LIMITED,
            AuditLogEntry.EventType.LOCKED_OUT
        };
        
        for (String eventType : eventTypes) {
            AuditLogEntry entry = new AuditLogEntry();
            entry.setEventType(eventType);
            assertEquals(eventType, entry.getEventType());
        }
    }
}
