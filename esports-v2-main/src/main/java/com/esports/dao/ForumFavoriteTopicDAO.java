package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.Sujet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ForumFavoriteTopicDAO {
    private final Connection con = DatabaseConnection.getInstance().getConnection();

    public void addToFavorites(int userId, int sujetId) {
        String sql = "INSERT IGNORE INTO forum_favorite_topic(user_id, sujet_id) VALUES(?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, sujetId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void removeFromFavorites(int userId, int sujetId) {
        String sql = "DELETE FROM forum_favorite_topic WHERE user_id=? AND sujet_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, sujetId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public boolean isFavorite(int userId, int sujetId) {
        String sql = "SELECT COUNT(*) c FROM forum_favorite_topic WHERE user_id=? AND sujet_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, sujetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("c") > 0;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public List<Sujet> getUserFavorites(int userId) {
        List<Sujet> list = new ArrayList<>();
        String sql = "SELECT s.* FROM forum_favorite_topic f JOIN sujets_forum s ON s.id=f.sujet_id WHERE f.user_id=? ORDER BY f.created_at DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Sujet s = new Sujet();
                    s.setId(rs.getInt("id"));
                    s.setTitre(rs.getString("titre"));
                    s.setContenu(rs.getString("contenu"));
                    list.add(s);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
}
