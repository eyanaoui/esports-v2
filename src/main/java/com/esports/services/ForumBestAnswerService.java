package com.esports.services;

import com.esports.dao.MessageDao;
import com.esports.models.Message;

public class ForumBestAnswerService {
    private final MessageDao messageDao = new MessageDao();
    private final ForumUserScoreService userScoreService = new ForumUserScoreService();

    public void markAsBestAnswer(int sujetId, int messageId) {
        messageDao.markBestAnswer(sujetId, messageId);
        Message msg = messageDao.getById(messageId);
        if (msg != null && msg.getUserId() != null && msg.getUserId() > 0) {
            userScoreService.incrementBestAnswerCount(msg.getUserId());
            userScoreService.addScore(msg.getUserId(), 10);
        }
    }

    public void removeBestAnswer(int sujetId) {
        messageDao.clearBestAnswer(sujetId);
    }

    public Message getBestAnswer(int sujetId) {
        return messageDao.getBestAnswer(sujetId);
    }
}
