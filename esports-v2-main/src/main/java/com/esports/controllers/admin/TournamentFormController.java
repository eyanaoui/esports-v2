package com.esports.controllers.admin;

import com.esports.dao.TournamentDAO;
import com.esports.models.Tournament;
import com.esports.services.DiscordWebhookService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;

public class TournamentFormController {
    @FXML private Label formTitle;
    @FXML private TextField nameField, gameField, maxTeamsField, prizeField;
    @FXML private TextArea descArea;
    @FXML private DatePicker startDatePicker, deadlinePicker;
    @FXML private ComboBox<String> formatCombo, statusCombo;
    @FXML private Button deleteBtn;

    // Error Labels for "Contrôle de Saisie"
    @FXML private Label nameError, gameError, formatError, maxTeamsError, dateError, deadlineError, prizeError, statusError, descError;

    private final TournamentDAO tournamentDAO = new TournamentDAO();
    private final DiscordWebhookService discordService = new DiscordWebhookService();
    private Tournament currentTournament;
    private Runnable onSuccess;

    @FXML
    public void initialize() {
        formatCombo.getItems().addAll("single_elimination", "double_elimination", "round_robin");
        statusCombo.getItems().addAll("open", "in_progress", "completed", "cancelled");
        hideErrors();
    }

    private void hideErrors() {
        Label[] errors = {nameError, gameError, formatError, maxTeamsError, dateError, deadlineError, prizeError, statusError, descError};
        for (Label l : errors) { if (l != null) { l.setVisible(false); l.setManaged(false); } }
    }

    public void setTournament(Tournament t) {
        this.currentTournament = t;
        if (t != null) {
            formTitle.setText("Edit Tournament");
            nameField.setText(t.getName());
            gameField.setText(t.getGame());
            descArea.setText(t.getDescription());
            formatCombo.setValue(t.getFormat());
            maxTeamsField.setText(String.valueOf(t.getMaxTeams()));
            prizeField.setText(t.getPrize());
            statusCombo.setValue(t.getStatus());
            if (t.getStartDate() != null) startDatePicker.setValue(LocalDate.parse(t.getStartDate().split(" ")[0]));
            deleteBtn.setVisible(true);
        } else {
            formTitle.setText("Add Tournament");
            deleteBtn.setVisible(false);
            statusCombo.setValue("open");
        }
    }

    public void setOnSuccess(Runnable onSuccess) { this.onSuccess = onSuccess; }

    @FXML
    private void handleSave() {
        hideErrors();
        boolean isValid = true;

        // --- Step 1: Validation (Contrôle de Saisie) ---
        if (nameField.getText().trim().isEmpty()) { showErr(nameError); isValid = false; }
        if (gameField.getText().trim().isEmpty()) { showErr(gameError); isValid = false; }
        if (formatCombo.getValue() == null) { showErr(formatError); isValid = false; }
        if (statusCombo.getValue() == null) { showErr(statusError); isValid = false; }
        if (startDatePicker.getValue() == null) { showErr(dateError); isValid = false; }
        if (deadlinePicker.getValue() == null) { showErr(deadlineError); isValid = false; }
        if (descArea.getText().trim().isEmpty()) { showErr(descError); isValid = false; }

        int maxT = 0;
        try {
            maxT = Integer.parseInt(maxTeamsField.getText());
            if (maxT <= 0) throw new Exception();
        } catch (Exception e) {
            showErr(maxTeamsError);
            isValid = false;
        }

        if (!isValid) return;

        // --- Step 2: Object Preparation ---
        boolean isNew = (currentTournament == null);
        if (isNew) {
            currentTournament = new Tournament();
        }

        currentTournament.setName(nameField.getText().trim());
        currentTournament.setGame(gameField.getText().trim());
        currentTournament.setDescription(descArea.getText().trim());
        currentTournament.setFormat(formatCombo.getValue());
        currentTournament.setStatus(statusCombo.getValue());
        currentTournament.setPrize(prizeField.getText().trim());
        currentTournament.setMaxTeams(maxT);
        currentTournament.setStartDate(startDatePicker.getValue().toString());
        currentTournament.setRegistrationDeadline(deadlinePicker.getValue().toString());
        currentTournament.setEndDate(startDatePicker.getValue().plusDays(1).toString());

        // --- Step 3: Persistence & AI API Trigger ---
        try {
            if (isNew) {
                tournamentDAO.add(currentTournament);

                // Fixed: Removed redundant '= 0' to clear IDE warning
                double prizeValue;
                try {
                    prizeValue = Double.parseDouble(prizeField.getText().replaceAll("[^0-9.]", ""));
                } catch (Exception e) {
                    prizeValue = 0.0;
                }

                String hypeVerdict = (prizeValue >= 1000) ? "🔥 MAJOR: High Stakes!"
                        : (maxT >= 16) ? "🏆 MASSIVE: Huge Bracket!"
                        : "⚔️ ELITE: Skill-Based Clash!";

                discordService.announceTournament(
                        currentTournament.getName() + " (" + hypeVerdict + ")",
                        currentTournament.getGame(),
                        currentTournament.getStartDate(),
                        currentTournament.getPrize()
                );
            } else {
                tournamentDAO.update(currentTournament);
            }

            if (onSuccess != null) { onSuccess.run(); }
            closeWindow();

        } catch (Exception e) {
            // Fixed: Replaced printStackTrace with more robust logging
            System.err.println("Save failed: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR, "Database Error: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void showErr(Label l) { if (l != null) { l.setVisible(true); l.setManaged(true); } }
    @FXML private void handleCancel() { closeWindow(); }
    @FXML private void handleDelete() {
        if (currentTournament != null && currentTournament.getId() != 0) {
            tournamentDAO.delete(currentTournament.getId());
            if (onSuccess != null) onSuccess.run();
            closeWindow();
        }
    }
    private void closeWindow() { ((Stage) nameField.getScene().getWindow()).close(); }
}