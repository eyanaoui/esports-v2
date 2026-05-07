package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private final Connection connection;

    public ProductDAO() {
        connection = DatabaseConnection.getInstance().getConnection();
    }

    public void addProduct(Product product) {
        String sql = """
                INSERT INTO product
                (category, description, image, is_active, name, price, stock)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, product.getCategory());
            ps.setString(2, product.getDescription());
            ps.setString(3, product.getImage());
            ps.setBoolean(4, product.isActive());
            ps.setString(5, product.getName());
            ps.setDouble(6, product.getPrice());
            ps.setInt(7, product.getStock());

            ps.executeUpdate();
            System.out.println("✅ Product added successfully.");

        } catch (SQLException e) {
            System.out.println("❌ Error while adding product: " + e.getMessage());
        }
    }

    public void updateProduct(Product product) {
        String sql = """
                UPDATE product
                SET category = ?,
                    description = ?,
                    image = ?,
                    is_active = ?,
                    name = ?,
                    price = ?,
                    stock = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, product.getCategory());
            ps.setString(2, product.getDescription());
            ps.setString(3, product.getImage());
            ps.setBoolean(4, product.isActive());
            ps.setString(5, product.getName());
            ps.setDouble(6, product.getPrice());
            ps.setInt(7, product.getStock());
            ps.setInt(8, product.getId());

            ps.executeUpdate();
            System.out.println("✅ Product updated successfully.");

        } catch (SQLException e) {
            System.out.println("❌ Error while updating product: " + e.getMessage());
        }
    }

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();

        String sql = """
                SELECT
                    p.*,
                    COALESCE(SUM(oi.quantity), 0) AS orders_count,
                    COALESCE(pf.predicted_qty, 0) AS predicted_qty,
                    COALESCE(pf.forecast_days, 0) AS forecast_days,
                    COALESCE(pf.recommended_reorder_qty, 0) AS recommended_reorder_qty
                FROM product p
                LEFT JOIN order_item oi ON oi.product_id = p.id
                LEFT JOIN product_forecast pf ON pf.id = (
                    SELECT pf2.id
                    FROM product_forecast pf2
                    WHERE pf2.product_id = p.id
                    ORDER BY pf2.generated_at DESC, pf2.id DESC
                    LIMIT 1
                )
                WHERE p.is_active = true
                GROUP BY
                    p.id,
                    p.category,
                    p.description,
                    p.image,
                    p.is_active,
                    p.name,
                    p.price,
                    p.stock,
                    pf.predicted_qty,
                    pf.forecast_days,
                    pf.recommended_reorder_qty
                ORDER BY p.id DESC
                """;

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while displaying products: " + e.getMessage());
        }

        return products;
    }

    public Product getProductById(int id) {
        String sql = """
                SELECT
                    p.*,
                    COALESCE(SUM(oi.quantity), 0) AS orders_count,
                    COALESCE(pf.predicted_qty, 0) AS predicted_qty,
                    COALESCE(pf.forecast_days, 0) AS forecast_days,
                    COALESCE(pf.recommended_reorder_qty, 0) AS recommended_reorder_qty
                FROM product p
                LEFT JOIN order_item oi ON oi.product_id = p.id
                LEFT JOIN product_forecast pf ON pf.id = (
                    SELECT pf2.id
                    FROM product_forecast pf2
                    WHERE pf2.product_id = p.id
                    ORDER BY pf2.generated_at DESC, pf2.id DESC
                    LIMIT 1
                )
                WHERE p.id = ?
                GROUP BY
                    p.id,
                    p.category,
                    p.description,
                    p.image,
                    p.is_active,
                    p.name,
                    p.price,
                    p.stock,
                    pf.predicted_qty,
                    pf.forecast_days,
                    pf.recommended_reorder_qty
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToProduct(rs);
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while searching product: " + e.getMessage());
        }

        return null;
    }

    public List<Product> searchProducts(String keyword) {
        List<Product> products = new ArrayList<>();

        String sql = """
                SELECT
                    p.*,
                    COALESCE(SUM(oi.quantity), 0) AS orders_count,
                    COALESCE(pf.predicted_qty, 0) AS predicted_qty,
                    COALESCE(pf.forecast_days, 0) AS forecast_days,
                    COALESCE(pf.recommended_reorder_qty, 0) AS recommended_reorder_qty
                FROM product p
                LEFT JOIN order_item oi ON oi.product_id = p.id
                LEFT JOIN product_forecast pf ON pf.id = (
                    SELECT pf2.id
                    FROM product_forecast pf2
                    WHERE pf2.product_id = p.id
                    ORDER BY pf2.generated_at DESC, pf2.id DESC
                    LIMIT 1
                )
                WHERE p.is_active = true
                AND (
                    LOWER(p.name) LIKE ?
                    OR LOWER(p.category) LIKE ?
                    OR LOWER(p.description) LIKE ?
                )
                GROUP BY
                    p.id,
                    p.category,
                    p.description,
                    p.image,
                    p.is_active,
                    p.name,
                    p.price,
                    p.stock,
                    pf.predicted_qty,
                    pf.forecast_days,
                    pf.recommended_reorder_qty
                ORDER BY p.id DESC
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String search = "%" + keyword.toLowerCase() + "%";

            ps.setString(1, search);
            ps.setString(2, search);
            ps.setString(3, search);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while searching products: " + e.getMessage());
        }

        return products;
    }

    public List<Product> getPopularProducts(int limit) {
        List<Product> products = new ArrayList<>();

        String sql = """
                SELECT
                    p.*,
                    COALESCE(SUM(oi.quantity), 0) AS orders_count,
                    COALESCE(pf.predicted_qty, 0) AS predicted_qty,
                    COALESCE(pf.forecast_days, 0) AS forecast_days,
                    COALESCE(pf.recommended_reorder_qty, 0) AS recommended_reorder_qty
                FROM product p
                LEFT JOIN order_item oi ON oi.product_id = p.id
                LEFT JOIN product_forecast pf ON pf.id = (
                    SELECT pf2.id
                    FROM product_forecast pf2
                    WHERE pf2.product_id = p.id
                    ORDER BY pf2.generated_at DESC, pf2.id DESC
                    LIMIT 1
                )
                WHERE p.is_active = true
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
                    pf.predicted_qty,
                    pf.forecast_days,
                    pf.recommended_reorder_qty
                ORDER BY orders_count DESC, p.id DESC
                LIMIT ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while loading popular products: " + e.getMessage());
        }

        return products;
    }

    public List<Product> getProductsByCategory(String category, int limit, int excludedProductId) {
        List<Product> products = new ArrayList<>();

        String sql = """
                SELECT
                    p.*,
                    COALESCE(SUM(oi.quantity), 0) AS orders_count,
                    COALESCE(pf.predicted_qty, 0) AS predicted_qty,
                    COALESCE(pf.forecast_days, 0) AS forecast_days,
                    COALESCE(pf.recommended_reorder_qty, 0) AS recommended_reorder_qty
                FROM product p
                LEFT JOIN order_item oi ON oi.product_id = p.id
                LEFT JOIN product_forecast pf ON pf.id = (
                    SELECT pf2.id
                    FROM product_forecast pf2
                    WHERE pf2.product_id = p.id
                    ORDER BY pf2.generated_at DESC, pf2.id DESC
                    LIMIT 1
                )
                WHERE p.is_active = true
                AND p.stock > 0
                AND p.category = ?
                AND p.id <> ?
                GROUP BY
                    p.id,
                    p.category,
                    p.description,
                    p.image,
                    p.is_active,
                    p.name,
                    p.price,
                    p.stock,
                    pf.predicted_qty,
                    pf.forecast_days,
                    pf.recommended_reorder_qty
                ORDER BY orders_count DESC, p.id DESC
                LIMIT ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setInt(2, excludedProductId);
            ps.setInt(3, limit);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while loading products by category: " + e.getMessage());
        }

        return products;
    }

    public boolean softDeleteProduct(int id) {
        String sql = "UPDATE product SET is_active = false WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("❌ Error while deactivating product: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStock(int productId, int newStock) {
        String sql = "UPDATE product SET stock = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, newStock);
            ps.setInt(2, productId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("❌ Error while updating stock: " + e.getMessage());
            return false;
        }
    }

    public boolean decreaseStock(int productId, int quantity) {
        String sql = "UPDATE product SET stock = stock - ? WHERE id = ? AND stock >= ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("❌ Error while decreasing stock: " + e.getMessage());
            return false;
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
            product.setPredictedQty(rs.getDouble("predicted_qty"));
        } catch (SQLException ignored) {
            product.setPredictedQty(0);
        }

        try {
            product.setForecastDays(rs.getInt("forecast_days"));
        } catch (SQLException ignored) {
            product.setForecastDays(0);
        }

        try {
            product.setRecommendedReorderQty(rs.getInt("recommended_reorder_qty"));
        } catch (SQLException ignored) {
            product.setRecommendedReorderQty(0);
        }

        return product;
    }
}