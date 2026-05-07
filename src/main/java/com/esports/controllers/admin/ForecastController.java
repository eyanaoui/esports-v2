package com.esports.controllers.admin;

import com.esports.models.ProductForecast;
import com.esports.services.ForecastService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Comparator;
import java.util.List;

public class ForecastController {

    @FXML
    private TextField searchField;

    @FXML
    private Label totalForecastsLabel;

    @FXML
    private Label totalPredictedLabel;

    @FXML
    private Label restockNeededLabel;

    @FXML
    private Label reorderQtyLabel;

    @FXML
    private TableView<ProductForecast> forecastTable;

    @FXML
    private TableColumn<ProductForecast, Integer> productIdColumn;

    @FXML
    private TableColumn<ProductForecast, String> productNameColumn;

    @FXML
    private TableColumn<ProductForecast, String> categoryColumn;

    @FXML
    private TableColumn<ProductForecast, Integer> stockColumn;

    @FXML
    private TableColumn<ProductForecast, Integer> forecastDaysColumn;

    @FXML
    private TableColumn<ProductForecast, Double> predictedQtyColumn;

    @FXML
    private TableColumn<ProductForecast, Integer> reorderQtyColumn;

    @FXML
    private TableColumn<ProductForecast, String> riskColumn;

    @FXML
    private TableColumn<ProductForecast, String> generatedAtColumn;

    private final ForecastService forecastService = new ForecastService();

    private ObservableList<ProductForecast> forecastList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadForecasts();
    }

    private void setupTable() {
        productIdColumn.setCellValueFactory(new PropertyValueFactory<>("productId"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        forecastDaysColumn.setCellValueFactory(new PropertyValueFactory<>("forecastDays"));
        predictedQtyColumn.setCellValueFactory(new PropertyValueFactory<>("predictedQty"));
        reorderQtyColumn.setCellValueFactory(new PropertyValueFactory<>("recommendedReorderQty"));
        riskColumn.setCellValueFactory(new PropertyValueFactory<>("riskLevel"));
        generatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("generatedAtText"));

        predictedQtyColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", value));
                }
            }
        });

        riskColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String risk, boolean empty) {
                super.updateItem(risk, empty);

                if (empty || risk == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(risk);

                if ("RESTOCK NEEDED".equals(risk)) {
                    setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                } else if ("WATCH STOCK".equals(risk)) {
                    setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void loadForecasts() {
        List<ProductForecast> forecasts = forecastService.getAllForecasts();

        forecastList = FXCollections.observableArrayList(forecasts);
        forecastTable.setItems(forecastList);

        updateStats(forecasts);
    }

    private void updateStats(List<ProductForecast> forecasts) {
        totalForecastsLabel.setText(String.valueOf(forecasts.size()));
        totalPredictedLabel.setText(String.format("%.2f", forecastService.totalPredictedQty(forecasts)));
        restockNeededLabel.setText(String.valueOf(forecastService.countRestockNeeded(forecasts)));
        reorderQtyLabel.setText(String.valueOf(forecastService.totalRecommendedReorder(forecasts)));
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();

        List<ProductForecast> forecasts = forecastService.searchForecasts(keyword);

        forecastList = FXCollections.observableArrayList(forecasts);
        forecastTable.setItems(forecastList);

        updateStats(forecasts);
    }

    @FXML
    public void handleRefresh() {
        searchField.clear();
        loadForecasts();
    }

    @FXML
    public void handleShowRestockNeeded() {
        List<ProductForecast> forecasts = forecastService.getRestockNeededForecasts();

        forecastList = FXCollections.observableArrayList(forecasts);
        forecastTable.setItems(forecastList);

        updateStats(forecasts);
    }

    @FXML
    public void handleSortByPredictedQty() {
        forecastList.sort(Comparator.comparingDouble(ProductForecast::getPredictedQty).reversed());
        forecastTable.refresh();
    }

    @FXML
    public void handleSortByReorderQty() {
        forecastList.sort(Comparator.comparingInt(ProductForecast::getRecommendedReorderQty).reversed());
        forecastTable.refresh();
    }
}