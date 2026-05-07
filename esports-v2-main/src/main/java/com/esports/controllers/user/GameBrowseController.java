package com.esports.controllers.user;

import com.esports.AppState;
import com.esports.dao.GameDAO;
import com.esports.models.Game;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class GameBrowseController {

    @FXML private TextField searchField;
    @FXML private FlowPane gamesContainer;

    private final GameDAO gameDAO = new GameDAO();

    @FXML
    public void initialize() {
        loadGames(gameDAO.getAll());
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();

        if (query.isEmpty()) {
            loadGames(gameDAO.getAll());
            return;
        }

        List<Game> filtered = gameDAO.getAll().stream()
                .filter(g -> g.getName().toLowerCase().contains(query)
                        || (g.getDescription() != null
                        && g.getDescription().toLowerCase().contains(query)))
                .collect(Collectors.toList());

        loadGames(filtered);
    }

    private void loadGames(List<Game> games) {
        gamesContainer.getChildren().clear();

        for (Game game : games) {
            gamesContainer.getChildren().add(createGameCard(game));
        }
    }

    private VBox createGameCard(Game game) {
        VBox card = new VBox(8);

        boolean dark = AppState.isDarkMode();

        card.setStyle(
                "-fx-background-color: " + (dark ? "#1a1a2e" : "white") + ";" +
                        "-fx-border-color: " + (dark ? "#2a2a4a" : "#e0e0e0") + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 15;" +
                        "-fx-cursor: hand;"
        );

        card.setPrefWidth(200);

        if (game.getCoverImage() != null && !game.getCoverImage().isEmpty()) {
            try {
                String imagePath = "target/classes/images/" + game.getCoverImage();

                Image img = new Image(new FileInputStream(imagePath));

                ImageView imageView = new ImageView(img);
                imageView.setFitWidth(170);
                imageView.setFitHeight(100);
                imageView.setPreserveRatio(true);

                card.getChildren().add(imageView);
            } catch (Exception e) {
                System.err.println("Failed to load game image: " + e.getMessage());
            }
        }

        String textColor = dark ? "#e0e0e0" : "#333";
        String muteColor = dark ? "#a0a0b0" : "#666";

        Label name = new Label(game.getName());
        name.setStyle(
                "-fx-font-size: 14;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + textColor + ";"
        );
        name.setWrapText(true);

        Label desc = new Label(
                game.getDescription() != null && !game.getDescription().isEmpty()
                        ? game.getDescription()
                        : "No description"
        );
        desc.setStyle(
                "-fx-font-size: 12;" +
                        "-fx-text-fill: " + muteColor + ";"
        );
        desc.setWrapText(true);
        desc.setMaxHeight(60);

        Label ranking = new Label("Click to view guides");
        ranking.setStyle(
                "-fx-font-size: 11;" +
                        "-fx-text-fill: " + muteColor + ";"
        );

        card.getChildren().addAll(name, desc, ranking);

        card.setOnMouseClicked(e -> openGuidesBrowse(game));

        return card;
    }

    private void openGuidesBrowse(Game game) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/guide-browse.fxml"));

            Scene scene = new Scene(loader.load(), 800, 600);

            String cssPath = AppState.isDarkMode() ? "/styles-dark.css" : "/styles.css";
            java.net.URL cssUrl = getClass().getResource(cssPath);

            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            GuideBrowseController controller = loader.getController();
            controller.setGame(game);

            Stage stage = new Stage();
            stage.setTitle("Guides — " + game.getName());
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}