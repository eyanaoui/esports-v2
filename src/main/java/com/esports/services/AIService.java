package com.esports.services;

import com.esports.dao.TournamentDAO;
import java.util.*;

public class AIService {
    private final TournamentDAO dao = new TournamentDAO();

    public String getTournamentPrediction(int id) {
        List<Map<String, Object>> teams = dao.getTournamentTeamStats(id);
        if (teams.isEmpty()) return "AI: No registered teams found for analysis.";

        Map<String, Object> winner = teams.stream()
                .max(Comparator.comparingDouble(m -> (double) m.get("elo")))
                .get();

        double winProb = 72.5 + ((double) winner.get("elo") / 200);

        return "════ NEURAL PREDICTION ════\n" +
                "PREDICTED CHAMPION: " + winner.get("name").toString().toUpperCase() + "\n" +
                "ELO RATING: " + winner.get("elo") + "\n" +
                "WIN PROBABILITY: " + String.format("%.1f%%", Math.min(99.4, winProb)) + "\n" +
                "═══════════════════════════";
    }

    public List<Map<String, Object>> generateFairMatchmaking(int tournamentId) {
        List<Map<String, Object>> teams = dao.getTournamentTeamStats(tournamentId);

        // The "AI" Logic: Sort by Elo
        teams.sort(Comparator.comparingDouble(m -> (double) m.get("elo")));

        return teams;
    }
}