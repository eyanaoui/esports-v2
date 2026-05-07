package com.esports.controllers;

import com.esports.AppState;
import com.esports.AuthenticationService;
import com.esports.components.SignatureCanvas;
import com.esports.dao.UserDAO;
import com.esports.models.User;
import com.esports.models.UserRole;
import com.esports.services.SignatureAuthService;
import com.esports.services.CaptchaService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.*;
import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LoginController {

    // Login fields
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Button loginButton;
    @FXML private Button fullscreenButton;
    @FXML private Button googleSignInButton;
    @FXML private Button signatureLoginButton;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Label errorLabel;
    @FXML private Label subtitleLabel;
    @FXML private VBox loginCard;
    @FXML private Label captchaQuestionLabel;
    @FXML private TextField captchaAnswerField;
    @FXML private Button refreshCaptchaButton;
    
    // Signature authentication fields
    @FXML private VBox signatureCard;
    @FXML private TextField signatureEmailField;
    @FXML private HBox signatureCanvasContainer;
    @FXML private Button clearSignatureButton;
    @FXML private Button submitSignatureButton;
    @FXML private Button usePasswordButton;
    @FXML private Button backFromSignatureButton;
    @FXML private Label signatureErrorLabel;
    @FXML private Label signatureInfoLabel;
    @FXML private Label signatureCaptchaQuestionLabel;
    @FXML private TextField signatureCaptchaAnswerField;
    @FXML private Button refreshSignatureCaptchaButton;
    private SignatureCanvas signatureCanvas;
    private int signatureAttempts = 0;
    private static final int MAX_SIGNATURE_ATTEMPTS = 3;
    private CaptchaService.CaptchaChallenge currentSignatureCaptcha;

    // Signup fields
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField signupEmailField;
    @FXML private PasswordField signupPasswordField;
    @FXML private Button generatePasswordButton;
    @FXML private Button showPasswordButton;
    @FXML private Label passwordStrengthLabel;
    @FXML private Label signupErrorLabel;
    @FXML private Label signupSuccessLabel;
    @FXML private VBox signupCard;
    @FXML private Button backToLoginButton;
    @FXML private HBox signupSignatureCanvasContainer;
    @FXML private Button clearSignupSignatureButton;
    @FXML private Button uploadSignupSignatureButton;
    @FXML private Label signupSignatureFileLabel;
    @FXML private Label signupCaptchaQuestionLabel;
    @FXML private TextField signupCaptchaAnswerField;
    @FXML private Button refreshSignupCaptchaButton;
    private SignatureCanvas signupSignatureCanvas;
    private BufferedImage uploadedSignatureImage;

    private AuthenticationService authService;
    private UserDAO userDAO;
    private SignatureOAuthController oauthController;
    private SignatureAuthService signatureAuthService;
    private CaptchaService captchaService;
    private CaptchaService.CaptchaChallenge currentLoginCaptcha;
    private CaptchaService.CaptchaChallenge currentSignupCaptcha;
    private static final String REMEMBER_ME_FILE = "remember_me.properties";

    @FXML
    public void initialize() {
        authService = new AuthenticationService();
        userDAO = new UserDAO();
        oauthController = new SignatureOAuthController();
        signatureAuthService = new SignatureAuthService();
        captchaService = new CaptchaService();
        
        // Generate initial CAPTCHAs
        refreshLoginCaptcha();
        refreshSignupCaptcha();
        refreshSignatureCaptcha();
        
        // Clear error when user starts typing (login)
        emailField.textProperty().addListener((obs, oldVal, newVal) -> clearError());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> clearError());
        
        // Clear error when user starts typing (signup)
        if (firstNameField != null) {
            firstNameField.textProperty().addListener((obs, oldVal, newVal) -> clearSignupMessages());
            lastNameField.textProperty().addListener((obs, oldVal, newVal) -> clearSignupMessages());
            signupEmailField.textProperty().addListener((obs, oldVal, newVal) -> clearSignupMessages());
            signupPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
                clearSignupMessages();
                updatePasswordStrength(newVal);
            });
        }
        
        // Initialize signature canvas if container exists
        if (signatureCanvasContainer != null) {
            signatureCanvas = new SignatureCanvas(400, 200);
            signatureCanvas.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-color: white;");
            signatureCanvasContainer.getChildren().add(signatureCanvas);
        }
        
        // Initialize signup signature canvas if container exists
        if (signupSignatureCanvasContainer != null) {
            signupSignatureCanvas = new SignatureCanvas(400, 200);
            signupSignatureCanvas.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-color: white;");
            signupSignatureCanvasContainer.getChildren().add(signupSignatureCanvas);
        }
        
        // Clear signature error when user starts typing email
        if (signatureEmailField != null) {
            signatureEmailField.textProperty().addListener((obs, oldVal, newVal) -> clearSignatureError());
        }
        
        // Load remembered credentials
        loadRememberedCredentials();
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String captchaAnswer = captchaAnswerField != null ? captchaAnswerField.getText().trim() : "";

        // Validate empty fields
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password");
            return;
        }
        
        // Validate CAPTCHA
        if (currentLoginCaptcha == null || captchaAnswer.isEmpty()) {
            showError("Please answer the CAPTCHA");
            return;
        }
        
        if (!captchaService.validateCaptcha(currentLoginCaptcha.getChallengeId(), captchaAnswer)) {
            showError("Incorrect CAPTCHA answer. Please try again.");
            refreshLoginCaptcha();
            return;
        }

        try {
            User user = authService.authenticate(email, password);
            
            if (user != null) {
                // Check if user is blocked
                if (user.getIsBlocked() != null && user.getIsBlocked()) {
                    showError("Your account has been blocked. Please contact support.");
                    passwordField.clear();
                    refreshLoginCaptcha();
                    return;
                }
                
                // Handle remember me
                if (rememberMeCheckbox.isSelected()) {
                    saveRememberedCredentials(email, password);
                } else {
                    clearRememberedCredentials();
                }
                
                // Store user in session
                AppState.setCurrentUser(user);
                
                // Route based on role
                if (user.getRoles().contains(UserRole.ADMIN)) {
                    navigateToAdminDashboard();
                } else if (user.getRoles().contains(UserRole.USER)) {
                    navigateToUserInterface();
                } else {
                    showError("No valid role assigned to user");
                }
            } else {
                showError("Invalid email or password");
                passwordField.clear();
                refreshLoginCaptcha();
            }
        } catch (RuntimeException e) {
            showError("Unable to connect to authentication service");
            refreshLoginCaptcha();
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowSignup() {
        // Hide login card, show signup card
        loginCard.setVisible(false);
        loginCard.setManaged(false);
        signupCard.setVisible(true);
        signupCard.setManaged(true);
        clearError();
        clearSignupMessages();
    }

    @FXML
    private void handleShowLogin() {
        // Hide signup card, show login card
        signupCard.setVisible(false);
        signupCard.setManaged(false);
        loginCard.setVisible(true);
        loginCard.setManaged(true);
        clearError();
        clearSignupMessages();
    }

    @FXML
    private void handleGoogleSignIn() {
        try {
            // Delegate to OAuth controller
            oauthController.handleGoogleSignIn();
        } catch (Exception e) {
            showError("Failed to initiate Google sign-in: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/signature-password-recovery.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Esports Platform - Password Recovery");
        } catch (Exception e) {
            showError("Failed to load password recovery screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleToggleFullscreen() {
        System.out.println("[DEBUG] Fullscreen button clicked!");
        try {
            Stage stage = (Stage) fullscreenButton.getScene().getWindow();
            System.out.println("[DEBUG] Stage found: " + stage);
            System.out.println("[DEBUG] Current state - Maximized: " + stage.isMaximized() + ", Fullscreen: " + stage.isFullScreen());
            
            if (stage.isMaximized() || stage.isFullScreen()) {
                // Exit fullscreen/maximized
                System.out.println("[DEBUG] Exiting fullscreen/maximized mode");
                stage.setFullScreen(false);
                stage.setMaximized(false);
                stage.setWidth(600);
                stage.setHeight(700);
                stage.centerOnScreen();
                fullscreenButton.setText("⛶ Fullscreen");
            } else {
                // Enter maximized mode (fullscreen doesn't work well on Windows)
                System.out.println("[DEBUG] Entering maximized mode");
                stage.setMaximized(true);
                fullscreenButton.setText("⛶ Exit Fullscreen");
            }
            System.out.println("[DEBUG] Fullscreen toggle completed successfully");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to toggle fullscreen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSignup() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = signupEmailField.getText().trim();
        String password = signupPasswordField.getText().trim();
        String captchaAnswer = signupCaptchaAnswerField != null ? signupCaptchaAnswerField.getText().trim() : "";

        // Validation - All fields required
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showSignupError("All fields are required");
            return;
        }
        
        // Validate CAPTCHA
        if (currentSignupCaptcha == null || captchaAnswer.isEmpty()) {
            showSignupError("Please answer the CAPTCHA");
            return;
        }
        
        if (!captchaService.validateCaptcha(currentSignupCaptcha.getChallengeId(), captchaAnswer)) {
            showSignupError("Incorrect CAPTCHA answer. Please try again.");
            refreshSignupCaptcha();
            return;
        }

        // First name validation
        if (firstName.length() < 2) {
            showSignupError("First name must be at least 2 characters");
            return;
        }
        if (firstName.length() > 50) {
            showSignupError("First name must not exceed 50 characters");
            return;
        }
        if (!firstName.matches("^[a-zA-Z\\s'-]+$")) {
            showSignupError("First name can only contain letters, spaces, hyphens and apostrophes");
            return;
        }

        // Last name validation
        if (lastName.length() < 2) {
            showSignupError("Last name must be at least 2 characters");
            return;
        }
        if (lastName.length() > 50) {
            showSignupError("Last name must not exceed 50 characters");
            return;
        }
        if (!lastName.matches("^[a-zA-Z\\s'-]+$")) {
            showSignupError("Last name can only contain letters, spaces, hyphens and apostrophes");
            return;
        }

        // Email validation
        if (!isValidEmail(email)) {
            showSignupError("Invalid email format (example: user@example.com)");
            return;
        }
        if (email.length() > 100) {
            showSignupError("Email must not exceed 100 characters");
            return;
        }

        // Password validation
        if (password.length() < 4) {
            showSignupError("Password must be at least 4 characters");
            return;
        }
        if (password.length() > 100) {
            showSignupError("Password must not exceed 100 characters");
            return;
        }
        if (password.contains(" ")) {
            showSignupError("Password cannot contain spaces");
            return;
        }

        // Signature validation - check if user has drawn OR uploaded a signature
        boolean hasDrawnSignature = signupSignatureCanvas != null && signupSignatureCanvas.hasContent();
        boolean hasUploadedSignature = uploadedSignatureImage != null;
        
        if (!hasDrawnSignature && !hasUploadedSignature) {
            showSignupError("Please draw or upload your signature");
            return;
        }

        // Check if email already exists
        try {
            User existingUser = userDAO.findByEmail(email);
            if (existingUser != null) {
                showSignupError("An account with this email already exists");
                return;
            }
        } catch (Exception e) {
            showSignupError("Error checking email availability: " + e.getMessage());
            return;
        }

        try {
            // Hash the password before storing
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            
            // Create new user with USER role
            List<UserRole> roles = new ArrayList<>();
            roles.add(UserRole.USER);
            
            User newUser = new User(firstName, lastName, email, hashedPassword, roles);
            
            boolean success = userDAO.add(newUser);
            
            if (success) {
                // Get the created user to retrieve the user ID
                User createdUser = userDAO.findByEmail(email);
                
                if (createdUser != null) {
                    // Save the signature (use uploaded image if available, otherwise use drawn signature)
                    try {
                        BufferedImage signatureImage;
                        if (uploadedSignatureImage != null) {
                            signatureImage = uploadedSignatureImage;
                        } else {
                            signatureImage = signupSignatureCanvas.getSignatureImage();
                        }
                        
                        boolean signatureSaved = signatureAuthService.saveSignature(createdUser.getId(), signatureImage);
                        
                        if (!signatureSaved) {
                            System.out.println("[WARNING] Failed to save signature for user: " + email);
                        }
                    } catch (Exception e) {
                        System.out.println("[ERROR] Error saving signature: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                showSignupSuccess("Account created successfully! You can now login.");
                
                // Clear form fields
                firstNameField.clear();
                lastNameField.clear();
                signupEmailField.clear();
                signupPasswordField.clear();
                if (signupSignatureCanvas != null) {
                    signupSignatureCanvas.clear();
                }
                uploadedSignatureImage = null;
                if (signupSignatureFileLabel != null) {
                    signupSignatureFileLabel.setVisible(false);
                    signupSignatureFileLabel.setText("");
                }
                
                // Refresh CAPTCHA for next signup
                refreshSignupCaptcha();
                
                // Auto-switch to login after 2 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        javafx.application.Platform.runLater(() -> {
                            // Pre-fill email in login form
                            emailField.setText(email);
                            handleShowLogin();
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                
            } else {
                showSignupError("Failed to create account. Please try again.");
            }
        } catch (Exception e) {
            showSignupError("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateToAdminDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/admin-dashboard.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Esports Admin Panel");
            stage.setMaximized(true);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            showError("Failed to load admin dashboard");
            e.printStackTrace();
        }
    }

    private void navigateToUserInterface() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/admin-dashboard.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Esports Platform");
            stage.setMaximized(true);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            showError("Failed to load user interface");
            e.printStackTrace();
        }
    }

    private void saveRememberedCredentials(String email, String password) {
        try {
            Properties props = new Properties();
            props.setProperty("email", email);
            props.setProperty("password", password); // In production, use encrypted storage
            props.setProperty("remember", "true");
            
            File file = new File(REMEMBER_ME_FILE);
            try (FileOutputStream out = new FileOutputStream(file)) {
                props.store(out, "Remember Me Credentials");
            }
        } catch (IOException e) {
            System.out.println("[WARNING] Failed to save remember me credentials: " + e.getMessage());
        }
    }

    private void loadRememberedCredentials() {
        try {
            File file = new File(REMEMBER_ME_FILE);
            if (file.exists()) {
                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream(file)) {
                    props.load(in);
                }
                
                String remember = props.getProperty("remember", "false");
                if ("true".equals(remember)) {
                    String email = props.getProperty("email", "");
                    String password = props.getProperty("password", "");
                    
                    emailField.setText(email);
                    passwordField.setText(password);
                    rememberMeCheckbox.setSelected(true);
                }
            }
        } catch (IOException e) {
            System.out.println("[WARNING] Failed to load remember me credentials: " + e.getMessage());
        }
    }

    private void clearRememberedCredentials() {
        try {
            File file = new File(REMEMBER_ME_FILE);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            System.out.println("[WARNING] Failed to clear remember me credentials: " + e.getMessage());
        }
    }

    private boolean isValidEmail(String email) {
        // More robust email validation
        if (email == null || email.isEmpty()) {
            return false;
        }
        // Check basic email pattern with proper domain
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
        errorLabel.setText("");
    }

    private void showSignupError(String message) {
        signupErrorLabel.setText(message);
        signupErrorLabel.setVisible(true);
        signupSuccessLabel.setVisible(false);
    }

    private void showSignupSuccess(String message) {
        signupSuccessLabel.setText(message);
        signupSuccessLabel.setVisible(true);
        signupErrorLabel.setVisible(false);
    }

    private void clearSignupMessages() {
        signupErrorLabel.setVisible(false);
        signupErrorLabel.setText("");
        signupSuccessLabel.setVisible(false);
        signupSuccessLabel.setText("");
    }
    
    /**
     * Show signature login screen.
     * 
     * Requirement 2.1: Display signature canvas on login screen
     * Requirement 2.10: Provide "Use Password Instead" fallback
     */
    @FXML
    private void handleShowSignatureLogin() {
        // Hide login and signup cards
        loginCard.setVisible(false);
        loginCard.setManaged(false);
        if (signupCard != null) {
            signupCard.setVisible(false);
            signupCard.setManaged(false);
        }
        
        // Show signature card
        if (signatureCard != null) {
            signatureCard.setVisible(true);
            signatureCard.setManaged(true);
            
            // Clear previous signature and errors
            if (signatureCanvas != null) {
                signatureCanvas.clear();
            }
            clearSignatureError();
            signatureAttempts = 0;
            
            // Refresh CAPTCHA
            refreshSignatureCaptcha();
            
            // Update info label
            if (signatureInfoLabel != null) {
                signatureInfoLabel.setText("Draw your signature to login");
                signatureInfoLabel.setVisible(true);
            }
        }
    }
    
    /**
     * Handle signature login submission.
     * 
     * Requirement 2.5: Submit signature for authentication
     * Requirement 2.6: Compare signature to stored signature
     * Requirement 2.8: Authenticate if similarity score >= 75
     * Requirement 2.9: Reject if similarity score < 75
     * Requirement 2.11: Fallback to password after 3 failed attempts
     * Requirement 2.12: Display similarity score feedback
     */
    @FXML
    private void handleSignatureLogin() {
        String email = signatureEmailField.getText().trim();
        String captchaAnswer = signatureCaptchaAnswerField != null ? signatureCaptchaAnswerField.getText().trim() : "";
        
        // Validate email
        if (email.isEmpty()) {
            showSignatureError("Please enter your email address");
            return;
        }
        
        // Validate CAPTCHA
        if (currentSignatureCaptcha == null || captchaAnswer.isEmpty()) {
            showSignatureError("Please answer the CAPTCHA");
            return;
        }
        
        if (!captchaService.validateCaptcha(currentSignatureCaptcha.getChallengeId(), captchaAnswer)) {
            showSignatureError("Incorrect CAPTCHA answer. Please try again.");
            refreshSignatureCaptcha();
            return;
        }
        
        // Check if signature has content
        if (signatureCanvas == null || !signatureCanvas.hasContent()) {
            showSignatureError("Please draw your signature");
            refreshSignatureCaptcha();
            return;
        }
        
        try {
            // Find user by email
            User user = userDAO.findByEmail(email);
            if (user == null) {
                showSignatureError("No account found with this email");
                refreshSignatureCaptcha();
                return;
            }
            
            // Check if user is blocked
            if (user.getIsBlocked() != null && user.getIsBlocked()) {
                showSignatureError("Your account has been blocked. Please contact support.");
                refreshSignatureCaptcha();
                return;
            }
            
            // Get signature image
            BufferedImage signatureImage = signatureCanvas.getSignatureImage();
            
            // Authenticate with signature
            SignatureAuthService.AuthenticationResult result = 
                signatureAuthService.authenticateWithSignature(user.getId(), signatureImage);
            
            if (result.isSuccess()) {
                // Authentication successful
                AppState.setCurrentUser(user);
                
                // Route based on role
                if (user.getRoles().contains(UserRole.ADMIN)) {
                    navigateToAdminDashboard();
                } else if (user.getRoles().contains(UserRole.USER)) {
                    navigateToUserInterface();
                } else {
                    showSignatureError("No valid role assigned to user");
                    refreshSignatureCaptcha();
                }
            } else {
                // Authentication failed
                signatureAttempts++;
                
                // Handle different failure reasons
                switch (result.getFailureReason()) {
                    case LOW_SIMILARITY:
                        String scoreMessage = String.format(
                            "Signature does not match (similarity: %.1f%%). Please try again. (%d/%d attempts)",
                            result.getSimilarityScore(),
                            signatureAttempts,
                            MAX_SIGNATURE_ATTEMPTS
                        );
                        showSignatureError(scoreMessage);
                        
                        // Clear canvas for retry
                        signatureCanvas.clear();
                        refreshSignatureCaptcha();
                        
                        // Check if max attempts reached
                        if (signatureAttempts >= MAX_SIGNATURE_ATTEMPTS) {
                            showSignatureError(
                                "Maximum attempts reached. Please use password authentication."
                            );
                            
                            // Disable signature login and show password option
                            if (submitSignatureButton != null) {
                                submitSignatureButton.setDisable(true);
                            }
                            if (signatureInfoLabel != null) {
                                signatureInfoLabel.setText(
                                    "Too many failed attempts. Click 'Use Password Instead' below."
                                );
                            }
                        }
                        break;
                        
                    case NO_SIGNATURE:
                        showSignatureError(
                            "No signature on file. Please set up signature authentication in settings."
                        );
                        refreshSignatureCaptcha();
                        break;
                        
                    case RATE_LIMITED:
                        showSignatureError(
                            "Too many attempts. Please wait 5 minutes or use password login."
                        );
                        refreshSignatureCaptcha();
                        break;
                        
                    case LOCKED_OUT:
                        showSignatureError(
                            "Account temporarily locked. Please wait 5 minutes or use password login."
                        );
                        refreshSignatureCaptcha();
                        break;
                        
                    default:
                        showSignatureError("Authentication failed. Please try again.");
                        refreshSignatureCaptcha();
                        break;
                }
            }
            
        } catch (IllegalArgumentException e) {
            showSignatureError(e.getMessage());
            refreshSignatureCaptcha();
        } catch (Exception e) {
            showSignatureError("Authentication error: " + e.getMessage());
            refreshSignatureCaptcha();
            e.printStackTrace();
        }
    }
    
    /**
     * Clear the signature canvas.
     * 
     * Requirement 2.2: Provide "Clear" button for signature canvas
     */
    @FXML
    private void handleClearSignature() {
        if (signatureCanvas != null) {
            signatureCanvas.clear();
            clearSignatureError();
        }
    }
    
    /**
     * Switch from signature login to password login.
     * 
     * Requirement 2.10: Provide "Use Password Instead" fallback option
     */
    @FXML
    private void handleUsePasswordInstead() {
        // Hide signature card
        if (signatureCard != null) {
            signatureCard.setVisible(false);
            signatureCard.setManaged(false);
        }
        
        // Show login card
        loginCard.setVisible(true);
        loginCard.setManaged(true);
        
        // Pre-fill email if provided
        if (signatureEmailField != null && !signatureEmailField.getText().trim().isEmpty()) {
            emailField.setText(signatureEmailField.getText().trim());
        }
        
        // Reset signature attempts
        signatureAttempts = 0;
        if (submitSignatureButton != null) {
            submitSignatureButton.setDisable(false);
        }
        
        clearError();
    }
    
    /**
     * Show signature error message.
     */
    private void showSignatureError(String message) {
        if (signatureErrorLabel != null) {
            signatureErrorLabel.setText(message);
            signatureErrorLabel.setVisible(true);
        }
    }
    
    /**
     * Clear signature error message.
     */
    private void clearSignatureError() {
        if (signatureErrorLabel != null) {
            signatureErrorLabel.setVisible(false);
            signatureErrorLabel.setText("");
        }
    }
    
    /**
     * Clear the signup signature canvas.
     * 
     * Requirement 1.1: Provide "Clear" button for signup signature canvas
     */
    @FXML
    private void handleClearSignupSignature() {
        if (signupSignatureCanvas != null) {
            signupSignatureCanvas.clear();
            clearSignupMessages();
        }
        // Clear uploaded signature
        uploadedSignatureImage = null;
        if (signupSignatureFileLabel != null) {
            signupSignatureFileLabel.setVisible(false);
            signupSignatureFileLabel.setText("");
        }
    }
    
    /**
     * Handle signature file upload (PDF, PNG, JPG).
     * 
     * Allows users to upload their signature from a file instead of drawing it.
     */
    @FXML
    private void handleUploadSignupSignature() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Signature File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"),
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        Stage stage = (Stage) uploadSignupSignatureButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            try {
                String fileName = selectedFile.getName().toLowerCase();
                BufferedImage signatureImage = null;
                
                if (fileName.endsWith(".pdf")) {
                    // Handle PDF file
                    signatureImage = convertPdfToImage(selectedFile);
                } else if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    // Handle image file
                    signatureImage = ImageIO.read(selectedFile);
                } else {
                    showSignupError("Unsupported file format. Please use PNG, JPG, or PDF.");
                    return;
                }
                
                if (signatureImage != null) {
                    // Store the uploaded image
                    uploadedSignatureImage = signatureImage;
                    
                    // Display the image on the canvas
                    displayImageOnCanvas(signatureImage, signupSignatureCanvas);
                    
                    // Show success message
                    if (signupSignatureFileLabel != null) {
                        signupSignatureFileLabel.setText("Signature uploaded: " + selectedFile.getName());
                        signupSignatureFileLabel.setVisible(true);
                    }
                    
                    clearSignupMessages();
                } else {
                    showSignupError("Failed to load signature image from file.");
                }
                
            } catch (Exception e) {
                showSignupError("Error loading signature file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Convert PDF file to BufferedImage (first page only).
     * 
     * Note: This is a simplified implementation. For production, consider using
     * Apache PDFBox library for better PDF handling.
     */
    private BufferedImage convertPdfToImage(File pdfFile) throws IOException {
        // For now, show an error message as PDF conversion requires additional libraries
        // In production, you would use Apache PDFBox:
        // PDDocument document = PDDocument.load(pdfFile);
        // PDFRenderer renderer = new PDFRenderer(document);
        // BufferedImage image = renderer.renderImageWithDPI(0, 300);
        // document.close();
        
        showSignupError("PDF upload requires additional setup. Please use PNG or JPG format for now.");
        return null;
    }
    
    /**
     * Display an uploaded image on the signature canvas.
     */
    private void displayImageOnCanvas(BufferedImage image, SignatureCanvas canvas) {
        if (canvas == null || image == null) return;
        
        // Clear the canvas first
        canvas.clear();
        
        // Get canvas dimensions
        int canvasWidth = (int) canvas.getWidth();
        int canvasHeight = (int) canvas.getHeight();
        
        // Calculate scaling to fit image within canvas while maintaining aspect ratio
        double scaleX = (double) canvasWidth / image.getWidth();
        double scaleY = (double) canvasHeight / image.getHeight();
        double scale = Math.min(scaleX, scaleY);
        
        int scaledWidth = (int) (image.getWidth() * scale);
        int scaledHeight = (int) (image.getHeight() * scale);
        
        // Center the image
        int x = (canvasWidth - scaledWidth) / 2;
        int y = (canvasHeight - scaledHeight) / 2;
        
        // Draw the image on the canvas
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        javafx.scene.image.Image fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(image, null);
        gc.drawImage(fxImage, x, y, scaledWidth, scaledHeight);
    }
    
    /**
     * Generate a secure password using AI-inspired algorithm.
     * 
     * Creates passwords that are:
     * - Strong (mix of uppercase, lowercase, numbers, symbols)
     * - Memorable (uses pronounceable patterns)
     * - Secure (cryptographically random)
     */
    @FXML
    private void handleGeneratePassword() {
        try {
            String generatedPassword = generateSecurePassword();
            signupPasswordField.setText(generatedPassword);
            
            // Show the password temporarily
            if (showPasswordButton != null) {
                showPasswordButton.setVisible(true);
            }
            
            // Update strength indicator
            updatePasswordStrength(generatedPassword);
            
            // Show success message
            if (passwordStrengthLabel != null) {
                passwordStrengthLabel.setText("Strong password generated!");
                passwordStrengthLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                passwordStrengthLabel.setVisible(true);
            }
            
        } catch (Exception e) {
            showSignupError("Failed to generate password: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Generate a secure, memorable password.
     * 
     * Algorithm:
     * 1. Use word patterns for memorability
     * 2. Add numbers and symbols for strength
     * 3. Mix case for complexity
     * 4. Ensure minimum 12 characters
     */
    private String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        
        // Consonants and vowels for pronounceable patterns
        String consonants = "bcdfghjklmnpqrstvwxyz";
        String vowels = "aeiou";
        String numbers = "0123456789";
        String symbols = "!@#$%&*";
        
        StringBuilder password = new StringBuilder();
        
        // Generate 2-3 pronounceable syllables (consonant-vowel-consonant pattern)
        int syllables = 2 + random.nextInt(2); // 2 or 3 syllables
        for (int i = 0; i < syllables; i++) {
            // Consonant
            char c1 = consonants.charAt(random.nextInt(consonants.length()));
            // Vowel
            char v = vowels.charAt(random.nextInt(vowels.length()));
            // Consonant
            char c2 = consonants.charAt(random.nextInt(consonants.length()));
            
            // Randomly capitalize first letter of syllable
            if (random.nextBoolean()) {
                c1 = Character.toUpperCase(c1);
            }
            
            password.append(c1).append(v).append(c2);
        }
        
        // Add 2-3 numbers
        int numCount = 2 + random.nextInt(2);
        for (int i = 0; i < numCount; i++) {
            password.append(numbers.charAt(random.nextInt(numbers.length())));
        }
        
        // Add 1-2 symbols
        int symbolCount = 1 + random.nextInt(2);
        for (int i = 0; i < symbolCount; i++) {
            password.append(symbols.charAt(random.nextInt(symbols.length())));
        }
        
        // Shuffle the password for better security
        return shuffleString(password.toString(), random);
    }
    
    /**
     * Shuffle a string randomly.
     */
    private String shuffleString(String input, SecureRandom random) {
        char[] characters = input.toCharArray();
        for (int i = characters.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }
        return new String(characters);
    }
    
    /**
     * Update password strength indicator based on password content.
     * 
     * Evaluates:
     * - Length
     * - Character variety (uppercase, lowercase, numbers, symbols)
     * - Common patterns
     */
    private void updatePasswordStrength(String password) {
        if (passwordStrengthLabel == null) return;
        
        if (password == null || password.isEmpty()) {
            passwordStrengthLabel.setVisible(false);
            if (showPasswordButton != null) {
                showPasswordButton.setVisible(false);
            }
            return;
        }
        
        int strength = calculatePasswordStrength(password);
        
        passwordStrengthLabel.setVisible(true);
        if (showPasswordButton != null) {
            showPasswordButton.setVisible(true);
        }
        
        if (strength >= 80) {
            passwordStrengthLabel.setText("Very Strong");
            passwordStrengthLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else if (strength >= 60) {
            passwordStrengthLabel.setText("Strong");
            passwordStrengthLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        } else if (strength >= 40) {
            passwordStrengthLabel.setText("Medium");
            passwordStrengthLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
        } else if (strength >= 20) {
            passwordStrengthLabel.setText("Weak");
            passwordStrengthLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
        } else {
            passwordStrengthLabel.setText("Very Weak");
            passwordStrengthLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
    }
    
    /**
     * Calculate password strength score (0-100).
     */
    private int calculatePasswordStrength(String password) {
        int score = 0;
        
        // Length score (max 30 points)
        if (password.length() >= 12) score += 30;
        else if (password.length() >= 8) score += 20;
        else if (password.length() >= 6) score += 10;
        else score += 5;
        
        // Character variety (max 40 points)
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSymbol = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
        
        if (hasLower) score += 10;
        if (hasUpper) score += 10;
        if (hasDigit) score += 10;
        if (hasSymbol) score += 10;
        
        // Complexity bonus (max 30 points)
        if (hasLower && hasUpper && hasDigit && hasSymbol) {
            score += 20; // All character types
        }
        
        // Penalize common patterns
        if (password.matches(".*123.*") || password.matches(".*abc.*") || 
            password.matches(".*password.*") || password.matches(".*qwerty.*")) {
            score -= 20;
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * Toggle password visibility (show/hide).
     */
    @FXML
    private void handleTogglePasswordVisibility() {
        // Note: JavaFX PasswordField doesn't support toggling visibility directly
        // This would require replacing the PasswordField with a TextField
        // For now, we'll show a tooltip with the password
        if (showPasswordButton != null && signupPasswordField != null) {
            String currentText = showPasswordButton.getText();
            if (currentText.contains("Show")) {
                // Show password in a dialog or tooltip
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Generated Password");
                alert.setHeaderText("Your password:");
                alert.setContentText(signupPasswordField.getText());
                alert.showAndWait();
                showPasswordButton.setText("Hide");
            } else {
                showPasswordButton.setText("Show");
            }
        }
    }
    
    /**
     * Get random CAPTCHA type.
     */
    private CaptchaService.CaptchaType getRandomCaptchaType() {
        CaptchaService.CaptchaType[] types = CaptchaService.CaptchaType.values();
        return types[new SecureRandom().nextInt(types.length)];
    }
    
    /**
     * Refresh login CAPTCHA.
     */
    private void refreshLoginCaptcha() {
        if (captchaService != null && captchaQuestionLabel != null) {
            currentLoginCaptcha = captchaService.generateCaptcha(getRandomCaptchaType());
            captchaQuestionLabel.setText(currentLoginCaptcha.getQuestion());
            if (captchaAnswerField != null) {
                captchaAnswerField.clear();
            }
        }
    }
    
    /**
     * Refresh signup CAPTCHA.
     */
    private void refreshSignupCaptcha() {
        if (captchaService != null && signupCaptchaQuestionLabel != null) {
            currentSignupCaptcha = captchaService.generateCaptcha(getRandomCaptchaType());
            signupCaptchaQuestionLabel.setText(currentSignupCaptcha.getQuestion());
            if (signupCaptchaAnswerField != null) {
                signupCaptchaAnswerField.clear();
            }
        }
    }
    
    /**
     * Refresh signature login CAPTCHA.
     */
    private void refreshSignatureCaptcha() {
        if (captchaService != null && signatureCaptchaQuestionLabel != null) {
            currentSignatureCaptcha = captchaService.generateCaptcha(getRandomCaptchaType());
            signatureCaptchaQuestionLabel.setText(currentSignatureCaptcha.getQuestion());
            if (signatureCaptchaAnswerField != null) {
                signatureCaptchaAnswerField.clear();
            }
        }
    }
    
    /**
     * Handle refresh CAPTCHA button click (login).
     */
    @FXML
    private void handleRefreshCaptcha() {
        refreshLoginCaptcha();
        clearError();
    }
    
    /**
     * Handle refresh CAPTCHA button click (signup).
     */
    @FXML
    private void handleRefreshSignupCaptcha() {
        refreshSignupCaptcha();
        clearSignupMessages();
    }
    
    /**
     * Handle refresh CAPTCHA button click (signature login).
     */
    @FXML
    private void handleRefreshSignatureCaptcha() {
        refreshSignatureCaptcha();
        clearSignatureError();
    }
}
