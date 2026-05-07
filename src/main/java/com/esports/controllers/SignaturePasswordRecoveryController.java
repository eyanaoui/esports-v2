package com.esports.controllers;

import com.esports.services.SignaturePasswordRecoveryService;
import com.esports.services.CaptchaService;
import com.esports.components.SignatureCanvas;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;

/**
 * Controller for signature-based password recovery.
 * 
 * Flow:
 * 1. User enters email
 * 2. User draws signature
 * 3. User enters new password
 * 4. User answers CAPTCHA
 * 5. System verifies signature and resets password if match
 */
public class SignaturePasswordRecoveryController {

    @FXML private TextField emailField;
    @FXML private HBox signatureCanvasContainer;
    @FXML private Button clearSignatureButton;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button resetPasswordButton;
    @FXML private Button backToLoginButton;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Label captchaQuestionLabel;
    @FXML private TextField captchaAnswerField;
    @FXML private Button refreshCaptchaButton;
    
    private SignatureCanvas signatureCanvas;
    private SignaturePasswordRecoveryService recoveryService;
    private CaptchaService captchaService;
    private CaptchaService.CaptchaChallenge currentCaptcha;
    
    @FXML
    public void initialize() {
        recoveryService = new SignaturePasswordRecoveryService();
        captchaService = new CaptchaService();
        
        // Generate initial CAPTCHA
        refreshCaptcha();
        
        // Initialize signature canvas
        if (signatureCanvasContainer != null) {
            signatureCanvas = new SignatureCanvas(400, 200);
            signatureCanvas.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-color: white;");
            signatureCanvasContainer.getChildren().add(signatureCanvas);
        }
        
        // Clear messages when user starts typing
        if (emailField != null) {
            emailField.textProperty().addListener((obs, oldVal, newVal) -> clearMessages());
        }
        if (newPasswordField != null) {
            newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> clearMessages());
        }
        if (confirmPasswordField != null) {
            confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> clearMessages());
        }
    }
    
    /**
     * Handle password reset with signature verification.
     */
    @FXML
    private void handleResetPassword() {
        String email = emailField.getText().trim();
        String newPassword = newPasswordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();
        String captchaAnswer = captchaAnswerField != null ? captchaAnswerField.getText().trim() : "";
        
        // Validate inputs
        if (email.isEmpty()) {
            showError("Please enter your email address");
            return;
        }
        
        if (!isValidEmail(email)) {
            showError("Invalid email format");
            return;
        }
        
        if (signatureCanvas == null || !signatureCanvas.hasContent()) {
            showError("Please draw your signature");
            return;
        }
        
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please enter and confirm your new password");
            return;
        }
        
        if (newPassword.length() < 4) {
            showError("Password must be at least 4 characters");
            return;
        }
        
        if (newPassword.length() > 100) {
            showError("Password must not exceed 100 characters");
            return;
        }
        
        if (newPassword.contains(" ")) {
            showError("Password cannot contain spaces");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        
        // Validate CAPTCHA
        if (currentCaptcha == null || captchaAnswer.isEmpty()) {
            showError("Please answer the CAPTCHA");
            return;
        }
        
        if (!captchaService.validateCaptcha(currentCaptcha.getChallengeId(), captchaAnswer)) {
            showError("Incorrect CAPTCHA answer. Please try again.");
            refreshCaptcha();
            return;
        }
        
        // Disable button during processing
        resetPasswordButton.setDisable(true);
        resetPasswordButton.setText("Verifying signature...");
        
        try {
            // Get signature image
            BufferedImage signatureImage = signatureCanvas.getSignatureImage();
            
            // Attempt password recovery
            SignaturePasswordRecoveryService.RecoveryResult result = 
                recoveryService.recoverPassword(email, signatureImage, newPassword);
            
            if (result.isSuccess()) {
                showSuccess(result.getMessage());
                
                // Clear form
                emailField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
                if (signatureCanvas != null) {
                    signatureCanvas.clear();
                }
                refreshCaptcha();
                
                // Navigate back to login after 3 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        javafx.application.Platform.runLater(() -> {
                            navigateToLogin();
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                
            } else {
                showError(result.getMessage());
                refreshCaptcha();
            }
            
        } catch (Exception e) {
            showError("An error occurred: " + e.getMessage());
            refreshCaptcha();
            e.printStackTrace();
        } finally {
            resetPasswordButton.setDisable(false);
            resetPasswordButton.setText("Reset Password");
        }
    }
    
    /**
     * Clear signature canvas.
     */
    @FXML
    private void handleClearSignature() {
        if (signatureCanvas != null) {
            signatureCanvas.clear();
        }
        clearMessages();
    }
    
    /**
     * Navigate back to login screen.
     */
    @FXML
    private void handleBackToLogin() {
        navigateToLogin();
    }
    
    /**
     * Navigate to login screen.
     */
    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Esports Platform - Login");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to load login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Validate email format.
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        // Basic email validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
    
    /**
     * Show error message.
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }
    
    /**
     * Show success message.
     */
    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }
    
    /**
     * Clear all messages.
     */
    private void clearMessages() {
        errorLabel.setVisible(false);
        errorLabel.setText("");
        successLabel.setVisible(false);
        successLabel.setText("");
    }
    
    /**
     * Get random CAPTCHA type.
     */
    private CaptchaService.CaptchaType getRandomCaptchaType() {
        CaptchaService.CaptchaType[] types = CaptchaService.CaptchaType.values();
        return types[new java.security.SecureRandom().nextInt(types.length)];
    }
    
    /**
     * Refresh CAPTCHA.
     */
    private void refreshCaptcha() {
        if (captchaService != null && captchaQuestionLabel != null) {
            currentCaptcha = captchaService.generateCaptcha(getRandomCaptchaType());
            captchaQuestionLabel.setText(currentCaptcha.getQuestion());
            if (captchaAnswerField != null) {
                captchaAnswerField.clear();
            }
        }
    }
    
    /**
     * Handle refresh CAPTCHA button click.
     */
    @FXML
    private void handleRefreshCaptcha() {
        refreshCaptcha();
        clearMessages();
    }
}
