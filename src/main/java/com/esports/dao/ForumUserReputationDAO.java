package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.ForumUserReputation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ForumUserReputationDAO {
    private final Connection con = DatabaseConnection.getInstance().getConnection();

    public ForumUserReputation getByUserId(int userId) {
        String sql = "SELECT * FROM forum_user_reputation WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public void ensureExists(int userId) {
        String sql = "INSERT IGNORE INTO forum_user_reputation(user_id,score,level,messages_count,likes_received,best_answers_count,rejected_messages_count) VALUES(?,0,'BRONZE',0,0,0,0)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void updateScoreAndLevel(int userId, int score, String level) {
        String sql = "UPDATE forum_user_reputation SET score=?, level=?, updated_at=NOW() WHERE user_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, score);
            ps.setString(2, level);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void incrementColumn(int userId, String column, int delta) {
        String sql = "UPDATE forum_user_reputation SET " + column + " = COALESCE(" + column + ",0)+?, updated_at=NOW() WHERE user_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public List<ForumUserReputation> getTopUsers(int limit) {
        List<ForumUserReputation> out = new ArrayList<>();
        String sql = "SELECT * FROM forum_user_reputation ORDER BY COALESCE(score,0) DESC LIMIT ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }

    private ForumUserReputation map(ResultSet rs) throws Exception {
        ForumUserReputation r = new ForumUserReputation();
        r.setId(rs.getInt("id"));
        r.setUserId(rs.getInt("user_id"));
        r.setScore(rs.getInt("score"));
        r.setLevel(rs.getString("level"));
        r.setMessagesCount(rs.getInt("messages_count"));
        r.setLikesReceived(rs.getInt("likes_received"));
        r.setBestAnswersCount(rs.getInt("best_answers_count"));
        r.setRejectedMessagesCount(rs.getInt("rejected_messages_count"));
        return r;
    }
}
