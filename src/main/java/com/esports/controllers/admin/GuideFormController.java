package com.esports.controllers.admin;

import com.esports.dao.GameDAO;
import com.esports.dao.GuideDAO;
import com.esports.models.Game;
import com.esports.models.Guide;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class GuideFormController {

    @FXML private Label formTitle;
    @FXML private TextField titleField, coverField;
    @FXML private TextArea descField;
    @FXML private ComboBox<String> difficultyBox;
    @FXML private ComboBox<Game> gameBox;
    @FXML private Button deleteBtn;

    private GuideDAO guideDAO = new GuideDAO();
    private GameDAO gameDAO   = new GameDAO();
    private Guide currentGuide;
    private Runnable onSuccess;

    @FXML
    public void initialize() {
        difficultyBox.setItems(FXCollections.observableArrayList("Easy", "Medium", "Hard"));
        gameBox.setItems(FXCollections.observableArrayList(gameDAO.getAll()));
    }

    public void setGuide(Guide guide) {
        this.currentGuide = guide;
        if (guide == null) {
            formTitle.setText("Add Guide");
            deleteBtn.setVisible(false);
        } else {
            formTitle.setText("Edit Guide");
            deleteBtn.setVisible(true);
            titleField.setText(guide.getTitle());
            descField.setText(guide.getDescription());
            difficultyBox.setValue(guide.getDifficulty());
            coverField.setText(guide.getCoverImage() != null ? guide.getCoverImage() : "");
            gameBox.getItems().stream()
                    .filter(g -> g.getId() == guide.getGameId())
                    .findFirst()
                    .ifPresent(gameBox::setValue);
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
        File file = fileChooser.showOpenDialog((Stage) titleField.getScene().getWindow());
        if (file != null) coverField.setText(file.getName());
    }

    @FXML
    private void handleSave() {
        if (!validateInputs()) return;
        if (currentGuide == null) {
            Guide g = new Guide(
                    gameBox.getValue().getId(),
                    titleField.getText().trim(),
                    descField.getText().trim(),
                    difficultyBox.getValue(),
                    1,
                    coverField.getText().trim()
            );
            guideDAO.add(g);
        } else {
            currentGuide.setTitle(titleField.getText().trim());
            currentGuide.setDescription(descField.getText().trim());
            currentGuide.setDifficulty(difficultyBox.getValue());
            currentGuide.setGameId(gameBox.getValue().getId());
            currentGuide.setCoverImage(coverField.getText().trim());
            guideDAO.update(currentGuide);
        }
        if (onSuccess != null) onSuccess.run();
        closeWindow();
    }

    @FXML
    private void handlePredictDifficulty() {
        String description = descField.getText().trim();
        if (description.isEmpty()) { showAlert("Enter a description first!"); return; }

        try {
            String json = "{\"description\": \"" + description.replace("\"", "'") + "\"}";
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    json, okhttp3.MediaType.parse("application/json"));
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("http://127.0.0.1:5000/predict/difficulty")
                    .post(body)
                    .build();
            okhttp3.Response response = client.newCall(request).execute();
            org.json.JSONObject result = new org.json.JSONObject(response.body().string());
            String difficulty  = result.getString("difficulty");
            double confidence  = result.getDouble("confidence");
            difficultyBox.setValue(difficulty);
            showInfo("🤖 Predicted: " + difficulty + " (" + (int)(confidence * 100) + "% confidence)");
        } catch (Exception e) {
            showAlert("Flask API not running! Start it with: python app.py");
        }
    }
    @FXML
    private void handleDelete() {
        if (currentGuide == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Guide");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete this guide?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                guideDAO.delete(currentGuide.getId());
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
        ((Stage) titleField.getScene().getWindow()).close();
    }

    private boolean validateInputs() {
        return true;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}