package com.esports.services;

import com.esports.dao.MessageDao;
import com.esports.models.ModerationResult;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForumModerationService {

    private static final Pattern LINK_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern LETTER_PATTERN = Pattern.compile(".*[A-Za-zÀ-ÿ].*");
    private final MessageDao messageDao = new MessageDao();

    public void approveMessage(int messageId) {
        messageDao.updateStatus(messageId, "ACCEPTED", "Approved by admin");
    }

    public void rejectMessage(int messageId, String reason) {
        messageDao.updateStatus(messageId, "REJECTED", reason == null || reason.isBlank() ? "Rejected by admin" : reason);
    }

    public ModerationResult moderateMessage(String message, int sujetId) {
        String content = message == null ? "" : message.trim();
        if (content.length() < 5) {
            return new ModerationResult("REJECTED", 0.95, "Message trop court");
        }
        if (!content.isEmpty() && Character.isDigit(content.charAt(0))) {
            return new ModerationResult("REJECTED", 0.98, "Le message ne doit pas commencer par un chiffre");
        }
        if (!LETTER_PATTERN.matcher(content).matches()) {
            return new ModerationResult("REJECTED", 0.99, "Le message doit contenir au moins une lettre");
        }

        int linkCount = countLinks(content);
        if (linkCount > 2) {
            return new ModerationResult("PENDING", 0.7, "Trop de liens externes");
        }
        if (messageDao.existsSameMessageInSujet(sujetId, content)) {
            return new ModerationResult("PENDING", 0.85, "Message répété");
        }
        if (hasWordRepeatedTooMuch(content)) {
            return new ModerationResult("PENDING", 0.75, "Mot répété trop souvent");
        }
        return new ModerationResult("ACCEPTED", Math.min(0.2 + (linkCount * 0.1), 0.5), null);
    }

    private int countLinks(String content) {
        Matcher m = LINK_PATTERN.matcher(content);
        int c = 0;
        while (m.find()) c++;
        return c;
    }

    private boolean hasWordRepeatedTooMuch(String content) {
        String[] parts = content.toLowerCase().split("[^a-zà-ÿ0-9]+");
        Map<String, Integer> counts = new HashMap<>();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            int next = counts.getOrDefault(p, 0) + 1;
            counts.put(p, next);
            if (next > 5) return true;
        }
        return false;
    }
}
