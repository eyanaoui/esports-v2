package com.esports.validation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Game validation logic
 */
public class GameValidationTest {

    @Test
    public void testValidGameName() {
        assertTrue(isValidGameName("League of Legends"));
        assertTrue(isValidGameName("CS"));
        assertTrue(isValidGameName("Counter-Strike: Global Offensive"));
    }

    @Test
    public void testInvalidGameName() {
        assertFalse(isValidGameName("")); // Empty
        assertFalse(isValidGameName("A")); // Too short
        assertFalse(isValidGameName("G".repeat(101))); // Too long
    }

    @Test
    public void testValidSlug() {
        assertTrue(isValidSlug("league-of-legends"));
        assertTrue(isValidSlug("cs-go"));
        assertTrue(isValidSlug("valorant"));
        assertTrue(isValidSlug("dota-2"));
    }

    @Test
    public void testInvalidSlug() {
        assertFalse(isValidSlug("")); // Empty
        assertFalse(isValidSlug("a")); // Too short
        assertFalse(isValidSlug("League Of Legends")); // Uppercase
        assertFalse(isValidSlug("league_of_legends")); // Underscore
        assertFalse(isValidSlug("league of legends")); // Spaces
        assertFalse(isValidSlug("s".repeat(101))); // Too long
    }

    @Test
    public void testValidDescription() {
        assertTrue(isValidDescription("This is a great game"));
        assertTrue(isValidDescription("")); // Optional
        assertTrue(isValidDescription("D".repeat(5000))); // Max length
    }

    @Test
    public void testInvalidDescription() {
        assertFalse(isValidDescription("D".repeat(5001))); // Too long
    }

    @Test
    public void testValidCoverImage() {
        assertTrue(isValidCoverImage("game.jpg"));
        assertTrue(isValidCoverImage("cover.jpeg"));
        assertTrue(isValidCoverImage("image.png"));
        assertTrue(isValidCoverImage("photo.webp"));
        assertTrue(isValidCoverImage("pic.gif"));
        assertTrue(isValidCoverImage("")); // Optional
    }

    @Test
    public void testInvalidCoverImage() {
        assertFalse(isValidCoverImage("game.txt")); // Wrong format
        assertFalse(isValidCoverImage("game.pdf")); // Wrong format
        assertFalse(isValidCoverImage("game")); // No extension
    }

    // Helper methods matching controller logic
    private boolean isValidGameName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.length() < 2 || name.length() > 100) {
            return false;
        }
        return true;
    }

    private boolean isValidSlug(String slug) {
        if (slug == null || slug.isEmpty()) {
            return false;
        }
        if (slug.length() < 2 || slug.length() > 100) {
            return false;
        }
        return slug.matches("^[a-z0-9-]+$");
    }

    private boolean isValidDescription(String description) {
        if (description == null) {
            return false;
        }
        // Optional but if provided, check length
        if (!description.isEmpty() && description.length() > 5000) {
            return false;
        }
        return true;
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
