package com.esports.services;

import com.esports.db.DatabaseConnection;
import com.esports.models.ForumNotification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ForumNotificationService {
    private final Connection con = DatabaseConnection.getInstance().getConnection();

    public void createNotification(Integer userId, Integer sujetId, Integer messageId, String message) {
        String sql = "INSERT INTO forum_notification(user_id, sujet_id, message_id, message, is_read) VALUES(?,?,?,?,0)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (userId == null) ps.setNull(1, java.sql.Types.INTEGER); else ps.setInt(1, userId);
            if (sujetId == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, sujetId);
            if (messageId == null) ps.setNull(3, java.sql.Types.INTEGER); else ps.setInt(3, messageId);
            ps.setString(4, message);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public List<ForumNotification> getUnreadNotifications(int userId) {
        List<ForumNotification> out = new ArrayList<>();
        String sql = "SELECT * FROM forum_notification WHERE user_id=? AND is_read=0 ORDER BY created_at DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ForumNotification n = new ForumNotification();
                    n.setId(rs.getInt("id"));
                    n.setUserId(rs.getInt("user_id"));
                    n.setSujetId((Integer) rs.getObject("sujet_id"));
                    n.setMessageId((Integer) rs.getObject("message_id"));
                    n.setMessage(rs.getString("message"));
                    n.setRead(rs.getBoolean("is_read"));
                    java.sql.Timestamp ts = rs.getTimestamp("created_at");
                    n.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
                    out.add(n);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }

    public List<ForumNotification> getRecentNotifications(int userId, int limit) {
        List<ForumNotification> out = new ArrayList<>();
        String sql = "SELECT * FROM forum_notification WHERE user_id=? ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ForumNotification n = new ForumNotification();
                    n.setId(rs.getInt("id"));
                    n.setUserId(rs.getInt("user_id"));
                    n.setSujetId((Integer) rs.getObject("sujet_id"));
                    n.setMessageId((Integer) rs.getObject("message_id"));
                    n.setMessage(rs.getString("message"));
                    n.setRead(rs.getBoolean("is_read"));
                    java.sql.Timestamp ts = rs.getTimestamp("created_at");
                    n.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
                    out.add(n);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }

    public void markAsRead(int notificationId) {
        String sql = "UPDATE forum_notification SET is_read=1 WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void markAllAsRead(int userId) {
        String sql = "UPDATE forum_notification SET is_read=1 WHERE user_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public int countUnread(int userId) {
        String sql = "SELECT COUNT(*) c FROM forum_notification WHERE user_id=? AND is_read=0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("c");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }
}
