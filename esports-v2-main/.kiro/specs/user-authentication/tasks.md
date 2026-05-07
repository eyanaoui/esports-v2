# Implementation Plan: User Authentication

## Overview

This implementation plan breaks down the user authentication feature into discrete coding tasks. The feature adds a login screen, validates credentials against the MySQL database using bcrypt password verification, stores authenticated user sessions, and routes users to role-appropriate interfaces. All tasks build incrementally on the existing JavaFX MVC architecture.

## Tasks

- [x] 1. Add jBCrypt dependency and enhance AppState for session management
  - Add jBCrypt Maven dependency to pom.xml
  - Add currentUser field to AppState class
  - Implement setCurrentUser(), getCurrentUser(), isAuthenticated(), and clearSession() methods
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 7.3_

- [ ]* 1.1 Write unit tests for AppState session management
  - Test setCurrentUser stores user object
  - Test getCurrentUser retrieves stored user
  - Test isAuthenticated returns correct boolean values
  - Test clearSession removes current user
  - _Requirements: 5.1, 5.2, 5.3_

- [x] 2. Implement UserDAO.findByEmail() method
  - Add findByEmail(String email) method to UserDAO
  - Query user table for matching email
  - Return User object with all fields including password hash
  - Return null if email not found
  - Handle database connection errors
  - _Requirements: 2.1, 2.2_

- [ ]* 2.1 Write unit tests for UserDAO.findByEmail()
  - Test findByEmail returns user with matching email
  - Test findByEmail returns null for non-existent email
  - Test findByEmail returns user with password hash populated
  - Test findByEmail handles database connection errors
  - _Requirements: 2.1, 2.2_

- [x] 3. Create AuthenticationService class
  - [x] 3.1 Implement AuthenticationService with authenticate() method
    - Create AuthenticationService class in com.esports package
    - Implement authenticate(String email, String password) method
    - Query UserDAO for user by email
    - Return null if user not found
    - _Requirements: 2.1, 2.4, 2.5_

  - [x] 3.2 Implement bcrypt password verification
    - Add verifyBcryptPassword(String plainPassword, String hashedPassword) method
    - Use BCrypt.checkpw() from jBCrypt library
    - Handle $2y$13$ format correctly
    - Return false for invalid hash format or verification errors
    - _Requirements: 2.3, 6.1, 6.2, 6.3, 6.4_

  - [ ]* 3.3 Write unit tests for AuthenticationService
    - Test successful authentication with valid credentials
    - Test failed authentication with non-existent email
    - Test failed authentication with incorrect password
    - Test bcrypt verification with valid $2y$13$ hash
    - Test bcrypt verification with invalid hash format
    - Test handling of null and empty string inputs
    - _Requirements: 2.1, 2.3, 2.4, 2.5, 6.1, 6.2, 6.3, 6.4_

- [x] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Create login screen FXML and LoginController
  - [x] 5.1 Create login.fxml view
    - Create /src/main/resources/views/login.fxml file
    - Add centered VBox container with email TextField, password PasswordField, login Button, and error Label
    - Apply existing styles.css for visual consistency
    - Set fx:controller to LoginController
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [x] 5.2 Implement LoginController class
    - Create LoginController class in com.esports.controllers package
    - Add FXML field bindings for emailField, passwordField, loginButton, errorLabel
    - Implement initialize() method to set up AuthenticationService
    - Implement handleLogin() method to collect form inputs and call AuthenticationService
    - Implement showError() and clearError() methods for error display
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 3.1, 3.2, 3.3, 3.4_

  - [x] 5.3 Implement role-based navigation in LoginController
    - Add navigateToAdminDashboard() method for ROLE_ADMIN users
    - Add navigateToUserInterface() method for ROLE_USER users
    - Implement role checking logic: if ROLE_ADMIN navigate to admin dashboard, else if ROLE_USER navigate to user interface
    - Store authenticated user in AppState before navigation
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 5.1, 5.2_

  - [x] 5.4 Implement error handling in LoginController
    - Display "Invalid email or password" for authentication failures
    - Display "Please enter both email and password" for empty fields
    - Display "Unable to connect to authentication service" for database errors
    - Clear password field after failed attempt
    - Clear error message when user starts typing
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [ ]* 5.5 Write unit tests for LoginController
    - Test handleLogin calls AuthenticationService with form inputs
    - Test successful login navigates to admin dashboard for ROLE_ADMIN
    - Test successful login navigates to user interface for ROLE_USER
    - Test failed login displays error message and clears password field
    - Test empty email/password shows validation error
    - _Requirements: 1.4, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3_

- [x] 6. Modify Main.java to start with login screen
  - Update Main.start() method to load login.fxml instead of admin dashboard
  - Call AppState.clearSession() on application start
  - Set stage title to "Esports Login"
  - Apply styles.css to login scene
  - _Requirements: 1.1, 1.5, 7.1, 7.2, 7.3_

- [x] 7. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ]* 8. Write integration tests for complete authentication flow
  - Test login with valid admin credentials navigates to admin dashboard
  - Test login with valid user credentials navigates to user interface
  - Test login with invalid email shows error and stays on login screen
  - Test login with invalid password shows error and stays on login screen
  - Test session stores user after successful login
  - Test application starts with no authenticated user
  - Test bcrypt password verification with real database hash
  - _Requirements: 1.1, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 7.1, 7.2, 7.3_

- [x] 9. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Unit tests validate specific examples and edge cases
- Integration tests validate end-to-end authentication flows
- The design does not include property-based tests as authentication is better suited for example-based testing
