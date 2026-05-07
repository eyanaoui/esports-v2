package com.esports.models;

import java.time.LocalDateTime;

public class Game {
    private int id;
    private String name;
    private String slug;
    private String description;
    private String coverImage;
    private boolean hasRanking;
    private LocalDateTime createdAt;

    public Game() {}

    public Game(String name, String slug, String description, String coverImage, boolean hasRanking) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.coverImage = coverImage;
        this.hasRanking = hasRanking;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public boolean isHasRanking() { return hasRanking; }
    public void setHasRanking(boolean hasRanking) { this.hasRanking = hasRanking; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return name; }
}