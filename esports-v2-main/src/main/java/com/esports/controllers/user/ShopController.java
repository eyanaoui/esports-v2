package com.esports.controllers.user;

import com.esports.dao.OrderDAO;
import com.esports.dao.OrderItemDAO;
import com.esports.dao.ProductDAO;
import com.esports.models.CartItem;
import com.esports.models.Order;
import com.esports.models.OrderItem;
import com.esports.models.Product;
import com.esports.services.EmailService;
import com.esports.services.InvoiceService;
import com.esports.services.OrderCreationResult;
import com.esports.services.OrderService;
import com.esports.services.PaymentResult;
import com.esports.services.StripePaymentService;
import com.esports.utils.OrderValidator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ShopController {

    @FXML
    private FlowPane productsContainer;

    @FXML
    private TextField searchField;

    @FXML
    private ListView<CartItem> cartListView;

    @FXML
    private Label cartTotalLabel;

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
    private Label orderMessageLabel;

    @FXML
    private Button payOnlineButton;

    @FXML
    private Button confirmOnlinePaymentButton;

    private final ProductDAO productDAO = new ProductDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderItemDAO orderItemDAO = new OrderItemDAO();
    private final OrderService orderService = new OrderService();
    private final InvoiceService invoiceService = new InvoiceService();
    private final EmailService emailService = new EmailService();
    private final StripePaymentService paymentService = new StripePaymentService();

    private List<Product> allProducts = new ArrayList<>();
    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();

    private String currentPaymentSessionId = null;
    private String currentOnlinePaymentMethod = null;

    @FXML
    public void initialize() {
        paymentMethodBox.getItems().clear();
        paymentMethodBox.getItems().addAll("Cash", "Card", "PayPal");

        cartListView.setItems(cartItems);

        loadProducts();
        refreshCartView();
        clearErrors();

        orderMessageLabel.setText("");

        if (confirmOnlinePaymentButton != null) {
            confirmOnlinePaymentButton.setDisable(true);
        }
    }

    private void loadProducts() {
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
            VBox card = createProductCard(product);
            productsContainer.getChildren().add(card);
        }
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(10);
        card.setPrefWidth(220);
        card.setPadding(new Insets(12));
        card.setStyle(
                "-fx-background-color: #161a30;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #2b2f55;" +
                        "-fx-border-radius: 12;"
        );

        ImageView imageView = new ImageView();
        imageView.setFitWidth(190);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(false);

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
        categoryLabel.setStyle("-fx-text-fill: #b8b8d1;");

        Label priceLabel = new Label("Price: " + String.format("%.2f TND", product.getPrice()));
        priceLabel.setStyle("-fx-text-fill: white;");

        Label stockLabel = new Label("Stock: " + product.getStock());
        stockLabel.setStyle("-fx-text-fill: white;");

        Label mlLabel = new Label("ML sales: " + String.format("%.2f", product.getPredictedQty()));
        mlLabel.setStyle("-fx-text-fill: #c4b5fd; -fx-font-size: 11; -fx-font-weight: bold;");

        Label descLabel = new Label(product.getDescription());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #d8d8e8;");

        int maxStock = Math.max(1, product.getStock());
        Spinner<Integer> qtySpinner = new Spinner<>(1, maxStock, 1);
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(90);

        Button addToCartBtn = new Button(product.getStock() > 0 ? "Add to Cart" : "Out of Stock");
        addToCartBtn.setDisable(product.getStock() <= 0);
        addToCartBtn.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");

        addToCartBtn.setOnAction(e -> handleAddToCart(product, qtySpinner.getValue()));

        HBox bottomRow = new HBox(10, qtySpinner, addToCartBtn);

        card.getChildren().addAll(
                imageView,
                nameLabel,
                categoryLabel,
                priceLabel,
                stockLabel,
                mlLabel,
                descLabel,
                bottomRow
        );

        return card;
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        if (keyword.isEmpty()) {
            renderProducts(allProducts);
            return;
        }

        List<Product> filtered = allProducts.stream()
                .filter(product ->
                        safeLower(product.getName()).contains(keyword)
                                || safeLower(product.getCategory()).contains(keyword)
                                || safeLower(product.getDescription()).contains(keyword))
                .collect(Collectors.toList());

        renderProducts(filtered);
    }

    @FXML
    public void handleRefresh() {
        searchField.clear();
        loadProducts();
    }

    private void handleAddToCart(Product product, int quantity) {
        if (product == null || quantity <= 0) {
            return;
        }

        CartItem existing = null;

        for (CartItem item : cartItems) {
            if (item.getProduct().getId() == product.getId()) {
                existing = item;
                break;
            }
        }

        int cartQuantity = quantity;

        if (existing != null) {
            cartQuantity += existing.getQuantity();
        }

        if (cartQuantity > product.getStock()) {
            orderMessageLabel.setStyle("-fx-text-fill: red;");
            orderMessageLabel.setText("Cannot add more than available stock for " + product.getName());
            return;
        }

        if (existing != null) {
            existing.increaseQuantity(quantity);
        } else {
            cartItems.add(new CartItem(product, quantity));
        }

        refreshCartView();

        orderMessageLabel.setStyle("-fx-text-fill: green;");
        orderMessageLabel.setText(product.getName() + " added to cart.");
    }

    @FXML
    public void handleRemoveSelected() {
        CartItem selected = cartListView.getSelectionModel().getSelectedItem();

        if (selected != null) {
            cartItems.remove(selected);
            refreshCartView();
        }
    }

    @FXML
    public void handleClearCart() {
        cartItems.clear();
        currentPaymentSessionId = null;
        currentOnlinePaymentMethod = null;

        if (confirmOnlinePaymentButton != null) {
            confirmOnlinePaymentButton.setDisable(true);
        }

        refreshCartView();
    }

    private void refreshCartView() {
        cartListView.refresh();

        double total = cartItems.stream()
                .mapToDouble(CartItem::getSubtotal)
                .sum();

        cartTotalLabel.setText("Cart Total: " + String.format("%.2f TND", total));
    }

    @FXML
    public void handlePayOnline() {
        clearErrors();
        orderMessageLabel.setText("");

        if (cartItems.isEmpty()) {
            orderMessageLabel.setStyle("-fx-text-fill: red;");
            orderMessageLabel.setText("Your cart is empty.");
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

        boolean isValid = validateOrderForm(firstName, lastName, email, phone, paymentMethod);

        if (!isValid) {
            return;
        }

        PaymentResult result = paymentService.createCheckoutSession(new ArrayList<>(cartItems), email, paymentMethod);

        if (!result.isSuccess()) {
            orderMessageLabel.setStyle("-fx-text-fill: red;");
            orderMessageLabel.setText(result.getMessage());
            return;
        }

        currentPaymentSessionId = result.getSessionId();
        currentOnlinePaymentMethod = paymentMethod;

        openBrowser(result.getCheckoutUrl());

        if (confirmOnlinePaymentButton != null) {
            confirmOnlinePaymentButton.setDisable(false);
        }

        orderMessageLabel.setStyle("-fx-text-fill: green;");
        orderMessageLabel.setText("Secure payment opened. Pay in browser, then click 'Confirm Payment'.");
    }

    @FXML
    public void handleConfirmOnlinePayment() {
        clearErrors();
        orderMessageLabel.setText("");

        if (currentPaymentSessionId == null || currentPaymentSessionId.trim().isEmpty()) {
            orderMessageLabel.setStyle("-fx-text-fill: red;");
            orderMessageLabel.setText("No payment session found. Click 'Pay Online' first.");
            return;
        }

        boolean paid = paymentService.isPaymentPaid(currentPaymentSessionId);

        if (!paid) {
            orderMessageLabel.setStyle("-fx-text-fill: orange;");
            orderMessageLabel.setText("Payment is not completed yet. Finish payment in browser, then try again.");
            return;
        }

        if (currentOnlinePaymentMethod != null) {
            paymentMethodBox.setValue(currentOnlinePaymentMethod);
        }

        placeOrderAfterSuccessfulPayment("PAID", "CONFIRMED");
    }

    @FXML
    public void handlePlaceOrder() {
        clearErrors();
        orderMessageLabel.setText("");

        String paymentMethod = paymentMethodBox.getValue();

        if (isOnlinePayment(paymentMethod)) {
            handlePayOnline();
            return;
        }

        placeOrderAfterSuccessfulPayment("PENDING", "NEW");
    }

    private void placeOrderAfterSuccessfulPayment(String paymentStatus, String orderStatus) {
        clearErrors();
        orderMessageLabel.setText("");

        if (cartItems.isEmpty()) {
            orderMessageLabel.setStyle("-fx-text-fill: red;");
            orderMessageLabel.setText("Your cart is empty.");
            return;
        }

        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String paymentMethod = paymentMethodBox.getValue();

        boolean isValid = validateOrderForm(firstName, lastName, email, phone, paymentMethod);

        if (!isValid) {
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

        OrderCreationResult result = orderService.placeOrder(order, new ArrayList<>(cartItems));

        if (!result.isSuccess()) {
            orderMessageLabel.setStyle("-fx-text-fill: red;");
            orderMessageLabel.setText(result.getMessage());
            return;
        }

        try {
            Order savedOrder = orderDAO.getOrderById(result.getOrderId());
            List<OrderItem> savedItems = orderItemDAO.getItemsByOrderId(result.getOrderId());

            File invoice = invoiceService.generateInvoice(savedOrder, savedItems);
            boolean emailSent = emailService.sendInvoice(savedOrder, invoice);

            orderMessageLabel.setStyle("-fx-text-fill: green;");

            if (emailSent) {
                orderMessageLabel.setText("Order placed successfully. Ref: " + result.getReference() + ". Invoice sent by email.");
            } else {
                orderMessageLabel.setText("Order placed successfully. Ref: " + result.getReference() + ". Invoice generated, but email not sent.");
            }

        } catch (Exception e) {
            orderMessageLabel.setStyle("-fx-text-fill: orange;");
            orderMessageLabel.setText("Order created. Ref: " + result.getReference() + ". Invoice error: " + e.getMessage());
        }

        cartItems.clear();
        currentPaymentSessionId = null;
        currentOnlinePaymentMethod = null;

        if (confirmOnlinePaymentButton != null) {
            confirmOnlinePaymentButton.setDisable(true);
        }

        refreshCartView();
        clearForm();
        loadProducts();
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

    private boolean isOnlinePayment(String paymentMethod) {
        return "Card".equalsIgnoreCase(paymentMethod)
                || "PayPal".equalsIgnoreCase(paymentMethod);
    }

    private void openBrowser(String url) {
        try {
            if (url == null || url.trim().isEmpty()) {
                orderMessageLabel.setStyle("-fx-text-fill: red;");
                orderMessageLabel.setText("Payment URL is empty.");
                return;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                orderMessageLabel.setStyle("-fx-text-fill: orange;");
                orderMessageLabel.setText("Open this payment URL manually: " + url);
            }

        } catch (Exception e) {
            orderMessageLabel.setStyle("-fx-text-fill: red;");
            orderMessageLabel.setText("Cannot open browser: " + e.getMessage());
        }
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

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}