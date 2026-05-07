package com.esports.models;

import java.util.ArrayList;
import java.util.List;

public class Sujet {
    private int id;
    private String titre;
    private String contenu;
    private String image;
    private double trendingScore;
    private String status;
    private String autoSummary;
    private String keywords;
    private java.time.LocalDateTime lastActivity;
    private int repliesCount;
    private java.time.LocalDateTime createdAt;
    private boolean pinned;
    private int viewsCount;
    private java.time.LocalDateTime updatedAt;
    private java.time.LocalDateTime archivedAt;
    private String archiveReason;
    private List<Message> messages = new ArrayList<>();

    // Empty constructor needed for DAO
    public Sujet() {}

    // Full constructor
    public Sujet(int id, String titre, String contenu, String image) {
        this.id = id;
        this.titre = titre;
        this.contenu = contenu;
        this.image = image;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public double getTrendingScore() { return trendingScore; }
    public void setTrendingScore(double trendingScore) { this.trendingScore = trendingScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAutoSummary() { return autoSummary; }
    public void setAutoSummary(String autoSummary) { this.autoSummary = autoSummary; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public java.time.LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(java.time.LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
    public int getRepliesCount() { return repliesCount; }
    public void setRepliesCount(int repliesCount) { this.repliesCount = repliesCount; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public int getViewsCount() { return viewsCount; }
    public void setViewsCount(int viewsCount) { this.viewsCount = viewsCount; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public java.time.LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(java.time.LocalDateTime archivedAt) { this.archivedAt = archivedAt; }
    public String getArchiveReason() { return archiveReason; }
    public void setArchiveReason(String archiveReason) { this.archiveReason = archiveReason; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
}
