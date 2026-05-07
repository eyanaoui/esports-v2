package com.esports.controllers.admin;

import com.esports.dao.UserDAO;
import com.esports.models.User;
import com.esports.models.UserRole;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.List;

public class UserFormController {

    @FXML private Label formTitle;
    @FXML private TextField firstNameField, lastNameField, emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<UserRole> roleCombo;
    @FXML private Button banBtn;

    private UserDAO userDAO = new UserDAO();
    private User currentUser;
    private Runnable onSuccess;

    @FXML
    public void initialize() {
        roleCombo.getItems().setAll(UserRole.values());
        roleCombo.getSelectionModel().selectFirst();
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (user == null) {
            formTitle.setText("Add User");
            banBtn.setVisible(false);
            roleCombo.getSelectionModel().select(UserRole.USER);
        } else {
            formTitle.setText("Edit User");
            banBtn.setVisible(true);
            
            // Update button text based on user's blocked status
            if (user.getIsBlocked() != null && user.getIsBlocked()) {
                banBtn.setText("Unban User");
                banBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
            } else {
                banBtn.setText("Ban User");
                banBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
            }
            
            firstNameField.setText(user.getFirstName());
            lastNameField.setText(user.getLastName());
            emailField.setText(user.getEmail());
            passwordField.setText(user.getPassword());
            // Set first role from list
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                roleCombo.getSelectionModel().select(user.getRoles().get(0));
            }
        }
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    private void handleSave() {
        if (!validateInputs()) return;
        
        try {
            boolean success;
            if (currentUser == null) {
                List<UserRole> roles = new ArrayList<>();
                roles.add(roleCombo.getValue());
                User u = new User(
                        firstNameField.getText().trim(),
                        lastNameField.getText().trim(),
                        emailField.getText().trim(),
                        hashPassword(passwordField.getText().trim()),
                        roles
                );
                success = userDAO.add(u);
            } else {
                currentUser.setFirstName(firstNameField.getText().trim());
                currentUser.setLastName(lastNameField.getText().trim());
                currentUser.setEmail(emailField.getText().trim());
                currentUser.setPassword(hashPassword(passwordField.getText().trim()));
                List<UserRole> roles = new ArrayList<>();
                roles.add(roleCombo.getValue());
                currentUser.setRoles(roles);
                success = userDAO.update(currentUser);
            }
            
            if (success) {
                showSuccessAlert(currentUser == null ? "User created successfully!" : "User updated successfully!");
                if (onSuccess != null) onSuccess.run();
                closeWindow();
            } else {
                showErrorAlert("Operation failed. No rows were affected.");
            }
        } catch (RuntimeException e) {
            showErrorAlert("Database operation failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleBanToggle() {
        if (currentUser == null) return;
        
        boolean isCurrentlyBlocked = currentUser.getIsBlocked() != null && currentUser.getIsBlocked();
        String action = isCurrentlyBlocked ? "unban" : "ban";
        String actionCapitalized = isCurrentlyBlocked ? "Unban" : "Ban";
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(actionCapitalized + " User");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to " + action + " this user?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Toggle the blocked status
                    currentUser.setIsBlocked(!isCurrentlyBlocked);
                    boolean success = userDAO.update(currentUser);
                    
                    if (success) {
                        showSuccessAlert("User " + action + "ned successfully!");
                        if (onSuccess != null) onSuccess.run();
                        closeWindow();
                    } else {
                        showErrorAlert(actionCapitalized + " operation failed. No rows were affected.");
                    }
                } catch (RuntimeException e) {
                    showErrorAlert("Database operation failed: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) firstNameField.getScene().getWindow()).close();
    }

    private boolean validateInputs() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        
        // First name validation
        if (firstName.isEmpty()) { 
            showAlert("First name is required!"); 
            return false; 
        }
        if (firstName.length() < 2) { 
            showAlert("First name must be at least 2 characters!"); 
            return false; 
        }
        if (firstName.length() > 50) { 
            showAlert("First name must not exceed 50 characters!"); 
            return false; 
        }
        if (!firstName.matches("^[a-zA-Z\\s'-]+$")) { 
            showAlert("First name can only contain letters, spaces, hyphens and apostrophes!"); 
            return false; 
        }
        
        // Last name validation
        if (lastName.isEmpty()) { 
            showAlert("Last name is required!"); 
            return false; 
        }
        if (lastName.length() < 2) { 
            showAlert("Last name must be at least 2 characters!"); 
            return false; 
        }
        if (lastName.length() > 50) { 
            showAlert("Last name must not exceed 50 characters!"); 
            return false; 
        }
        if (!lastName.matches("^[a-zA-Z\\s'-]+$")) { 
            showAlert("Last name can only contain letters, spaces, hyphens and apostrophes!"); 
            return false; 
        }
        
        // Email validation
        if (email.isEmpty()) { 
            showAlert("Email is required!"); 
            return false; 
        }
        if (!isValidEmail(email)) { 
            showAlert("Invalid email format (example: user@example.com)!"); 
            return false; 
        }
        if (email.length() > 100) { 
            showAlert("Email must not exceed 100 characters!"); 
            return false; 
        }
        
        // Password validation
        if (password.isEmpty()) { 
            showAlert("Password is required!"); 
            return false; 
        }
        if (password.length() < 4) { 
            showAlert("Password must be at least 4 characters!"); 
            return false; 
        }
        if (password.length() > 100) { 
            showAlert("Password must not exceed 100 characters!"); 
            return false; 
        }
        if (password.contains(" ")) { 
            showAlert("Password cannot contain spaces!"); 
            return false; 
        }
        
        // Role validation
        if (roleCombo.getValue() == null) { 
            showAlert("Please select a role!"); 
            return false; 
        }
        
        return true;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showErrorAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showSuccessAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }
}
