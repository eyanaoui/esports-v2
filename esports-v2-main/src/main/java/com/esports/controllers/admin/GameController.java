package com.esports.controllers.admin;

import com.esports.dao.GameDAO;
import com.esports.models.Game;
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
import java.util.List;
import java.util.stream.Collectors;

public class GameController {

    @FXML private TextField searchField;
    @FXML private TableView<Game> gameTable;
    @FXML private TableColumn<Game, Integer> colId;
    @FXML private TableColumn<Game, String>  colName, colSlug, colDescription;
    @FXML private TextField nameField, slugField, coverField;

    @FXML private TableColumn<Game, Boolean> colRanking;
    @FXML private TableColumn<Game, String> colCreatedAt;

    private GameDAO gameDAO = new GameDAO();
    private ObservableList<Game> gameList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSlug.setCellValueFactory(new PropertyValueFactory<>("slug"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colRanking.setCellValueFactory(new PropertyValueFactory<>("hasRanking"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        loadGames();

        gameTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Game selected = gameTable.getSelectionModel().getSelectedItem();
                if (selected != null) openForm(selected);
            }
        });
    }

    private void loadGames() {
        gameList.setAll(gameDAO.getAll());
        gameTable.setItems(gameList);
    }

    @FXML
    private void handleAdd() {
        openForm(null);
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) { loadGames(); return; }
        List<Game> filtered = gameDAO.getAll().stream()
                .filter(g -> g.getName().toLowerCase().contains(query)
                        || g.getSlug().toLowerCase().contains(query))
                .collect(Collectors.toList());
        gameList.setAll(filtered);
        gameTable.setItems(gameList);
    }

    private void openForm(Game game) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/game-form.fxml"));
            Stage stage = new Stage();
            stage.setTitle(game == null ? "Add Game" : "Edit Game");
            stage.setScene(new Scene(loader.load()));
            stage.initModality(Modality.APPLICATION_MODAL);
            GameFormController controller = loader.getController();
            controller.setGame(game);
            controller.setOnSuccess(this::loadGames);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}