package com.esports.controllers.admin;

import javafx.stage.FileChooser;
import java.io.File;
import com.esports.dao.GameDAO;
import com.esports.models.Game;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class GameFormController {

    @FXML private Label formTitle;
    @FXML private TextField nameField, slugField, coverField;
    @FXML private TextArea descField;
    @FXML private CheckBox rankingCheck;
    @FXML private Button deleteBtn;

    private GameDAO gameDAO = new GameDAO();
    private Game currentGame;
    private Runnable onSuccess;

    public void setGame(Game game) {
        this.currentGame = game;
        if (game == null) {
            formTitle.setText("Add Game");
            deleteBtn.setVisible(false);
        } else {
            formTitle.setText("Edit Game");
            deleteBtn.setVisible(true);
            nameField.setText(game.getName());
            slugField.setText(game.getSlug());
            descField.setText(game.getDescription());
            coverField.setText(game.getCoverImage() != null ? game.getCoverImage() : "");
            rankingCheck.setSelected(game.isHasRanking());
        }
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Cover Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        File file = fileChooser.showOpenDialog((Stage) nameField.getScene().getWindow());
        if (file != null) {
            try {
                // copy to src/main/resources/images/
                File srcDir = new File("src/main/resources/images/");
                srcDir.mkdirs();
                java.nio.file.Files.copy(file.toPath(),
                        new File(srcDir, file.getName()).toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // also copy to target/classes/images/ so it works immediately
                File targetDir = new File("target/classes/images/");
                targetDir.mkdirs();
                java.nio.file.Files.copy(file.toPath(),
                        new File(targetDir, file.getName()).toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                coverField.setText(file.getName());
            } catch (Exception e) {
                e.printStackTrace();
                coverField.setText(file.getName());
            }
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInputs()) return;
        if (currentGame == null) {
            Game g = new Game(
                    nameField.getText().trim(),
                    slugField.getText().trim(),
                    descField.getText().trim(),
                    coverField.getText().trim(),
                    rankingCheck.isSelected()
            );
            gameDAO.add(g);
        } else {
            currentGame.setName(nameField.getText().trim());
            currentGame.setSlug(slugField.getText().trim());
            currentGame.setDescription(descField.getText().trim());
            currentGame.setCoverImage(coverField.getText().trim());
            currentGame.setHasRanking(rankingCheck.isSelected());
            gameDAO.update(currentGame);
        }
        if (onSuccess != null) onSuccess.run();
        closeWindow();
    }

    @FXML
    private void handleDelete() {
        if (currentGame == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Game");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete this game?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                gameDAO.delete(currentGame.getId());
                if (onSuccess != null) onSuccess.run();
                closeWindow();
            }
        });
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    private boolean validateInputs() {
        return true;
    }

    @FXML
    private void handleFetchRawg() {
        String gameName = nameField.getText().trim();

        try {
            String apiKey = "cb7ae6fdec6b4c12b91583f874c7c31b";
            String encodedName = java.net.URLEncoder.encode(gameName, "UTF-8");
            String url = "https://api.rawg.io/api/games?key=" + apiKey + "&search=" + encodedName + "&page_size=1";

            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
            okhttp3.Response response = client.newCall(request).execute();

            String body = response.body().string();
            org.json.JSONObject json = new org.json.JSONObject(body);
            org.json.JSONArray results = json.getJSONArray("results");

            if (results.length() == 0) { showAlert("No game found on RAWG!"); return; }

            org.json.JSONObject game = results.getJSONObject(0);

            String name             = game.getString("name");
            String slug             = game.getString("slug");
            String backgroundImage  = game.optString("background_image", "");

            // fetch full details to get description
            String detailUrl = "https://api.rawg.io/api/games/" + slug + "?key=" + apiKey;
            okhttp3.Request detailRequest = new okhttp3.Request.Builder().url(detailUrl).build();
            okhttp3.Response detailResponse = client.newCall(detailRequest).execute();
            org.json.JSONObject detailJson = new org.json.JSONObject(detailResponse.body().string());
            String description = detailJson.optString("description_raw", "");

            // fill fields
            nameField.setText(name);
            slugField.setText(slug);
            descField.setText(description);

            // download cover image
            if (!backgroundImage.isEmpty()) {
                javafx.application.Platform.runLater(() -> {
                    try {
                        String fileName = slug + ".jpg";

                        okhttp3.Request imgRequest = new okhttp3.Request.Builder()
                                .url(backgroundImage).build();
                        okhttp3.Response imgResponse = client.newCall(imgRequest).execute();

                        byte[] imageBytes = imgResponse.body().bytes();
                        new File("src/main/resources/images/").mkdirs();
                        new File("target/classes/images/").mkdirs();
                        java.nio.file.Files.write(
                                java.nio.file.Paths.get("src/main/resources/images/" + fileName), imageBytes);
                        java.nio.file.Files.write(
                                java.nio.file.Paths.get("target/classes/images/" + fileName), imageBytes);

                        coverField.setText(fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to fetch from RAWG: " + e.getMessage());
        }
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}