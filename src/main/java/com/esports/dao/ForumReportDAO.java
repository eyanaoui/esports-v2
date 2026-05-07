package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.ForumReport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ForumReportDAO {
    private final Connection con = DatabaseConnection.getInstance().getConnection();

    public void createReport(Integer userId, Integer sujetId, int messageId, String reason, String description) {
        String sql = "INSERT INTO forum_report(user_id, sujet_id, message_id, reason, description, status) VALUES(?,?,?,?,?,'PENDING')";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (userId == null) ps.setNull(1, java.sql.Types.INTEGER); else ps.setInt(1, userId);
            if (sujetId == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, sujetId);
            ps.setInt(3, messageId);
            ps.setString(4, reason);
            ps.setString(5, description);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void resolveReport(int reportId) {
        updateStatus(reportId, "RESOLVED");
    }

    public void rejectReport(int reportId) {
        updateStatus(reportId, "REJECTED");
    }

    public List<ForumReport> getPendingReports() {
        List<ForumReport> out = new ArrayList<>();
        String sql = "SELECT * FROM forum_report WHERE status='PENDING' ORDER BY created_at DESC";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ForumReport r = new ForumReport();
                r.setId(rs.getInt("id"));
                r.setUserId((Integer) rs.getObject("user_id"));
                r.setSujetId((Integer) rs.getObject("sujet_id"));
                r.setMessageId(rs.getInt("message_id"));
                r.setReason(rs.getString("reason"));
                r.setDescription(rs.getString("description"));
                r.setStatus(rs.getString("status"));
                out.add(r);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }

    private void updateStatus(int reportId, String status) {
        String sql = "UPDATE forum_report SET status=?, resolved_at=NOW() WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, reportId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
