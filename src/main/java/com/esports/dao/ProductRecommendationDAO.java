package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductRecommendationDAO {

    private final Connection connection;

    public ProductRecommendationDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public List<Product> getRecommendedProductsForProduct(int productId, int limit) {
        List<Product> products = new ArrayList<>();

        String sql = """
                SELECT
                    p.*,
                    pr.score AS recommendation_score,
                    COALESCE(SUM(oi.quantity), 0) AS orders_count
                FROM product_recommendation pr
                INNER JOIN product p ON p.id = pr.recommended_product_id
                LEFT JOIN order_item oi ON oi.product_id = p.id
                WHERE pr.product_id = ?
                AND p.is_active = true
                AND p.stock > 0
                GROUP BY
                    p.id,
                    p.category,
                    p.description,
                    p.image,
                    p.is_active,
                    p.name,
                    p.price,
                    p.stock,
                    pr.score
                ORDER BY pr.score DESC, orders_count DESC
                LIMIT ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, limit);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while loading product recommendations: " + e.getMessage());
        }

        return products;
    }

    public List<Product> getRecommendedProductsForCart(List<Integer> productIds, int limit) {
        List<Product> products = new ArrayList<>();

        if (productIds == null || productIds.isEmpty()) {
            return products;
        }

        String placeholders = String.join(",", productIds.stream().map(id -> "?").toList());

        String sql = """
                SELECT
                    p.*,
                    MAX(pr.score) AS recommendation_score,
                    COALESCE(SUM(oi.quantity), 0) AS orders_count
                FROM product_recommendation pr
                INNER JOIN product p ON p.id = pr.recommended_product_id
                LEFT JOIN order_item oi ON oi.product_id = p.id
                WHERE pr.product_id IN (%s)
                AND pr.recommended_product_id NOT IN (%s)
                AND p.is_active = true
                AND p.stock > 0
                GROUP BY
                    p.id,
                    p.category,
                    p.description,
                    p.image,
                    p.is_active,
                    p.name,
                    p.price,
                    p.stock
                ORDER BY recommendation_score DESC, orders_count DESC
                LIMIT ?
                """.formatted(placeholders, placeholders);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;

            for (Integer productId : productIds) {
                ps.setInt(index++, productId);
            }

            for (Integer productId : productIds) {
                ps.setInt(index++, productId);
            }

            ps.setInt(index, limit);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while loading cart recommendations: " + e.getMessage());
        }

        return products;
    }

    public void clearRecommendations() {
        String sql = "DELETE FROM product_recommendation";

        try (Statement st = connection.createStatement()) {
            st.executeUpdate(sql);

        } catch (SQLException e) {
            System.out.println("❌ Error while clearing recommendations: " + e.getMessage());
        }
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product product = new Product();

        product.setId(rs.getInt("id"));
        product.setCategory(rs.getString("category"));
        product.setDescription(rs.getString("description"));
        product.setImage(rs.getString("image"));
        product.setActive(rs.getBoolean("is_active"));
        product.setName(rs.getString("name"));
        product.setPrice(rs.getDouble("price"));
        product.setStock(rs.getInt("stock"));

        try {
            product.setOrdersCount(rs.getInt("orders_count"));
        } catch (SQLException ignored) {
            product.setOrdersCount(0);
        }

        try {
            product.setRecommendationScore(rs.getDouble("recommendation_score"));
        } catch (SQLException ignored) {
            product.setRecommendationScore(0);
        }

        return product;
    }
}