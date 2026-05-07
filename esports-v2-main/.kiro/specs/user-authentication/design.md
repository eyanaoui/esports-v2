# Design Document: User Authentication

## Overview

This design implements a login and authentication system for the esports JavaFX application. The system will validate user credentials against the MySQL database, verify bcrypt-hashed passwords, store authenticated user sessions, and route users to role-appropriate interfaces (Admin Dashboard or User Interface).

The authentication system integrates with the existing JavaFX architecture, following the MVC pattern used throughout the application. It leverages the existing UserDAO for database access and extends AppState for session management.

## Architecture

### Component Structure

```
┌─────────────────────────────────────────────────────────────┐
│                      Application Entry                       │
│                         (Main.java)                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      Login Screen                            │
│              (login.fxml + LoginController)                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                  AuthenticationService                       │
│         • validateCredentials(email, password)               │
│         • verifyBcryptPassword(plain, hash)                  │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
┌──────────────────┐    ┌──────────────────┐
│     UserDAO      │    │     AppState     │
│  • findByEmail() │    │ • setCurrentUser()│
└──────────────────┘    └──────────────────┘
                                 │
                    ┌────────────┴────────────┐
                    ▼                         ▼
        ┌──────────────────┐    ┌──────────────────┐
        │ Admin Dashboard  │    │  User Interface  │
        │   (ROLE_ADMIN)   │    │   (ROLE_USER)    │
        └──────────────────┘    └──────────────────┘
```

### Authentication Flow

1. Application starts → Display login screen
2. User enters email and password → Submit credentials
3. AuthenticationService queries UserDAO for user by email
4. If user exists → Verify bcrypt password hash
5. If password valid → Store user in AppState session
6. Route to appropriate interface based on user role
7. If authentication fails → Display error message and allow retry

### Security Considerations

- Passwords are never stored in plaintext
- Bcrypt verification uses constant-time comparison
- Failed login attempts do not reveal whether email exists
- Session is cleared on application start
- No bypass routes to protected screens

## Components and Interfaces

### 1. LoginController

**Responsibility**: Handle user input, coordinate authentication, display errors, and navigate to appropriate screens.

**FXML Bindings**:
- `TextField emailField` - Email input
- `PasswordField passwordField` - Password input
- `Button loginButton` - Submit credentials
- `Label errorLabel` - Display error messages

**Methods**:
```java
public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    
    private AuthenticationService authService;
    
    public void initialize();
    
    @FXML
    private void handleLogin();
    
    private void navigateToAdminDashboard();
    
    private void navigateToUserInterface();
    
    private void showError(String message);
    
    private void clearError();
}
```

### 2. AuthenticationService

**Responsibility**: Validate credentials, verify passwords, and manage authentication logic.

**Methods**:
```java
public class AuthenticationService {
    private UserDAO userDAO;
    
    public AuthenticationService();
    
    public User authenticate(String email, String password);
    
    private boolean verifyBcryptPassword(String plainPassword, String hashedPassword);
    
    private User findUserByEmail(String email);
}
```

**Authentication Logic**:
- Query database for user by email
- Return null if user not found
- Verify bcrypt password if user exists
- Return User object on success, null on failure

### 3. UserDAO Enhancement

**New Method**:
```java
public User findByEmail(String email);
```

This method queries the user table for a matching email and returns the User object with all fields populated, including the password hash.

### 4. AppState Enhancement

**New Fields and Methods**:
```java
public class AppState {
    private static boolean darkMode = false;
    private static User currentUser = null;
    
    public static User getCurrentUser();
    public static void setCurrentUser(User user);
    public static boolean isAuthenticated();
    public static void clearSession();
}
```

### 5. Main.java Modification

**Updated Entry Point**:
```java
@Override
public void start(Stage stage) throws Exception {
    AppState.clearSession(); // Ensure no user is logged in
    
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
    Scene scene = new Scene(loader.load());
    stage.setTitle("Esports Login");
    stage.setScene(scene);
    stage.show();
    scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
}
```

## Data Models

### User Model (Existing)

The existing User model already contains all necessary fields:
- `int id` - Primary key
- `String email` - Login identifier
- `String password` - Bcrypt hash (format: $2y$13$...)
- `String firstName` - User's first name
- `String lastName` - User's last name
- `List<UserRole> roles` - User roles (ADMIN, USER)

### UserRole Enum (Existing)

