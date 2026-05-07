package com.esports.services;

import com.esports.dao.MessageDao;
import com.esports.dao.SujetDao;
import com.esports.models.Message;
import com.esports.models.Sujet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ForumKeywordService {
    private static final Set<String> STOP_WORDS = new LinkedHashSet<>(Arrays.asList(
            "le","la","les","un","une","de","des","du","et","est","pour","dans","avec","sur","qui","que",
            "donc","mais","ou","car","ce","cet","cette","ces","au","aux","en","par","se","sa","son","ses",
            "je","tu","il","elle","nous","vous","ils","elles","ne","pas","plus","moins","tres","aussi"
    ));

    private final SujetDao sujetDao = new SujetDao();
    private final MessageDao messageDao = new MessageDao();

    public String generateKeywords(int sujetId) {
        Sujet sujet = sujetDao.getById(sujetId);
        if (sujet == null) return "";

        StringBuilder corpus = new StringBuilder();
        corpus.append(safe(sujet.getTitre())).append(' ').append(safe(sujet.getContenu())).append(' ');
        List<Message> accepted = messageDao.getAcceptedBySujet(sujetId);
        for (Message m : accepted) corpus.append(safe(m.getContenu())).append(' ');

        Map<String, Integer> counts = buildWordCounts(corpus.toString());
        String result = counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(8)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));
        sujetDao.updateKeywords(sujetId, result);
        return result;
    }

    private Map<String, Integer> buildWordCounts(String text) {
        Map<String, Integer> map = new HashMap<>();
        String[] words = text.toLowerCase().split("[^a-zà-ÿ0-9]+");
        for (String w : words) {
            if (w == null || w.isBlank() || w.length() <= 2) continue;
            if (STOP_WORDS.contains(w)) continue;
            map.put(w, map.getOrDefault(w, 0) + 1);
        }
        return map;
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}
