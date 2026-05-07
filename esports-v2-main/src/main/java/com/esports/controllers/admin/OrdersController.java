package com.esports.controllers.admin;

import com.esports.dao.OrderDAO;
import com.esports.dao.OrderItemDAO;
import com.esports.models.Order;
import com.esports.models.OrderItem;
import com.esports.services.EmailService;
import com.esports.services.InvoiceService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class OrdersController {

    @FXML
    private TextField searchField;

    @FXML
    private TextField referenceSearchField;

    @FXML
    private TableView<Order> orderTable;

    @FXML
    private TableColumn<Order, Integer> idColumn;

    @FXML
    private TableColumn<Order, String> referenceColumn;

    @FXML
    private TableColumn<Order, String> firstNameColumn;

    @FXML
    private TableColumn<Order, String> lastNameColumn;

    @FXML
    private TableColumn<Order, String> emailColumn;

    @FXML
    private TableColumn<Order, String> phoneColumn;

    @FXML
    private TableColumn<Order, String> paymentMethodColumn;

    @FXML
    private TableColumn<Order, String> paymentStatusColumn;

    @FXML
    private TableColumn<Order, String> statusColumn;

    @FXML
    private TableColumn<Order, Double> totalColumn;

    @FXML
    private TableColumn<Order, String> createdAtColumn;

    @FXML
    private Label messageLabel;

    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderItemDAO orderItemDAO = new OrderItemDAO();
    private final InvoiceService invoiceService = new InvoiceService();
    private final EmailService emailService = new EmailService();

    private ObservableList<Order> orderList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        referenceColumn.setCellValueFactory(new PropertyValueFactory<>("reference"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerFirstName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerLastName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("customerEmail"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("customerPhone"));
        paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));

        createdAtColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getCreatedAt() != null
                                ? cellData.getValue().getCreatedAt().toString()
                                : ""
                )
        );

        messageLabel.setText("");
        loadOrders();
    }

    private void loadOrders() {
        List<Order> orders = orderDAO.getAllOrders();
        orderList = FXCollections.observableArrayList(orders);
        orderTable.setItems(orderList);
    }

    @FXML
    public void handleRefresh() {
        searchField.clear();
        referenceSearchField.clear();
        messageLabel.setText("");
        loadOrders();
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        if (keyword.isEmpty()) {
            orderTable.setItems(orderList);
            return;
        }

        List<Order> filtered = orderList.stream()
                .filter(order ->
                        safeLower(order.getReference()).contains(keyword)
                                || safeLower(order.getCustomerFirstName()).contains(keyword)
                                || safeLower(order.getCustomerLastName()).contains(keyword)
                                || safeLower(order.getCustomerEmail()).contains(keyword)
                                || safeLower(order.getStatus()).contains(keyword)
                                || safeLower(order.getPaymentStatus()).contains(keyword)
                )
                .collect(Collectors.toList());

        orderTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    public void handleSearchByReference() {
        messageLabel.setText("");

        String reference = referenceSearchField.getText() == null ? "" : referenceSearchField.getText().trim();

        if (reference.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please enter an order reference.");
            return;
        }

        Order order = orderDAO.getOrderByReference(reference);

        if (order == null) {
            orderTable.setItems(FXCollections.observableArrayList());
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("No order found with reference: " + reference);
            return;
        }

        orderTable.setItems(FXCollections.observableArrayList(order));
        messageLabel.setStyle("-fx-text-fill: green;");
        messageLabel.setText("Order found: " + reference);
    }

    @FXML
    public void handleGenerateInvoice() {
        Order selectedOrder = orderTable.getSelectionModel().getSelectedItem();

        if (selectedOrder == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select an order.");
            return;
        }

        try {
            List<OrderItem> items = orderItemDAO.getItemsByOrderId(selectedOrder.getId());

            if (items.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Warning", "No items found for this order.");
                return;
            }

            File invoice = invoiceService.generateInvoice(selectedOrder, items);

            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Invoice generated: " + invoice.getAbsolutePath());

            showAlert(Alert.AlertType.INFORMATION, "Invoice Generated", "Invoice generated successfully:\n" + invoice.getAbsolutePath());

        } catch (Exception e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Invoice error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Invoice generation failed:\n" + e.getMessage());
        }
    }

    @FXML
    public void handleSendInvoice() {
        Order selectedOrder = orderTable.getSelectionModel().getSelectedItem();

        if (selectedOrder == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select an order.");
            return;
        }

        try {
            List<OrderItem> items = orderItemDAO.getItemsByOrderId(selectedOrder.getId());

            if (items.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Warning", "No items found for this order.");
                return;
            }

            File invoice = invoiceService.generateInvoice(selectedOrder, items);
            boolean sent = emailService.sendInvoice(selectedOrder, invoice);

            if (sent) {
                messageLabel.setStyle("-fx-text-fill: green;");
                messageLabel.setText("Invoice sent to: " + selectedOrder.getCustomerEmail());
                showAlert(Alert.AlertType.INFORMATION, "Email Sent", "Invoice sent successfully.");
            } else {
                messageLabel.setStyle("-fx-text-fill: orange;");
                messageLabel.setText("Invoice generated but email not sent. Check SMTP configuration.");
                showAlert(Alert.AlertType.WARNING, "Email Not Sent", "Invoice generated but email not sent.\nCheck ESPORTS_MAIL_USERNAME and ESPORTS_MAIL_PASSWORD.");
            }

        } catch (Exception e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Email error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Email sending failed:\n" + e.getMessage());
        }
    }

    @FXML
    public void handleDeleteSelected() {
        Order selectedOrder = orderTable.getSelectionModel().getSelectedItem();

        if (selectedOrder == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select an order.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Order");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete this order?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            boolean itemsDeleted = orderItemDAO.deleteItemsByOrderId(selectedOrder.getId());
            boolean orderDeleted = false;

            if (itemsDeleted) {
                orderDeleted = orderDAO.deleteOrder(selectedOrder.getId());
            }

            if (orderDeleted) {
                loadOrders();
                messageLabel.setStyle("-fx-text-fill: green;");
                messageLabel.setText("Order deleted successfully.");
                showAlert(Alert.AlertType.INFORMATION, "Success", "Order deleted successfully.");
            } else {
                messageLabel.setStyle("-fx-text-fill: red;");
                messageLabel.setText("Order was not deleted.");
                showAlert(Alert.AlertType.ERROR, "Error", "Order was not deleted.");
            }
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
}