```java
public enum UserRole {
    ADMIN("Admin"),
    USER("User");
}
```

### Database Schema (Existing)

The user table already exists with the following relevant columns:
- `id` INT PRIMARY KEY
- `email` VARCHAR(255) UNIQUE
- `password` VARCHAR(255) - Bcrypt hash
- `first_name` VARCHAR(100)
- `last_name` VARCHAR(100)
- `roles` JSON - Array of role strings

## Bcrypt Password Verification

### Library Selection

Use **jBCrypt** library for bcrypt password hashing and verification.

**Maven Dependency**:
```xml
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```

### Verification Implementation

```java
import org.mindrot.jbcrypt.BCrypt;

private boolean verifyBcryptPassword(String plainPassword, String hashedPassword) {
    try {
        // BCrypt.checkpw handles $2y$ format automatically
        return BCrypt.checkpw(plainPassword, hashedPassword);
    } catch (Exception e) {
        // Invalid hash format or verification error
        return false;
    }
}
```

### Bcrypt Format Handling

The database stores passwords in `$2y$13$...` format. The jBCrypt library automatically handles the `$2y$` prefix (PHP bcrypt format) and cost factor 13.

## Error Handling

### Authentication Errors

1. **Invalid Credentials**: Display "Invalid email or password" for both non-existent email and wrong password
2. **Database Connection Error**: Display "Unable to connect to authentication service"
3. **Invalid Hash Format**: Treat as authentication failure
4. **Empty Fields**: Display "Please enter both email and password"

### Error Display Strategy

- Show errors in a Label below the login button
- Style error text in red
- Clear error message when user starts typing
- Clear password field after failed attempt
- Keep email field populated for retry

### Exception Handling

```java
try {
    User user = authService.authenticate(email, password);
    if (user != null) {
        // Success
    } else {
        showError("Invalid email or password");
    }
} catch (RuntimeException e) {
    showError("Unable to connect to authentication service");
    e.printStackTrace();
}
```

## Testing Strategy

### Why Property-Based Testing Is Not Applicable

Property-based testing (PBT) is not appropriate for this feature because:

1. **UI Interaction**: The login screen is a JavaFX UI component with specific user interactions (button clicks, text input) that are better tested with example-based scenarios
2. **External Dependencies**: Authentication requires database queries and bcrypt verification, which are integration points rather than pure functions
3. **Deterministic Behavior**: Authentication has specific, deterministic outcomes (success/failure) based on concrete examples rather than universal properties across infinite inputs
4. **State Management**: Session management involves side effects (storing user in AppState) rather than pure transformations

Instead, this feature uses a combination of unit tests (for isolated components), integration tests (for database and authentication flow), and manual testing (for UI behavior).

### Unit Tests

Unit tests will use Mockito to mock external dependencies and test components in isolation.

1. **AuthenticationService Tests** (Validates Requirements 2, 6)
   - Test successful authentication with valid credentials
   - Test failed authentication with non-existent email
   - Test failed authentication with incorrect password
   - Test bcrypt verification with valid $2y$13$ hash
   - Test bcrypt verification with invalid hash
   - Test bcrypt verification with malformed hash format
   - Test handling of null email input
   - Test handling of null password input
   - Test handling of empty string inputs

2. **UserDAO Tests** (Validates Requirement 2)
   - Test findByEmail returns user with matching email
   - Test findByEmail returns null for non-existent email
   - Test findByEmail returns user with password hash populated
   - Test findByEmail handles database connection errors

3. **AppState Session Management Tests** (Validates Requirement 5)
   - Test setCurrentUser stores user object
   - Test getCurrentUser retrieves stored user
   - Test isAuthenticated returns true when user is set
   - Test isAuthenticated returns false when user is null
   - Test clearSession removes current user
   - Test getCurrentUser returns null after clearSession

4. **LoginController Tests** (Validates Requirements 1, 3, 4, 7)
   - Test handleLogin calls AuthenticationService with form inputs
   - Test successful login navigates to admin dashboard for ROLE_ADMIN
   - Test successful login navigates to user interface for ROLE_USER
   - Test failed login displays error message
   - Test failed login clears password field
   - Test error message displays "Invalid email or password"
   - Test empty email shows validation error
   - Test empty password shows validation error

### Integration Tests

Integration tests will use a test database or in-memory database to verify end-to-end flows.

