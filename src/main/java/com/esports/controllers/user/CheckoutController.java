package com.esports.controllers.user;

import com.esports.dao.OrderDAO;
import com.esports.dao.OrderItemDAO;
import com.esports.models.CartItem;
import com.esports.models.Order;
import com.esports.models.OrderItem;
import com.esports.models.Product;
import com.esports.services.CartService;
import com.esports.services.EmailService;
import com.esports.services.InvoiceService;
import com.esports.services.OrderCreationResult;
import com.esports.services.OrderService;
import com.esports.services.PaymentResult;
import com.esports.services.RecommendationService;
import com.esports.services.StripePaymentService;
import com.esports.utils.OrderValidator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CheckoutController {

    @FXML
    private ListView<CartItem> checkoutCartListView;

    @FXML
    private FlowPane recommendationsContainer;

    @FXML
    private Label itemCountLabel;

    @FXML
    private Label subtotalLabel;

    @FXML
    private Label itemsSummaryLabel;

    @FXML
    private Label checkoutTotalLabel;

    @FXML
    private TextField firstNameField;

    @FXML
    private Label firstNameError;

    @FXML
    private TextField lastNameField;

    @FXML
    private Label lastNameError;

    @FXML
    private TextField emailField;

    @FXML
    private Label emailError;

    @FXML
    private TextField phoneField;

    @FXML
    private Label phoneError;

    @FXML
    private ComboBox<String> paymentMethodBox;

    @FXML
    private Label paymentError;

    @FXML
    private Label messageLabel;

    @FXML
    private Button confirmOnlinePaymentButton;

    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderItemDAO orderItemDAO = new OrderItemDAO();
    private final OrderService orderService = new OrderService();
    private final InvoiceService invoiceService = new InvoiceService();
    private final EmailService emailService = new EmailService();
    private final StripePaymentService paymentService = new StripePaymentService();
    private final RecommendationService recommendationService = new RecommendationService();

    private String currentPaymentSessionId;
    private String currentOnlinePaymentMethod;

    @FXML
    public void initialize() {
        paymentMethodBox.getItems().clear();
        paymentMethodBox.getItems().addAll("Cash", "Card", "PayPal");

        checkoutCartListView.setItems(CartService.getCartItems());
        checkoutCartListView.setPlaceholder(new Label("Your cart is empty."));

        configureCartList();
        refreshSummary();
        refreshRecommendations();
        clearErrors();

        messageLabel.setText("");

        if (confirmOnlinePaymentButton != null) {
            confirmOnlinePaymentButton.setDisable(true);
        }

        paymentMethodBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentPaymentSessionId = null;
            currentOnlinePaymentMethod = null;

            if (confirmOnlinePaymentButton != null) {
                confirmOnlinePaymentButton.setDisable(true);
            }
        });
    }

    private void configureCartList() {
        checkoutCartListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(CartItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || item.getProduct() == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                Product product = item.getProduct();

                ImageView productImage = new ImageView();
                productImage.setFitWidth(86);
                productImage.setFitHeight(64);
                productImage.setPreserveRatio(false);
                productImage.setSmooth(true);

                try {
                    String imagePath = product.getImage();

                    if (imagePath != null && !imagePath.trim().isEmpty()) {
                        if (imagePath.startsWith("http") || imagePath.startsWith("file:")) {
                            productImage.setImage(new Image(imagePath, true));
                        } else {
                            productImage.setImage(new Image(getClass().getResourceAsStream("/images/" + imagePath)));
                        }
                    }
                } catch (Exception e) {
                    System.out.println("⚠ Checkout image not found for product: " + product.getName());
                }

                StackPane imageBox = new StackPane(productImage);
                imageBox.setPrefSize(92, 70);
                imageBox.setMinSize(92, 70);
                imageBox.setMaxSize(92, 70);
                imageBox.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.06);" +
                                "-fx-background-radius: 16;" +
                                "-fx-border-color: rgba(99,102,241,0.25);" +
                                "-fx-border-radius: 16;" +
                                "-fx-padding: 4;"
                );

                Label productName = new Label(product.getName());
                productName.setStyle(
                        "-fx-text-fill: white;" +
                                "-fx-font-size: 16;" +
                                "-fx-font-weight: bold;"
                );

                Label categoryLabel = new Label(product.getCategory());
                categoryLabel.setStyle(
                        "-fx-text-fill: #94a3b8;" +
                                "-fx-font-size: 12;"
                );

                VBox leftInfo = new VBox(5, productName, categoryLabel);
                leftInfo.setAlignment(Pos.CENTER_LEFT);

                Label qtyBadge = new Label("x" + item.getQuantity());
                qtyBadge.setStyle(
                        "-fx-background-color: rgba(99,102,241,0.25);" +
                                "-fx-text-fill: #c7d2fe;" +
                                "-fx-padding: 6 10;" +
                                "-fx-background-radius: 999;" +
                                "-fx-font-weight: bold;"
                );

                Label unitPrice = new Label(String.format("Unit: %.2f TND", product.getPrice()));
                unitPrice.setStyle(
                        "-fx-text-fill: #cbd5e1;" +
                                "-fx-font-size: 12;"
                );

                Label subtotal = new Label(String.format("%.2f TND", item.getSubtotal()));
                subtotal.setStyle(
                        "-fx-text-fill: #22c55e;" +
                                "-fx-font-size: 15;" +
                                "-fx-font-weight: bold;"
                );

                VBox rightInfo = new VBox(6, qtyBadge, unitPrice, subtotal);
                rightInfo.setAlignment(Pos.CENTER_RIGHT);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row = new HBox(16, imageBox, leftInfo, spacer, rightInfo);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(14));
                row.setStyle(
                        "-fx-background-color: linear-gradient(to right, rgba(15,23,42,0.96), rgba(30,41,59,0.92));" +
                                "-fx-background-radius: 18;" +
                                "-fx-border-color: rgba(99,102,241,0.22);" +
                                "-fx-border-radius: 18;"
                );

                setText(null);
                setGraphic(row);
                setStyle("-fx-background-color: transparent; -fx-padding: 6 0 6 0;");
            }
        });
    }

    private void refreshSummary() {
        double total = CartService.getTotal();
        int totalQuantity = CartService.getItemCount();
        int itemLines = CartService.getCartItems().size();

        subtotalLabel.setText(String.format("%.2f TND", total));
        checkoutTotalLabel.setText(String.format("%.2f TND", total));
        itemsSummaryLabel.setText(String.valueOf(totalQuantity));
        itemCountLabel.setText(itemLines + " item line(s) • " + totalQuantity + " unit(s)");

        checkoutCartListView.refresh();
    }

    private void refreshRecommendations() {
        recommendationsContainer.getChildren().clear();

        List<Product> recommendedProducts =
                recommendationService.getRecommendationsForCart(new ArrayList<>(CartService.getCartItems()), 4);

        if (recommendedProducts == null || recommendedProducts.isEmpty()) {
            Label empty = new Label("Add products to cart to see ML recommendations.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");
            recommendationsContainer.getChildren().add(empty);
            return;
        }

        for (Product product : recommendedProducts) {
            recommendationsContainer.getChildren().add(createRecommendationCard(product));
        }
    }

    private VBox createRecommendationCard(Product product) {
        VBox card = new VBox(8);
        card.setPrefWidth(175);
        card.setPadding(new Insets(12));
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #10172f, #171b34);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: rgba(139,92,246,0.35);" +
                        "-fx-border-radius: 18;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0, 0, 4);"
        );

        ImageView imageView = new ImageView();
        imageView.setFitWidth(150);
        imageView.setFitHeight(82);
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
            System.out.println("⚠ Recommendation image not found for product: " + product.getName());
        }

        Label nameLabel = new Label(product.getName());
        nameLabel.setWrapText(true);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");

        Label categoryLabel = new Label(product.getCategory());
        categoryLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11;");

        Label priceLabel = new Label(String.format("%.2f TND", product.getPrice()));
        priceLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 13; -fx-font-weight: bold;");

        Label scoreLabel = new Label("ML Score: " + formatScore(product.getRecommendationScore()));
        scoreLabel.setStyle(
                "-fx-background-color: rgba(139,92,246,0.20);" +
                        "-fx-text-fill: #c4b5fd;" +
                        "-fx-background-radius: 999;" +
                        "-fx-padding: 5 9;" +
                        "-fx-font-size: 11;" +
                        "-fx-font-weight: bold;"
        );

        Button addButton = new Button("Add");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setDisable(product.getStock() <= 0);
        addButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #8b5cf6, #6366f1);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 8;" +
                        "-fx-cursor: hand;"
        );

        addButton.setOnAction(event -> addRecommendedProductToCart(product));

        card.getChildren().addAll(
                imageView,
                nameLabel,
                categoryLabel,
                priceLabel,
                scoreLabel,
                addButton
        );

        return card;
    }

    private String formatScore(double score) {
        double cleanScore = score;

        if (cleanScore <= 1.0) {
            cleanScore = cleanScore * 100;
        }

        if (cleanScore > 100) {
            cleanScore = 100;
        }

        if (cleanScore < 0) {
            cleanScore = 0;
        }

        return String.format("%.0f%%", cleanScore);
    }

    private void addRecommendedProductToCart(Product product) {
        if (product == null) {
            showMessage("#f87171", "Invalid recommended product.");
            return;
        }

        int currentQuantity = CartService.getQuantityForProduct(product.getId());

        if (currentQuantity + 1 > product.getStock()) {
            showMessage("#f87171", "Cannot add more than available stock for " + product.getName());
            return;
        }

        CartService.addToCart(product, 1);

        refreshSummary();
        refreshRecommendations();

        showMessage("#86efac", product.getName() + " added from ML recommendations.");
    }

    @FXML
    public void handleRemoveSelected() {
        CartItem selected = checkoutCartListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showMessage("#f59e0b", "Please select an item to remove.");
            return;
        }

        CartService.removeItem(selected);
        refreshSummary();
        refreshRecommendations();
        showMessage("#fca5a5", "Selected item removed from cart.");
    }

    @FXML
    public void handleClearCart() {
        CartService.clearCart();

        currentPaymentSessionId = null;
        currentOnlinePaymentMethod = null;

        if (confirmOnlinePaymentButton != null) {
            confirmOnlinePaymentButton.setDisable(true);
        }

        refreshSummary();
        refreshRecommendations();
        showMessage("#fca5a5", "Cart cleared.");
    }

    @FXML
    public void handlePayOnline() {
        clearErrors();
        messageLabel.setText("");

        if (CartService.isEmpty()) {
            showMessage("#f87171", "Your cart is empty.");
            return;
        }

        String paymentMethod = paymentMethodBox.getValue();

        if (!isOnlinePayment(paymentMethod)) {
            paymentError.setText("Choose Card or PayPal.");
            return;
        }

        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();

        if (!validateOrderForm(firstName, lastName, email, phone, paymentMethod)) {
            return;
        }

        PaymentResult paymentResult =
                paymentService.createCheckoutSession(new ArrayList<>(CartService.getCartItems()), email, paymentMethod);

        if (!paymentResult.isSuccess()) {
            showMessage("#f87171", paymentResult.getMessage());
            return;
        }

        currentPaymentSessionId = paymentResult.getSessionId();
        currentOnlinePaymentMethod = paymentMethod;

        openBrowser(paymentResult.getCheckoutUrl());

        if (confirmOnlinePaymentButton != null) {
            confirmOnlinePaymentButton.setDisable(false);
        }

        showMessage("#93c5fd", "Secure payment opened. Complete payment in browser, then click Confirm Payment.");
    }

    @FXML
    public void handleConfirmOnlinePayment() {
        clearErrors();
        messageLabel.setText("");

        if (currentPaymentSessionId == null || currentPaymentSessionId.trim().isEmpty()) {
            showMessage("#f59e0b", "No payment session found. Click Pay Online first.");
            return;
        }

        boolean paid = paymentService.isPaymentPaid(currentPaymentSessionId);

        if (!paid) {
            showMessage("#f59e0b", "Payment not completed yet. Finish payment in browser and try again.");
            return;
        }

        if (currentOnlinePaymentMethod != null) {
            paymentMethodBox.setValue(currentOnlinePaymentMethod);
        }

        if (confirmOnlinePaymentButton != null) {
            confirmOnlinePaymentButton.setDisable(true);
        }

        showMessage("#86efac", currentOnlinePaymentMethod + " payment confirmed. Creating order...");
        placeOrder("PAID", "CONFIRMED");
    }

    @FXML
    public void handlePlaceOrder() {
        clearErrors();
        messageLabel.setText("");

        if (CartService.isEmpty()) {
            showMessage("#f87171", "Your cart is empty.");
            return;
        }

        String paymentMethod = paymentMethodBox.getValue();

        if (isOnlinePayment(paymentMethod)) {
            handlePayOnline();
            return;
        }

        placeOrder("PENDING", "NEW");
    }

    private void placeOrder(String paymentStatus, String orderStatus) {
        if (CartService.isEmpty()) {
            showMessage("#f87171", "Your cart is empty.");
            return;
        }

        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String paymentMethod = paymentMethodBox.getValue();

        if (!validateOrderForm(firstName, lastName, email, phone, paymentMethod)) {
            return;
        }

        Order order = new Order(
                LocalDateTime.now(),
                email,
                firstName,
                lastName,
                phone,
                paymentMethod,
                paymentStatus,
                null,
                orderStatus,
                0.0
        );

        OrderCreationResult result =
                orderService.placeOrder(order, new ArrayList<>(CartService.getCartItems()));

        if (!result.isSuccess()) {
            showMessage("#f87171", result.getMessage());
            return;
        }

        try {
            Order savedOrder = orderDAO.getOrderById(result.getOrderId());
            List<OrderItem> savedItems = orderItemDAO.getItemsByOrderId(result.getOrderId());

            File invoice = invoiceService.generateInvoice(savedOrder, savedItems);
            boolean emailSent = emailService.sendInvoice(savedOrder, invoice);

            if (emailSent) {
                showMessage("#86efac", "Order confirmed. Ref: " + result.getReference() + ". Invoice sent by email.");
            } else {
                showMessage("#fde68a", "Order confirmed. Ref: " + result.getReference() + ". Invoice generated, email not sent.");
            }

        } catch (Exception e) {
            showMessage("#facc15", "Order created. Ref: " + result.getReference() + ". Invoice error: " + e.getMessage());
        }

        CartService.clearCart();
        currentPaymentSessionId = null;
        currentOnlinePaymentMethod = null;

        if (confirmOnlinePaymentButton != null) {
            confirmOnlinePaymentButton.setDisable(true);
        }

        refreshSummary();
        refreshRecommendations();
        clearForm();
    }

    private boolean validateOrderForm(String firstName, String lastName, String email, String phone, String paymentMethod) {
        String firstNameMsg = OrderValidator.validateFirstName(firstName);
        String lastNameMsg = OrderValidator.validateLastName(lastName);
        String emailMsg = OrderValidator.validateEmail(email);
        String phoneMsg = OrderValidator.validatePhone(phone);
        String paymentMsg = OrderValidator.validatePaymentMethod(paymentMethod);

        boolean isValid = true;

        if (!firstNameMsg.isEmpty()) {
            firstNameError.setText(firstNameMsg);
            isValid = false;
        }

        if (!lastNameMsg.isEmpty()) {
            lastNameError.setText(lastNameMsg);
            isValid = false;
        }

        if (!emailMsg.isEmpty()) {
            emailError.setText(emailMsg);
            isValid = false;
        }

        if (!phoneMsg.isEmpty()) {
            phoneError.setText(phoneMsg);
            isValid = false;
        }

        if (!paymentMsg.isEmpty()) {
            paymentError.setText(paymentMsg);
            isValid = false;
        }

        return isValid;
    }

    @FXML
    public void handleBackToShop() {
        loadInsideContentArea("/views/user/shop-browse.fxml");
    }

    private void openBrowser(String url) {
        try {
            if (url == null || url.trim().isEmpty()) {
                showMessage("#f87171", "Payment URL is empty.");
                return;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                showMessage("#f59e0b", "Open this payment URL manually: " + url);
            }

        } catch (Exception e) {
            showMessage("#f87171", "Cannot open browser: " + e.getMessage());
        }
    }

    private void loadInsideContentArea(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent view = loader.load();

            AnchorPane contentArea = (AnchorPane) checkoutCartListView.getScene().lookup("#contentArea");

            if (contentArea != null) {
                AnchorPane.setTopAnchor(view, 0.0);
                AnchorPane.setBottomAnchor(view, 0.0);
                AnchorPane.setLeftAnchor(view, 0.0);
                AnchorPane.setRightAnchor(view, 0.0);
                contentArea.getChildren().setAll(view);
            } else {
                checkoutCartListView.getScene().setRoot(view);
            }

        } catch (IOException e) {
            System.out.println("❌ Error while loading view: " + path + " - " + e.getMessage());
        }
    }

    private boolean isOnlinePayment(String paymentMethod) {
        return "Card".equalsIgnoreCase(paymentMethod)
                || "PayPal".equalsIgnoreCase(paymentMethod);
    }

    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();
        paymentMethodBox.setValue(null);
        clearErrors();
    }

    private void clearErrors() {
        firstNameError.setText("");
        lastNameError.setText("");
        emailError.setText("");
        phoneError.setText("");
        paymentError.setText("");
    }

    private void showMessage(String color, String text) {
        messageLabel.setStyle(
                "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 12;" +
                        "-fx-font-weight: bold;"
        );
        messageLabel.setText(text);
    }
}