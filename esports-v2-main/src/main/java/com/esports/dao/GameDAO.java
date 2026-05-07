package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.Game;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GameDAO {

    private Connection con = DatabaseConnection.getInstance().getConnection();

    public List<Game> getAll() {
        List<Game> games = new ArrayList<>();
        String sql = "SELECT * FROM game";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Game g = new Game();
                g.setId(rs.getInt("id"));
                g.setName(rs.getString("name"));
                g.setSlug(rs.getString("slug"));
                g.setDescription(rs.getString("description"));
                g.setCoverImage(rs.getString("cover_image"));
                g.setHasRanking(rs.getBoolean("has_ranking"));
                g.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                games.add(g);
            }
        } catch (SQLException e) {
        }
        return games;
    }

    public void add(Game g) {
        String sql = "INSERT INTO game (name, slug, description, cover_image, has_ranking, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, g.getName());
            ps.setString(2, g.getSlug());
            ps.setString(3, g.getDescription());
            ps.setString(4, g.getCoverImage());
            ps.setBoolean(5, g.isHasRanking());
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public void update(Game g) {
        String sql = "UPDATE game SET name=?, slug=?, description=?, cover_image=?, has_ranking=? WHERE id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, g.getName());
            ps.setString(2, g.getSlug());
            ps.setString(3, g.getDescription());
            ps.setString(4, g.getCoverImage());
            ps.setBoolean(5, g.isHasRanking());
            ps.setInt(6, g.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM game WHERE id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Game deleted!");
        } catch (SQLException e) {
        }
    }
}