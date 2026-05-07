package com.esports.services;

import com.esports.dao.MessageDao;
import com.esports.db.DatabaseConnection;
import com.esports.models.Message;
import com.esports.models.MessageHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ForumMessageHistoryService {
    private final Connection con = DatabaseConnection.getInstance().getConnection();
    private final MessageDao messageDao = new MessageDao();

    public void updateMessageWithHistory(int messageId, String newContent) {
        Message current = messageDao.getById(messageId);
        if (current == null) return;
        String insert = "INSERT INTO message_history(message_id, old_content, new_content) VALUES(?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(insert)) {
            ps.setInt(1, messageId);
            ps.setString(2, current.getContenu());
            ps.setString(3, newContent);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
        current.setContenu(newContent);
        messageDao.update(current);
    }

    public List<MessageHistory> getHistoryByMessage(int messageId) {
        List<MessageHistory> out = new ArrayList<>();
        String sql = "SELECT * FROM message_history WHERE message_id=? ORDER BY date_modif DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MessageHistory h = new MessageHistory();
                    h.setId(rs.getInt("id"));
                    h.setMessageId(rs.getInt("message_id"));
                    h.setOldContent(rs.getString("old_content"));
                    h.setNewContent(rs.getString("new_content"));
                    java.sql.Timestamp ts = rs.getTimestamp("date_modif");
                    h.setDateModif(ts == null ? null : ts.toLocalDateTime());
                    out.add(h);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }
}
