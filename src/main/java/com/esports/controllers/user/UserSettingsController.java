package com.esports.controllers.user;

import com.esports.AppState;
import com.esports.components.SignatureCanvas;
import com.esports.controllers.SignatureOAuthController;
import com.esports.dao.SignatureDAO;
import com.esports.models.SignatureData;
import com.esports.models.User;
import com.esports.services.SignatureAuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class UserSettingsController {

    @FXML private ImageView profilePictureView;
    @FXML private Label profilePictureLabel;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private Label passwordStatusLabel;
    @FXML private Label googleStatusLabel;
    @FXML private Label googleEmailLabel;
    @FXML private Label signatureStatusLabel;
    @FXML private Button googleActionButton;

    private SignatureOAuthController oauthController;
    private SignatureAuthService signatureAuthService;
    private SignatureDAO signatureDAO;

    @FXML
    public void initialize() {
        oauthController = new SignatureOAuthController();
        signatureAuthService = new SignatureAuthService();
        signatureDAO = new SignatureDAO();

        loadUserProfile();
    }

    private void loadUserProfile() {
        User currentUser = AppState.getCurrentUser();

        if (currentUser == null) {
            showError("No user logged in");
            return;
        }

        nameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
        emailLabel.setText(currentUser.getEmail());

        if (currentUser.getRoles() != null && !currentUser.getRoles().isEmpty()) {
            roleLabel.setText(currentUser.getRoles().get(0).toString());
        } else {
            roleLabel.setText("USER");
        }

        if (currentUser.getPassword() != null && !currentUser.getPassword().isEmpty()) {
            passwordStatusLabel.setText("Enabled");
            passwordStatusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #27ae60;");
        } else {
            passwordStatusLabel.setText("Not set");
            passwordStatusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #e74c3c;");
        }

        if (currentUser.getGoogleId() != null && !currentUser.getGoogleId().isEmpty()) {
            googleStatusLabel.setText("Connected");
            googleStatusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #27ae60;");

            googleEmailLabel.setText("Google account: " + currentUser.getEmail());
            googleEmailLabel.setVisible(true);

            googleActionButton.setText("Disconnect Google");
            googleActionButton.setStyle(
                    "-fx-background-color: #e74c3c;" +
                            "-fx-text-fill: white;" +
                            "-fx-padding: 8 15;" +
                            "-fx-background-radius: 8;" +
                            "-fx-cursor: hand;"
            );

            if (currentUser.getProfilePictureUrl() != null && !currentUser.getProfilePictureUrl().isEmpty()) {
                try {
                    Image profileImage = new Image(currentUser.getProfilePictureUrl(), true);
                    profilePictureView.setImage(profileImage);
                    profilePictureLabel.setText("From Google");
                } catch (Exception e) {
                    System.err.println("Failed to load profile picture: " + e.getMessage());
                }
            }
        } else {
            googleStatusLabel.setText("Not connected");
            googleStatusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");

            googleEmailLabel.setVisible(false);

            googleActionButton.setText("Connect Google");
            googleActionButton.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-border-color: #dadce0;" +
                            "-fx-border-width: 1;" +
                            "-fx-text-fill: #3c4043;" +
                            "-fx-padding: 8 15;" +
                            "-fx-background-radius: 8;" +
                            "-fx-cursor: hand;"
            );
        }

        SignatureData signatureData = signatureDAO.getSignature(currentUser.getId());

        if (signatureData != null) {
            signatureStatusLabel.setText("Enabled");
            signatureStatusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #27ae60;");
        } else {
            signatureStatusLabel.setText("Not set up");
            signatureStatusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");
        }
    }

    @FXML
    private void handleGoogleAction() {
        User currentUser = AppState.getCurrentUser();

        if (currentUser == null) {
            showError("No user logged in");
            return;
        }

        if (currentUser.getGoogleId() != null && !currentUser.getGoogleId().isEmpty()) {
            handleDisconnectGoogle();
        } else {
            handleConnectGoogle();
        }
    }

    private void handleConnectGoogle() {
        try {
            oauthController.handleGoogleSignIn();

            showInfo(
                    "Please complete the Google sign-in process in your browser.\n\n" +
                            "After signing in, your Google account will be linked to this account."
            );
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to initiate Google sign-in: " + e.getMessage());
        }
    }

    private void handleDisconnectGoogle() {
        User currentUser = AppState.getCurrentUser();

        if (currentUser == null) {
            showError("No user logged in");
            return;
        }

        if (currentUser.getPassword() == null || currentUser.getPassword().isEmpty()) {
            showError(
                    "Cannot disconnect Google account.\n\n" +
                            "You must set a password first to maintain access to your account.\n\n" +
                            "Please set a password before disconnecting Google."
            );
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Disconnect Google Account");
        confirmAlert.setHeaderText("Are you sure you want to disconnect your Google account?");
        confirmAlert.setContentText(
                "You will no longer be able to sign in with Google.\n" +
                        "You can still sign in with your password."
        );

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                oauthController.handleDisconnectGoogle();

                Platform.runLater(() -> {
                    loadUserProfile();
                    showSuccess("Google account disconnected successfully");
                });
            } catch (Exception e) {
                e.printStackTrace();
                showError("Failed to disconnect Google account: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleChangePassword() {
        showInfo("Change password functionality will be implemented in a future update.");
    }

    @FXML
    private void handleSetupSignature() {
        User currentUser = AppState.getCurrentUser();

        if (currentUser == null) {
            showError("No user logged in");
            return;
        }

        SignatureData existingSignature = signatureDAO.getSignature(currentUser.getId());
        boolean isUpdate = existingSignature != null;

        if (isUpdate) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Update Signature");
            confirmAlert.setHeaderText("You already have a signature set up");
            confirmAlert.setContentText("Do you want to replace your existing signature?");

            Optional<ButtonType> result = confirmAlert.showAndWait();

            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(isUpdate ? "Update Signature" : "Set Up Signature");
        dialog.setResizable(false);

        SignatureCanvas signatureCanvas = new SignatureCanvas(400, 200);
        signatureCanvas.setStyle(
                "-fx-border-color: #e0e0e0;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 10;"
        );

        Label titleLabel = new Label(isUpdate ? "Update Your Signature" : "Set Up Your Signature");
        titleLabel.setStyle(
                "-fx-font-size: 18;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #667eea;"
        );

        Label instructionLabel = new Label("Draw your signature in the box below (minimum 50 pixels)");
        instructionLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(400);

        Button clearButton = new Button("Clear");
        clearButton.setStyle(
                "-fx-background-color: #f0f0f0;" +
                        "-fx-text-fill: #333;" +
                        "-fx-font-size: 13;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );

        clearButton.setOnAction(e -> {
            signatureCanvas.clear();
            errorLabel.setVisible(false);
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
                "-fx-background-color: #f0f0f0;" +
                        "-fx-text-fill: #333;" +
                        "-fx-font-size: 13;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );

        cancelButton.setOnAction(e -> dialog.close());

        Button saveButton = new Button("Save Signature");
        saveButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #667eea, #764ba2);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );

        saveButton.setOnAction(e -> {
            try {
                if (!signatureCanvas.hasContent()) {
                    errorLabel.setText("Please draw your signature before saving");
                    errorLabel.setVisible(true);
                    return;
                }

                BufferedImage signatureImage = signatureCanvas.getSignatureImage();

                boolean success = signatureAuthService.saveSignature(currentUser.getId(), signatureImage);

                if (success) {
                    dialog.close();
                    loadUserProfile();

                    showSuccess(
                            isUpdate
                                    ? "Signature updated successfully! You can now use it to login."
                                    : "Signature saved successfully! You can now use it to login."
                    );
                } else {
                    errorLabel.setText("Failed to save signature. Please try again.");
                    errorLabel.setVisible(true);
                }
            } catch (IllegalArgumentException ex) {
                errorLabel.setText(ex.getMessage());
                errorLabel.setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                errorLabel.setText("Error saving signature: " + ex.getMessage());
                errorLabel.setVisible(true);
            }
        });

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(clearButton, cancelButton, saveButton);
        buttonBox.setStyle("-fx-alignment: center;");

        VBox dialogLayout = new VBox(15);
        dialogLayout.setPadding(new Insets(30));
        dialogLayout.setStyle("-fx-background-color: white;");
        dialogLayout.getChildren().addAll(
                titleLabel,
                instructionLabel,
                signatureCanvas,
                errorLabel,
                buttonBox
        );

        Scene dialogScene = new Scene(dialogLayout);
        dialog.setScene(dialogScene);
        dialog.showAndWait();
    }

    @FXML
    private void handleDeleteAccount() {
        Alert confirmAlert = new Alert(Alert.AlertType.WARNING);
        confirmAlert.setTitle("Delete Account");
        confirmAlert.setHeaderText("WARNING: This action cannot be undone!");
        confirmAlert.setContentText(
                "Are you sure you want to permanently delete your account?\n\n" +
                        "All your data will be lost."
        );

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            showInfo("Account deletion functionality will be implemented in a future update.");
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/game-browse.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) nameLabel.getScene().getWindow();
            stage.setScene(scene);

            addStylesheet(scene);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to navigate back");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            AppState.clearSession();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) nameLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Esports Login");

            addStylesheet(scene);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to logout");
        }
    }

    private void addStylesheet(Scene scene) {
        URL cssUrl = getClass().getResource("/styles.css");

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}