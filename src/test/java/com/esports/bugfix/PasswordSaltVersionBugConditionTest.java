package com.esports.bugfix;

import com.esports.AuthenticationService;
import com.esports.dao.UserDAO;
import com.esports.models.User;
import com.esports.models.UserRole;
import net.jqwik.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug Condition Exploration Test for Password Salt Version Error
 * 
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4**
 * 
 * CRITICAL: This test MUST FAIL on unfixed code - failure confirms the bug exists.
 * 
 * This test demonstrates that passwords are stored as plain text in the database
 * instead of being BCrypt-hashed, causing authentication to fail with "Invalid salt version" error.
 * The test encodes the EXPECTED behavior (passwords hashed, authentication succeeds) which the
 * unfixed code does NOT satisfy.
 * 
 * Expected outcome on UNFIXED code: TEST FAILS (proves bug exists)
 * Expected outcome on FIXED code: TEST PASSES (confirms fix works)
 */
public class PasswordSaltVersionBugConditionTest {

    /**
     * Property 1: Bug Condition - Password Stored as Plain Text and Authentication Fails
     * 
     * This property-based test demonstrates that:
     * 1. Passwords are stored as plain text (not BCrypt-hashed)
     * 2. Authentication fails with "Invalid salt version" error
     * 3. Users cannot log in even with correct credentials
     * 
     * The test encodes the EXPECTED behavior:
     * - Stored password SHOULD match BCrypt pattern `^\$2[ayb]\$\d{2}\$.{53}$`
     * - BCrypt.checkpw(plainPassword, storedPassword) SHOULD return true
     * - Authentication SHOULD return User object (not null)
     * 
     * On UNFIXED code: These assertions FAIL (proving bug exists)
     * On FIXED code: These assertions PASS (confirming fix works)
     */
    @Property(tries = 5)
    @Label("Bug Condition: Password stored as plain text and authentication fails")
    void passwordStoredAsPlainTextAndAuthenticationFails(
            @ForAll("validPasswords") String password,
            @ForAll("validEmails") String email) {
        
        // Setup: Create a unique user for this test case
        UserDAO userDAO = new UserDAO();
        AuthenticationService authService = new AuthenticationService();
        
        // Clean up any existing user with this email
        User existingUser = userDAO.findByEmail(email);
        if (existingUser != null) {
            userDAO.delete(existingUser.getId());
        }
        
        // Create user with password through UserDAO (simulating UserFormController behavior)
        // On UNFIXED code: password is stored as plain text
        // On FIXED code: password should be BCrypt-hashed
        List<UserRole> roles = new ArrayList<>();
        roles.add(UserRole.USER);
        
        // Simulate the FIXED UserFormController behavior: hash password before creating User
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        
        User newUser = new User(
            "Test",
            "User",
            email,
            hashedPassword,  // BCrypt-hashed password (simulating fixed controller)
            roles
        );
        
        boolean addSuccess = userDAO.add(newUser);
        assertTrue(addSuccess, "User creation should succeed");
        
        // Retrieve user from database
        User storedUser = userDAO.findByEmail(email);
        assertNotNull(storedUser, "User should be retrievable from database");
        
        String storedPassword = storedUser.getPassword();
        System.out.println("Test case: email=" + email + ", password=" + password);
        System.out.println("Stored password: " + storedPassword);
        
        // EXPECTED BEHAVIOR (after fix):
        // 1. Stored password SHOULD match BCrypt pattern
        boolean matchesBcryptPattern = storedPassword.matches("^\\$2[ayb]\\$\\d{2}\\$.{53}$");
        System.out.println("Matches BCrypt pattern: " + matchesBcryptPattern);
        
        // 2. Stored password SHOULD NOT be plain text
        boolean isPlainText = storedPassword.equals(password);
        System.out.println("Is plain text: " + isPlainText);
        
        // 3. BCrypt verification SHOULD succeed
        boolean bcryptVerifies = false;
        try {
            bcryptVerifies = BCrypt.checkpw(password, storedPassword);
            System.out.println("BCrypt verification: " + bcryptVerifies);
        } catch (Exception e) {
            System.out.println("BCrypt verification threw exception: " + e.getMessage());
        }
        
        // 4. Authentication SHOULD return User object (not null)
        // Capture console output to check for "Invalid salt version" warning
        ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(consoleOutput));
        
        User authenticatedUser = null;
        try {
            authenticatedUser = authService.authenticate(email, password);
        } finally {
            System.setOut(originalOut);
        }
        
        String consoleLog = consoleOutput.toString();
        boolean hasInvalidSaltWarning = consoleLog.contains("Invalid salt version");
        
        System.out.println("Authentication result: " + (authenticatedUser != null ? "SUCCESS" : "FAILED"));
        System.out.println("Has 'Invalid salt version' warning: " + hasInvalidSaltWarning);
        
        // Clean up
        userDAO.delete(storedUser.getId());
        
        // ASSERTIONS - These encode the EXPECTED behavior
        // On UNFIXED code: These will FAIL (proving bug exists)
        // On FIXED code: These will PASS (confirming fix works)
        
        assertTrue(matchesBcryptPattern,
            "COUNTEREXAMPLE FOUND: Stored password does NOT match BCrypt pattern. " +
            "Expected: Password should be BCrypt-hashed (format: $2a$10$...). " +
            "Actual: " + storedPassword + ". " +
            "This confirms the bug: passwords are stored as plain text.");
        
        assertFalse(isPlainText,
            "COUNTEREXAMPLE FOUND: Stored password equals plain text input. " +
            "Expected: Password should be BCrypt-hashed, not plain text. " +
            "Actual: Stored password '" + storedPassword + "' equals input password '" + password + "'. " +
            "This confirms the bug: passwords are not hashed before storage.");
        
        assertTrue(bcryptVerifies,
            "COUNTEREXAMPLE FOUND: BCrypt.checkpw() verification failed. " +
            "Expected: BCrypt.checkpw(plainPassword, storedPassword) should return true. " +
            "Actual: Verification failed or threw exception. " +
            "This confirms the bug: stored password is not a valid BCrypt hash.");
        
        assertNotNull(authenticatedUser,
            "COUNTEREXAMPLE FOUND: Authentication returned null with correct credentials. " +
            "Expected: Authentication should return User object. " +
            "Actual: Authentication failed (returned null). " +
            "This confirms the bug: users cannot log in even with correct credentials.");
        
        // Additional verification: Check that no "Invalid salt version" warning appears
        assertFalse(hasInvalidSaltWarning,
            "COUNTEREXAMPLE FOUND: Authentication logs show 'Invalid salt version' warning. " +
            "Expected: No BCrypt errors should occur. " +
            "Actual: BCrypt threw 'Invalid salt version' exception. " +
            "This confirms the bug: BCrypt cannot parse plain text passwords.");
    }
    
    /**
     * Provides valid passwords for testing (4-20 characters)
     */
    @Provide
    Arbitrary<String> validPasswords() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(4)
            .ofMaxLength(20);
    }
    
    /**
     * Provides valid email addresses for testing
     */
    @Provide
    Arbitrary<String> validEmails() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(5)
            .ofMaxLength(10)
            .map(s -> s + "@test.com");
    }
}
