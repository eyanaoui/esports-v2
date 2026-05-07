package com.esports.services;

import com.esports.dao.MessageDao;
import com.esports.dao.SujetDao;
import com.esports.models.Message;
import com.esports.models.Sujet;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ForumPdfExportService {
    private final SujetDao sujetDao = new SujetDao();
    private final MessageDao messageDao = new MessageDao();

    public void exportTopicToPdf(int sujetId, File destination) throws Exception {
        Sujet sujet = sujetDao.getById(sujetId);
        if (sujet == null) throw new IllegalArgumentException("Sujet introuvable");

        File output = destination;
        if (output.isDirectory()) {
            output = new File(output, "forum_topic_" + sujetId + ".pdf");
        }
        if (!output.getName().toLowerCase().endsWith(".pdf")) {
            output = new File(output.getParentFile(), "forum_topic_" + sujetId + ".pdf");
        }

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(output));
        document.open();
        document.add(new Paragraph("Forum Topic Export"));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Titre: " + safe(sujet.getTitre())));
        document.add(new Paragraph("Contenu: " + safe(sujet.getContenu())));
        document.add(new Paragraph("Status: " + safe(sujet.getStatus())));
        document.add(new Paragraph("Trending score: " + sujet.getTrendingScore()));
        document.add(new Paragraph("Résumé: " + safe(sujet.getAutoSummary())));
        document.add(new Paragraph("Mots-clés: " + safe(sujet.getKeywords())));
        String googleLink = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(
                safe(sujet.getTitre()) + " game", java.nio.charset.StandardCharsets.UTF_8
        );
        document.add(new Paragraph("QR/Link: " + googleLink));
        document.add(new Paragraph("Date export: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Messages ACCEPTED:"));

        List<Message> messages = messageDao.getAcceptedBySujet(sujetId);
        if (messages.isEmpty()) {
            document.add(new Paragraph("- Aucun message ACCEPTED"));
        } else {
            for (Message m : messages) {
                document.add(new Paragraph("- " + safe(m.getContenu())));
            }
        }
        document.close();
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}
