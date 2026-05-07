package com.esports.services;

import com.esports.dao.ForumReportDAO;
import com.esports.dao.SujetDao;
import com.esports.models.ForumReport;
import com.esports.models.ForumUserReputation;
import com.esports.models.Sujet;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.List;

public class ForumAdminPdfReportService {
    private final SujetDao sujetDao = new SujetDao();
    private final ForumStatisticsService stats = new ForumStatisticsService();
    private final ForumReputationService reputationService = new ForumReputationService();
    private final ForumReportDAO reportDAO = new ForumReportDAO();

    public void exportForumAdminReport(File destination) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(destination));
        document.open();
        document.add(new Paragraph("Rapport Admin Forum"));
        document.add(new Paragraph("Date export: " + LocalDateTime.now()));
        document.add(new Paragraph(" "));
        List<Sujet> topics = sujetDao.getAll();
        document.add(new Paragraph("Total sujets: " + topics.size()));
        int totalMessages = topics.stream().mapToInt(Sujet::getRepliesCount).sum();
        document.add(new Paragraph("Total messages: " + totalMessages));
        document.add(new Paragraph("ACCEPTED/PENDING/REJECTED: "
                + stats.getMessagesByStatusStats().getOrDefault("ACCEPTED", 0) + "/"
                + stats.getMessagesByStatusStats().getOrDefault("PENDING", 0) + "/"
                + stats.getMessagesByStatusStats().getOrDefault("REJECTED", 0)));
        document.add(new Paragraph("HOT sujets: " + stats.getHotTopicsCount()));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Top 5 sujets actifs:"));
        for (Sujet s : stats.getTop5TrendingTopics()) {
            document.add(new Paragraph("- " + s.getTitre() + " | score=" + s.getTrendingScore()));
        }
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Top users:"));
        for (ForumUserReputation u : reputationService.getTopUsers(5)) {
            document.add(new Paragraph("- User #" + u.getUserId() + " | " + u.getLevel() + " | score=" + u.getScore()));
        }
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Messages signalés (pending):"));
        for (ForumReport r : reportDAO.getPendingReports()) {
            document.add(new Paragraph("- report#" + r.getId() + " message#" + r.getMessageId() + " reason=" + r.getReason()));
        }
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Ces services avancés enrichissent le module Forum en le rendant plus intelligent, interactif et administrable. Le système recommande automatiquement des sujets similaires, détecte les doublons lors de la création d’un sujet, récompense les utilisateurs grâce à un score de réputation et des badges, permet de sauvegarder des sujets favoris, offre un mécanisme de signalement des messages, archive automatiquement les sujets inactifs et génère un rapport PDF complet pour l’administrateur."));
        document.close();
    }
}
