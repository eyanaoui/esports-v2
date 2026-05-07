package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MessageDao {

    private final Connection con = DatabaseConnection.getInstance().getConnection();

    private void init() {
        MessagesForumMetadata.ensureLoaded(con);
    }

    public List<Message> getBySujet(int sujetId) {
        return getBySujetAndStatus(sujetId, "ACCEPTED");
    }

    public List<Message> getBySujetAndStatus(int sujetId, String status) {
        init();
        List<Message> list = new ArrayList<>();
        String qId = MessagesForumMetadata.qId();
        String qFk = MessagesForumMetadata.qFk();
        String sql = "SELECT * FROM messages_forum WHERE " + qFk + " = ? ";
        if (status != null && !status.isBlank() && MessagesForumMetadata.hasColumn("status")) {
            sql += "AND " + MessagesForumMetadata.q("status") + " = ? ";
        }
        sql += "ORDER BY COALESCE(is_best,0) DESC, " + qId;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sujetId);
            if (status != null && !status.isBlank() && MessagesForumMetadata.hasColumn("status")) {
                ps.setString(2, status);
            }
            ResultSet rs = ps.executeQuery();
            String fkName = MessagesForumMetadata.fkSujetColumn();
            String contentName = MessagesForumMetadata.contentColumn();
            while (rs.next()) {
                Message m = new Message();
                m.setId(rs.getInt(MessagesForumMetadata.idColumn()));
                m.setSujetId(rs.getInt(fkName));
                m.setContenu(rs.getString(contentName));
                if (MessagesForumMetadata.hasColumn("status")) m.setStatus(rs.getString("status"));
                if (MessagesForumMetadata.hasColumn("spam_score")) m.setSpamScore(rs.getDouble("spam_score"));
                if (MessagesForumMetadata.hasColumn("moderation_reason")) m.setModerationReason(rs.getString("moderation_reason"));
                if (MessagesForumMetadata.hasColumn("likes")) m.setLikes(rs.getInt("likes"));
                if (MessagesForumMetadata.hasColumn("dislikes")) m.setDislikes(rs.getInt("dislikes"));
                if (MessagesForumMetadata.hasColumn("is_best")) m.setBest(rs.getBoolean("is_best"));
                if (MessagesForumMetadata.hasColumn("file_path")) m.setFilePath(rs.getString("file_path"));
                if (MessagesForumMetadata.hasColumn("report_count")) m.setReportCount(rs.getInt("report_count"));
                if (MessagesForumMetadata.hasColumn("updated_at")) {
                    Timestamp up = rs.getTimestamp("updated_at");
                    m.setUpdatedAt(up == null ? null : up.toLocalDateTime());
                }
                String created = MessagesForumMetadata.firstExistingColumn("created_at", "date_creation");
                if (created != null) {
                    Timestamp ts = rs.getTimestamp(created);
                    m.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
                }
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void add(Message m) {
        addWithModeration(m);
    }

    public void addWithModeration(Message m) {
        init();
        String qFk = MessagesForumMetadata.qFk();
        String qContent = MessagesForumMetadata.qContent();
        String authorCol = MessagesForumMetadata.authorColumn();
        String createdAtCol = MessagesForumMetadata.createdAtColumn();
        String likesCol = MessagesForumMetadata.firstExistingColumn("nombre_likes", "likes_count", "nb_likes");
        boolean hasAuthor = authorCol != null && !authorCol.isBlank();
        boolean hasCreatedAt = createdAtCol != null && !createdAtCol.isBlank();
        boolean hasLikes = likesCol != null && !likesCol.isBlank();
        boolean hasStatus = MessagesForumMetadata.hasColumn("status");
        boolean hasSpamScore = MessagesForumMetadata.hasColumn("spam_score");
        boolean hasReason = MessagesForumMetadata.hasColumn("moderation_reason");
        boolean hasLikes2 = MessagesForumMetadata.hasColumn("likes");
        boolean hasDislikes = MessagesForumMetadata.hasColumn("dislikes");
        boolean hasBest = MessagesForumMetadata.hasColumn("is_best");
        boolean hasFile = MessagesForumMetadata.hasColumn("file_path");

        StringBuilder cols = new StringBuilder(qFk).append(", ").append(qContent);
        StringBuilder vals = new StringBuilder("?, ?");
        if (hasAuthor) {
            cols.append(", ").append(MessagesForumMetadata.qAuthor());
            vals.append(", ?");
        }
        if (hasCreatedAt) {
            cols.append(", ").append(MessagesForumMetadata.qCreatedAt());
            vals.append(", NOW()");
        }
        if (hasLikes) {
            cols.append(", ").append(MessagesForumMetadata.q(likesCol));
            vals.append(", 0");
        }
        if (hasStatus) {
            cols.append(", ").append(MessagesForumMetadata.q("status"));
            vals.append(", ?");
        }
        if (hasSpamScore) {
            cols.append(", ").append(MessagesForumMetadata.q("spam_score"));
            vals.append(", ?");
        }
        if (hasReason) {
            cols.append(", ").append(MessagesForumMetadata.q("moderation_reason"));
            vals.append(", ?");
        }
        if (hasLikes2) {
            cols.append(", ").append(MessagesForumMetadata.q("likes"));
            vals.append(", 0");
        }
        if (hasDislikes) {
            cols.append(", ").append(MessagesForumMetadata.q("dislikes"));
            vals.append(", 0");
        }
        if (hasBest) {
            cols.append(", ").append(MessagesForumMetadata.q("is_best"));
            vals.append(", 0");
        }
        if (hasFile) {
            cols.append(", ").append(MessagesForumMetadata.q("file_path"));
            vals.append(", ?");
        }

        String sql = "INSERT INTO messages_forum (" + cols + ") VALUES (" + vals + ")";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            ps.setInt(i++, m.getSujetId());
            ps.setString(i++, m.getContenu());
            if (hasAuthor) {
                // Fallback auteur technique en l'absence d'authentification branchée.
                ps.setInt(i, 1);
                i++;
            }
            if (hasStatus) {
                ps.setString(i++, m.getStatus() == null ? "ACCEPTED" : m.getStatus());
            }
            if (hasSpamScore) {
                ps.setDouble(i++, m.getSpamScore());
            }
            if (hasReason) {
                ps.setString(i++, m.getModerationReason());
            }
            if (hasFile) {
                ps.setString(i, m.getFilePath());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(Message m) {
        init();
        String qId = MessagesForumMetadata.qId();
        String qFk = MessagesForumMetadata.qFk();
        String qContent = MessagesForumMetadata.qContent();
        String sql = "UPDATE messages_forum SET " + qContent + " = ?, updated_at = NOW() WHERE " + qId + " = ? AND " + qFk + " = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.getContenu());
            ps.setInt(2, m.getId());
            ps.setInt(3, m.getSujetId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        init();
        String qId = MessagesForumMetadata.qId();
        String sql = "DELETE FROM messages_forum WHERE " + qId + " = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Liste admin : messages + titre du sujet. */
    public List<AdminForumMessageRow> getAllForAdmin(String statusFilter) {
        init();
        List<AdminForumMessageRow> list = new ArrayList<>();
        String qId = MessagesForumMetadata.qId();
        String qFk = MessagesForumMetadata.qFk();
        String fkName = MessagesForumMetadata.fkSujetColumn();
        String contentName = MessagesForumMetadata.contentColumn();

        String sql = "SELECT m.* , s.titre AS sujet_titre " +
                "FROM messages_forum m LEFT JOIN sujets_forum s ON m." + qFk + " = s.id " +
                "WHERE 1=1 ";
        if (statusFilter != null && !statusFilter.isBlank() && MessagesForumMetadata.hasColumn("status")) {
            sql += "AND m." + MessagesForumMetadata.q("status") + " = ? ";
        }
        sql += "ORDER BY m." + qId + " DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (statusFilter != null && !statusFilter.isBlank() && MessagesForumMetadata.hasColumn("status")) {
                ps.setString(1, statusFilter);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AdminForumMessageRow row = new AdminForumMessageRow();
                row.id = rs.getInt(MessagesForumMetadata.idColumn());
                row.sujetId = rs.getInt(fkName);
                row.sujetTitre = rs.getString("sujet_titre");
                row.contenu = rs.getString(contentName);
                row.status = MessagesForumMetadata.hasColumn("status") ? rs.getString("status") : "ACCEPTED";
                row.spamScore = MessagesForumMetadata.hasColumn("spam_score") ? rs.getDouble("spam_score") : 0;
                row.moderationReason = MessagesForumMetadata.hasColumn("moderation_reason") ? rs.getString("moderation_reason") : "";
                list.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean existsSameMessageInSujet(int sujetId, String content) {
        init();
        String sql = "SELECT COUNT(*) c FROM messages_forum WHERE " + MessagesForumMetadata.qFk() + " = ? AND LOWER(" +
                MessagesForumMetadata.qContent() + ") = LOWER(?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sujetId);
            ps.setString(2, content);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("c") > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int countMessagesBySujet(int sujetId) {
        init();
        String sql = "SELECT COUNT(*) c FROM messages_forum WHERE " + MessagesForumMetadata.qFk() + " = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sujetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("c");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countMessagesBySujetSince24h(int sujetId) {
        init();
        String created = MessagesForumMetadata.firstExistingColumn("created_at", "date_creation");
        if (created == null) return 0;
        String sql = "SELECT COUNT(*) c FROM messages_forum WHERE " + MessagesForumMetadata.qFk() + " = ? AND "
                + MessagesForumMetadata.q(created) + " >= (NOW() - INTERVAL 1 DAY)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sujetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("c");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Message> getAcceptedBySujet(int sujetId) {
        return getBySujetAndStatus(sujetId, "ACCEPTED");
    }

    public void updateStatus(int messageId, String status, String reason) {
        init();
        if (!MessagesForumMetadata.hasColumn("status")) return;
        String sql = "UPDATE messages_forum SET " + MessagesForumMetadata.q("status") + " = ?";
        if (MessagesForumMetadata.hasColumn("moderation_reason")) {
            sql += ", " + MessagesForumMetadata.q("moderation_reason") + " = ?";
        }
        sql += " WHERE " + MessagesForumMetadata.qId() + " = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            int i = 2;
            if (MessagesForumMetadata.hasColumn("moderation_reason")) {
                ps.setString(i++, reason);
            }
            ps.setInt(i, messageId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getTodayMessagesCount() {
        init();
        String created = MessagesForumMetadata.firstExistingColumn("created_at", "date_creation");
        if (created == null) return 0;
        String sql = "SELECT COUNT(*) c FROM messages_forum WHERE DATE(" + MessagesForumMetadata.q(created) + ") = CURDATE()";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getPendingMessagesCount() {
        init();
        if (!MessagesForumMetadata.hasColumn("status")) return 0;
        String sql = "SELECT COUNT(*) c FROM messages_forum WHERE " + MessagesForumMetadata.q("status") + " = 'PENDING'";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getRejectedMessagesCount() {
        return getCountByStatus("REJECTED");
    }

    public int getCountByStatus(String status) {
        init();
        if (!MessagesForumMetadata.hasColumn("status")) return 0;
        String sql = "SELECT COUNT(*) c FROM messages_forum WHERE " + MessagesForumMetadata.q("status") + " = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("c");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Message getById(int messageId) {
        init();
        String sql = "SELECT * FROM messages_forum WHERE " + MessagesForumMetadata.qId() + " = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Message m = new Message();
                    m.setId(rs.getInt(MessagesForumMetadata.idColumn()));
                    m.setSujetId(rs.getInt(MessagesForumMetadata.fkSujetColumn()));
                    m.setContenu(rs.getString(MessagesForumMetadata.contentColumn()));
                    if (MessagesForumMetadata.hasColumn("likes")) m.setLikes(rs.getInt("likes"));
                    if (MessagesForumMetadata.hasColumn("dislikes")) m.setDislikes(rs.getInt("dislikes"));
                    if (MessagesForumMetadata.hasColumn("is_best")) m.setBest(rs.getBoolean("is_best"));
                    if (MessagesForumMetadata.hasColumn("file_path")) m.setFilePath(rs.getString("file_path"));
                    if (MessagesForumMetadata.hasColumn("report_count")) m.setReportCount(rs.getInt("report_count"));
                    if (MessagesForumMetadata.hasColumn("auteur_id")) m.setUserId(rs.getInt("auteur_id"));
                    if (MessagesForumMetadata.hasColumn("author_id")) m.setUserId(rs.getInt("author_id"));
                    return m;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void incrementLikes(int messageId) {
        init();
        if (!MessagesForumMetadata.hasColumn("likes")) return;
        String sql = "UPDATE messages_forum SET likes = COALESCE(likes,0)+1, updated_at=NOW() WHERE " + MessagesForumMetadata.qId() + "=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void incrementDislikes(int messageId) {
        init();
        if (!MessagesForumMetadata.hasColumn("dislikes")) return;
        String sql = "UPDATE messages_forum SET dislikes = COALESCE(dislikes,0)+1, updated_at=NOW() WHERE " + MessagesForumMetadata.qId() + "=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Message> getTopMessagesByLikes(int sujetId) {
        init();
        List<Message> out = new ArrayList<>();
        String sql = "SELECT * FROM messages_forum WHERE " + MessagesForumMetadata.qFk() + "=? ORDER BY COALESCE(likes,0) DESC, " + MessagesForumMetadata.qId() + " ASC LIMIT 5";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sujetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message m = new Message();
                    m.setId(rs.getInt(MessagesForumMetadata.idColumn()));
                    m.setSujetId(rs.getInt(MessagesForumMetadata.fkSujetColumn()));
                    m.setContenu(rs.getString(MessagesForumMetadata.contentColumn()));
                    m.setLikes(MessagesForumMetadata.hasColumn("likes") ? rs.getInt("likes") : 0);
                    m.setBest(MessagesForumMetadata.hasColumn("is_best") && rs.getBoolean("is_best"));
                    out.add(m);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    public void clearBestAnswer(int sujetId) {
        init();
        if (!MessagesForumMetadata.hasColumn("is_best")) return;
        String sql = "UPDATE messages_forum SET is_best = 0 WHERE " + MessagesForumMetadata.qFk() + " = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sujetId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void markBestAnswer(int sujetId, int messageId) {
        clearBestAnswer(sujetId);
        init();
        if (!MessagesForumMetadata.hasColumn("is_best")) return;
        String sql = "UPDATE messages_forum SET is_best = 1, updated_at=NOW() WHERE " + MessagesForumMetadata.qId() + " = ? AND " + MessagesForumMetadata.qFk() + " = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, sujetId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public Message getBestAnswer(int sujetId) {
        init();
        if (!MessagesForumMetadata.hasColumn("is_best")) return null;
        String sql = "SELECT * FROM messages_forum WHERE " + MessagesForumMetadata.qFk() + " = ? AND is_best = 1 LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sujetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Message m = new Message();
                    m.setId(rs.getInt(MessagesForumMetadata.idColumn()));
                    m.setSujetId(rs.getInt(MessagesForumMetadata.fkSujetColumn()));
                    m.setContenu(rs.getString(MessagesForumMetadata.contentColumn()));
                    m.setLikes(MessagesForumMetadata.hasColumn("likes") ? rs.getInt("likes") : 0);
                    m.setDislikes(MessagesForumMetadata.hasColumn("dislikes") ? rs.getInt("dislikes") : 0);
                    m.setBest(true);
                    return m;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public void setAttachmentPath(int messageId, String path) {
        init();
        if (!MessagesForumMetadata.hasColumn("file_path")) return;
        String sql = "UPDATE messages_forum SET file_path=?, updated_at=NOW() WHERE " + MessagesForumMetadata.qId() + "=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, path);
            ps.setInt(2, messageId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Message> getMessagesByPage(int sujetId, int page, int size) {
        init();
        List<Message> list = new ArrayList<>();
        int offset = Math.max(0, (page - 1) * size);
        String sql = "SELECT * FROM messages_forum WHERE " + MessagesForumMetadata.qFk() + " = ? AND " +
                "(status IS NULL OR status='ACCEPTED') ORDER BY COALESCE(is_best,0) DESC, COALESCE(created_at, NOW()) ASC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sujetId);
            ps.setInt(2, size);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message m = new Message();
                    m.setId(rs.getInt(MessagesForumMetadata.idColumn()));
                    m.setSujetId(rs.getInt(MessagesForumMetadata.fkSujetColumn()));
                    m.setContenu(rs.getString(MessagesForumMetadata.contentColumn()));
                    m.setLikes(MessagesForumMetadata.hasColumn("likes") ? rs.getInt("likes") : 0);
                    m.setDislikes(MessagesForumMetadata.hasColumn("dislikes") ? rs.getInt("dislikes") : 0);
                    m.setBest(MessagesForumMetadata.hasColumn("is_best") && rs.getBoolean("is_best"));
                    m.setFilePath(MessagesForumMetadata.hasColumn("file_path") ? rs.getString("file_path") : null);
                    m.setReportCount(MessagesForumMetadata.hasColumn("report_count") ? rs.getInt("report_count") : 0);
                    if (MessagesForumMetadata.hasColumn("updated_at")) {
                        Timestamp up = rs.getTimestamp("updated_at");
                        m.setUpdatedAt(up == null ? null : up.toLocalDateTime());
                    }
                    list.add(m);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public int countMessages(int sujetId) {
        init();
        String sql = "SELECT COUNT(*) c FROM messages_forum WHERE " + MessagesForumMetadata.qFk() + " = ? AND (status IS NULL OR status='ACCEPTED')";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sujetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("c");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public void incrementReportCount(int messageId) {
        init();
        if (!MessagesForumMetadata.hasColumn("report_count")) return;
        String sql = "UPDATE messages_forum SET report_count = COALESCE(report_count,0)+1, updated_at=NOW() WHERE " + MessagesForumMetadata.qId() + "=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Stats admin : titre sujet -> nombre de messages. */
    public Map<String, Integer> countBySujetForAdmin() {
        init();
        Map<String, Integer> map = new LinkedHashMap<>();
        String qFk = MessagesForumMetadata.qFk();
        String sql = "SELECT COALESCE(s.titre, CONCAT('Sujet #', m." + qFk + ")) AS label, COUNT(*) AS c " +
                "FROM messages_forum m LEFT JOIN sujets_forum s ON m." + qFk + " = s.id " +
                "GROUP BY label ORDER BY c DESC";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("label"), rs.getInt("c"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static class AdminForumMessageRow {
        public int id;
        public int sujetId;
        public String sujetTitre;
        public String contenu;
        public String status;
        public double spamScore;
        public String moderationReason;
    }
}
