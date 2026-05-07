package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.ForumUserBadge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ForumUserBadgeDAO {
    private final Connection con = DatabaseConnection.getInstance().getConnection();

    public List<ForumUserBadge> getUserBadges(int userId) {
        List<ForumUserBadge> list = new ArrayList<>();
        String sql = "SELECT * FROM forum_user_badge WHERE user_id=? ORDER BY earned_at DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ForumUserBadge b = new ForumUserBadge();
                    b.setId(rs.getInt("id"));
                    b.setUserId(rs.getInt("user_id"));
                    b.setBadgeName(rs.getString("badge_name"));
                    b.setBadgeDescription(rs.getString("badge_description"));
                    list.add(b);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public boolean hasBadge(int userId, String badgeName) {
        String sql = "SELECT COUNT(*) c FROM forum_user_badge WHERE user_id=? AND badge_name=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, badgeName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("c") > 0;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public void assignBadge(int userId, String badgeName, String description) {
        String sql = "INSERT INTO forum_user_badge(user_id,badge_name,badge_description) VALUES(?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, badgeName);
            ps.setString(3, description);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
