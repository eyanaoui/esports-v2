package com.esports.services;

import com.esports.dao.ForumUserReputationDAO;
import com.esports.models.ForumUserReputation;

import java.util.List;

public class ForumReputationService {
    private final ForumUserReputationDAO dao = new ForumUserReputationDAO();
    private final ForumBadgeService badgeService = new ForumBadgeService();

    public void addPoints(int userId, int points, String reason) {
        dao.ensureExists(userId);
        ForumUserReputation rep = dao.getByUserId(userId);
        int score = (rep == null ? 0 : rep.getScore()) + points;
        dao.updateScoreAndLevel(userId, score, calculateLevel(score));
        badgeService.evaluateBadges(userId);
    }

    public void onMessageAccepted(int userId) {
        dao.ensureExists(userId);
        dao.incrementColumn(userId, "messages_count", 1);
        addPoints(userId, 5, "Message accepted");
    }

    public void onBestAnswer(int userId) {
        dao.ensureExists(userId);
        dao.incrementColumn(userId, "best_answers_count", 1);
        addPoints(userId, 10, "Best answer");
    }

    public void onLikeReceived(int userId) {
        dao.ensureExists(userId);
        dao.incrementColumn(userId, "likes_received", 1);
        addPoints(userId, 1, "Like received");
    }

    public void onMessageRejected(int userId) {
        dao.ensureExists(userId);
        dao.incrementColumn(userId, "rejected_messages_count", 1);
        addPoints(userId, -3, "Message rejected");
    }

    public List<ForumUserReputation> getTopUsers(int limit) {
        return dao.getTopUsers(limit);
    }

    public String calculateLevel(int score) {
        if (score >= 200) return "DIAMOND";
        if (score >= 100) return "GOLD";
        if (score >= 50) return "SILVER";
        return "BRONZE";
    }
}
