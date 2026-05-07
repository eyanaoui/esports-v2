package com.esports.controllers;

import com.esports.AppState;
import com.esports.dao.OAuthTokenDAO;
import com.esports.dao.UserDAO;
import com.esports.models.GoogleUserProfile;
import com.esports.models.OAuthTokens;
import com.esports.models.User;
import com.esports.models.UserRole;
import com.esports.services.OAuthService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for OAuth 2.0 authentication flow.
 * 
 * This controller manages the complete OAuth flow including:
 * - Initiating OAuth authorization with Google
 * - Handling OAuth callbacks and token exchange
 * - Linking or creating user accounts
 * - Managing OAuth tokens
 * - Disconnecting Google accounts
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.9, 7.9, 12.3, 12.7
 */
public class SignatureOAuthController {
    
    @FXML private Button googleSignInButton;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    
    private final OAuthService oauthService;
    private final OAuthTokenDAO oauthTokenDAO;
    private final UserDAO userDAO;
    
    // Session storage for OAuth state and code verifier
    private static final Map<String, String> sessionStorage = new HashMap<>();
    
    /**
     * Initialize the controller with required services.
     */
    public SignatureOAuthController() {
        this.oauthService = new OAuthService();
        this.oauthTokenDAO = new OAuthTokenDAO();
        this.userDAO = new UserDAO();
    }
    
    /**
     * Constructor for testing with dependency injection.
     */
    SignatureOAuthController(OAuthService oauthService, OAuthTokenDAO oauthTokenDAO, UserDAO userDAO) {
        this.oauthService = oauthService;
        this.oauthTokenDAO = oauthTokenDAO;
        this.userDAO = userDAO;
    }
    
