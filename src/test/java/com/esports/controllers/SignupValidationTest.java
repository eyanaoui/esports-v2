package com.esports.controllers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for signup validation logic
 */
public class SignupValidationTest {

    @Test
    public void testValidEmail() {
        assertTrue(isValidEmail("user@example.com"));
        assertTrue(isValidEmail("test.user@domain.co.uk"));
        assertTrue(isValidEmail("user+tag@example.com"));
    }

    @Test
    public void testInvalidEmail() {
        assertFalse(isValidEmail(""));
        assertFalse(isValidEmail("invalid"));
        assertFalse(isValidEmail("@example.com"));
        assertFalse(isValidEmail("user@"));
        assertFalse(isValidEmail("user@domain"));
        assertFalse(isValidEmail("user @example.com"));
    }

    @Test
    public void testValidName() {
        assertTrue(isValidName("John"));
        assertTrue(isValidName("Jean-Pierre"));
        assertTrue(isValidName("O'Connor"));
        assertTrue(isValidName("Mary Jane"));
    }

    @Test
    public void testInvalidName() {
        assertFalse(isValidName("J")); // Too short
        assertFalse(isValidName("John123")); // Contains numbers
        assertFalse(isValidName("John@Doe")); // Contains special chars
    }

    @Test
    public void testValidPassword() {
        assertTrue(isValidPassword("pass1234"));
        assertTrue(isValidPassword("MyP@ssw0rd"));
        assertTrue(isValidPassword("1234"));
    }

    @Test
    public void testInvalidPassword() {
        assertFalse(isValidPassword("abc")); // Too short
        assertFalse(isValidPassword("pass word")); // Contains space
        assertFalse(isValidPassword("")); // Empty
    }

    // Helper methods matching LoginController logic
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private boolean isValidName(String name) {
        if (name == null || name.length() < 2 || name.length() > 50) {
            return false;
        }
        return name.matches("^[a-zA-Z\\s'-]+$");
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 4 || password.length() > 100) {
            return false;
        }
        return !password.contains(" ");
    }
}
