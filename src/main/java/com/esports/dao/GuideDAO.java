package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.Guide;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GuideDAO {

    private Connection con = DatabaseConnection.getInstance().getConnection();

    public List<Guide> getAll() {
        List<Guide> guides = new ArrayList<>();
        String sql = "SELECT * FROM guide";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Guide g = new Guide();
                g.setId(rs.getInt("id"));
                g.setGameId(rs.getInt("game_id"));
                g.setTitle(rs.getString("title"));
                g.setDescription(rs.getString("description"));
                g.setDifficulty(rs.getString("difficulty"));
                g.setAuthorId(rs.getInt("author_id"));
                g.setCoverImage(rs.getString("cover_image"));
                guides.add(g);
            }
        } catch (SQLException e) {
        }
        return guides;
    }

    public List<Guide> getByGameId(int gameId) {
        List<Guide> guides = new ArrayList<>();
        String sql = "SELECT * FROM guide WHERE game_id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, gameId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Guide g = new Guide();
                g.setId(rs.getInt("id"));
                g.setGameId(rs.getInt("game_id"));
                g.setTitle(rs.getString("title"));
                g.setDescription(rs.getString("description"));
                g.setDifficulty(rs.getString("difficulty"));
                g.setAuthorId(rs.getInt("author_id"));
                g.setCoverImage(rs.getString("cover_image"));
                guides.add(g);
            }
        } catch (SQLException e) {
        }
        return guides;
    }

    public void add(Guide g) {
        String sql = "INSERT INTO guide (game_id, title, description, difficulty, author_id, cover_image) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, g.getGameId());
            ps.setString(2, g.getTitle());
            ps.setString(3, g.getDescription());
            ps.setString(4, g.getDifficulty());
            ps.setInt(5, g.getAuthorId());
            ps.setString(6, g.getCoverImage());
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public void update(Guide g) {
        String sql = "UPDATE guide SET game_id=?, title=?, description=?, difficulty=?, author_id=?, cover_image=? WHERE id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, g.getGameId());
            ps.setString(2, g.getTitle());
            ps.setString(3, g.getDescription());
            ps.setString(4, g.getDifficulty());
            ps.setInt(5, g.getAuthorId());
            ps.setString(6, g.getCoverImage());
            ps.setInt(7, g.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM guide WHERE id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }
}