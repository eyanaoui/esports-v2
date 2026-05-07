package com.esports.validation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GuideStep validation logic
 */
public class GuideStepValidationTest {

    @Test
    public void testValidStepTitle() {
        assertTrue(isValidTitle("Install the game"));
        assertTrue(isValidTitle("Step 1: Setup"));
        assertTrue(isValidTitle("ABC"));
    }

    @Test
    public void testInvalidStepTitle() {
        assertFalse(isValidTitle("")); // Empty
        assertFalse(isValidTitle("AB")); // Too short
        assertFalse(isValidTitle("T".repeat(201))); // Too long
    }

    @Test
    public void testValidContent() {
        assertTrue(isValidContent("Follow these instructions to complete the step"));
        assertTrue(isValidContent("Short text"));
        assertTrue(isValidContent("C".repeat(5000))); // Max length
    }

    @Test
    public void testInvalidContent() {
        assertFalse(isValidContent("")); // Empty
        assertFalse(isValidContent("Short")); // Too short
        assertFalse(isValidContent("C".repeat(5001))); // Too long
    }

    @Test
    public void testValidStepOrder() {
        assertTrue(isValidStepOrder(1));
        assertTrue(isValidStepOrder(50));
        assertTrue(isValidStepOrder(999));
    }

    @Test
    public void testInvalidStepOrder() {
        assertFalse(isValidStepOrder(0)); // Too low
        assertFalse(isValidStepOrder(-1)); // Negative
        assertFalse(isValidStepOrder(1000)); // Too high
    }

    @Test
    public void testValidImage() {
        assertTrue(isValidImage("step1.jpg"));
        assertTrue(isValidImage("screenshot.png"));
        assertTrue(isValidImage("")); // Optional
    }

    @Test
    public void testInvalidImage() {
        assertFalse(isValidImage("step.txt")); // Wrong format
        assertFalse(isValidImage("step.pdf")); // Wrong format
    }

    @Test
    public void testValidVideoUrl() {
        assertTrue(isValidVideoUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
        assertTrue(isValidVideoUrl("https://youtu.be/dQw4w9WgXcQ"));
        assertTrue(isValidVideoUrl("http://youtube.com/watch?v=abc123"));
        assertTrue(isValidVideoUrl("")); // Optional
    }

    @Test
    public void testInvalidVideoUrl() {
        assertFalse(isValidVideoUrl("https://vimeo.com/123456")); // Not YouTube
        assertFalse(isValidVideoUrl("https://google.com")); // Not YouTube
        assertFalse(isValidVideoUrl("not-a-url")); // Invalid URL
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

    private boolean isValidContent(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        if (content.length() < 10 || content.length() > 5000) {
            return false;
        }
        return true;
    }

    private boolean isValidStepOrder(int order) {
        return order >= 1 && order <= 999;
    }

    private boolean isValidImage(String image) {
        if (image == null) {
            return false;
        }
        // Optional but if provided, check format
        if (!image.isEmpty()) {
            return image.matches(".*\\.(jpg|jpeg|png|webp|gif)$");
        }
        return true;
    }

    private boolean isValidVideoUrl(String video) {
        if (video == null) {
            return false;
        }
        // Optional but if provided, check format
        if (!video.isEmpty()) {
            return video.matches("^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/.+$");
        }
        return true;
    }
}
