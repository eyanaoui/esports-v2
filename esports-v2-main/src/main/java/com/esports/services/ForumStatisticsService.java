package com.esports.services;

import com.esports.dao.MessageDao;
import com.esports.dao.SujetDao;
import com.esports.models.ForumUserReputation;
import com.esports.models.Sujet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ForumStatisticsService {
    private final SujetDao sujetDao = new SujetDao();
    private final MessageDao messageDao = new MessageDao();

    public String getMostRepliedTopic() {
        return sujetDao.getMostRepliedTopic();
    }

    public double getAverageMessagesPerTopic() {
        return sujetDao.getAverageMessagesPerTopic();
    }

    public List<Sujet> getTop5TrendingTopics() {
        return sujetDao.getTop5TrendingTopics();
    }

    public int getTodayMessagesCount() {
        return messageDao.getTodayMessagesCount();
    }

    public int getPendingMessagesCount() {
        return messageDao.getPendingMessagesCount();
    }

    public int getHotTopicsCount() {
        return sujetDao.getHotTopicsCount();
    }

    public int getRejectedMessagesCount() {
        return messageDao.getRejectedMessagesCount();
    }

    public List<ForumUserReputation> getTopForumUsers() {
        return new ForumReputationService().getTopUsers(10);
    }

    public Map<String, Integer> getMessagesByStatusStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("ACCEPTED", messageDao.getCountByStatus("ACCEPTED"));
        stats.put("PENDING", messageDao.getCountByStatus("PENDING"));
        stats.put("REJECTED", messageDao.getCountByStatus("REJECTED"));
        return stats;
    }
}
