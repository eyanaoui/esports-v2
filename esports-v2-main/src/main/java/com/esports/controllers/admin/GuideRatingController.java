package com.esports.controllers.admin;

import com.esports.dao.GuideRatingDAO;
import com.esports.models.GuideRating;
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

public class GuideRatingController {

    @FXML private TextField searchField;
    @FXML private TableView<GuideRating> ratingTable;
    @FXML private TableColumn<GuideRating, Integer> colId, colGuideId, colUserId, colRating;
    @FXML private TableColumn<GuideRating, String>  colComment, colDate;

    private GuideRatingDAO ratingDAO = new GuideRatingDAO();
    private ObservableList<GuideRating> ratingList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colGuideId.setCellValueFactory(new PropertyValueFactory<>("guideId"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colRating.setCellValueFactory(new PropertyValueFactory<>("ratingValue"));
        colComment.setCellValueFactory(new PropertyValueFactory<>("comment"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        loadRatings();

        ratingTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                GuideRating selected = ratingTable.getSelectionModel().getSelectedItem();
                if (selected != null) openForm(selected);
            }
        });
    }

    private void loadRatings() {
        ratingList.setAll(ratingDAO.getAll());
        ratingTable.setItems(ratingList);
    }

    @FXML
    private void handleAdd() { openForm(null); }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) { loadRatings(); return; }
        List<GuideRating> filtered = ratingDAO.getAll().stream()
                .filter(r -> r.getComment() != null &&
                        r.getComment().toLowerCase().contains(query))
                .collect(Collectors.toList());
        ratingList.setAll(filtered);
        ratingTable.setItems(ratingList);
    }

    private void openForm(GuideRating rating) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/guide-rating-form.fxml"));
            Stage stage = new Stage();
            stage.setTitle(rating == null ? "Add Rating" : "Edit Rating");
            stage.setScene(new Scene(loader.load()));
            stage.initModality(Modality.APPLICATION_MODAL);
            GuideRatingFormController controller = loader.getController();
            controller.setRating(rating);
            controller.setOnSuccess(this::loadRatings);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}