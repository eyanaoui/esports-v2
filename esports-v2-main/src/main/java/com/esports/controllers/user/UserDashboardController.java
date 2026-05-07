package com.esports.controllers.user;

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

public class UserDashboardController {

    @FXML private AnchorPane contentArea;
    @FXML private Button darkModeBtn;

    @FXML
    public void initialize() {
        updateDarkModeButton();
        showGames();
    }

    @FXML
    private void showGames() {loadView("/views/user/game-browse.fxml");}


    @FXML
    private void showShop() {
        loadView("/views/user/shop-browse.fxml");
    }

    @FXML
    private void showCheckout() {
        loadView("/views/user/checkout.fxml");
    }

    @FXML
    private void showForum() {
        loadView("/views/user/ForumView.fxml");
    }

    @FXML
    private void showMessages() {
        loadView("/views/user/MessageView.fxml");
    }

    @FXML
    private void showSettings() {
        loadView("/views/user/user-settings.fxml");
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
            e.printStackTrace();
        }
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateDarkModeButton() {
        if (darkModeBtn != null) {
            darkModeBtn.setText(AppState.isDarkMode() ? "☀️  Light Mode" : "🌙  Dark Mode");
        }
    }
}