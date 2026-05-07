package com.esports.services;

import com.esports.dao.ForumFavoriteTopicDAO;
import com.esports.models.Sujet;

import java.util.List;

public class ForumFavoriteService {
    private final ForumFavoriteTopicDAO dao = new ForumFavoriteTopicDAO();

    public void addToFavorites(int userId, int sujetId) {
        dao.addToFavorites(userId, sujetId);
    }

    public void removeFromFavorites(int userId, int sujetId) {
        dao.removeFromFavorites(userId, sujetId);
    }

    public boolean isFavorite(int userId, int sujetId) {
        return dao.isFavorite(userId, sujetId);
    }

    public List<Sujet> getUserFavorites(int userId) {
        return dao.getUserFavorites(userId);
    }
}
