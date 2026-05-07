package com.esports.services;

import com.esports.dao.ForumUserBadgeDAO;
import com.esports.dao.ForumUserReputationDAO;
import com.esports.models.ForumUserBadge;
import com.esports.models.ForumUserReputation;

import java.util.List;

public class ForumBadgeService {
    private final ForumUserBadgeDAO badgeDAO = new ForumUserBadgeDAO();
    private final ForumUserReputationDAO repDAO = new ForumUserReputationDAO();

    public void evaluateBadges(int userId) {
        ForumUserReputation r = repDAO.getByUserId(userId);
        if (r == null) return;
        if (r.getMessagesCount() >= 1) assignBadgeIfNotExists(userId, "Débutant", "Premier message publié");
        if (r.getMessagesCount() >= 10) assignBadgeIfNotExists(userId, "Actif", "10 messages publiés");
        if (r.getMessagesCount() >= 50) assignBadgeIfNotExists(userId, "Expert", "50 messages publiés");
        if (r.getBestAnswersCount() >= 3) assignBadgeIfNotExists(userId, "Helper", "3 meilleures réponses");
        if (r.getLikesReceived() >= 20) assignBadgeIfNotExists(userId, "Popular", "20 likes reçus");
    }

    public List<ForumUserBadge> getUserBadges(int userId) {
        return badgeDAO.getUserBadges(userId);
    }

    public void assignBadgeIfNotExists(int userId, String badgeName, String description) {
        if (!badgeDAO.hasBadge(userId, badgeName)) {
            badgeDAO.assignBadge(userId, badgeName, description);
        }
    }
}
