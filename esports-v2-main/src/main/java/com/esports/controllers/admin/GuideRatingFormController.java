package com.esports.controllers.admin;

import com.esports.dao.GuideDAO;
import com.esports.dao.GuideRatingDAO;
import com.esports.models.Guide;
import com.esports.models.GuideRating;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class GuideRatingFormController {

    @FXML private Label formTitle;
    @FXML private ComboBox<Guide> guideBox;
    @FXML private ComboBox<Integer> ratingBox;
    @FXML private TextArea commentField;
    @FXML private Button deleteBtn;

    private GuideRatingDAO ratingDAO = new GuideRatingDAO();
    private GuideDAO guideDAO        = new GuideDAO();
    private GuideRating currentRating;
    private Runnable onSuccess;

    @FXML
    public void initialize() {
        guideBox.setItems(FXCollections.observableArrayList(guideDAO.getAll()));
        ratingBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
    }

    public void setRating(GuideRating rating) {
        this.currentRating = rating;
        if (rating == null) {
            formTitle.setText("Add Rating");
            deleteBtn.setVisible(false);
        } else {
            formTitle.setText("Edit Rating");
            deleteBtn.setVisible(true);
            ratingBox.setValue(rating.getRatingValue());
            commentField.setText(rating.getComment() != null ? rating.getComment() : "");
            guideBox.getItems().stream()
                    .filter(g -> g.getId() == rating.getGuideId())
                    .findFirst()
                    .ifPresent(guideBox::setValue);
        }
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    private void handleSave() {
        if (!validateInputs()) return;
        if (currentRating == null) {
            GuideRating r = new GuideRating(
                    guideBox.getValue().getId(),
                    1,
                    ratingBox.getValue(),
                    commentField.getText().trim()
            );
            ratingDAO.add(r);
        } else {
            currentRating.setGuideId(guideBox.getValue().getId());
            currentRating.setRatingValue(ratingBox.getValue());
            currentRating.setComment(commentField.getText().trim());
            ratingDAO.update(currentRating);
        }
        if (onSuccess != null) onSuccess.run();
        closeWindow();
    }

    @FXML
    private void handleDelete() {
        if (currentRating == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Rating");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete this rating?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                ratingDAO.delete(currentRating.getId());
                if (onSuccess != null) onSuccess.run();
                closeWindow();
            }
        });
    }

    @FXML
    private void handleCancel() { closeWindow(); }

    private void closeWindow() {
        ((Stage) commentField.getScene().getWindow()).close();
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
}