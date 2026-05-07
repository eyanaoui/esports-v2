package com.esports.models;

import java.time.LocalDateTime;

public class GuideRating {
    private int id;
    private int guideId;
    private int userId;
    private int ratingValue;
    private String comment;
    private LocalDateTime createdAt;

    public GuideRating() {}

    public GuideRating(int guideId, int userId, int ratingValue, String comment) {
        this.guideId = guideId;
        this.userId = userId;
        this.ratingValue = ratingValue;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getGuideId() { return guideId; }
    public void setGuideId(int guideId) { this.guideId = guideId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getRatingValue() { return ratingValue; }
    public void setRatingValue(int ratingValue) { this.ratingValue = ratingValue; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return "Rating: " + ratingValue; }
}