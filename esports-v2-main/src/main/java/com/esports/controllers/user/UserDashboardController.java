package com.esports.controllers.user;

import com.esports.AppState;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class UserDashboardController {

    @FXML
    private AnchorPane contentArea;

    private String currentView = "/views/user/game-browse.fxml";

    @FXML
    public void initialize() {
        showGames();
    }

    @FXML
    private void showGames() {
        currentView = "/views/user/game-browse.fxml";
        loadView(currentView);
    }

    @FXML
    private void showShop() {
        currentView = "/views/user/shop-browse.fxml";
        loadView(currentView);
    }

    @FXML
    private void showCheckout() {
        currentView = "/views/user/checkout.fxml";
        loadView(currentView);
    }

    @FXML
    private void showForum() {
        currentView = "/views/user/ForumView.fxml";
        loadView(currentView);
    }

    @FXML
    private void showMessages() {
        currentView = "/views/user/MessageView.fxml";
        loadView(currentView);
    }

    @FXML
    private void showSettings() {
        currentView = "/views/user/user-settings.fxml";
        loadView(currentView);
    }

    @FXML
    private void handleLogout() {
        try {
            AppState.clearSession();

            URL loginUrl = getClass().getResource("/views/login.fxml");
            if (loginUrl == null) {
                System.out.println("FXML not found: /views/login.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(loginUrl);
            Scene scene = new Scene(loader.load());

            URL cssUrl = getClass().getResource("/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Esports Platform");
            stage.setMaximized(true);

        } catch (IOException e) {
            System.out.println("Error during logout: " + e.getMessage());
        }
    }

    private void loadView(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);

            if (resource == null) {
                System.out.println("FXML not found: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);

        } catch (IOException e) {
            System.out.println("Error loading view " + fxmlPath + ": " + e.getMessage());
        }
    }
}