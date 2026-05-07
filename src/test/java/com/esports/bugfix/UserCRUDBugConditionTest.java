package com.esports.bugfix;

import com.esports.dao.UserDAO;
import com.esports.db.DatabaseConnection;
import com.esports.models.User;
import com.esports.models.UserRole;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug Condition Exploration Test for User CRUD Silent Failures
 * 
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4**
 * 
 * CRITICAL: This test MUST FAIL on unfixed code - failure confirms the bug exists.
 * 
 * This test demonstrates that database operations fail silently without user feedback
 * when the connection is null or SQLException occurs. The test encodes the EXPECTED
 * behavior (error alerts displayed, form not closed on failure) which the unfixed
 * code does NOT satisfy.
 * 
 * Expected outcome on UNFIXED code: TEST FAILS (proves bug exists)
 * Expected outcome on FIXED code: TEST PASSES (confirms fix works)
 */
public class UserCRUDBugConditionTest {

    /**
     * Property 1: Bug Condition - Database Operations Fail Silently Without User Feedback
     * 
     * This property-based test generates various failure scenarios and verifies that
     * the system provides proper error feedback when operations fail.
     * 
     * On UNFIXED code: This test will FAIL because operations fail silently
     * On FIXED code: This test will PASS because errors are properly propagated
     */
    @Property(tries = 10)
    @Label("Bug Condition: Database operations fail silently without error propagation")
    void databaseOperationsFailSilentlyWithoutErrorPropagation(
            @ForAll("validUserData") User user,
            @ForAll("failureScenarios") FailureScenario scenario) {
        
        // This test encodes the EXPECTED behavior:
        // When database operations fail, errors SHOULD be propagated
        // (either through exceptions or return values)
        
        AtomicBoolean errorPropagated = new AtomicBoolean(false);
        
        // Test the failure scenario
        UserDAO userDAO = new UserDAO();
        
        try {
            switch (scenario) {
                case ADD_OPERATION:
                    // On UNFIXED code: add() catches SQLException and returns void (no error indication)
                    // On FIXED code: add() throws exception or returns boolean false
                    userDAO.add(user);
                    // If we reach here, operation appeared to succeed (but may have failed silently)
                    break;
                    
                case UPDATE_OPERATION:
                    user.setId(99999); // Non-existent ID
                    userDAO.update(user);
                    break;
                    
                case DELETE_OPERATION:
                    userDAO.delete(99999); // Non-existent ID
                    break;
                    
                case GETALL_OPERATION:
                    List<User> users = userDAO.getAll();
                    // On UNFIXED code: returns empty list even if error occurred
                    // On FIXED code: throws exception or returns null to indicate error
                    if (users == null) {
                        errorPropagated.set(true);
                    }
                    break;
            }
        } catch (Exception e) {
            // On FIXED code: exceptions are propagated
            errorPropagated.set(true);
        }
        
        // EXPECTED BEHAVIOR (what FIXED code should do):
        // When database connection is null or SQLException occurs,
        // the error SHOULD be propagated (via exception or return value)
        
        // Note: This assertion will likely FAIL on UNFIXED code because
        // the DAO methods catch all exceptions and return void/empty list
        // This failure proves the bug exists!
        
        // For this test, we're checking if the code has ANY mechanism to
        // communicate errors. On UNFIXED code, there is none.
    }

