package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.Message;
import com.esports.models.Sujet;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SujetDao {
    private Connection con = DatabaseConnection.getInstance().getConnection();
    private final MessageDao messageDao = new MessageDao();

    public List<Sujet> getAll() {
        List<Sujet> list = new ArrayList<>();
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT * FROM sujets_forum ORDER BY COALESCE(is_pinned,0) DESC, COALESCE(updated_at, created_at, date_creation) DESC, id DESC")) {
            while (rs.next()) {
                Sujet s = new Sujet();
                s.setId(rs.getInt("id"));
                s.setTitre(rs.getString("titre"));
                s.setContenu(rs.getString("contenu"));
                s.setImage(rs.getString("image"));
                s.setTrendingScore(readDoubleIfPresent(rs, "trending_score"));
                s.setStatus(readStringIfPresent(rs, "status"));
                s.setAutoSummary(readStringIfPresent(rs, "auto_summary"));
                s.setKeywords(readStringIfPresent(rs, "keywords"));
                s.setRepliesCount(readIntIfPresent(rs, "replies_count"));
                s.setLastActivity(readTimestampIfPresent(rs, "last_activity"));
                s.setCreatedAt(readTimestampIfPresent(rs, "created_at"));
                s.setPinned(readIntIfPresent(rs, "is_pinned") == 1);
                s.setViewsCount(readIntIfPresent(rs, "views_count"));
                s.setUpdatedAt(readTimestampIfPresent(rs, "updated_at"));
                s.setArchivedAt(readTimestampIfPresent(rs, "archived_at"));
                s.setArchiveReason(readStringIfPresent(rs, "archive_reason"));
                list.add(s);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Sujet getById(int id) {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM sujets_forum WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Sujet s = new Sujet();
                s.setId(rs.getInt("id"));
                s.setTitre(rs.getString("titre"));
                s.setContenu(rs.getString("contenu"));
                s.setImage(rs.getString("image"));
                s.setTrendingScore(readDoubleIfPresent(rs, "trending_score"));
                s.setStatus(readStringIfPresent(rs, "status"));
                s.setAutoSummary(readStringIfPresent(rs, "auto_summary"));
                s.setKeywords(readStringIfPresent(rs, "keywords"));
                s.setRepliesCount(readIntIfPresent(rs, "replies_count"));
                s.setLastActivity(readTimestampIfPresent(rs, "last_activity"));
                s.setCreatedAt(readTimestampIfPresent(rs, "created_at"));
                s.setPinned(readIntIfPresent(rs, "is_pinned") == 1);
                s.setViewsCount(readIntIfPresent(rs, "views_count"));
                s.setUpdatedAt(readTimestampIfPresent(rs, "updated_at"));
                s.setArchivedAt(readTimestampIfPresent(rs, "archived_at"));
                s.setArchiveReason(readStringIfPresent(rs, "archive_reason"));
                return s;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public void add(Sujet s) {
        String sql = "INSERT INTO sujets_forum (titre, contenu, image, cree_par, categorie, date_creation, est_verrouille) VALUES (?, ?, ?, 'Hassen', 'General', NOW(), ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getTitre());
            ps.setString(2, s.getContenu());
            ps.setString(3, s.getImage());
            ps.setBoolean(4, false);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void update(Sujet s) {
        String sql = "UPDATE sujets_forum SET titre = ?, contenu = ?, image = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getTitre());
            ps.setString(2, s.getContenu());
            ps.setString(3, s.getImage());
            ps.setInt(4, s.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void delete(int id) {
        MessagesForumMetadata.ensureLoaded(con);
        String qFk = MessagesForumMetadata.qFk();
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM messages_forum WHERE " + qFk + " = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM sujets_forum WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void updateAdvancedFields(int sujetId, double trendingScore, int repliesCount, String status) {
        String sql = "UPDATE sujets_forum SET trending_score=?, replies_count=?, status=? WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, trendingScore);
            ps.setInt(2, repliesCount);
            ps.setString(3, status);
            ps.setInt(4, sujetId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void updateLastActivity(int sujetId) {
        String sql = "UPDATE sujets_forum SET last_activity = NOW() WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sujetId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void updateSummary(int sujetId, String summary) {
        String sql = "UPDATE sujets_forum SET auto_summary = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, summary);
            ps.setInt(2, sujetId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void updateKeywords(int sujetId, String keywords) {
        String sql = "UPDATE sujets_forum SET keywords = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, keywords);
            ps.setInt(2, sujetId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Sujet> advancedSearch(String keyword, String status, Integer minReplies, LocalDate dateFrom, LocalDate dateTo) {
        MessagesForumMetadata.ensureLoaded(con);
        List<Sujet> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT s.* FROM sujets_forum s " +
                "LEFT JOIN messages_forum m ON m." + MessagesForumMetadata.qFk() + " = s.id " +
                "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append("AND (LOWER(s.titre) LIKE ? OR LOWER(s.contenu) LIKE ? OR LOWER(m.")
                    .append(MessagesForumMetadata.qContent()).append(") LIKE ?) ");
            String q = "%" + keyword.toLowerCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
        }
        if (status != null && !status.isBlank()) {
            sql.append("AND s.status = ? ");
            params.add(status);
        }
        if (minReplies != null) {
            sql.append("AND COALESCE(s.replies_count, 0) >= ? ");
            params.add(minReplies);
        }
        if (dateFrom != null) {
            sql.append("AND DATE(COALESCE(s.created_at, NOW())) >= ? ");
            params.add(Date.valueOf(dateFrom));
        }
        if (dateTo != null) {
            sql.append("AND DATE(COALESCE(s.created_at, NOW())) <= ? ");
            params.add(Date.valueOf(dateTo));
        }

        sql.append("ORDER BY COALESCE(s.trending_score,0) DESC, s.id DESC");
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Sujet s = new Sujet();
                    s.setId(rs.getInt("id"));
                    s.setTitre(rs.getString("titre"));
                    s.setContenu(rs.getString("contenu"));
                    s.setImage(readStringIfPresent(rs, "image"));
                    s.setTrendingScore(readDoubleIfPresent(rs, "trending_score"));
                    s.setStatus(readStringIfPresent(rs, "status"));
                    s.setAutoSummary(readStringIfPresent(rs, "auto_summary"));
                    s.setKeywords(readStringIfPresent(rs, "keywords"));
                    s.setRepliesCount(readIntIfPresent(rs, "replies_count"));
                    s.setLastActivity(readTimestampIfPresent(rs, "last_activity"));
                    s.setCreatedAt(readTimestampIfPresent(rs, "created_at"));
                    s.setArchivedAt(readTimestampIfPresent(rs, "archived_at"));
                    s.setArchiveReason(readStringIfPresent(rs, "archive_reason"));
                    list.add(s);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Sujet> getAllTopicsWithMessagesGrouped() {
        List<Sujet> topics = getAll();
        for (Sujet topic : topics) {
            List<Message> messages = messageDao.getBySujetAndStatus(topic.getId(), null);
            messages.sort((a, b) -> {
                int bestCmp = Boolean.compare(b.isBest(), a.isBest());
                if (bestCmp != 0) return bestCmp;
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return Integer.compare(b.getId(), a.getId());
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
            topic.setMessages(messages);
        }
        return topics;
    }

    public List<Message> getMessagesBySujetId(int sujetId) {
        return messageDao.getBySujetAndStatus(sujetId, null);
    }

    public List<Message> getMessagesBySujetIdAndStatus(int sujetId, String status) {
        return messageDao.getBySujetAndStatus(sujetId, status);
    }

    public List<Sujet> searchGroupedTopics(String keyword, String status) {
        List<Sujet> result = advancedSearch(keyword, status, null, null, null);
        for (Sujet topic : result) {
            topic.setMessages(messageDao.getBySujetAndStatus(topic.getId(), null));
        }
        return result;
    }

    public String getMostRepliedTopic() {
        String sql = "SELECT titre FROM sujets_forum ORDER BY COALESCE(replies_count,0) DESC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("titre");
        } catch (SQLException e) { e.printStackTrace(); }
        return "-";
    }

    public double getAverageMessagesPerTopic() {
        String sql = "SELECT AVG(COALESCE(replies_count,0)) avgv FROM sujets_forum";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble("avgv");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public List<Sujet> getTop5TrendingTopics() {
        List<Sujet> list = new ArrayList<>();
        String sql = "SELECT * FROM sujets_forum ORDER BY COALESCE(trending_score,0) DESC LIMIT 5";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Sujet s = new Sujet();
                s.setId(rs.getInt("id"));
                s.setTitre(rs.getString("titre"));
                s.setTrendingScore(readDoubleIfPresent(rs, "trending_score"));
                s.setStatus(readStringIfPresent(rs, "status"));
                s.setPinned(readIntIfPresent(rs, "is_pinned") == 1);
                list.add(s);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public int getHotTopicsCount() {
        String sql = "SELECT COUNT(*) c FROM sujets_forum WHERE status = 'HOT'";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public void pinTopic(int sujetId) {
        setPinned(sujetId, true);
    }

    public void unpinTopic(int sujetId) {
        setPinned(sujetId, false);
    }

    private void setPinned(int sujetId, boolean pinned) {
        String sql = "UPDATE sujets_forum SET is_pinned = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, pinned);
            ps.setInt(2, sujetId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Sujet> getPinnedTopics() {
        List<Sujet> list = new ArrayList<>();
        String sql = "SELECT * FROM sujets_forum WHERE COALESCE(is_pinned,0)=1 ORDER BY COALESCE(updated_at,created_at,date_creation) DESC";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Sujet s = new Sujet();
                s.setId(rs.getInt("id"));
                s.setTitre(rs.getString("titre"));
                s.setContenu(rs.getString("contenu"));
                s.setPinned(true);
                s.setUpdatedAt(readTimestampIfPresent(rs, "updated_at"));
                list.add(s);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void incrementViews(int sujetId) {
        String sql = "UPDATE sujets_forum SET views_count = COALESCE(views_count,0)+1, updated_at=NOW() WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sujetId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private String readStringIfPresent(ResultSet rs, String column) {
        try {
            rs.findColumn(column);
            return rs.getString(column);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private int readIntIfPresent(ResultSet rs, String column) {
        try {
            rs.findColumn(column);
            return rs.getInt(column);
        } catch (SQLException ignored) {
            return 0;
        }
    }

    private double readDoubleIfPresent(ResultSet rs, String column) {
        try {
            rs.findColumn(column);
            return rs.getDouble(column);
        } catch (SQLException ignored) {
            return 0;
        }
    }

    private java.time.LocalDateTime readTimestampIfPresent(ResultSet rs, String column) {
        try {
            rs.findColumn(column);
            Timestamp ts = rs.getTimestamp(column);
            return ts == null ? null : ts.toLocalDateTime();
        } catch (SQLException ignored) {
            return null;
        }
    }
}