1. **Complete Authentication Flow Tests** (Validates Requirements 1-7)
   - Test login with valid admin credentials navigates to admin dashboard
   - Test login with valid user credentials navigates to user interface
   - Test login with invalid email shows error and stays on login screen
   - Test login with invalid password shows error and stays on login screen
   - Test session stores user after successful login
   - Test application starts with no authenticated user
   - Test bcrypt password verification with real database hash

2. **Role-Based Routing Tests** (Validates Requirement 4)
   - Test user with ROLE_ADMIN routes to admin dashboard
   - Test user with ROLE_USER routes to user interface
   - Test user with both roles routes to admin dashboard (admin takes precedence)
   - Test user with no roles shows error message

3. **Security Tests** (Validates Requirement 7)
   - Test application starts with login screen, not admin dashboard
   - Test AppState initializes with null currentUser
   - Test cannot navigate to protected screens without authentication

### Manual Testing Checklist

- [ ] Login screen displays on application start (Requirement 1)
- [ ] Email and password fields accept input (Requirement 1)
- [ ] Login button triggers authentication (Requirement 1)
- [ ] Valid admin credentials navigate to admin dashboard (Requirement 4)
- [ ] Valid user credentials navigate to user interface (Requirement 4)
- [ ] Invalid credentials show "Invalid email or password" (Requirement 3)
- [ ] Error message clears when typing (Requirement 3)
- [ ] Password field clears after failed attempt (Requirement 3)
- [ ] Application cannot bypass login screen (Requirement 7)
- [ ] Session persists across screen navigation (Requirement 5)
- [ ] Bcrypt passwords with $2y$13$ format verify correctly (Requirement 6)

## Implementation Notes

### JavaFX Scene Navigation

Use the existing pattern from AdminDashboardController for scene transitions:

```java
FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/admin-dashboard.fxml"));
Scene scene = new Scene(loader.load());
Stage stage = (Stage) loginButton.getScene().getWindow();
stage.setScene(scene);
stage.setTitle("Esports Admin Panel");
stage.setMaximized(true);
```

### Role-Based Routing Logic

```java
if (user.getRoles().contains(UserRole.ADMIN)) {
    navigateToAdminDashboard();
} else if (user.getRoles().contains(UserRole.USER)) {
    navigateToUserInterface();
} else {
    showError("No valid role assigned to user");
}
```

### FXML Layout

The login screen should be centered, minimal, and follow the existing application styling:
- Centered VBox container
- Application title/logo
- Email TextField
- Password PasswordField
- Login Button
- Error Label (initially hidden)

### Styling Consistency

Apply the existing styles.css to maintain visual consistency with the rest of the application. The login screen should support both light and dark mode themes.

## Dependencies

### New Dependencies

1. **jBCrypt** (org.mindrot:jbcrypt:0.4) - For bcrypt password verification

### Existing Dependencies

- JavaFX (org.openjfx:javafx-controls, javafx-fxml)
- MySQL Connector (mysql:mysql-connector-java)
- JUnit Jupiter (org.junit.jupiter:junit-jupiter) - For unit tests
- Mockito (org.mockito:mockito-core) - For mocking in tests

## Deployment Considerations

### Database Requirements

- MySQL database must be running on localhost:3306
- Database name: esports_db
- User table must exist with bcrypt-hashed passwords
- At least one user record must exist for testing

### Test User Setup

For testing, ensure at least one user exists with known credentials:
```sql
-- Example: Create test admin user
-- Password: "admin123" hashed with bcrypt cost 13
INSERT INTO user (first_name, last_name, email, password, roles, created_at, is_blocked)
VALUES ('Admin', 'User', 'admin@test.com', '$2y$13$...', '["ADMIN"]', NOW(), false);
```

### Configuration

No additional configuration files needed. Database connection uses existing DatabaseConnection singleton.

## Future Enhancements

1. **Password Reset**: Add "Forgot Password" functionality
2. **Remember Me**: Persist session across application restarts
3. **Account Lockout**: Lock account after N failed attempts
4. **Session Timeout**: Auto-logout after inactivity
5. **Logout Button**: Add explicit logout functionality
6. **Registration**: Allow new users to create accounts
7. **Multi-Factor Authentication**: Add 2FA support
8. **Audit Logging**: Log all authentication attempts

## References

- jBCrypt Documentation: https://www.mindrot.org/projects/jBCrypt/
- JavaFX Documentation: https://openjfx.io/
- Bcrypt Specification: https://en.wikipedia.org/wiki/Bcrypt
