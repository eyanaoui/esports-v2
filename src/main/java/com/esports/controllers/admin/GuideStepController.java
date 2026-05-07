package com.esports.controllers.admin;

import com.esports.dao.GuideStepDAO;
import com.esports.models.GuideStep;
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

public class GuideStepController {

    @FXML private TextField searchField;
    @FXML private TableView<GuideStep> stepTable;
    @FXML private TableColumn<GuideStep, Integer> colId, colGuideId, colOrder;
    @FXML private TableColumn<GuideStep, String>  colTitle, colContent, colVideo;

    private GuideStepDAO stepDAO = new GuideStepDAO();
    private ObservableList<GuideStep> stepList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colGuideId.setCellValueFactory(new PropertyValueFactory<>("guideId"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colContent.setCellValueFactory(new PropertyValueFactory<>("content"));
        colOrder.setCellValueFactory(new PropertyValueFactory<>("stepOrder"));
        colVideo.setCellValueFactory(new PropertyValueFactory<>("videoUrl"));
        loadSteps();

        stepTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                GuideStep selected = stepTable.getSelectionModel().getSelectedItem();
                if (selected != null) openForm(selected);
            }
        });
    }

    private void loadSteps() {
        stepList.setAll(stepDAO.getAll());
        stepTable.setItems(stepList);
    }

    @FXML
    private void handleAdd() { openForm(null); }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) { loadSteps(); return; }
        List<GuideStep> filtered = stepDAO.getAll().stream()
                .filter(s -> s.getTitle().toLowerCase().contains(query)
                        || s.getContent().toLowerCase().contains(query))
                .collect(Collectors.toList());
        stepList.setAll(filtered);
        stepTable.setItems(stepList);
    }

    private void openForm(GuideStep step) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/guide-step-form.fxml"));
            Stage stage = new Stage();
            stage.setTitle(step == null ? "Add Step" : "Edit Step");
            stage.setScene(new Scene(loader.load()));
            stage.initModality(Modality.APPLICATION_MODAL);
            GuideStepFormController controller = loader.getController();
            controller.setStep(step);
            controller.setOnSuccess(this::loadSteps);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}