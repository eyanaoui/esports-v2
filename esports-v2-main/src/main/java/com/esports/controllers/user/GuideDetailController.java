package com.esports.controllers.user;

import com.esports.AppState;
import com.esports.dao.GuideRatingDAO;
import com.esports.dao.GuideStepDAO;
import com.esports.models.Guide;
import com.esports.models.GuideRating;
import com.esports.models.GuideStep;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.awt.Desktop;
import java.io.FileInputStream;
import java.net.URI;
import java.util.List;

public class GuideDetailController {

    @FXML private Label guideTitleLabel;
    @FXML private Label guideDescLabel;
    @FXML private Label difficultyLabel;
    @FXML private VBox stepsContainer;
    @FXML private VBox ratingsContainer;
    @FXML private ComboBox<Integer> ratingBox;
    @FXML private TextField commentField;

    private final GuideStepDAO stepDAO = new GuideStepDAO();
    private final GuideRatingDAO ratingDAO = new GuideRatingDAO();

    private Guide currentGuide;

    @FXML
    public void initialize() {
        ratingBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
    }

    public void setGuide(Guide guide) {
        this.currentGuide = guide;

        guideTitleLabel.setText(guide.getTitle());
        guideDescLabel.setText(guide.getDescription() != null ? guide.getDescription() : "");

        String diffColor = switch (guide.getDifficulty()) {
            case "Easy" -> "#2ecc71";
            case "Medium" -> "#f39c12";
            case "Hard" -> "#e74c3c";
            default -> "#999";
        };

        difficultyLabel.setText(guide.getDifficulty());
        difficultyLabel.setStyle(
                "-fx-background-color: " + diffColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 2 8 2 8;" +
                        "-fx-background-radius: 99;" +
                        "-fx-font-size: 12;"
        );

        loadSteps();
        loadRatings();
    }

    private void loadSteps() {
        stepsContainer.getChildren().clear();

        List<GuideStep> steps = stepDAO.getByGuideId(currentGuide.getId());
        boolean dark = AppState.isDarkMode();

        for (GuideStep step : steps) {
            VBox stepCard = new VBox(8);
            stepCard.setStyle(
                    "-fx-background-color: " + (dark ? "#1a1a2e" : "#f8f9fa") + ";" +
                            "-fx-border-color: " + (dark ? "#2a2a4a" : "#e0e0e0") + ";" +
                            "-fx-border-radius: 6;" +
                            "-fx-background-radius: 6;" +
                            "-fx-padding: 12;"
            );

            String textColor = dark ? "#e0e0e0" : "#333";
            String muteColor = dark ? "#a0a0b0" : "#444";

            Label stepNum = new Label("Step " + step.getStepOrder() + " — " + step.getTitle());
            stepNum.setStyle(
                    "-fx-font-weight: bold;" +
                            "-fx-font-size: 13;" +
                            "-fx-text-fill: " + textColor + ";"
            );

            Label content = new Label(step.getContent());
            content.setWrapText(true);
            content.setStyle(
                    "-fx-font-size: 12;" +
                            "-fx-text-fill: " + muteColor + ";"
            );

            stepCard.getChildren().addAll(stepNum, content);

            if (step.getImage() != null && !step.getImage().isEmpty()) {
                try {
                    String imagePath = "target/classes/images/" + step.getImage();
                    Image img = new Image(new FileInputStream(imagePath));

                    ImageView imageView = new ImageView(img);
                    imageView.setFitWidth(300);
                    imageView.setFitHeight(180);
                    imageView.setPreserveRatio(true);

                    stepCard.getChildren().add(imageView);
                } catch (Exception e) {
                    System.err.println("Failed to load step image: " + e.getMessage());
                }
            }

            if (step.getVideoUrl() != null && !step.getVideoUrl().isEmpty()) {
                Hyperlink video = new Hyperlink("▶ Watch Video");
                video.setStyle("-fx-text-fill: #3498db; -fx-font-size: 12;");

                video.setOnAction(e -> {
                    try {
                        Desktop.getDesktop().browse(new URI(step.getVideoUrl()));
                    } catch (Exception ex) {
                        System.err.println("Failed to open video URL: " + ex.getMessage());
                    }
                });

                stepCard.getChildren().add(video);
            }

            stepsContainer.getChildren().add(stepCard);
        }

        if (steps.isEmpty()) {
            stepsContainer.getChildren().add(new Label("No steps available for this guide."));
        }
    }

