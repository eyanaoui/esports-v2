package com.esports.services;

import com.esports.dao.SujetDao;
import com.esports.models.Sujet;

import java.util.List;
import java.util.stream.Collectors;

public class ForumDuplicateDetectionService {
    private final ForumRecommendationService recommendationService = new ForumRecommendationService();
    private final SujetDao sujetDao = new SujetDao();

    public List<Sujet> findSimilarTopics(String title, String content) {
        String source = (title == null ? "" : title) + " " + (content == null ? "" : content);
        return sujetDao.getAll().stream()
                .filter(s -> calculateSimilarity(source, (s.getTitre() + " " + s.getContenu())) >= 60.0)
                .collect(Collectors.toList());
    }

    public double calculateSimilarity(String text1, String text2) {
        return recommendationService.calculateSimilarity(text1, text2);
    }
}
