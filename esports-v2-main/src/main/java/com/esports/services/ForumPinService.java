package com.esports.services;

import com.esports.dao.SujetDao;
import com.esports.models.Sujet;

import java.util.List;

public class ForumPinService {
    private final SujetDao sujetDao = new SujetDao();

    public void pinTopic(int sujetId) {
        sujetDao.pinTopic(sujetId);
    }

    public void unpinTopic(int sujetId) {
        sujetDao.unpinTopic(sujetId);
    }

    public List<Sujet> getPinnedTopics() {
        return sujetDao.getPinnedTopics();
    }
}
