package com.esports.validation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for User validation logic
 */
public class UserValidationTest {

    @Test
    public void testValidFirstName() {
        assertTrue(isValidName("John"));
        assertTrue(isValidName("Jean-Pierre"));
        assertTrue(isValidName("O'Connor"));
        assertTrue(isValidName("Mary Jane"));
    }

    @Test
    public void testInvalidFirstName() {
        assertFalse(isValidName("")); // Empty
        assertFalse(isValidName("J")); // Too short
        assertFalse(isValidName("A".repeat(51))); // Too long
        assertFalse(isValidName("John123")); // Contains numbers
        assertFalse(isValidName("John@Doe")); // Contains special chars
    }

    @Test
    public void testValidLastName() {
        assertTrue(isValidName("Smith"));
        assertTrue(isValidName("Van Der Berg"));
        assertTrue(isValidName("O'Neill"));
    }

    @Test
    public void testInvalidLastName() {
        assertFalse(isValidName("")); // Empty
        assertFalse(isValidName("S")); // Too short
        assertFalse(isValidName("B".repeat(51))); // Too long
    }

    @Test
    public void testValidEmail() {
        assertTrue(isValidEmail("user@example.com"));
        assertTrue(isValidEmail("test.user@domain.co.uk"));
        assertTrue(isValidEmail("user+tag@example.com"));
        assertTrue(isValidEmail("admin@test.org"));
    }

    @Test
    public void testInvalidEmail() {
        assertFalse(isValidEmail("")); // Empty
        assertFalse(isValidEmail("invalid")); // No @
        assertFalse(isValidEmail("@example.com")); // No user
        assertFalse(isValidEmail("user@")); // No domain
        assertFalse(isValidEmail("user@domain")); // No TLD
        assertFalse(isValidEmail("user @example.com")); // Space
        assertFalse(isValidEmail("E".repeat(101))); // Too long
    }

    @Test
    public void testValidPassword() {
        assertTrue(isValidPassword("pass1234"));
        assertTrue(isValidPassword("MyP@ssw0rd"));
        assertTrue(isValidPassword("1234"));
        assertTrue(isValidPassword("abcd"));
    }

    @Test
    public void testInvalidPassword() {
        assertFalse(isValidPassword("")); // Empty
        assertFalse(isValidPassword("abc")); // Too short
        assertFalse(isValidPassword("pass word")); // Contains space
        assertFalse(isValidPassword("P".repeat(101))); // Too long
    }

    // Helper methods matching controller logic
    private boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.length() < 2 || name.length() > 50) {
            return false;
        }
        return name.matches("^[a-zA-Z\\s'-]+$");
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        if (email.length() > 100) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        if (password.length() < 4 || password.length() > 100) {
            return false;
        }
        return !password.contains(" ");
    }
}
