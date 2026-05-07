package com.esports.controllers.admin;

import com.esports.dao.TeamDAO;
import com.esports.models.Team;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class TeamFormController {
    @FXML private Label formTitle;
    @FXML private TextField nameField, logoField, captainField;
    @FXML private TextArea descField;
    @FXML private Button deleteBtn;

    // Error Labels
    @FXML private Label nameError, logoError, descError, captainError;

    private TeamDAO teamDAO = new TeamDAO();
    private Team currentTeam;
    private Runnable onSuccess;

    @FXML
    public void initialize() {
        clearErrors();
    }

    private void clearErrors() {
        Label[] labels = {nameError, logoError, descError, captainError};
        for (Label l : labels) {
            if (l != null) {
                l.setVisible(false);
                l.setManaged(false);
            }
        }
    }

    private void showFieldError(Label label, String message) {
        if (label != null) {
            label.setText("⚠️ " + message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    public void setTeam(Team team) {
        this.currentTeam = team;
        if (team != null) {
            formTitle.setText("Edit Team");
            nameField.setText(team.getName());
            logoField.setText(team.getLogo());
            descField.setText(team.getDescription());
            captainField.setText(String.valueOf(team.getCaptain_id()));
            deleteBtn.setVisible(true);
        } else {
            formTitle.setText("Add Team");
            deleteBtn.setVisible(false);
        }
    }

    public void setOnSuccess(Runnable onSuccess) { this.onSuccess = onSuccess; }

    @FXML
    private void handleSave() {
        clearErrors();
        boolean isValid = true;

        // Validation
        if (nameField.getText().trim().isEmpty()) {
            showFieldError(nameError, "Team name is required");
            isValid = false;
        }
        if (logoField.getText().trim().isEmpty()) {
            showFieldError(logoError, "Logo path is required");
            isValid = false;
        }
        if (descField.getText().trim().isEmpty()) {
            showFieldError(descError, "Description is required");
            isValid = false;
        }

        int captainId = -1;
        try {
            captainId = Integer.parseInt(captainField.getText().trim());
            if (captainId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showFieldError(captainError, "Invalid ID (positive number only)");
            isValid = false;
        }

        if (!isValid) return;

        // Execution
        if (currentTeam == null) {
            Team t = new Team(nameField.getText().trim(), logoField.getText().trim(), descField.getText().trim(), captainId);
            teamDAO.add(t);
        } else {
            currentTeam.setName(nameField.getText().trim());
            currentTeam.setLogo(logoField.getText().trim());
            currentTeam.setDescription(descField.getText().trim());
            currentTeam.setCaptain_id(captainId);
            teamDAO.update(currentTeam);
        }

        if (onSuccess != null) onSuccess.run();
        handleCancel();
    }

    @FXML
    private void handleDelete() {
        if (currentTeam != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Deletion");
            alert.setHeaderText("Delete Team: " + currentTeam.getName());
            alert.setContentText("Are you sure you want to delete this team? This action cannot be undone.");

            if (alert.showAndWait().get() == ButtonType.OK) {
                teamDAO.delete(currentTeam.getId());
                if (onSuccess != null) onSuccess.run();
                handleCancel();
            }
        }
    }

    @FXML private void handleBrowse() {
        /* FileChooser logic can be added here later */
    }

    @FXML private void handleCancel() {
        ((Stage) nameField.getScene().getWindow()).close();
    }
}