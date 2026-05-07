package com.esports.controllers.user;

import com.esports.dao.ProductDAO;
import com.esports.models.CartItem;
import com.esports.models.Product;
import com.esports.services.CartService;
import com.esports.services.ProductReviewApiService;
import com.esports.services.RecommendationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShopBrowseController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> categoryFilterBox;

    @FXML
    private ComboBox<String> sortBox;

    @FXML
    private FlowPane productsContainer;

    @FXML
    private FlowPane recommendationsContainer;

    @FXML
    private ListView<CartItem> cartListView;

    @FXML
    private Label cartTotalLabel;

    @FXML
    private Label cartCountLabel;

    @FXML
    private Label messageLabel;

    private final ProductDAO productDAO = new ProductDAO();
    private final ProductReviewApiService productReviewApiService = new ProductReviewApiService();
    private final RecommendationService recommendationService = new RecommendationService();

    private final Map<Integer, ProductReviewApiService.RatingSummary> ratingCache = new HashMap<>();

    private List<Product> allProducts = new ArrayList<>();

    @FXML
    public void initialize() {
        setupFilters();

        cartListView.setItems(CartService.getCartItems());

        loadProducts();
        refreshCartSummary();
        refreshRecommendations();

        messageLabel.setText("");
    }

    private void setupFilters() {
        categoryFilterBox.getItems().clear();
        categoryFilterBox.getItems().addAll(
                "All",
                "Accessories",
                "Games",
                "Consoles",
                "Headsets",
                "Controllers",
                "PC Items",
                "Gaming Chairs",
                "Keyboards",
                "Mouses"
        );
        categoryFilterBox.setValue("All");

        sortBox.getItems().clear();
        sortBox.getItems().addAll(
                "Newest",
                "Name A-Z",
                "Price Low to High",
                "Price High to Low",
                "Most Ordered",
                "Stock High to Low",
                "ML Predicted Sales",
                "Best Rated"
        );
        sortBox.setValue("Newest");
    }

    private void loadProducts() {
        ratingCache.clear();
        allProducts = productDAO.getAllProducts();
        renderProducts(allProducts);
    }

    private void renderProducts(List<Product> products) {
        productsContainer.getChildren().clear();

        if (products == null || products.isEmpty()) {
            Label emptyLabel = new Label("No products found.");
            emptyLabel.setStyle("-fx-text-fill: #c7d2fe; -fx-font-size: 14; -fx-font-weight: bold;");
            productsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Product product : products) {
            VBox card = createProductCard(product, false);
            productsContainer.getChildren().add(card);
        }
    }

    private VBox createProductCard(Product product, boolean smallCard) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));

        if (smallCard) {
            card.setPrefWidth(190);
            card.setMinHeight(260);
        } else {
            card.setPrefWidth(235);
            card.setMinHeight(430);
        }

        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #171b34, #101326);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #2f365f;" +
                        "-fx-border-radius: 18;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0, 0, 4);"
        );

        ImageView imageView = new ImageView();
        imageView.setFitWidth(smallCard ? 160 : 205);
        imageView.setFitHeight(smallCard ? 90 : 125);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);

        try {
            String imagePath = product.getImage();

            if (imagePath != null && !imagePath.trim().isEmpty()) {
                if (imagePath.startsWith("http") || imagePath.startsWith("file:")) {
                    imageView.setImage(new Image(imagePath, true));
                } else {
                    imageView.setImage(new Image(getClass().getResourceAsStream("/images/" + imagePath)));
                }
            }
        } catch (Exception e) {
            System.out.println("⚠ Image not found for product: " + product.getName());
        }

        Label nameLabel = new Label(product.getName());
        nameLabel.setWrapText(true);
        nameLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");

        Label categoryLabel = new Label(product.getCategory());
        categoryLabel.setStyle(
                "-fx-background-color: #2d335d;" +
                        "-fx-background-radius: 999;" +
                        "-fx-padding: 4 9;" +
                        "-fx-text-fill: #d7dcff;" +
                        "-fx-font-size: 11;"
        );

        Label priceLabel = new Label(String.format("%.2f TND", product.getPrice()));
        priceLabel.setStyle("-fx-text-fill: #7CFFB2; -fx-font-size: 15; -fx-font-weight: bold;");

        Label ratingLabel = new Label(buildRatingText(product));
        ratingLabel.setStyle(
                "-fx-background-color: rgba(250,204,21,0.13);" +
                        "-fx-background-radius: 999;" +
                        "-fx-padding: 5 9;" +
                        "-fx-text-fill: #fde68a;" +
                        "-fx-font-size: 11;" +
                        "-fx-font-weight: bold;"
        );

        Label stockLabel = new Label("Stock: " + product.getStock());
        stockLabel.setStyle("-fx-text-fill: #c8cbe8;");

        Label ordersLabel = new Label("Ordered: " + product.getOrdersCount());
        ordersLabel.setStyle("-fx-text-fill: #a8accf; -fx-font-size: 11;");

        Label mlLabel = new Label("ML sales: " + String.format("%.2f", product.getPredictedQty()));
        mlLabel.setStyle(
                "-fx-background-color: rgba(139, 92, 246, 0.18);" +
                        "-fx-background-radius: 999;" +
                        "-fx-padding: 4 9;" +
                        "-fx-text-fill: #c4b5fd;" +
                        "-fx-font-size: 11;" +
                        "-fx-font-weight: bold;"
        );

        Label descLabel = new Label(product.getDescription());
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(smallCard ? 35 : 55);
        descLabel.setStyle("-fx-text-fill: #d8d8e8; -fx-font-size: 12;");

        Spinner<Integer> qtySpinner = new Spinner<>(1, Math.max(1, product.getStock()), 1);
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(78);

        Button addBtn = new Button(product.getStock() > 0 ? "Add" : "Out");
        addBtn.setDisable(product.getStock() <= 0);
        addBtn.setStyle(
                "-fx-background-color: #8e44ff;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 7 14;" +
                        "-fx-cursor: hand;"
        );

        addBtn.setOnAction(e -> addToCart(product, qtySpinner.getValue()));

        HBox actionRow = new HBox(8, qtySpinner, addBtn);
        actionRow.setStyle("-fx-alignment: center-left;");

        Button reviewBtn = new Button("⭐ Review");
        reviewBtn.setMaxWidth(Double.MAX_VALUE);
        reviewBtn.setStyle(
                "-fx-background-color: rgba(250,204,21,0.16);" +
                        "-fx-border-color: rgba(250,204,21,0.35);" +
                        "-fx-text-fill: #fde68a;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-padding: 7 14;" +
                        "-fx-cursor: hand;"
        );

        reviewBtn.setOnAction(event -> openReviewDialog(product));

        Button viewReviewsBtn = new Button("💬 View Reviews");
        viewReviewsBtn.setMaxWidth(Double.MAX_VALUE);
        viewReviewsBtn.setStyle(
                "-fx-background-color: rgba(99,102,241,0.16);" +
                        "-fx-border-color: rgba(99,102,241,0.35);" +
                        "-fx-text-fill: #c7d2fe;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-padding: 7 14;" +
                        "-fx-cursor: hand;"
        );

        viewReviewsBtn.setOnAction(event -> openReviewsListDialog(product));

        if (smallCard) {
            card.getChildren().addAll(
                    imageView,
                    nameLabel,
                    categoryLabel,
                    priceLabel,
                    ratingLabel,
                    actionRow
            );
        } else {
            card.getChildren().addAll(
                    imageView,
                    nameLabel,
                    categoryLabel,
                    priceLabel,
                    ratingLabel,
                    stockLabel,
                    ordersLabel,
                    mlLabel,
                    descLabel,
                    actionRow,
                    reviewBtn,
                    viewReviewsBtn
            );
        }

        return card;
    }

    private String buildRatingText(Product product) {
        ProductReviewApiService.RatingSummary summary = getRatingSummary(product.getId());

        if (summary.getReviewCount() <= 0) {
            return "☆ No reviews yet";
        }

        return buildStars(summary.getAverageRating())
                + " "
                + String.format("%.1f", summary.getAverageRating())
                + "/5 ("
                + summary.getReviewCount()
                + ")";
    }

    private ProductReviewApiService.RatingSummary getRatingSummary(int productId) {
        if (ratingCache.containsKey(productId)) {
            return ratingCache.get(productId);
        }

        ProductReviewApiService.RatingSummary summary = productReviewApiService.getRatingSummary(productId);
        ratingCache.put(productId, summary);

        return summary;
    }

    private String buildStars(double average) {
        int rounded = (int) Math.round(average);
        StringBuilder stars = new StringBuilder();

        for (int i = 1; i <= 5; i++) {
            if (i <= rounded) {
                stars.append("★");
            } else {
                stars.append("☆");
            }
        }

        return stars.toString();
    }

    private void openReviewDialog(Product product) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Leave a Review");
        dialog.setHeaderText("Review product: " + product.getName());

        ButtonType submitButtonType = new ButtonType("Submit Review", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, cancelButtonType);

        TextField customerNameField = new TextField();
        customerNameField.setPromptText("Your name");

        ComboBox<Integer> ratingBox = new ComboBox<>();
        ratingBox.getItems().addAll(1, 2, 3, 4, 5);
        ratingBox.setPromptText("Rating");
        ratingBox.setPrefWidth(160);

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Write your review...");
        commentArea.setWrapText(true);
        commentArea.setPrefHeight(120);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12; -fx-font-weight: bold;");

        VBox content = new VBox(10);
        content.setPadding(new Insets(12));
        content.getChildren().addAll(
                new Label("Your name"),
                customerNameField,
                new Label("Rating"),
                ratingBox,
                new Label("Comment"),
                commentArea,
                errorLabel
        );

        dialog.getDialogPane().setContent(content);

        Node submitButton = dialog.getDialogPane().lookupButton(submitButtonType);

        submitButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String customerName = customerNameField.getText();
            Integer rating = ratingBox.getValue();
            String comment = commentArea.getText();

            String validationMessage = validateReviewForm(customerName, rating, comment);

            if (!validationMessage.isEmpty()) {
                errorLabel.setText(validationMessage);
                event.consume();
                return;
            }

            ProductReviewApiService.ReviewResult result =
                    productReviewApiService.addReview(
                            product.getId(),
                            customerName.trim(),
                            rating,
                            comment.trim()
                    );

            if (result.isSuccess()) {
                ratingCache.remove(product.getId());
                showMessage("#86efac", "Review added successfully using Review API.");
                loadProducts();
            } else {
                errorLabel.setText(result.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    private void openReviewsListDialog(Product product) {
        List<ProductReviewApiService.ApiReview> reviews =
                productReviewApiService.getReviewsByProduct(product.getId());

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Product Reviews");
        dialog.setHeaderText("Reviews for: " + product.getName());

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(12);
        content.setPadding(new Insets(14));
        content.setPrefWidth(520);

        if (reviews.isEmpty()) {
            Label emptyLabel = new Label("No comments yet for this product.");
            emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13;");
            content.getChildren().add(emptyLabel);
        } else {
            for (ProductReviewApiService.ApiReview review : reviews) {
                Label starsLabel = new Label(review.getStars() + "  " + review.getRating() + "/5");
                starsLabel.setStyle("-fx-text-fill: #facc15; -fx-font-weight: bold; -fx-font-size: 14;");

                Label customerLabel = new Label("By " + review.getCustomerName() + " • " + review.getCreatedAt());
                customerLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");

                Label commentLabel = new Label(review.getComment());
                commentLabel.setWrapText(true);
                commentLabel.setStyle("-fx-text-fill: #dbeafe; -fx-font-size: 13;");

                VBox reviewCard = new VBox(5, starsLabel, customerLabel, commentLabel);
                reviewCard.setStyle(
                        "-fx-background-color: #0f172a;" +
                                "-fx-background-radius: 14;" +
                                "-fx-border-color: #2b315d;" +
                                "-fx-border-radius: 14;" +
                                "-fx-padding: 12;"
                );

                content.getChildren().add(reviewCard);
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(420);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.showAndWait();
    }

    private String validateReviewForm(String customerName, Integer rating, String comment) {
        if (customerName == null || customerName.trim().isEmpty()) {
            return "Name is required.";
        }

        if (customerName.trim().length() < 2) {
            return "Name must contain at least 2 characters.";
        }

        if (rating == null) {
            return "Rating is required.";
        }

        if (rating < 1 || rating > 5) {
            return "Rating must be between 1 and 5.";
        }

        if (comment == null || comment.trim().isEmpty()) {
            return "Comment is required.";
        }

        if (comment.trim().length() < 5) {
            return "Comment must contain at least 5 characters.";
        }

        return "";
    }

    private void addToCart(Product product, int quantity) {
        if (product == null || quantity <= 0) {
            showMessage("#ff5c7a", "Invalid product quantity.");
            return;
        }

        int currentCartQuantity = CartService.getQuantityForProduct(product.getId());
        int newTotalQuantity = currentCartQuantity + quantity;

        if (newTotalQuantity > product.getStock()) {
            showMessage("#ff5c7a", "Cannot add more than available stock for " + product.getName());
            return;
        }

        CartService.addToCart(product, quantity);

        refreshCartSummary();
        refreshRecommendations();

        showMessage("#2ecc71", product.getName() + " added to cart.");
    }

    @FXML
    public void handleSearch() {
        applyFilters();
    }

    @FXML
    public void handleRefresh() {
        searchField.clear();
        categoryFilterBox.setValue("All");
        sortBox.setValue("Newest");

        loadProducts();
        refreshCartSummary();
        refreshRecommendations();

        messageLabel.setText("");
    }

    @FXML
    public void handleCategoryFilter() {
        applyFilters();
    }

    @FXML
    public void handleSort() {
        applyFilters();
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String category = categoryFilterBox.getValue();
        String sort = sortBox.getValue();

        List<Product> filtered = allProducts.stream()
                .filter(product -> keyword.isEmpty()
                        || safeLower(product.getName()).contains(keyword)
                        || safeLower(product.getCategory()).contains(keyword)
                        || safeLower(product.getDescription()).contains(keyword))
                .filter(product -> category == null
                        || category.equals("All")
                        || safeLower(product.getCategory()).equals(category.toLowerCase()))
                .collect(Collectors.toList());

        if ("Name A-Z".equals(sort)) {
            filtered.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
        } else if ("Price Low to High".equals(sort)) {
            filtered.sort(Comparator.comparingDouble(Product::getPrice));
        } else if ("Price High to Low".equals(sort)) {
            filtered.sort(Comparator.comparingDouble(Product::getPrice).reversed());
        } else if ("Most Ordered".equals(sort)) {
            filtered.sort(Comparator.comparingInt(Product::getOrdersCount).reversed());
        } else if ("Stock High to Low".equals(sort)) {
            filtered.sort(Comparator.comparingInt(Product::getStock).reversed());
        } else if ("ML Predicted Sales".equals(sort)) {
            filtered.sort(Comparator.comparingDouble(Product::getPredictedQty).reversed());
        } else if ("Best Rated".equals(sort)) {
            filtered.sort(Comparator.comparingDouble((Product product) ->
                    getRatingSummary(product.getId()).getAverageRating()
            ).reversed());
        } else {
            filtered.sort(Comparator.comparingInt(Product::getId).reversed());
        }

        renderProducts(filtered);
    }

    @FXML
    public void handleRemoveSelected() {
        CartItem selected = cartListView.getSelectionModel().getSelectedItem();

        if (selected != null) {
            CartService.removeItem(selected);
            refreshCartSummary();
            refreshRecommendations();
        }
    }

    @FXML
    public void handleClearCart() {
        CartService.clearCart();
        refreshCartSummary();
        refreshRecommendations();
        messageLabel.setText("");
    }

    @FXML
    public void handleGoToCheckout() {
        if (CartService.isEmpty()) {
            showMessage("#ff5c7a", "Your cart is empty.");
            return;
        }

        loadInsideContentArea("/views/user/checkout.fxml");
    }

    private void refreshCartSummary() {
        cartListView.refresh();
        cartTotalLabel.setText(String.format("Total: %.2f TND", CartService.getTotal()));
        cartCountLabel.setText("Items: " + CartService.getItemCount());
    }

    private void refreshRecommendations() {
        recommendationsContainer.getChildren().clear();

        List<Product> recommendedProducts =
                recommendationService.getRecommendationsForCart(new ArrayList<>(CartService.getCartItems()), 4);

        if (recommendedProducts.isEmpty()) {
            Label empty = new Label("No recommendations yet.");
            empty.setStyle("-fx-text-fill: #8b8fae;");
            recommendationsContainer.getChildren().add(empty);
            return;
        }

        for (Product product : recommendedProducts) {
            recommendationsContainer.getChildren().add(createProductCard(product, true));
        }
    }

    private void loadInsideContentArea(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent view = loader.load();

            AnchorPane contentArea = (AnchorPane) productsContainer.getScene().lookup("#contentArea");

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
            System.out.println("❌ Error loading view: " + path + " - " + e.getMessage());
        }
    }

    private void showMessage(String color, String text) {
        messageLabel.setStyle(
                "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-font-weight: bold;"
        );
        messageLabel.setText(text);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}