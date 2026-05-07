package com.esports.services;

import com.esports.dao.SujetDao;
import com.esports.models.Sujet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForumDuplicateService {
    private final SujetDao sujetDao = new SujetDao();

    public List<Sujet> findSimilarTopics(String title, String content) {
        List<Sujet> all = sujetDao.getAll();
        List<Sujet> similar = new ArrayList<>();
        String source = safe(title) + " " + safe(content);
        for (Sujet s : all) {
            double sim = calculateSimilarity(source, safe(s.getTitre()) + " " + safe(s.getContenu()));
            if (sim > 60.0) similar.add(s);
        }
        return similar;
    }

    public double calculateSimilarity(String text1, String text2) {
        Set<String> a = tokenize(text1);
        Set<String> b = tokenize(text2);
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (inter.size() * 100.0) / union.size();
    }

    private Set<String> tokenize(String text) {
        Set<String> out = new HashSet<>();
        if (text == null) return out;
        String[] words = text.toLowerCase().split("[^a-zà-ÿ0-9]+");
        for (String w : words) {
            if (w != null && w.length() > 2) out.add(w);
        }
        return out;
    }

    private String safe(String v) { return v == null ? "" : v; }
}
