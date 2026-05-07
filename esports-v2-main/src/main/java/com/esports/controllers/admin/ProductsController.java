package com.esports.controllers.admin;

import com.esports.dao.ProductDAO;
import com.esports.models.Product;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import com.esports.services.ProductExcelExportService;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductsController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Product> productTable;

    @FXML
    private TableColumn<Product, Integer> idColumn;

    @FXML
    private TableColumn<Product, String> nameColumn;

    @FXML
    private TableColumn<Product, String> categoryColumn;

    @FXML
    private TableColumn<Product, String> descriptionColumn;

    @FXML
    private TableColumn<Product, Double> priceColumn;

    @FXML
    private TableColumn<Product, Integer> stockColumn;

    @FXML
    private TableColumn<Product, Integer> ordersCountColumn;

    @FXML
    private TableColumn<Product, Integer> forecastDaysColumn;

    @FXML
    private TableColumn<Product, Integer> reorderQtyColumn;

    @FXML
    private TableColumn<Product, String> riskColumn;

    @FXML
    private TableColumn<Product, String> imageColumn;

    @FXML
    private Label totalProductsLabel;

    @FXML
    private Label totalStockLabel;

    @FXML
    private Label totalOrdersLabel;

    @FXML
    private Label averagePriceLabel;

    @FXML
    private Label restockLabel;

    @FXML
    private BarChart<String, Number> ordersBarChart;

    @FXML
    private PieChart categoryPieChart;

    @FXML
    private Label messageLabel;

    @FXML
    private Button runForecastButton;

    private final ProductDAO productDAO = new ProductDAO();

    private ObservableList<Product> productList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadProducts();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
        ordersCountColumn.setCellValueFactory(new PropertyValueFactory<>("ordersCount"));
        forecastDaysColumn.setCellValueFactory(new PropertyValueFactory<>("forecastDays"));
        reorderQtyColumn.setCellValueFactory(new PropertyValueFactory<>("recommendedReorderQty"));
        riskColumn.setCellValueFactory(new PropertyValueFactory<>("mlRiskLevel"));
        imageColumn.setCellValueFactory(new PropertyValueFactory<>("image"));

        priceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f TND", value));
                }
            }
        });

        stockColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);

                if (empty || stock == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(String.valueOf(stock));

                if (stock <= 0) {
                    setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                } else if (stock <= 10) {
                    setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                }
            }
        });

        ordersCountColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer count, boolean empty) {
                super.updateItem(count, empty);

                if (empty || count == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(count));
                    setStyle("-fx-text-fill: #6366f1; -fx-font-weight: bold;");
                }
            }
        });

        reorderQtyColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer qty, boolean empty) {
                super.updateItem(qty, empty);

                if (empty || qty == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(String.valueOf(qty));

                if (qty > 0) {
                    setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
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

                if ("RESTOCK".equalsIgnoreCase(risk)) {
                    setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                } else if ("WATCH".equalsIgnoreCase(risk)) {
                    setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void loadProducts() {
        List<Product> products = productDAO.getAllProducts();

        productList = FXCollections.observableArrayList(products);
        productTable.setItems(productList);

        updateStatsAndCharts(productList);

        if (messageLabel != null) {
            messageLabel.setText("Loaded " + products.size() + " active products with latest sales forecast.");
            messageLabel.setStyle("-fx-text-fill: #86efac;");
        }
    }

    private void updateStatsAndCharts(List<Product> products) {
        int totalProducts = products.size();
        int totalStock = products.stream().mapToInt(Product::getStock).sum();
        int totalOrders = products.stream().mapToInt(Product::getOrdersCount).sum();
        double avgPrice = products.stream().mapToDouble(Product::getPrice).average().orElse(0.0);

        int restockNeeded = (int) products.stream()
                .filter(product -> product.getRecommendedReorderQty() > 0)
                .count();

        totalProductsLabel.setText(String.valueOf(totalProducts));
        totalStockLabel.setText(String.valueOf(totalStock));
        totalOrdersLabel.setText(String.valueOf(totalOrders));
        averagePriceLabel.setText(String.format("%.2f TND", avgPrice));

        if (restockLabel != null) {
            restockLabel.setText(String.valueOf(restockNeeded));
        }

        updateBarChart(products);
        updatePieChart(products);
    }

    private void updateBarChart(List<Product> products) {
        ordersBarChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Orders");

        products.stream()
                .sorted(Comparator.comparingInt(Product::getOrdersCount).reversed())
                .limit(8)
                .forEach(product ->
                        series.getData().add(new XYChart.Data<>(product.getName(), product.getOrdersCount()))
                );

        ordersBarChart.getData().add(series);
    }

    private void updatePieChart(List<Product> products) {
        categoryPieChart.getData().clear();

        Map<String, Long> categoryCount = products.stream()
                .collect(Collectors.groupingBy(
                        product -> product.getCategory() == null ? "Unknown" : product.getCategory(),
                        Collectors.counting()
                ));

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        for (Map.Entry<String, Long> entry : categoryCount.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        categoryPieChart.setData(pieData);
    }

    @FXML
    public void handleRunMlModels() {
        if (runForecastButton != null) {
            runForecastButton.setDisable(true);
            runForecastButton.setText("Updating...");
        }

        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: #c7d2fe;");
            messageLabel.setText("Updating sales forecast. Please wait...");
        }

        Thread thread = new Thread(() -> {
            ForecastRunResult result = runForecastPipeline();

            Platform.runLater(() -> {
                if (runForecastButton != null) {
                    runForecastButton.setDisable(false);
                    runForecastButton.setText("📈 Update Forecast");
                }

                if (result.success()) {
                    loadProducts();

                    if (messageLabel != null) {
                        messageLabel.setStyle("-fx-text-fill: #86efac;");
                        messageLabel.setText("Sales forecast updated successfully.");
                    }
                } else {
                    if (messageLabel != null) {
                        messageLabel.setStyle("-fx-text-fill: #f87171;");
                        messageLabel.setText(result.message());
                    }

                    showAlert(Alert.AlertType.ERROR, "Forecast Error", result.message());
                }
            });
        });

        thread.setDaemon(true);
        thread.start();
    }

    private ForecastRunResult runForecastPipeline() {
        File mlFolder = findMlFolder();

        if (mlFolder == null || !mlFolder.exists()) {
            return new ForecastRunResult(false, "Forecast folder not found. Expected folder: ml/");
        }

        String[] commands = {
                "py",
                "python",
                System.getProperty("user.home") + "\\AppData\\Local\\Python\\pythoncore-3.14-64\\python.exe"
        };

        StringBuilder errors = new StringBuilder();

        for (String pythonCommand : commands) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(
                        pythonCommand,
                        "run_all_models.py"
                );

                processBuilder.directory(mlFolder);
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();

                String output = new String(process.getInputStream().readAllBytes());

                int exitCode = process.waitFor();

                System.out.println("========== FORECAST OUTPUT ==========");
                System.out.println(output);
                System.out.println("=====================================");

                if (exitCode == 0) {
                    return new ForecastRunResult(true, "Sales forecast updated successfully.");
                }

                errors.append("Command failed: ")
                        .append(pythonCommand)
                        .append("\n")
                        .append(output)
                        .append("\n");

            } catch (Exception e) {
                errors.append("Command not working: ")
                        .append(pythonCommand)
                        .append(" -> ")
                        .append(e.getMessage())
                        .append("\n");
            }
        }

        return new ForecastRunResult(false, "Could not update sales forecast.\n" + errors);
    }

    private File findMlFolder() {
        File current = new File("ml");

        if (current.exists()) {
            return current;
        }

        File parent = new File("../ml");

        if (parent.exists()) {
            return parent;
        }

        File projectRoot = new File(System.getProperty("user.dir"), "ml");

        if (projectRoot.exists()) {
            return projectRoot;
        }

        return null;
    }

    @FXML
    public void handleRefresh() {
        searchField.clear();
        loadProducts();
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        if (keyword.isEmpty()) {
            productTable.setItems(productList);
            updateStatsAndCharts(productList);

            if (messageLabel != null) {
                messageLabel.setText("Showing all products.");
                messageLabel.setStyle("-fx-text-fill: #86efac;");
            }

            return;
        }

        List<Product> filtered = productList.stream()
                .filter(product ->
                        safeLower(product.getName()).contains(keyword)
                                || safeLower(product.getCategory()).contains(keyword)
                                || safeLower(product.getDescription()).contains(keyword)
                                || String.valueOf(product.getPrice()).contains(keyword)
                                || String.valueOf(product.getStock()).contains(keyword)
                                || product.getMlRiskLevel().toLowerCase().contains(keyword))
                .collect(Collectors.toList());

        ObservableList<Product> filteredList = FXCollections.observableArrayList(filtered);
        productTable.setItems(filteredList);
        updateStatsAndCharts(filteredList);

        if (messageLabel != null) {
            messageLabel.setText(filtered.size() + " product(s) found.");
            messageLabel.setStyle("-fx-text-fill: #c7d2fe;");
        }
    }

    @FXML
    public void handleSortByName() {
        sortAndDisplay(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
    }

    @FXML
    public void handleSortByPriceAsc() {
        sortAndDisplay(Comparator.comparingDouble(Product::getPrice));
    }

    @FXML
    public void handleSortByPriceDesc() {
        sortAndDisplay(Comparator.comparingDouble(Product::getPrice).reversed());
    }

    @FXML
    public void handleSortByStockAsc() {
        sortAndDisplay(Comparator.comparingInt(Product::getStock));
    }

    @FXML
    public void handleSortByStockDesc() {
        sortAndDisplay(Comparator.comparingInt(Product::getStock).reversed());
    }

    @FXML
    public void handleSortByOrders() {
        sortAndDisplay(Comparator.comparingInt(Product::getOrdersCount).reversed());
    }

    @FXML
    public void handleSortByReorderQty() {
        sortAndDisplay(Comparator.comparingInt(Product::getRecommendedReorderQty).reversed());
    }

    private void sortAndDisplay(Comparator<Product> comparator) {
        List<Product> sorted = productTable.getItems().stream()
                .sorted(comparator)
                .collect(Collectors.toList());

        ObservableList<Product> sortedList = FXCollections.observableArrayList(sorted);
        productTable.setItems(sortedList);
        updateStatsAndCharts(sortedList);
    }

    @FXML
    public void handleDeleteSelected() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();

        if (selectedProduct == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a product.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate Product");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to deactivate this product?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            boolean deleted = productDAO.softDeleteProduct(selectedProduct.getId());

            if (deleted) {
                loadProducts();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Product deactivated successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Product was not deactivated.");
            }
        }
    }

    @FXML
    public void handleAddProduct() {
        loadInsideContentArea("/views/admin/product-form.fxml");
    }

    @FXML
    public void handleEditSelected() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();

        if (selectedProduct == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a product to modify.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/product-form.fxml"));
            Parent formView = loader.load();

            ProductFormController controller = loader.getController();
            controller.setProduct(selectedProduct);

            AnchorPane contentArea = (AnchorPane) productTable.getScene().lookup("#contentArea");

            if (contentArea != null) {
                AnchorPane.setTopAnchor(formView, 0.0);
                AnchorPane.setBottomAnchor(formView, 0.0);
                AnchorPane.setLeftAnchor(formView, 0.0);
                AnchorPane.setRightAnchor(formView, 0.0);
                contentArea.getChildren().setAll(formView);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleExportExcel() {
        try {
            List<Product> productsToExport = productTable.getItems();

            if (productsToExport == null || productsToExport.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Export Excel", "No products to export.");
                return;
            }

            ProductExcelExportService exportService = new ProductExcelExportService();
            File file = exportService.exportProducts(productsToExport);

            showAlert(Alert.AlertType.INFORMATION, "Export Excel", "Excel file generated successfully:\n" + file.getAbsolutePath());

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export Excel Error", e.getMessage());
        }
    }

    private void loadInsideContentArea(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent view = loader.load();

            AnchorPane contentArea = (AnchorPane) productTable.getScene().lookup("#contentArea");

            if (contentArea != null) {
                AnchorPane.setTopAnchor(view, 0.0);
                AnchorPane.setBottomAnchor(view, 0.0);
                AnchorPane.setLeftAnchor(view, 0.0);
                AnchorPane.setRightAnchor(view, 0.0);
                contentArea.getChildren().setAll(view);
            } else {
                System.out.println("❌ contentArea not found.");
            }

        } catch (IOException e) {
            System.out.println("❌ Error loading view: " + path);
            e.printStackTrace();
        }
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record ForecastRunResult(boolean success, String message) {
    }
}