    @FXML
    public void initialize() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
        if (statusLabel != null) {
            statusLabel.setVisible(false);
        }
    }
    
    /**
     * Check if OAuth service is enabled and configured.
     * 
     * @return true if OAuth is ready to use
     */
    public boolean isOAuthEnabled() {
        return oauthService.isEnabled();
    }
    
    /**
     * Handle "Sign in with Google" button click.
     * 
     * Initiates the OAuth 2.0 authorization flow by:
     * 1. Generating state and code verifier for security
     * 2. Creating authorization URL
     * 3. Opening browser to Google's authorization page
     * 
     * Requirement 1.1: Redirect to OAuth provider authorization page
     * Requirement 1.10: Generate state parameter for CSRF protection
     * Requirement 7.6: Implement PKCE flow
     */
    @FXML
    public void handleGoogleSignIn() {
        // Check if OAuth is enabled
        if (!oauthService.isEnabled()) {
            showError("Google OAuth is not configured. Please contact support.");
            return;
        }
        
        try {
            // Show loading indicator
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(true);
            }
            if (statusLabel != null) {
                statusLabel.setText("Redirecting to Google...");
                statusLabel.setVisible(true);
            }
            
            // Generate state and code verifier for security
            String state = oauthService.generateState();
            String codeVerifier = oauthService.generateCodeVerifier();
            
            // Store in session for later validation
            sessionStorage.put("oauth_state", state);
            sessionStorage.put("oauth_code_verifier", codeVerifier);
            
            // Generate authorization URL
            String authUrl = oauthService.generateAuthorizationUrl(state, codeVerifier);
            
            // Open browser to authorization URL
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(authUrl));
                
                if (statusLabel != null) {
                    statusLabel.setText("Please complete authorization in your browser...");
                }
            } else {
                showError("Unable to open browser. Please manually navigate to:\n" + authUrl);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to initiate Google sign-in: " + e.getMessage());
        } finally {
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(false);
            }
        }
    }
    
    /**
     * Handle OAuth callback after user authorization.
     * 
     * This method is called when Google redirects back to the application
     * with an authorization code. It:
     * 1. Validates the state parameter (CSRF protection)
     * 2. Exchanges authorization code for tokens
     * 3. Fetches user profile from Google
     * 4. Links or creates user account
     * 5. Stores encrypted tokens
     * 6. Navigates to appropriate dashboard
     * 
     * @param code Authorization code from Google
     * @param state State parameter for CSRF validation
     * 
     * Requirement 1.2: Exchange authorization code for tokens
     * Requirement 1.3: Retrieve user profile information
     * Requirement 1.4: Link Google account to existing user
     * Requirement 1.5: Create new user if email doesn't exist
     * Requirement 1.9: Display error message on failure
     * Requirement 7.7: Validate redirect URI
     */
    public void handleOAuthCallback(String code, String state) {
        // Run in background thread to avoid blocking UI
        Task<User> authTask = new Task<User>() {
            @Override
            protected User call() throws Exception {
                updateMessage("Validating authorization...");
                
                // Validate state parameter (CSRF protection)
                String expectedState = sessionStorage.get("oauth_state");
                if (!oauthService.validateState(state, expectedState)) {
                    throw new SecurityException("Invalid state parameter. Possible CSRF attack.");
                }
                
                updateMessage("Exchanging authorization code...");
                
                // Get code verifier from session
                String codeVerifier = sessionStorage.get("oauth_code_verifier");
                
                // Exchange code for tokens
                OAuthTokens tokens = oauthService.exchangeCodeForTokens(code, codeVerifier);
                
                updateMessage("Fetching user profile...");
                
                // Fetch user profile from Google
                GoogleUserProfile profile = oauthService.fetchUserProfile(tokens.getEncryptedAccessToken());
                
                updateMessage("Setting up account...");
                
                // Link or create user account
                User user = linkOrCreateUser(profile);
                
                // Store encrypted tokens
                tokens.setUserId(user.getId());
                oauthTokenDAO.saveTokens(
                    user.getId(),
                    tokens.getEncryptedAccessToken(),
                    tokens.getEncryptedRefreshToken(),
                    tokens.getExpiresAt()
                );
                
                // Update last login
                user.setLastLogin(LocalDateTime.now());
                userDAO.update(user);
                
                // Clear session storage
                sessionStorage.remove("oauth_state");
                sessionStorage.remove("oauth_code_verifier");
                
                return user;
            }
        };
        
        // Handle success
        authTask.setOnSucceeded(event -> {
            User user = authTask.getValue();
            AppState.setCurrentUser(user);
            
            // Navigate to appropriate dashboard
            if (user.getRoles().contains(UserRole.ADMIN)) {
                navigateToAdminDashboard();
            } else {
                navigateToUserInterface();
            }
        });
        
        // Handle failure
        authTask.setOnFailed(event -> {
            Throwable error = authTask.getException();
            error.printStackTrace();
            
            String errorMessage;
            if (error instanceof SecurityException) {
                errorMessage = "Security validation failed. Please try again.";
            } else if (error.getMessage().contains("Network")) {
                errorMessage = "Network error. Please check your internet connection.";
            } else {
                errorMessage = "Authentication failed: " + error.getMessage();
            }
            
            showError(errorMessage);
            
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(false);
            }
            if (statusLabel != null) {
                statusLabel.setVisible(false);
            }
        });
        
        // Bind status label to task message
        if (statusLabel != null) {
            statusLabel.textProperty().bind(authTask.messageProperty());
            statusLabel.setVisible(true);
        }
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        
        // Start background task
        new Thread(authTask).start();
    }
    
    /**
     * Link Google account to existing user or create new user.
     * 
     * @param profile Google user profile
     * @return User account (existing or newly created)
     * 
     * Requirement 1.4: Link Google account to existing user
     * Requirement 1.5: Create new user if email doesn't exist
     * Requirement 1.6: Store profile picture URL
     */
    private User linkOrCreateUser(GoogleUserProfile profile) {
        // Check if user with this email already exists
        User existingUser = userDAO.findByEmail(profile.getEmail());
        
        if (existingUser != null) {
            // Link Google account to existing user
            existingUser.setGoogleId(profile.getGoogleId());
            existingUser.setProfilePictureUrl(profile.getProfilePictureUrl());
            userDAO.update(existingUser);
            return existingUser;
        } else {
            // Create new user with Google profile information
            String[] nameParts = profile.getName().split(" ", 2);
            String firstName = nameParts.length > 0 ? nameParts[0] : "User";
            String lastName = nameParts.length > 1 ? nameParts[1] : "";
            
            List<UserRole> roles = new ArrayList<>();
            roles.add(UserRole.USER);
            
            User newUser = new User(firstName, lastName, profile.getEmail(), null, roles);
            newUser.setGoogleId(profile.getGoogleId());
            newUser.setProfilePictureUrl(profile.getProfilePictureUrl());
            
            userDAO.add(newUser);
            
            // Retrieve the user to get the generated ID
            return userDAO.findByEmail(profile.getEmail());
        }
    }
    
    /**
     * Disconnect Google account from current user.
     * 
     * This method:
     * 1. Revokes OAuth tokens with Google
     * 2. Deletes stored tokens from database
     * 3. Removes Google ID and profile picture from user
     * 
     * Requirement 7.9: Revoke tokens when disconnecting
     * Requirement 7.10: Delete stored tokens
     * Requirement 12.3: Allow disabling authentication methods
     */
    public void handleDisconnectGoogle() {
        User currentUser = AppState.getCurrentUser();
        if (currentUser == null) {
            showError("No user logged in");
            return;
        }
        
        // Check if user has other authentication methods
        if (currentUser.getPassword() == null || currentUser.getPassword().isEmpty()) {
            showError("Cannot disconnect Google account. Please set a password first to maintain account access.");
            return;
        }
        
        Task<Void> disconnectTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Disconnecting Google account...");
                
                // Get stored tokens
                OAuthTokens tokens = oauthTokenDAO.getTokensByUserId(currentUser.getId());
                
                if (tokens != null) {
                    // Revoke tokens with Google
                    try {
                        oauthService.revokeTokens(tokens.getEncryptedAccessToken());
                    } catch (Exception e) {
                        // Log but don't fail - continue with local cleanup
                        System.err.println("Failed to revoke tokens with Google: " + e.getMessage());
                    }
                    
                    // Delete tokens from database
                    oauthTokenDAO.deleteTokens(currentUser.getId());
                }
                
                // Remove Google ID and profile picture from user
                currentUser.setGoogleId(null);
                currentUser.setProfilePictureUrl(null);
                userDAO.update(currentUser);
                
                return null;
            }
        };
        
        disconnectTask.setOnSucceeded(event -> {
            showSuccess("Google account disconnected successfully");
            AppState.setCurrentUser(currentUser); // Update session
        });
        
        disconnectTask.setOnFailed(event -> {
            Throwable error = disconnectTask.getException();
            error.printStackTrace();
            showError("Failed to disconnect Google account: " + error.getMessage());
        });
        
        if (statusLabel != null) {
            statusLabel.textProperty().bind(disconnectTask.messageProperty());
            statusLabel.setVisible(true);
        }
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        
        new Thread(disconnectTask).start();
    }
    
    /**
     * Navigate to admin dashboard.
     */
    private void navigateToAdminDashboard() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/admin-dashboard.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = (Stage) (googleSignInButton != null ? googleSignInButton.getScene().getWindow() : 
                                      statusLabel.getScene().getWindow());
                stage.setScene(scene);
                stage.setTitle("Esports Admin Panel");
                stage.setMaximized(true);
                scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            } catch (Exception e) {
                e.printStackTrace();
                showError("Failed to load admin dashboard");
            }
        });
    }
    
    /**
     * Navigate to user interface.
     */
    private void navigateToUserInterface() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/game-browse.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = (Stage) (googleSignInButton != null ? googleSignInButton.getScene().getWindow() : 
                                      statusLabel.getScene().getWindow());
                stage.setScene(scene);
                stage.setTitle("Esports Platform");
                stage.setMaximized(true);
                scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            } catch (Exception e) {
                e.printStackTrace();
                showError("Failed to load user interface");
            }
        });
    }
    
    /**
     * Show error alert dialog.
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("OAuth Error");
            alert.setHeaderText("Authentication Failed");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Show success alert dialog.
     */
    private void showSuccess(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
