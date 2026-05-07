package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.ProductReview;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductReviewDAO {

    private final Connection connection;

    public ProductReviewDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public boolean addReview(ProductReview review) {
        String sql = """
                INSERT INTO product_review
                (product_id, customer_name, rating, comment, status, created_at)
                VALUES (?, ?, ?, ?, ?, NOW())
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, review.getProductId());
            ps.setString(2, review.getCustomerName());
            ps.setInt(3, review.getRating());
            ps.setString(4, review.getComment());
            ps.setString(5, review.getStatus());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("❌ Error while adding product review: " + e.getMessage());
            return false;
        }
    }

    public List<ProductReview> getAllReviews() {
        List<ProductReview> reviews = new ArrayList<>();

        String sql = """
                SELECT
                    pr.id,
                    pr.product_id,
                    p.name AS product_name,
                    pr.customer_name,
                    pr.rating,
                    pr.comment,
                    pr.status,
                    pr.created_at
                FROM product_review pr
                INNER JOIN product p ON p.id = pr.product_id
                ORDER BY pr.created_at DESC, pr.id DESC
                """;

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                reviews.add(mapResultSetToReview(rs));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while loading reviews: " + e.getMessage());
        }

        return reviews;
    }

    public List<ProductReview> getVisibleReviews() {
        List<ProductReview> reviews = new ArrayList<>();

        String sql = """
                SELECT
                    pr.id,
                    pr.product_id,
                    p.name AS product_name,
                    pr.customer_name,
                    pr.rating,
                    pr.comment,
                    pr.status,
                    pr.created_at
                FROM product_review pr
                INNER JOIN product p ON p.id = pr.product_id
                WHERE pr.status = 'VISIBLE'
                ORDER BY pr.created_at DESC, pr.id DESC
                """;

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                reviews.add(mapResultSetToReview(rs));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while loading visible reviews: " + e.getMessage());
        }

        return reviews;
    }

    public List<ProductReview> getVisibleReviewsByProduct(int productId) {
        List<ProductReview> reviews = new ArrayList<>();

        String sql = """
                SELECT
                    pr.id,
                    pr.product_id,
                    p.name AS product_name,
                    pr.customer_name,
                    pr.rating,
                    pr.comment,
                    pr.status,
                    pr.created_at
                FROM product_review pr
                INNER JOIN product p ON p.id = pr.product_id
                WHERE pr.product_id = ?
                AND pr.status = 'VISIBLE'
                ORDER BY pr.created_at DESC, pr.id DESC
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                reviews.add(mapResultSetToReview(rs));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while loading product reviews: " + e.getMessage());
        }

        return reviews;
    }

    public boolean updateStatus(int reviewId, String status) {
        String sql = "UPDATE product_review SET status = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, reviewId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("❌ Error while updating review status: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteReview(int reviewId) {
        String sql = "DELETE FROM product_review WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reviewId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("❌ Error while deleting review: " + e.getMessage());
            return false;
        }
    }

    public double getAverageRatingForProduct(int productId) {
        String sql = """
                SELECT AVG(rating) AS avg_rating
                FROM product_review
                WHERE product_id = ?
                AND status = 'VISIBLE'
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("avg_rating");
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while calculating average rating: " + e.getMessage());
        }

        return 0.0;
    }

    public int getReviewCountForProduct(int productId) {
        String sql = """
                SELECT COUNT(*) AS total
                FROM product_review
                WHERE product_id = ?
                AND status = 'VISIBLE'
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while counting reviews: " + e.getMessage());
        }

        return 0;
    }

    private ProductReview mapResultSetToReview(ResultSet rs) throws SQLException {
        ProductReview review = new ProductReview();

        review.setId(rs.getInt("id"));
        review.setProductId(rs.getInt("product_id"));
        review.setProductName(rs.getString("product_name"));
        review.setCustomerName(rs.getString("customer_name"));
        review.setRating(rs.getInt("rating"));
        review.setComment(rs.getString("comment"));
        review.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            review.setCreatedAt(createdAt.toLocalDateTime());
        }

        return review;
    }
}