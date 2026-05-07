package com.esports.services;

import com.esports.db.DatabaseConnection;
import com.esports.models.ForumUserScore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ForumUserScoreService {
    private final Connection con = DatabaseConnection.getInstance().getConnection();

    public void addScore(int userId, int points) {
        ensureRow(userId);
        String sql = "UPDATE forum_user_score SET score = COALESCE(score,0) + ? WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, points);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void incrementMessageCount(int userId) {
        ensureRow(userId);
        updateCounter(userId, "messages_count", 1);
    }

    public void incrementBestAnswerCount(int userId) {
        ensureRow(userId);
        updateCounter(userId, "best_answers_count", 1);
    }

    public void incrementLikesReceived(int userId) {
        ensureRow(userId);
        updateCounter(userId, "likes_received", 1);
    }

    public List<ForumUserScore> getTopForumUsers() {
        List<ForumUserScore> out = new ArrayList<>();
        String sql = "SELECT * FROM forum_user_score ORDER BY COALESCE(score,0) DESC LIMIT 10";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ForumUserScore s = new ForumUserScore();
                s.setId(rs.getInt("id"));
                s.setUserId(rs.getInt("user_id"));
                s.setScore(rs.getInt("score"));
                s.setMessagesCount(rs.getInt("messages_count"));
                s.setBestAnswersCount(rs.getInt("best_answers_count"));
                s.setLikesReceived(rs.getInt("likes_received"));
                out.add(s);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }

    private void updateCounter(int userId, String col, int delta) {
        String sql = "UPDATE forum_user_score SET " + col + " = COALESCE(" + col + ",0) + ? WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void ensureRow(int userId) {
        String sql = "INSERT IGNORE INTO forum_user_score(user_id, score, messages_count, best_answers_count, likes_received) VALUES(?,0,0,0,0)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
