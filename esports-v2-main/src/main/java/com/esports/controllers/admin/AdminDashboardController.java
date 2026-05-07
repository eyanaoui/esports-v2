package com.esports.controllers.admin;

import com.esports.AppState;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class AdminDashboardController {

    @FXML
    private AnchorPane contentArea;

    @FXML
    private Button darkModeBtn;

    private String currentView = "/views/admin/game-view.fxml";

    @FXML
    public void initialize() {
        updateDarkModeButton();
        showGames();
    }

    @FXML
    public void showGames() {
        currentView = "/views/admin/game-view.fxml";
        loadView(currentView);
    }

    @FXML
    public void showGuides() {
        currentView = "/views/admin/guide-view.fxml";
        loadView(currentView);
    }

    @FXML
    public void showSteps() {
        currentView = "/views/admin/guide-step-view.fxml";
        loadView(currentView);
    }

    @FXML
    public void showRatings() {
        currentView = "/views/admin/guide-rating-view.fxml";
        loadView(currentView);
    }

    @FXML
    public void showStats() {
        currentView = "/views/admin/stats-view.fxml";
        loadView(currentView);
    }

    @FXML
    public void showProducts() {
        currentView = "/views/admin/products.fxml";
        loadView(currentView);
    }

    @FXML
    public void showOrders() {
        currentView = "/views/admin/orders.fxml";
        loadView(currentView);
    }

    @FXML
    public void showTournaments() {
        currentView = "/views/admin/tournament-view.fxml";
        loadView(currentView);
    }

    @FXML
    public void showUserView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/user-dashboard.fxml"));
            Scene scene = new Scene(loader.load());

            addStylesheet(scene);

            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Esports User Dashboard");
            stage.setMaximized(true);

        } catch (IOException e) {
            System.out.println("❌ Error loading user dashboard");
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleDarkMode() {
        AppState.setDarkMode(!AppState.isDarkMode());

        Scene scene = contentArea.getScene();
        if (scene != null) {
            scene.getStylesheets().clear();
            addStylesheet(scene);
        }

        updateDarkModeButton();
        loadView(currentView);
    }

    @FXML
    private void handleLogout() {
        try {
            AppState.clearSession();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Scene scene = new Scene(loader.load());

            URL cssUrl = getClass().getResource("/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Esports Login");
            stage.setMaximized(true);

        } catch (IOException e) {
            System.out.println("❌ Error during logout");
            e.printStackTrace();
        }
    }

    private void loadView(String path) {
        try {
            URL resource = getClass().getResource(path);

            if (resource == null) {
                System.out.println("❌ FXML NOT FOUND: " + path);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();

            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);

            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            System.out.println("❌ Error loading view: " + path);
            e.printStackTrace();
        }
    }

    private void addStylesheet(Scene scene) {
        String cssPath = AppState.isDarkMode() ? "/styles-dark.css" : "/styles.css";
        URL cssUrl = getClass().getResource(cssPath);

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.out.println("❌ CSS file not found: " + cssPath);
        }
    }

    private void updateDarkModeButton() {
        if (darkModeBtn != null) {
            darkModeBtn.setText(AppState.isDarkMode() ? "☀️  Light Mode" : "🌙  Dark Mode");
        }
    }
}