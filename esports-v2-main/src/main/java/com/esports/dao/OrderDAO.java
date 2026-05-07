package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.Order;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    private final Connection connection;

    public OrderDAO() {
        connection = DatabaseConnection.getInstance().getConnection();
    }

    public int addOrder(Order order) {
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

        } catch (SQLException e) {
            System.out.println("❌ Error while adding order: " + e.getMessage());
        }

        return -1;
    }

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM `order` ORDER BY id DESC";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                orders.add(mapResultSetToOrder(rs));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while loading orders: " + e.getMessage());
        }

        return orders;
    }

    public Order getOrderById(int id) {
        String sql = "SELECT * FROM `order` WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToOrder(rs);
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while searching order by id: " + e.getMessage());
        }

        return null;
    }

    public Order getOrderByReference(String reference) {
        String sql = "SELECT * FROM `order` WHERE reference = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, reference);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToOrder(rs);
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while searching order by reference: " + e.getMessage());
        }

        return null;
    }

    public boolean deleteOrder(int id) {
        String sql = "DELETE FROM `order` WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("❌ Error while deleting order: " + e.getMessage());
            return false;
        }
    }

    private Order mapResultSetToOrder(ResultSet rs) throws SQLException {
        Order order = new Order();

        order.setId(rs.getInt("id"));

        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        if (createdAtTimestamp != null) {
            order.setCreatedAt(createdAtTimestamp.toLocalDateTime());
        }

        order.setCustomerEmail(rs.getString("customer_email"));
        order.setCustomerFirstName(rs.getString("customer_first_name"));
        order.setCustomerLastName(rs.getString("customer_last_name"));
        order.setCustomerPhone(rs.getString("customer_phone"));
        order.setPaymentMethod(rs.getString("payment_method"));
        order.setPaymentStatus(rs.getString("payment_status"));
        order.setReference(rs.getString("reference"));
        order.setStatus(rs.getString("status"));
        order.setTotalAmount(rs.getDouble("total_amount"));

        return order;
    }
}