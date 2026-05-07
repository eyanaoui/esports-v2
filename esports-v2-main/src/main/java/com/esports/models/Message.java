package com.esports.models;

public class Message {
    private int id;
    private int sujetId; // This links the comment to the topic
    private String contenu;
    private String status;
    private double spamScore;
    private String moderationReason;
    private java.time.LocalDateTime createdAt;
    private int likes;
    private int dislikes;
    private boolean best;
    private String filePath;
    private java.time.LocalDateTime updatedAt;
    private Integer userId;
    private int reportCount;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSujetId() { return sujetId; }
    public void setSujetId(int sujetId) { this.sujetId = sujetId; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getSpamScore() { return spamScore; }
    public void setSpamScore(double spamScore) { this.spamScore = spamScore; }
    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public int getDislikes() { return dislikes; }
    public void setDislikes(int dislikes) { this.dislikes = dislikes; }
    public boolean isBest() { return best; }
    public void setBest(boolean best) { this.best = best; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }
}