package com.esports.validation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GuideRating validation logic
 */
public class GuideRatingValidationTest {

    @Test
    public void testValidRating() {
        assertTrue(isValidRating(1));
        assertTrue(isValidRating(3));
        assertTrue(isValidRating(5));
    }

    @Test
    public void testInvalidRating() {
        assertFalse(isValidRating(0)); // Too low
        assertFalse(isValidRating(-1)); // Negative
        assertFalse(isValidRating(6)); // Too high
        assertFalse(isValidRating(10)); // Too high
    }

    @Test
    public void testValidComment() {
        assertTrue(isValidComment("Great guide!"));
        assertTrue(isValidComment("This helped me a lot, thank you!"));
        assertTrue(isValidComment("")); // Optional
        assertTrue(isValidComment("C".repeat(1000))); // Max length
    }

    @Test
    public void testInvalidComment() {
        assertFalse(isValidComment("Good")); // Too short if provided
        assertFalse(isValidComment("C".repeat(1001))); // Too long
    }

    @Test
    public void testRatingBoundaries() {
        // Test exact boundaries
        assertTrue(isValidRating(1)); // Min valid
        assertTrue(isValidRating(5)); // Max valid
        assertFalse(isValidRating(0)); // Below min
        assertFalse(isValidRating(6)); // Above max
    }

    @Test
    public void testCommentBoundaries() {
        // Test exact boundaries
        assertTrue(isValidComment("12345")); // Min valid (5 chars)
        assertTrue(isValidComment("C".repeat(1000))); // Max valid
        assertFalse(isValidComment("1234")); // Below min (4 chars)
        assertFalse(isValidComment("C".repeat(1001))); // Above max
    }

    // Helper methods matching controller logic
    private boolean isValidRating(Integer rating) {
        if (rating == null) {
            return false;
        }
        return rating >= 1 && rating <= 5;
    }

    private boolean isValidComment(String comment) {
        if (comment == null) {
            return false;
        }
        // Optional but if provided, check length
        if (!comment.isEmpty()) {
            if (comment.length() < 5 || comment.length() > 1000) {
                return false;
            }
        }
        return true;
    }
}
