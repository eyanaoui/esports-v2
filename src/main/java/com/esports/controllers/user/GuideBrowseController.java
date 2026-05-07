package com.esports.controllers.user;

import com.esports.AppState;
import com.esports.dao.GuideDAO;
import com.esports.dao.GuideStepDAO;
import com.esports.models.Game;
import com.esports.models.Guide;
import com.esports.models.GuideStep;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class GuideBrowseController {

    @FXML private Label gameTitleLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> difficultyFilter;
    @FXML private FlowPane guidesContainer;
    @FXML private FlowPane similarGamesContainer;

    private GuideDAO guideDAO   = new GuideDAO();
    private Game currentGame;

    @FXML
    public void initialize() {
        difficultyFilter.setItems(FXCollections.observableArrayList("All", "Easy", "Medium", "Hard"));
        difficultyFilter.setValue("All");
    }

    public void setGame(Game game) {
        this.currentGame = game;
        gameTitleLabel.setText("Guides — " + game.getName());
        loadGuides();
        loadSimilarGames();
    }

    private void loadGuides() {
        applyFilters(guideDAO.getByGameId(currentGame.getId()));
    }

    @FXML private void handleSearch() { applyFilters(guideDAO.getByGameId(currentGame.getId())); }
    @FXML private void handleFilter() { applyFilters(guideDAO.getByGameId(currentGame.getId())); }

    private void applyFilters(List<Guide> guides) {
        String query      = searchField.getText().toLowerCase().trim();
        String difficulty = difficultyFilter.getValue();
        List<Guide> filtered = guides.stream()
                .filter(g -> query.isEmpty() || g.getTitle().toLowerCase().contains(query))
                .filter(g -> difficulty == null || difficulty.equals("All")
                        || g.getDifficulty().equals(difficulty))
                .collect(Collectors.toList());
        guidesContainer.getChildren().clear();
        for (Guide guide : filtered) {
            guidesContainer.getChildren().add(createGuideCard(guide));
        }
    }

    private VBox createGuideCard(Guide guide) {
        VBox card = new VBox(8);
        boolean dark = !AppState.isDarkMode();
        card.setStyle(
                "-fx-background-color: " + (dark ? "#1a1a2e" : "white") + ";" +
                        "-fx-border-color: "     + (dark ? "#2a2a4a" : "#e0e0e0") + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 15;" +
                        "-fx-cursor: hand;"
        );
        card.setPrefWidth(220);

        // YouTube thumbnail
        try {
            GuideStepDAO stepDAO = new GuideStepDAO();
            List<GuideStep> steps = stepDAO.getByGuideId(guide.getId());
            String thumbnailUrl = steps.stream()
                    .filter(s -> s.getVideoUrl() != null && s.getVideoUrl().contains("youtube.com/watch?v="))
                    .findFirst()
                    .map(s -> {
                        String videoId = s.getVideoUrl().split("v=")[1];
                        if (videoId.contains("&")) videoId = videoId.split("&")[0];
                        return "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                    })
                    .orElse(null);

            if (thumbnailUrl != null) {
                Image img = new Image(thumbnailUrl, true);
                ImageView imageView = new ImageView(img);
                imageView.setFitWidth(190);
                imageView.setFitHeight(110);
                imageView.setPreserveRatio(true);
                card.getChildren().add(imageView);
            }
        } catch (Exception e) {
        }

        String textColor = dark ? "#e0e0e0" : "#333";
        String muteColor = dark ? "#a0a0b0" : "#666";

        Label title = new Label(guide.getTitle());
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        title.setWrapText(true);

        Label desc = new Label(guide.getDescription() != null ?
                guide.getDescription() : "No description");
        desc.setStyle("-fx-font-size: 12; -fx-text-fill: " + muteColor + ";");
        desc.setWrapText(true);
        desc.setMaxHeight(60);

        String diffColor = switch (guide.getDifficulty()) {
            case "Easy"   -> "#2ecc71";
            case "Medium" -> "#f39c12";
            case "Hard"   -> "#e74c3c";
            default       -> "#999";
        };

        Label difficulty = new Label(guide.getDifficulty());
        difficulty.setStyle(
                "-fx-background-color: " + diffColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 2 8 2 8;" +
                        "-fx-background-radius: 99;" +
                        "-fx-font-size: 11;"
        );

        card.getChildren().addAll(title, desc, difficulty);
        card.setOnMouseClicked(e -> openGuideDetail(guide));
        return card;
    }

    private void loadSimilarGames() {
        similarGamesContainer.getChildren().clear();
        try {
            String json = "{\"game_name\": \"" + currentGame.getName().replace("\"", "'") + "\"}";
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    json, okhttp3.MediaType.parse("application/json"));
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("http://127.0.0.1:5000/predict/recommend")
                    .post(body).build();
            okhttp3.Response response = client.newCall(request).execute();
            org.json.JSONObject result = new org.json.JSONObject(response.body().string());
            org.json.JSONArray recommendations = result.getJSONArray("recommendations");

            if (recommendations.length() == 0) {
                similarGamesContainer.getChildren().add(new Label("No similar games found."));
                return;
            }

            for (int i = 0; i < recommendations.length(); i++) {
                org.json.JSONObject rec = recommendations.getJSONObject(i);
                String name  = rec.getString("name");
                double score = rec.getDouble("score");
                Label chip   = new Label(name + " (" + (int)(score * 100) + "% match)");
                chip.setStyle(
                        "-fx-background-color: #3498db;" +
                                "-fx-text-fill: white;" +
                                "-fx-padding: 5 12 5 12;" +
                                "-fx-background-radius: 99;" +
                                "-fx-font-size: 12;" +
                                "-fx-cursor: hand;"
                );
                similarGamesContainer.getChildren().add(chip);
            }
        } catch (Exception e) {
            similarGamesContainer.getChildren().add(new Label("Flask API not running."));
        }
    }

    private void openGuideDetail(Guide guide) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/guide-detail.fxml"));
            Stage stage = new Stage();
            stage.setTitle(guide.getTitle());
            Scene scene = new Scene(loader.load(), 800, 600);

            String cssPath = AppState.isDarkMode() ? "/styles-dark.css" : "/styles.css";
            java.net.URL cssUrl = getClass().getResource(cssPath);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            GuideDetailController controller = loader.getController();
            controller.setGuide(guide);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}