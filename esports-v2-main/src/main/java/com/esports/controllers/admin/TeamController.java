package com.esports.controllers.admin;

import com.esports.dao.TeamDAO;
import com.esports.models.Team;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.esports.services.PandaScoreService;


public class TeamController {
    @FXML private TextField searchField;
    @FXML private TableView<Team> teamTable;
    @FXML private TableColumn<Team, String> colName;
    @FXML private TableColumn<Team, String> colLogo;
    @FXML private TableColumn<Team, String> colDescription;
    @FXML private TableColumn<Team, Integer> colCaptain;
    @FXML private TableColumn<Team, LocalDateTime> colCreatedAt;

    private TeamDAO teamDAO = new TeamDAO();
    private ObservableList<Team> teamList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colCaptain.setCellValueFactory(new PropertyValueFactory<>("captain_id"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("created_at"));
        colLogo.setCellValueFactory(new PropertyValueFactory<>("logo"));
        colLogo.setCellFactory(this::createLogoCell);

        loadTeams();

        teamTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Team selected = teamTable.getSelectionModel().getSelectedItem();
                if (selected != null) openForm(selected);
            }
        });
    }

    private void loadTeams() {
        teamList.setAll(teamDAO.getAll());
        teamTable.setItems(teamList);
    }

    @FXML private void handleAdd() { openForm(null); }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) {
            loadTeams();
            return;
        }
        List<Team> filtered = teamDAO.getAll().stream()
                .filter(t -> (t.getName() != null && t.getName().toLowerCase().contains(query)) ||
                        (t.getDescription() != null && t.getDescription().toLowerCase().contains(query)))
                .collect(Collectors.toList());
        teamList.setAll(filtered);
    }

    private void openForm(Team team) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/team-form.fxml"));
            Parent parent = loader.load();
            TeamFormController controller = loader.getController();
            controller.setTeam(team);
            controller.setOnSuccess(this::loadTeams);
            Stage stage = new Stage();
            stage.setTitle(team == null ? "Add Team" : "Edit Team");
            stage.setScene(new Scene(parent));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            System.err.println("Error opening form: " + e.getMessage());
        }
    }

    private TableCell<Team, String> createLogoCell(TableColumn<Team, String> column) {
        return new TableCell<>() {
            private final javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();

            @Override
            protected void updateItem(String url, boolean empty) {
                super.updateItem(url, empty);
                if (empty || url == null || url.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        // This creates a 40x40 thumbnail with aspect ratio preserved
                        javafx.scene.image.Image img = new javafx.scene.image.Image(url, 40, 40, true, true, true);
                        imageView.setImage(img);
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(new Label("No Preview"));
                    }
                }
            }
        };
    }

    private PandaScoreService pandaService = new PandaScoreService();

    @FXML
    private void handleShowLiveResults() {
        Team selected = teamTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String rawJson = pandaService.getTeamResults(selected.getName());

        // Quick parsing logic to extract match names and results
        StringBuilder displayResults = new StringBuilder();

        if (rawJson.contains("\"name\":")) {
            // We look for the "name" of the match and the "score"
            String[] matches = rawJson.split("\\{\"id\":"); // Split by individual match objects

            for (int i = 1; i < Math.min(matches.length, 4); i++) { // Get top 3 results
                try {
                    String matchName = matches[i].split("\"name\":\"")[1].split("\"")[0];
                    displayResults.append("🎮 ").append(matchName).append("\n");
                } catch (Exception e) {
                    displayResults.append("Match data found...\n");
                }
            }
        } else {
            displayResults.append("No recent professional matches found for this team name.");
        }

        Alert resultsAlert = new Alert(Alert.AlertType.INFORMATION);
        resultsAlert.setTitle("Real-World Results: " + selected.getName());
        resultsAlert.setHeaderText("Latest Matches via PandaScore API");
        resultsAlert.setContentText(displayResults.toString());
        resultsAlert.show();
    }
}