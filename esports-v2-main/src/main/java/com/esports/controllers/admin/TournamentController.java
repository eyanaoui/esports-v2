package com.esports.controllers.admin;

import com.esports.dao.TournamentDAO;
import com.esports.models.Tournament;
import com.esports.services.AIService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.stream.Collectors;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import java.util.Map;
import java.util.List;

public class TournamentController {
    @FXML private TableView<Tournament> tournamentTable;
    @FXML private TableColumn<Tournament, String> colName, colGame, colStatus, colStartDate;
    @FXML private TableColumn<Tournament, Void> colActions;
    @FXML private TextField searchField;

    private final TournamentDAO tournamentDAO = new TournamentDAO();
    private final AIService aiService = new AIService();
    private final ObservableList<Tournament> tournamentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colGame.setCellValueFactory(new PropertyValueFactory<>("game"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));

        setupActions();
        loadTournaments();
    }

    private void setupActions() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button edit = new Button("✎");
            private final Button del = new Button("🗑");
            private final HBox container = new HBox(10, edit, del);
            {
                edit.setOnAction(e -> openForm(getTableView().getItems().get(getIndex())));
                del.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    public void loadTournaments() {
        tournamentList.setAll(tournamentDAO.getAll());
        tournamentTable.setItems(tournamentList);
    }

    @FXML private void handleAdd() { openForm(null); }

    @FXML private void handleSearch() {
        String q = searchField.getText().toLowerCase();
        ObservableList<Tournament> filtered = tournamentDAO.getAll().stream()
                .filter(t -> t.getName().toLowerCase().contains(q) || t.getGame().toLowerCase().contains(q))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        tournamentTable.setItems(filtered);
    }

    @FXML private void handleAIPrediction() {
        Tournament t = tournamentTable.getSelectionModel().getSelectedItem();
        if (t != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("AI Analysis for " + t.getName());
            alert.setContentText(aiService.getTournamentPrediction(t.getId()));
            alert.show();
        }
    }

    private void openForm(Tournament t) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/tournament-form.fxml"));
            Parent root = loader.load();
            TournamentFormController ctrl = loader.getController();
            ctrl.setTournament(t);
            ctrl.setOnSuccess(this::loadTournaments);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) { System.err.println("Error: " + e.getMessage()); }
    }

    private void handleDelete(Tournament t) {
        tournamentDAO.delete(t.getId());
        loadTournaments();
    }

    @FXML
    private void handleFairMatchmaking() {
        Tournament t = tournamentTable.getSelectionModel().getSelectedItem();
        if (t == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a tournament first!");
            alert.show();
            return;
        }

        List<Map<String, Object>> sortedTeams = aiService.generateFairMatchmaking(t.getId());

        if (sortedTeams == null || sortedTeams.size() < 2) {
            new Alert(Alert.AlertType.INFORMATION, "Not enough teams to generate a bracket (Min: 2).").show();
            return;
        }

        showBracketWindow(t.getName(), sortedTeams);
    }

    private void showBracketWindow(String tournamentName, List<Map<String, Object>> teams) {
        Stage stage = new Stage();
        stage.setTitle("AI Fair Matchmaking - " + tournamentName);

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: #1e1e2e; -fx-padding: 30;");
        root.setAlignment(Pos.TOP_CENTER);

        Label header = new Label("NEURAL BALANCED BRACKET");
        header.setStyle("-fx-text-fill: #89b4fa; -fx-font-size: 22; -fx-font-weight: bold;");
        root.getChildren().add(header);

        VBox bracketContainer = new VBox(15);
        bracketContainer.setAlignment(Pos.CENTER);

        for (int i = 0; i < teams.size() - 1; i += 2) {
            Map<String, Object> team1 = teams.get(i);
            Map<String, Object> team2 = teams.get(i + 1);

            double elo1 = ((Number) team1.get("elo")).doubleValue();
            double elo2 = ((Number) team2.get("elo")).doubleValue();
            double diff = Math.abs(elo1 - elo2);

            HBox matchCard = new HBox(15);
            matchCard.setStyle("-fx-background-color: #313244; -fx-padding: 20; -fx-background-radius: 15; " +
                    "-fx-border-color: #94e2d5; -fx-border-width: 1; -fx-border-radius: 15;");
            matchCard.setAlignment(Pos.CENTER);
            matchCard.setMinWidth(500);

            VBox t1Box = createTeamBox(team1.get("name").toString(), elo1);
            Label vsLabel = new Label("VS");
            vsLabel.setStyle("-fx-text-fill: #f38ba8; -fx-font-weight: bold; -fx-font-size: 16;");
            VBox t2Box = createTeamBox(team2.get("name").toString(), elo2);

            VBox fairBox = new VBox(2);
            fairBox.setAlignment(Pos.CENTER);
            fairBox.setStyle("-fx-padding: 0 0 0 20;");
            Label fairLabel = new Label("ELO DELTA");
            fairLabel.setStyle("-fx-text-fill: #94e2d5; -fx-font-size: 9;");
            Label fairValue = new Label("±" + String.format("%.1f", diff));
            fairValue.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            fairBox.getChildren().addAll(fairLabel, fairValue);

            matchCard.getChildren().addAll(t1Box, vsLabel, t2Box, fairBox);
            bracketContainer.getChildren().add(matchCard);
        }

        // ADDED: Handle odd team out
        if (teams.size() % 2 != 0) {
            Label byeLabel = new Label("BYE: " + teams.get(teams.size()-1).get("name") + " (Advances automatically)");
            byeLabel.setStyle("-fx-text-fill: #fab387; -fx-font-style: italic; -fx-padding: 10;");
            bracketContainer.getChildren().add(byeLabel);
        }

        ScrollPane scroll = new ScrollPane(bracketContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

        root.getChildren().add(scroll);
        stage.setScene(new Scene(root, 620.0, 600.0));
        stage.show();
    }

    private VBox createTeamBox(String name, double elo) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        Label nameLbl = new Label(name.toUpperCase());
        nameLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label eloLbl = new Label("ELO: " + elo);
        eloLbl.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 11;");
        box.getChildren().addAll(nameLbl, eloLbl);
        return box;
    }

    @FXML
    private void handleExportReport() {
        try {
            // This will save in your project's root folder
            java.io.PrintWriter writer = new java.io.PrintWriter("tournament_report.txt");

            writer.println("==================================================");
            writer.println("       ESPORTS TOURNAMENT MASTER REPORT          ");
            writer.println("       Generated on: " + java.time.LocalDateTime.now());
            writer.println("==================================================");
            writer.println("");

            for (Tournament t : tournamentTable.getItems()) {
                writer.println("TOURNAMENT: " + t.getName().toUpperCase());
                writer.println("--------------------------------------------------");
                writer.println(" > GAME:      " + t.getGame());
                writer.println(" > STATUS:    " + t.getStatus());
                writer.println(" > START:     " + t.getStartDate());
                writer.println(" > PRIZE:     " + (t.getPrize() != null ? t.getPrize() : "0.0"));
                writer.println(" > CAPACITY:  " + t.getMaxTeams() + " Teams Max");
                writer.println("");
            }

            writer.println("==================================================");
            writer.println("             END OF OFFICIAL REPORT               ");
            writer.println("==================================================");
            writer.close();

            // Professional feedback for the demo
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("Rich Data Export Complete");
            alert.setContentText("Full report saved to: tournament_report.txt");
            alert.show();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()).show();
        }
    }
}