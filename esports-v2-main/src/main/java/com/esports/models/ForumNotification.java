package com.esports.models;

import java.time.LocalDateTime;

public class ForumNotification {
    private int id;
    private Integer userId;
    private Integer sujetId;
    private Integer messageId;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getSujetId() { return sujetId; }
    public void setSujetId(Integer sujetId) { this.sujetId = sujetId; }
    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