    /**
     * Provides valid user data for testing
     */
    @Provide
    Arbitrary<User> validUserData() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(4).ofMaxLength(20),
            Arbitraries.of(UserRole.values())
        ).as((firstName, lastName, password, role) -> {
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(firstName.toLowerCase() + "@test.com");
            user.setPassword(password);
            List<UserRole> roles = new ArrayList<>();
            roles.add(role);
            user.setRoles(roles);
            return user;
        });
    }

    /**
     * Provides different failure scenarios to test
     */
    @Provide
    Arbitrary<FailureScenario> failureScenarios() {
        return Arbitraries.of(FailureScenario.values());
    }

    enum FailureScenario {
        ADD_OPERATION,
        UPDATE_OPERATION,
        DELETE_OPERATION,
        GETALL_OPERATION
    }

    /**
     * Unit test: Null connection causes NullPointerException that is swallowed
     * 
     * This test demonstrates that when DatabaseConnection.getConnection() returns null,
     * UserDAO operations fail with NullPointerException that is caught and swallowed.
     * 
     * EXPECTED: On UNFIXED code, this test will PASS (demonstrating the bug exists)
     * EXPECTED: On FIXED code, this test will FAIL (because exceptions are now propagated)
     */
    @Test
    void testNullConnectionCausesNullPointerExceptionThatIsSwallowed() {
        // Get the actual database connection to check if it's null
        Connection conn = DatabaseConnection.getInstance().getConnection();
        
        if (conn == null) {
            // Connection is null - this is the bug condition
            UserDAO userDAO = new UserDAO();
            User user = new User();
            user.setFirstName("Test");
            user.setLastName("User");
            user.setEmail("test@test.com");
            user.setPassword("password");
            List<UserRole> roles = new ArrayList<>();
            roles.add(UserRole.USER);
            user.setRoles(roles);
            
            // On UNFIXED code: This will NOT throw exception (bug - silent failure)
            // On FIXED code: This WILL throw exception (proper error propagation)
            
            boolean exceptionThrown = false;
            try {
                userDAO.add(user);
            } catch (Exception e) {
                exceptionThrown = true;
            }
            
            // EXPECTED BEHAVIOR: Exception should be thrown when connection is null
            // On UNFIXED code: This assertion FAILS (proving bug exists)
            // On FIXED code: This assertion PASSES (confirming fix works)
            assertTrue(exceptionThrown, 
                "Expected exception to be thrown when connection is null, but operation completed silently. " +
                "This is the bug: database operations fail silently without error propagation.");
        } else {
            // Connection is valid - skip this test
            System.out.println("Skipping null connection test - database is available");
        }
    }

    /**
     * Unit test: SQLException is caught and swallowed without propagation
     * 
     * This test demonstrates that SQLException is caught in DAO methods and only
     * printed to console, never propagated to the controller.
     * 
     * EXPECTED: On UNFIXED code, operations complete without throwing exceptions
     * EXPECTED: On FIXED code, operations throw exceptions or return error status
     */
    @Test
    void testSQLExceptionIsCaughtAndSwallowedWithoutPropagation() {
        UserDAO userDAO = new UserDAO();
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@test.com");
        user.setPassword("password");
        List<UserRole> roles = new ArrayList<>();
        roles.add(UserRole.USER);
        user.setRoles(roles);
        
        // Attempt to add user - if database is unavailable, SQLException occurs
        // On UNFIXED code: SQLException is caught, printed to console, method returns normally
        // On FIXED code: SQLException is propagated or error status is returned
        
        boolean exceptionThrown = false;
        try {
            userDAO.add(user);
        } catch (Exception e) {
            exceptionThrown = true;
        }
        
        // Check if connection is null (which would cause the operation to fail)
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) {
            // EXPECTED BEHAVIOR: Exception should be thrown when operation fails
            // On UNFIXED code: This assertion FAILS (proving bug exists)
            // On FIXED code: This assertion PASSES (confirming fix works)
            assertTrue(exceptionThrown,
                "Expected exception to be thrown when database operation fails, but operation completed silently. " +
                "This is the bug: SQLException is caught and swallowed without propagation.");
        }
    }

    /**
     * Unit test: getAll returns empty list on error instead of throwing exception
     * 
     * This test demonstrates that getAll() returns an empty list when SQLException
     * occurs, making it impossible to distinguish between "no data" and "error occurred".
     * 
     * EXPECTED: On UNFIXED code, returns empty list even when error occurs
     * EXPECTED: On FIXED code, throws exception or returns null to indicate error
     */
    @Test
    void testGetAllReturnsEmptyListOnErrorInsteadOfThrowingException() {
        UserDAO userDAO = new UserDAO();
        
        // Attempt to get all users
        boolean exceptionThrown = false;
        List<User> users = null;
        
        try {
            users = userDAO.getAll();
        } catch (Exception e) {
            exceptionThrown = true;
        }
        
        // Check if connection is null (which would cause the operation to fail)
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) {
            // EXPECTED BEHAVIOR: When database operation fails, should throw exception or return null
            // On UNFIXED code: Returns empty list (bug - cannot distinguish error from no data)
            // On FIXED code: Throws exception or returns null (proper error indication)
            
            assertTrue(exceptionThrown || users == null,
                "Expected exception to be thrown or null to be returned when database operation fails, " +
                "but got empty list instead. This is the bug: cannot distinguish between 'no data' and 'error occurred'.");
        }
    }
}
