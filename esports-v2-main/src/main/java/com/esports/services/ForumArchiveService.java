package com.esports.services;

import com.esports.dao.SujetDao;
import com.esports.db.DatabaseConnection;
import com.esports.models.Sujet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public class ForumArchiveService {
    private final Connection con = DatabaseConnection.getInstance().getConnection();
    private final SujetDao sujetDao = new SujetDao();

    public void archiveInactiveTopics() {
        String sql = "UPDATE sujets_forum SET status='ARCHIVED', archived_at=NOW(), archive_reason=? " +
                "WHERE (last_activity IS NULL OR last_activity < (NOW() - INTERVAL 30 DAY)) AND COALESCE(status,'ACTIVE') <> 'ARCHIVED'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "Sujet inactif depuis 30 jours");
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void archiveTopic(int sujetId, String reason) {
        String sql = "UPDATE sujets_forum SET status='ARCHIVED', archived_at=NOW(), archive_reason=? WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reason == null || reason.isBlank() ? "Archivage manuel" : reason);
            ps.setInt(2, sujetId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void restoreTopic(int sujetId) {
        String sql = "UPDATE sujets_forum SET status='ACTIVE', archived_at=NULL, archive_reason=NULL WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sujetId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public List<Sujet> getArchivedTopics() {
        List<Sujet> out = new ArrayList<>();
        for (Sujet s : sujetDao.getAll()) {
            if ("ARCHIVED".equalsIgnoreCase(s.getStatus())) out.add(s);
        }
        return out;
    }
}
