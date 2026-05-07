package com.esports.controllers.admin;

import com.esports.dao.GuideDAO;
import com.esports.models.Guide;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class GuideController {

    @FXML private TextField searchField;
    @FXML private TableView<Guide> guideTable;
    @FXML private TableColumn<Guide, Integer> colId, colGameId;
    @FXML private TableColumn<Guide, String>  colTitle, colDesc, colDifficulty;

    private GuideDAO guideDAO = new GuideDAO();
    private ObservableList<Guide> guideList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDifficulty.setCellValueFactory(new PropertyValueFactory<>("difficulty"));
        colGameId.setCellValueFactory(new PropertyValueFactory<>("gameId"));
        loadGuides();

        // double click to open edit popup
        guideTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Guide selected = guideTable.getSelectionModel().getSelectedItem();
                if (selected != null) openForm(selected);
            }
        });
    }

    private void loadGuides() {
        guideList.setAll(guideDAO.getAll());
        guideTable.setItems(guideList);
    }

    @FXML
    private void handleAdd() {
        openForm(null);
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) { loadGuides(); return; }
        List<Guide> filtered = guideDAO.getAll().stream()
                .filter(g -> g.getTitle().toLowerCase().contains(query)
                        || g.getDifficulty().toLowerCase().contains(query))
                .collect(Collectors.toList());
        guideList.setAll(filtered);
        guideTable.setItems(guideList);
    }

    private void openForm(Guide guide) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/guide-form.fxml"));            Stage stage = new Stage();
            stage.setTitle(guide == null ? "Add Guide" : "Edit Guide");
            stage.setScene(new Scene(loader.load()));
            stage.initModality(Modality.APPLICATION_MODAL);
            GuideFormController controller = loader.getController();
            controller.setGuide(guide);
            controller.setOnSuccess(this::loadGuides);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}