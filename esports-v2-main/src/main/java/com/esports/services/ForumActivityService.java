package com.esports.services;

import com.esports.db.DatabaseConnection;
import com.esports.models.ForumActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ForumActivityService {
    private final Connection con = DatabaseConnection.getInstance().getConnection();

    public void addActivity(Integer userId, Integer sujetId, Integer messageId, String actionType, String description) {
        String sql = "INSERT INTO forum_activity(user_id, sujet_id, message_id, action_type, description) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (userId == null) ps.setNull(1, java.sql.Types.INTEGER); else ps.setInt(1, userId);
            if (sujetId == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, sujetId);
            if (messageId == null) ps.setNull(3, java.sql.Types.INTEGER); else ps.setInt(3, messageId);
            ps.setString(4, actionType);
            ps.setString(5, description);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public List<ForumActivity> getRecentActivities(int limit) {
        List<ForumActivity> out = new ArrayList<>();
        String sql = "SELECT * FROM forum_activity ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ForumActivity a = new ForumActivity();
                    a.setId(rs.getInt("id"));
                    a.setUserId((Integer) rs.getObject("user_id"));
                    a.setSujetId((Integer) rs.getObject("sujet_id"));
                    a.setMessageId((Integer) rs.getObject("message_id"));
                    a.setActionType(rs.getString("action_type"));
                    a.setDescription(rs.getString("description"));
                    java.sql.Timestamp ts = rs.getTimestamp("created_at");
                    a.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
                    out.add(a);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }
}
