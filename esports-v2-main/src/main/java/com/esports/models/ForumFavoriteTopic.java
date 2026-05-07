package com.esports.models;

import java.time.LocalDateTime;

public class ForumFavoriteTopic {
    private int id;
    private int userId;
    private int sujetId;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getSujetId() { return sujetId; }
    public void setSujetId(int sujetId) { this.sujetId = sujetId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
