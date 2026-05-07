package com.esports.validation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Guide validation logic
 */
public class GuideValidationTest {

    @Test
    public void testValidGuideTitle() {
        assertTrue(isValidTitle("How to play League of Legends"));
        assertTrue(isValidTitle("Beginner's Guide"));
        assertTrue(isValidTitle("Pro"));
    }

    @Test
    public void testInvalidGuideTitle() {
        assertFalse(isValidTitle("")); // Empty
        assertFalse(isValidTitle("AB")); // Too short
        assertFalse(isValidTitle("T".repeat(201))); // Too long
    }

    @Test
    public void testValidDescription() {
        assertTrue(isValidDescription("This guide will help you master the game"));
        assertTrue(isValidDescription("Short desc"));
        assertTrue(isValidDescription("D".repeat(5000))); // Max length
    }

    @Test
    public void testInvalidDescription() {
        assertFalse(isValidDescription("")); // Empty
        assertFalse(isValidDescription("Short")); // Too short
        assertFalse(isValidDescription("D".repeat(5001))); // Too long
    }

    @Test
    public void testValidDifficulty() {
        assertTrue(isValidDifficulty("Easy"));
        assertTrue(isValidDifficulty("Medium"));
        assertTrue(isValidDifficulty("Hard"));
    }

    @Test
    public void testInvalidDifficulty() {
        assertFalse(isValidDifficulty("")); // Empty
        assertFalse(isValidDifficulty(null)); // Null
        assertFalse(isValidDifficulty("Extreme")); // Invalid value
        assertFalse(isValidDifficulty("easy")); // Wrong case
    }

    @Test
    public void testValidCoverImage() {
        assertTrue(isValidCoverImage("guide.jpg"));
        assertTrue(isValidCoverImage("cover.png"));
        assertTrue(isValidCoverImage("")); // Optional
    }

    @Test
    public void testInvalidCoverImage() {
        assertFalse(isValidCoverImage("guide.txt")); // Wrong format
        assertFalse(isValidCoverImage("guide.doc")); // Wrong format
    }

    // Helper methods matching controller logic
    private boolean isValidTitle(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        if (title.length() < 3 || title.length() > 200) {
            return false;
        }
        return true;
    }

    private boolean isValidDescription(String description) {
        if (description == null || description.isEmpty()) {
            return false;
        }
        if (description.length() < 10 || description.length() > 5000) {
            return false;
        }
        return true;
    }

    private boolean isValidDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isEmpty()) {
            return false;
        }
        return difficulty.equals("Easy") || difficulty.equals("Medium") || difficulty.equals("Hard");
    }

    private boolean isValidCoverImage(String cover) {
        if (cover == null) {
            return false;
        }
        // Optional but if provided, check format
        if (!cover.isEmpty()) {
            return cover.matches(".*\\.(jpg|jpeg|png|webp|gif)$");
        }
        return true;
    }
}
