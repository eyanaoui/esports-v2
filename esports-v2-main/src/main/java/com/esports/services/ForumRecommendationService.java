package com.esports.services;

import com.esports.dao.SujetDao;
import com.esports.models.Message;
import com.esports.models.Sujet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ForumRecommendationService {
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "le","la","les","de","des","un","une","et","pour","dans","avec","the","a","an","to","in","on","for","of","du","au","aux"
    ));

    private final SujetDao sujetDao = new SujetDao();

    public List<Sujet> recommendSimilarTopics(int sujetId, int limit) {
        Sujet current = sujetDao.getById(sujetId);
        if (current == null) return List.of();
        String currentText = buildTopicText(current);
        List<Sujet> all = sujetDao.getAllTopicsWithMessagesGrouped();
        return all.stream()
                .filter(s -> s.getId() != sujetId)
                .sorted((a, b) -> Double.compare(
                        calculateSimilarity(buildTopicText(b), currentText),
                        calculateSimilarity(buildTopicText(a), currentText)))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public double calculateSimilarity(String text1, String text2) {
        Set<String> a = extractImportantWords(text1);
        Set<String> b = extractImportantWords(text2);
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (inter.size() * 100.0) / union.size();
    }

    public Set<String> extractImportantWords(String text) {
        Set<String> words = new HashSet<>();
        if (text == null) return words;
        for (String token : text.toLowerCase().split("[^a-zà-ÿ0-9]+")) {
            if (token == null || token.length() < 3 || STOP_WORDS.contains(token)) continue;
            words.add(token);
        }
        return words;
    }

    private String buildTopicText(Sujet s) {
        StringBuilder sb = new StringBuilder();
        sb.append(s.getTitre()).append(' ').append(s.getContenu()).append(' ');
        if (s.getMessages() != null) {
            for (Message m : s.getMessages()) sb.append(m.getContenu()).append(' ');
        }
        return sb.toString();
    }
}
