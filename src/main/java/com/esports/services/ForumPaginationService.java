package com.esports.services;

import com.esports.dao.MessageDao;
import com.esports.models.Message;

import java.util.List;

public class ForumPaginationService {
    private final MessageDao messageDao = new MessageDao();

    public List<Message> getMessagesByPage(int sujetId, int page, int size) {
        return messageDao.getMessagesByPage(sujetId, page, size);
    }

    public int countMessages(int sujetId) {
        return messageDao.countMessages(sujetId);
    }
}
