package com.esports.models;

import java.time.LocalDateTime;

public class ForumActivity {
    private int id;
    private Integer userId;
    private Integer sujetId;
    private Integer messageId;
    private String actionType;
    private String description;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getSujetId() { return sujetId; }
    public void setSujetId(Integer sujetId) { this.sujetId = sujetId; }
    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
