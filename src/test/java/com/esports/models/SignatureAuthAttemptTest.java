package com.esports.models;

import org.junit.jupiter.api.Test;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SignatureAuthAttempt model class.
 * Validates basic model functionality including constructors, getters, setters, and toString.
 */
class SignatureAuthAttemptTest {

    @Test
    void testDefaultConstructor() {
        SignatureAuthAttempt attempt = new SignatureAuthAttempt();
        assertNotNull(attempt);
        assertEquals(0, attempt.getId());
        assertEquals(0, attempt.getUserId());
        assertEquals(0.0, attempt.getSimilarityScore());
        assertFalse(attempt.isSuccess());
        assertNull(attempt.getAttemptTime());
    }

    @Test
    void testParameterizedConstructor() {
        SignatureAuthAttempt attempt = new SignatureAuthAttempt(123, 85.5, true);
        
        assertEquals(123, attempt.getUserId());
        assertEquals(85.5, attempt.getSimilarityScore());
        assertTrue(attempt.isSuccess());
    }

    @Test
    void testSettersAndGetters() {
        SignatureAuthAttempt attempt = new SignatureAuthAttempt();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        
        attempt.setId(1);
        attempt.setUserId(456);
        attempt.setSimilarityScore(72.3);
        attempt.setSuccess(false);
        attempt.setAttemptTime(now);
        
        assertEquals(1, attempt.getId());
        assertEquals(456, attempt.getUserId());
        assertEquals(72.3, attempt.getSimilarityScore());
        assertFalse(attempt.isSuccess());
        assertEquals(now, attempt.getAttemptTime());
    }

    @Test
    void testToString() {
        SignatureAuthAttempt attempt = new SignatureAuthAttempt(789, 90.0, true);
        attempt.setId(5);
        
        String result = attempt.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("id=5"));
        assertTrue(result.contains("userId=789"));
        assertTrue(result.contains("similarityScore=90.0"));
        assertTrue(result.contains("success=true"));
    }

    @Test
    void testSimilarityScoreBounds() {
        SignatureAuthAttempt attempt = new SignatureAuthAttempt();
        
        // Test minimum score
        attempt.setSimilarityScore(0.0);
        assertEquals(0.0, attempt.getSimilarityScore());
        
        // Test maximum score
        attempt.setSimilarityScore(100.0);
        assertEquals(100.0, attempt.getSimilarityScore());
        
        // Test mid-range score
        attempt.setSimilarityScore(75.0);
        assertEquals(75.0, attempt.getSimilarityScore());
    }
}
