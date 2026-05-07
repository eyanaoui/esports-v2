package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.GuideRating;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GuideRatingDAO {

    private Connection con = DatabaseConnection.getInstance().getConnection();

    public List<GuideRating> getByGuideId(int guideId) {
        List<GuideRating> ratings = new ArrayList<>();
        String sql = "SELECT * FROM guide_rating WHERE guide_id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, guideId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                GuideRating r = new GuideRating();
                r.setId(rs.getInt("id"));
                r.setGuideId(rs.getInt("guide_id"));
                r.setUserId(rs.getInt("user_id"));
                r.setRatingValue(rs.getInt("rating_value"));
                r.setComment(rs.getString("comment"));
                r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                ratings.add(r);
            }
        } catch (SQLException e) {
        }
        return ratings;
    }

    public void add(GuideRating r) {
        String sql = "INSERT INTO guide_rating (guide_id, user_id, rating_value, comment, created_at) VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, r.getGuideId());
            ps.setInt(2, r.getUserId());
            ps.setInt(3, r.getRatingValue());
            ps.setString(4, r.getComment());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM guide_rating WHERE id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public List<GuideRating> getAll() {
        List<GuideRating> ratings = new ArrayList<>();
        String sql = "SELECT * FROM guide_rating";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                GuideRating r = new GuideRating();
                r.setId(rs.getInt("id"));
                r.setGuideId(rs.getInt("guide_id"));
                r.setUserId(rs.getInt("user_id"));
                r.setRatingValue(rs.getInt("rating_value"));
                r.setComment(rs.getString("comment"));
                r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                ratings.add(r);
            }
        } catch (SQLException e) {
        }
        return ratings;
    }

    public void update(GuideRating r) {
        String sql = "UPDATE guide_rating SET guide_id=?, rating_value=?, comment=? WHERE id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, r.getGuideId());
            ps.setInt(2, r.getRatingValue());
            ps.setString(3, r.getComment());
            ps.setInt(4, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }
}