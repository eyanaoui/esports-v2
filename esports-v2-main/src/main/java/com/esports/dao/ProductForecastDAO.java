package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.ProductForecast;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductForecastDAO {

    private final Connection connection;

    public ProductForecastDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public List<ProductForecast> getAllForecasts() {
        List<ProductForecast> forecasts = new ArrayList<>();

        String sql = """
                SELECT
                    pf.id,
                    pf.product_id,
                    p.name AS product_name,
                    p.category,
                    p.stock AS current_stock,
                    p.price,
                    pf.forecast_days,
                    pf.predicted_qty,
                    pf.recommended_reorder_qty,
                    pf.generated_at
                FROM product_forecast pf
                INNER JOIN product p ON p.id = pf.product_id
                ORDER BY pf.recommended_reorder_qty DESC, pf.predicted_qty DESC
                """;

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                forecasts.add(mapResultSetToForecast(rs));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while loading product forecasts: " + e.getMessage());
        }

        return forecasts;
    }

    public List<ProductForecast> searchForecasts(String keyword) {
        List<ProductForecast> forecasts = new ArrayList<>();

        String sql = """
                SELECT
                    pf.id,
                    pf.product_id,
                    p.name AS product_name,
                    p.category,
                    p.stock AS current_stock,
                    p.price,
                    pf.forecast_days,
                    pf.predicted_qty,
                    pf.recommended_reorder_qty,
                    pf.generated_at
                FROM product_forecast pf
                INNER JOIN product p ON p.id = pf.product_id
                WHERE LOWER(p.name) LIKE ?
                OR LOWER(p.category) LIKE ?
                ORDER BY pf.recommended_reorder_qty DESC, pf.predicted_qty DESC
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String search = "%" + keyword.toLowerCase() + "%";

            ps.setString(1, search);
            ps.setString(2, search);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                forecasts.add(mapResultSetToForecast(rs));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while searching forecasts: " + e.getMessage());
        }

        return forecasts;
    }

    public List<ProductForecast> getRestockNeededForecasts() {
        List<ProductForecast> forecasts = new ArrayList<>();

        String sql = """
                SELECT
                    pf.id,
                    pf.product_id,
                    p.name AS product_name,
                    p.category,
                    p.stock AS current_stock,
                    p.price,
                    pf.forecast_days,
                    pf.predicted_qty,
                    pf.recommended_reorder_qty,
                    pf.generated_at
                FROM product_forecast pf
                INNER JOIN product p ON p.id = pf.product_id
                WHERE pf.recommended_reorder_qty > 0
                ORDER BY pf.recommended_reorder_qty DESC
                """;

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                forecasts.add(mapResultSetToForecast(rs));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while loading restock forecasts: " + e.getMessage());
        }

        return forecasts;
    }

    private ProductForecast mapResultSetToForecast(ResultSet rs) throws SQLException {
        ProductForecast forecast = new ProductForecast();

        forecast.setId(rs.getInt("id"));
        forecast.setProductId(rs.getInt("product_id"));
        forecast.setProductName(rs.getString("product_name"));
        forecast.setCategory(rs.getString("category"));
        forecast.setCurrentStock(rs.getInt("current_stock"));
        forecast.setPrice(rs.getDouble("price"));
        forecast.setForecastDays(rs.getInt("forecast_days"));
        forecast.setPredictedQty(rs.getDouble("predicted_qty"));
        forecast.setRecommendedReorderQty(rs.getInt("recommended_reorder_qty"));

        Timestamp generatedAt = rs.getTimestamp("generated_at");
        if (generatedAt != null) {
            forecast.setGeneratedAt(generatedAt.toLocalDateTime());
        }

        return forecast;
    }
}