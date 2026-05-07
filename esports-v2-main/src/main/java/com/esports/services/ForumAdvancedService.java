package com.esports.services;

import com.esports.dao.MessageDao;
import com.esports.dao.SujetDao;
import com.esports.models.Message;
import com.esports.models.Sujet;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ForumAdvancedService {
    private static final Set<String> STOP_WORDS = new LinkedHashSet<>(Arrays.asList(
            "le","la","les","un","une","de","des","du","et","est","pour","dans","avec","sur","qui","que",
            "donc","mais","ou","car","ce","cet","cette","ces","au","aux","en","par","se","sa","son","ses",
            "je","tu","il","elle","nous","vous","ils","elles","ne","pas","plus","moins","tres","aussi"
    ));

    private final MessageDao messageDao = new MessageDao();
    private final SujetDao sujetDao = new SujetDao();
    private final ForumKeywordService keywordService = new ForumKeywordService();

    public double calculateTrendingScore(int sujetId) {
        int total = messageDao.countMessagesBySujet(sujetId);
        int last24h = messageDao.countMessagesBySujetSince24h(sujetId);
        double score = total * 3.0 + last24h * 5.0;
        Sujet sujet = sujetDao.getById(sujetId);
        String status = computeStatus(score, sujet == null ? null : sujet.getLastActivity());
        sujetDao.updateAdvancedFields(sujetId, score, total, status);
        return score;
    }

    public void updateTopicActivity(int sujetId) {
        sujetDao.updateLastActivity(sujetId);
        calculateTrendingScore(sujetId);
    }

    public String generateSummary(int sujetId) {
        List<Message> messages = messageDao.getAcceptedBySujet(sujetId);
        if (messages.isEmpty()) {
            String empty = "Aucun message disponible pour générer un résumé.";
            sujetDao.updateSummary(sujetId, empty);
            return empty;
        }

        Map<String, Integer> counts = new HashMap<>();
        for (Message m : messages) {
            String[] words = safe(m.getContenu()).toLowerCase().split("[^a-zà-ÿ0-9]+");
            for (String w : words) {
                if (w == null || w.isBlank() || w.length() <= 2 || STOP_WORDS.contains(w)) continue;
                counts.put(w, counts.getOrDefault(w, 0) + 1);
            }
        }
        List<String> top = counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(4)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        String summary;
        if (top.isEmpty()) {
            summary = "Résumé automatique : la discussion contient des échanges variés et des avis de la communauté.";
        } else {
            summary = "Résumé automatique : la discussion parle principalement de " + String.join(", ", top) +
                    ". Les participants donnent des conseils et partagent leurs avis.";
        }
        sujetDao.updateSummary(sujetId, summary);
        keywordService.generateKeywords(sujetId);
        return summary;
    }

    public String generateKeywords(int sujetId) {
        return keywordService.generateKeywords(sujetId);
    }

    private String computeStatus(double score, LocalDateTime lastActivity) {
        if (score >= 20) return "HOT";
        if (lastActivity != null && lastActivity.isBefore(LocalDateTime.now().minusDays(7))) return "INACTIVE";
        return "ACTIVE";
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}
