package com.esports.services;

import com.esports.db.DatabaseConnection;
import com.esports.models.CartItem;
import com.esports.models.Order;
import com.esports.models.Product;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderService {

    private final Connection connection;

    public OrderService() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public OrderCreationResult placeOrder(Order order, List<CartItem> cartItems) {
        if (order == null) {
            return OrderCreationResult.error("Order is null.");
        }

        if (cartItems == null || cartItems.isEmpty()) {
            return OrderCreationResult.error("Cart is empty.");
        }

        try {
            connection.setAutoCommit(false);

            double totalAmount = calculateAndCheckStock(cartItems);

            if (order.getCreatedAt() == null) {
                order.setCreatedAt(LocalDateTime.now());
            }

            if (order.getReference() == null || order.getReference().trim().isEmpty()) {
                order.setReference(generateReference());
            }

            order.setTotalAmount(totalAmount);

            int orderId = insertOrder(order);

            if (orderId == -1) {
                connection.rollback();
                return OrderCreationResult.error("Order creation failed.");
            }

            for (CartItem item : cartItems) {
                Product product = item.getProduct();

                if (product == null) {
                    connection.rollback();
                    return OrderCreationResult.error("Invalid cart item: product is null.");
                }

                insertOrderItem(
                        orderId,
                        product.getId(),
                        item.getQuantity(),
                        product.getPrice()
                );

                decreaseStock(product.getId(), item.getQuantity());
            }

            connection.commit();
            return OrderCreationResult.success(orderId, order.getReference(), totalAmount);

        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                System.out.println("❌ Rollback error: " + rollbackException.getMessage());
            }

            return OrderCreationResult.error(e.getMessage());

        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("❌ AutoCommit reset error: " + e.getMessage());
            }
        }
    }

    private double calculateAndCheckStock(List<CartItem> cartItems) throws SQLException {
        double total = 0.0;

        String sql = """
                SELECT id, name, price, stock
                FROM product
                WHERE id = ? AND is_active = true
                FOR UPDATE
                """;

        for (CartItem item : cartItems) {
            Product cartProduct = item.getProduct();

            if (cartProduct == null) {
                throw new SQLException("Invalid cart item: product is null.");
            }

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, cartProduct.getId());

                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    throw new SQLException("Product not found or inactive: " + cartProduct.getName());
                }

                double currentPrice = rs.getDouble("price");
                int currentStock = rs.getInt("stock");

                if (currentStock < item.getQuantity()) {
                    throw new SQLException("Not enough stock for: " + cartProduct.getName());
                }

                cartProduct.setPrice(currentPrice);
                cartProduct.setStock(currentStock);

                total += currentPrice * item.getQuantity();
            }
        }

        return total;
    }

    private int insertOrder(Order order) throws SQLException {
        String sql = """
                INSERT INTO `order`
                (created_at, customer_email, customer_first_name, customer_last_name, customer_phone,
                 payment_method, payment_status, reference, status, total_amount)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(order.getCreatedAt()));
            ps.setString(2, order.getCustomerEmail());
            ps.setString(3, order.getCustomerFirstName());
            ps.setString(4, order.getCustomerLastName());
            ps.setString(5, order.getCustomerPhone());
            ps.setString(6, order.getPaymentMethod());
            ps.setString(7, order.getPaymentStatus());
            ps.setString(8, order.getReference());
            ps.setString(9, order.getStatus());
            ps.setDouble(10, order.getTotalAmount());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return -1;
    }

    private void insertOrderItem(int orderId, int productId, int quantity, double unitPrice) throws SQLException {
        String sql = """
                INSERT INTO order_item
                (order_ref_id, product_id, quantity, unit_price)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            ps.setDouble(4, unitPrice);

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new SQLException("Order item insertion failed.");
            }
        }
    }

    private void decreaseStock(int productId, int quantity) throws SQLException {
        String sql = """
                UPDATE product
                SET stock = stock - ?
                WHERE id = ? AND stock >= ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new SQLException("Stock update failed for product id: " + productId);
            }
        }
    }

    private String generateReference() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}