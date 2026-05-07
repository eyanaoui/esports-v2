package com.esports.models;

import java.time.LocalDateTime;

public class MessageHistory {
    private int id;
    private int messageId;
    private String oldContent;
    private String newContent;
    private LocalDateTime dateModif;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMessageId() { return messageId; }
    public void setMessageId(int messageId) { this.messageId = messageId; }
    public String getOldContent() { return oldContent; }
    public void setOldContent(String oldContent) { this.oldContent = oldContent; }
    public String getNewContent() { return newContent; }
    public void setNewContent(String newContent) { this.newContent = newContent; }
    public LocalDateTime getDateModif() { return dateModif; }
    public void setDateModif(LocalDateTime dateModif) { this.dateModif = dateModif; }
}