    private void loadRatings() {
        ratingsContainer.getChildren().clear();

        List<GuideRating> ratings = ratingDAO.getByGuideId(currentGuide.getId());
        boolean dark = AppState.isDarkMode();

        for (GuideRating rating : ratings) {
            HBox row = new HBox(10);
            row.setStyle(
                    "-fx-background-color: " + (dark ? "#1a1a2e" : "#f8f9fa") + ";" +
                            "-fx-border-color: " + (dark ? "#2a2a4a" : "#e0e0e0") + ";" +
                            "-fx-border-radius: 6;" +
                            "-fx-background-radius: 6;" +
                            "-fx-padding: 10;"
            );

            int ratingValue = Math.max(0, Math.min(5, rating.getRatingValue()));

            Label starLabel = new Label("★".repeat(ratingValue) + "☆".repeat(5 - ratingValue));
            starLabel.setStyle(
                    "-fx-font-size: 13;" +
                            "-fx-text-fill: #f1c40f;"
            );

            Label comment = new Label(
                    rating.getComment() != null && !rating.getComment().isEmpty()
                            ? rating.getComment()
                            : "No comment"
            );
            comment.setWrapText(true);
            comment.setStyle(
                    "-fx-font-size: 12;" +
                            "-fx-text-fill: " + (dark ? "#a0a0b0" : "#555") + ";"
            );

            Label sentimentBadge = new Label("...");
            sentimentBadge.setStyle(
                    "-fx-background-color: #ccc;" +
                            "-fx-text-fill: white;" +
                            "-fx-padding: 2 8 2 8;" +
                            "-fx-background-radius: 99;" +
                            "-fx-font-size: 11;"
            );

            if (rating.getComment() != null && !rating.getComment().isEmpty()) {
                analyzeSentimentAsync(rating.getComment(), sentimentBadge);
            } else {
                sentimentBadge.setText("N/A");
            }

            row.getChildren().addAll(starLabel, comment, sentimentBadge);
            ratingsContainer.getChildren().add(row);
        }

        if (ratings.isEmpty()) {
            ratingsContainer.getChildren().add(new Label("No ratings yet. Be the first to rate!"));
        }
    }

    private void analyzeSentimentAsync(String comment, Label sentimentBadge) {
        new Thread(() -> {
            try {
                String json = "{\"comment\": \"" + comment.replace("\"", "'") + "\"}";

                OkHttpClient client = new OkHttpClient();

                RequestBody body = RequestBody.create(
                        json,
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url("http://127.0.0.1:5000/predict/sentiment")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.body() == null) {
                        Platform.runLater(() -> sentimentBadge.setText("N/A"));
                        return;
                    }

                    JSONObject result = new JSONObject(response.body().string());
                    String sentiment = result.optString("sentiment", "N/A");

                    Platform.runLater(() -> {
                        sentimentBadge.setText(sentiment);

                        if ("positive".equalsIgnoreCase(sentiment)) {
                            sentimentBadge.setStyle(
                                    "-fx-background-color: #2ecc71;" +
                                            "-fx-text-fill: white;" +
                                            "-fx-padding: 2 8 2 8;" +
                                            "-fx-background-radius: 99;" +
                                            "-fx-font-size: 11;"
                            );
                        } else if ("negative".equalsIgnoreCase(sentiment)) {
                            sentimentBadge.setStyle(
                                    "-fx-background-color: #e74c3c;" +
                                            "-fx-text-fill: white;" +
                                            "-fx-padding: 2 8 2 8;" +
                                            "-fx-background-radius: 99;" +
                                            "-fx-font-size: 11;"
                            );
                        } else {
                            sentimentBadge.setStyle(
                                    "-fx-background-color: #95a5a6;" +
                                            "-fx-text-fill: white;" +
                                            "-fx-padding: 2 8 2 8;" +
                                            "-fx-background-radius: 99;" +
                                            "-fx-font-size: 11;"
                            );
                        }
                    });
                }

            } catch (Exception e) {
                Platform.runLater(() -> sentimentBadge.setText("N/A"));
            }
        }).start();
    }

    @FXML
    private void handleSubmitRating() {
        if (ratingBox.getValue() == null) {
            showAlert("Please select a rating!");
            return;
        }

        GuideRating rating = new GuideRating(
                currentGuide.getId(),
                1,
                ratingBox.getValue(),
                commentField.getText().trim()
        );

        ratingDAO.add(rating);

        commentField.clear();
        ratingBox.setValue(null);

        loadRatings();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}