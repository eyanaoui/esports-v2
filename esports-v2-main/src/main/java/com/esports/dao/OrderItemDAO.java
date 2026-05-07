package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderItemDAO {

    private final Connection connection;

    public OrderItemDAO() {
        connection = DatabaseConnection.getInstance().getConnection();
    }

    public boolean addOrderItem(OrderItem item) {
        String sql = """
                INSERT INTO order_item
                (order_ref_id, product_id, quantity, unit_price)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, item.getOrderRefId());
            ps.setInt(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setDouble(4, item.getUnitPrice());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("❌ Error while adding order item: " + e.getMessage());
            return false;
        }
    }

    public List<OrderItem> getItemsByOrderId(int orderId) {
        List<OrderItem> items = new ArrayList<>();

        String sql = """
                SELECT
                    oi.id,
                    oi.order_ref_id,
                    oi.product_id,
                    p.name AS product_name,
                    oi.quantity,
                    oi.unit_price
                FROM order_item oi
                INNER JOIN product p ON p.id = oi.product_id
                WHERE oi.order_ref_id = ?
                ORDER BY oi.id ASC
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, orderId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                OrderItem item = new OrderItem();
                item.setId(rs.getInt("id"));
                item.setOrderRefId(rs.getInt("order_ref_id"));
                item.setProductId(rs.getInt("product_id"));
                item.setProductName(rs.getString("product_name"));
                item.setQuantity(rs.getInt("quantity"));
                item.setUnitPrice(rs.getDouble("unit_price"));

                items.add(item);
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while loading order items: " + e.getMessage());
        }

        return items;
    }

    public boolean deleteItemsByOrderId(int orderId) {
        String sql = "DELETE FROM order_item WHERE order_ref_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("❌ Error while deleting order items: " + e.getMessage());
            return false;
        }
    }
}