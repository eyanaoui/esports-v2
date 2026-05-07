package com.esports.controllers.admin;

import com.esports.services.ProductReviewApiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.stream.Collectors;

public class ProductReviewsAdminController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<ProductReviewApiService.ApiReview> reviewsTable;

    @FXML
    private TableColumn<ProductReviewApiService.ApiReview, Integer> idColumn;

    @FXML
    private TableColumn<ProductReviewApiService.ApiReview, String> productColumn;

    @FXML
    private TableColumn<ProductReviewApiService.ApiReview, String> customerColumn;

    @FXML
    private TableColumn<ProductReviewApiService.ApiReview, Integer> ratingColumn;

    @FXML
    private TableColumn<ProductReviewApiService.ApiReview, String> starsColumn;

    @FXML
    private TableColumn<ProductReviewApiService.ApiReview, String> commentColumn;

    @FXML
    private TableColumn<ProductReviewApiService.ApiReview, String> statusColumn;

    @FXML
    private TableColumn<ProductReviewApiService.ApiReview, String> createdAtColumn;

    @FXML
    private Label totalReviewsLabel;

    @FXML
    private Label visibleReviewsLabel;

    @FXML
    private Label hiddenReviewsLabel;

    @FXML
    private Label averageRatingLabel;

    @FXML
    private Label messageLabel;

    private final ProductReviewApiService reviewApiService = new ProductReviewApiService();

    private ObservableList<ProductReviewApiService.ApiReview> reviewsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadReviews();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());

        productColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getProductName()));

        customerColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getCustomerName()));

        ratingColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getRating()).asObject());

        starsColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getStars()));

        commentColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getComment()));

        statusColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus()));

        createdAtColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getCreatedAt()));

        starsColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String stars, boolean empty) {
                super.updateItem(stars, empty);

                if (empty || stars == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(stars);
                    setStyle("-fx-text-fill: #facc15; -fx-font-weight: bold;");
                }
            }
        });

        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(status);

                if ("VISIBLE".equalsIgnoreCase(status)) {
                    setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void loadReviews() {
        List<ProductReviewApiService.ApiReview> reviews = reviewApiService.getAllReviewsForAdmin();

        reviewsList = FXCollections.observableArrayList(reviews);
        reviewsTable.setItems(reviewsList);

        updateStats(reviews);

        if (reviews.isEmpty()) {
            showMessage("#f59e0b", "No reviews loaded. Make sure Review API is running on port 8090.");
        } else {
            showMessage("#86efac", "Loaded " + reviews.size() + " review(s) from Review API.");
        }
    }

    private void updateStats(List<ProductReviewApiService.ApiReview> reviews) {
        int total = reviews.size();

        long visible = reviews.stream()
                .filter(review -> "VISIBLE".equalsIgnoreCase(review.getStatus()))
                .count();

        long hidden = reviews.stream()
                .filter(review -> "HIDDEN".equalsIgnoreCase(review.getStatus()))
                .count();

        double average = reviews.stream()
                .mapToInt(ProductReviewApiService.ApiReview::getRating)
                .average()
                .orElse(0.0);

        totalReviewsLabel.setText(String.valueOf(total));
        visibleReviewsLabel.setText(String.valueOf(visible));
        hiddenReviewsLabel.setText(String.valueOf(hidden));
        averageRatingLabel.setText(String.format("%.2f / 5", average));
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        if (keyword.isEmpty()) {
            reviewsTable.setItems(reviewsList);
            updateStats(reviewsList);
            showMessage("#86efac", "Showing all reviews.");
            return;
        }

        List<ProductReviewApiService.ApiReview> filtered = reviewsList.stream()
                .filter(review ->
                        safeLower(review.getProductName()).contains(keyword)
                                || safeLower(review.getCustomerName()).contains(keyword)
                                || safeLower(review.getComment()).contains(keyword)
                                || safeLower(review.getStatus()).contains(keyword)
                                || String.valueOf(review.getRating()).contains(keyword))
                .collect(Collectors.toList());

        ObservableList<ProductReviewApiService.ApiReview> filteredList = FXCollections.observableArrayList(filtered);
        reviewsTable.setItems(filteredList);
        updateStats(filtered);

        showMessage("#93c5fd", filtered.size() + " review(s) found.");
    }

    @FXML
    public void handleRefresh() {
        searchField.clear();
        loadReviews();
    }

    @FXML
    public void handleShowSelected() {
        ProductReviewApiService.ApiReview selected = reviewsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Review Moderation", "Please select a review.");
            return;
        }

        ProductReviewApiService.ReviewResult result = reviewApiService.showReview(selected.getId());

        if (result.isSuccess()) {
            loadReviews();
            showMessage("#86efac", "Review is now visible.");
        } else {
            showMessage("#f87171", result.getMessage());
        }
    }

    @FXML
    public void handleHideSelected() {
        ProductReviewApiService.ApiReview selected = reviewsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Review Moderation", "Please select a review.");
            return;
        }

        ProductReviewApiService.ReviewResult result = reviewApiService.hideReview(selected.getId());

        if (result.isSuccess()) {
            loadReviews();
            showMessage("#fca5a5", "Review hidden successfully.");
        } else {
            showMessage("#f87171", result.getMessage());
        }
    }

    @FXML
    public void handleDeleteSelected() {
        ProductReviewApiService.ApiReview selected = reviewsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Review Moderation", "Please select a review.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Review");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete this review permanently?\n\nCustomer: "
                + selected.getCustomerName()
                + "\nProduct: "
                + selected.getProductName()
                + "\nComment: "
                + selected.getComment());

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            ProductReviewApiService.ReviewResult result = reviewApiService.deleteReview(selected.getId());

            if (result.isSuccess()) {
                loadReviews();
                showMessage("#fca5a5", "Review deleted successfully.");
            } else {
                showMessage("#f87171", result.getMessage());
            }
        }
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private void showMessage(String color, String text) {
        messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 12;");
        messageLabel.setText(text);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}