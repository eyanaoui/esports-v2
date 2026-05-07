package com.esports.services;

import com.esports.dao.MessageDao;
import com.esports.models.Message;

import java.util.List;

public class ForumVoteService {
    private final MessageDao messageDao = new MessageDao();
    private final ForumUserScoreService userScoreService = new ForumUserScoreService();

    public void likeMessage(int messageId, int userId) {
        messageDao.incrementLikes(messageId);
        Message message = messageDao.getById(messageId);
        if (message != null && message.getUserId() != null && message.getUserId() > 0) {
            userScoreService.incrementLikesReceived(message.getUserId());
            userScoreService.addScore(message.getUserId(), 1);
        }
    }

    public void dislikeMessage(int messageId, int userId) {
        messageDao.incrementDislikes(messageId);
    }

    public List<Message> getTopMessagesByLikes(int sujetId) {
        return messageDao.getTopMessagesByLikes(sujetId);
    }
}
