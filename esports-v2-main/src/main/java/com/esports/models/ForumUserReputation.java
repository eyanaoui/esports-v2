package com.esports.models;

import java.time.LocalDateTime;

public class ForumUserReputation {
    private int id;
    private int userId;
    private int score;
    private String level;
    private int messagesCount;
    private int likesReceived;
    private int bestAnswersCount;
    private int rejectedMessagesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ForumUserReputation() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public int getMessagesCount() { return messagesCount; }
    public void setMessagesCount(int messagesCount) { this.messagesCount = messagesCount; }
    public int getLikesReceived() { return likesReceived; }
    public void setLikesReceived(int likesReceived) { this.likesReceived = likesReceived; }
    public int getBestAnswersCount() { return bestAnswersCount; }
    public void setBestAnswersCount(int bestAnswersCount) { this.bestAnswersCount = bestAnswersCount; }
    public int getRejectedMessagesCount() { return rejectedMessagesCount; }
    public void setRejectedMessagesCount(int rejectedMessagesCount) { this.rejectedMessagesCount = rejectedMessagesCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
