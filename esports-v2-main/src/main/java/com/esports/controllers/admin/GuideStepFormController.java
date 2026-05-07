package com.esports.controllers.admin;

import com.esports.dao.GuideDAO;
import com.esports.dao.GuideStepDAO;
import com.esports.models.Guide;
import com.esports.models.GuideStep;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class GuideStepFormController {

    @FXML private Label formTitle;
    @FXML private ComboBox<Guide> guideBox;
    @FXML private TextField titleField, orderField, imageField, videoField;
    @FXML private TextArea contentField;
    @FXML private Button deleteBtn;

    private GuideStepDAO stepDAO = new GuideStepDAO();
    private GuideDAO guideDAO    = new GuideDAO();
    private GuideStep currentStep;
    private Runnable onSuccess;

    @FXML
    public void initialize() {
        guideBox.setItems(FXCollections.observableArrayList(guideDAO.getAll()));
    }

    public void setStep(GuideStep step) {
        this.currentStep = step;
        if (step == null) {
            formTitle.setText("Add Step");
            deleteBtn.setVisible(false);
        } else {
            formTitle.setText("Edit Step");
            deleteBtn.setVisible(true);
            titleField.setText(step.getTitle());
            contentField.setText(step.getContent());
            orderField.setText(String.valueOf(step.getStepOrder()));
            imageField.setText(step.getImage() != null ? step.getImage() : "");
            videoField.setText(step.getVideoUrl() != null ? step.getVideoUrl() : "");
            guideBox.getItems().stream()
                    .filter(g -> g.getId() == step.getGuideId())
                    .findFirst()
                    .ifPresent(guideBox::setValue);
        }
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        File file = fileChooser.showOpenDialog((Stage) titleField.getScene().getWindow());
        if (file != null) imageField.setText(file.getName());
    }

    @FXML
    private void handleSave() {
        if (!validateInputs()) return;
        int order;
        try {
            order = Integer.parseInt(orderField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert("Step order must be a number!"); return;
        }
        if (currentStep == null) {
            GuideStep s = new GuideStep(
                    guideBox.getValue().getId(),
                    titleField.getText().trim(),
                    contentField.getText().trim(),
                    order,
                    imageField.getText().trim(),
                    videoField.getText().trim()
            );
            stepDAO.add(s);
        } else {
            currentStep.setGuideId(guideBox.getValue().getId());
            currentStep.setTitle(titleField.getText().trim());
            currentStep.setContent(contentField.getText().trim());
            currentStep.setStepOrder(order);
            currentStep.setImage(imageField.getText().trim());
            currentStep.setVideoUrl(videoField.getText().trim());
            stepDAO.update(currentStep);
        }
        if (onSuccess != null) onSuccess.run();
        closeWindow();
    }

    @FXML
    private void handleDelete() {
        if (currentStep == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Step");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete this step?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                stepDAO.delete(currentStep.getId());
                if (onSuccess != null) onSuccess.run();
                closeWindow();
            }
        });
    }

    @FXML
    private void handleCancel() { closeWindow(); }

    private void closeWindow() {
        ((Stage) titleField.getScene().getWindow()).close();
    }

    private boolean validateInputs() {
        return true;
    }

    @FXML
    private void handleFetchYoutube() {
        String videoUrl = videoField.getText().trim();
        if (videoUrl.isEmpty()) { showAlert("Enter a YouTube URL first!"); return; }

        try {
            // extract video ID
            String videoId = "";
            if (videoUrl.contains("v=")) {
                videoId = videoUrl.split("v=")[1];
                if (videoId.contains("&")) videoId = videoId.split("&")[0];
            } else if (videoUrl.contains("youtu.be/")) {
                videoId = videoUrl.split("youtu.be/")[1];
            }

            if (videoId.isEmpty()) { showAlert("Invalid YouTube URL!"); return; }

            String apiKey = "AIzaSyCQlFNlxRG-eUWQOPPTpTX5y6HtGvim2IQ";
            String url = "https://www.googleapis.com/youtube/v3/videos?id=" + videoId
                    + "&key=" + apiKey + "&part=snippet";

            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
            okhttp3.Response response = client.newCall(request).execute();

            org.json.JSONObject json = new org.json.JSONObject(response.body().string());
            org.json.JSONArray items = json.getJSONArray("items");

            if (items.length() == 0) { showAlert("Video not found!"); return; }

            org.json.JSONObject snippet = items.getJSONObject(0).getJSONObject("snippet");
            String title = snippet.getString("title");
            String thumbnailUrl = snippet.getJSONObject("thumbnails")
                    .getJSONObject("high").getString("url");

            // fill title field if empty
            if (titleField.getText().trim().isEmpty()) {
                titleField.setText(title);
            }

            // download thumbnail
            String fileName = videoId + ".jpg";
            okhttp3.Request imgRequest = new okhttp3.Request.Builder()
                    .url(thumbnailUrl).build();
            okhttp3.Response imgResponse = client.newCall(imgRequest).execute();
            byte[] imageBytes = imgResponse.body().bytes();
            new java.io.File("src/main/resources/images/").mkdirs();
            new java.io.File("target/classes/images/").mkdirs();
            java.nio.file.Files.write(
                    java.nio.file.Paths.get("src/main/resources/images/" + fileName), imageBytes);
            java.nio.file.Files.write(
                    java.nio.file.Paths.get("target/classes/images/" + fileName), imageBytes);

            imageField.setText(fileName);


        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to fetch from YouTube: " + e.getMessage());
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