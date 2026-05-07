package com.esports.models;

public class Guide {
    private int id;
    private int gameId;
    private String title;
    private String description;
    private String difficulty;
    private int authorId;
    private String coverImage;

    public Guide() {}

    public Guide(int gameId, String title, String description, String difficulty, int authorId, String coverImage) {
        this.gameId = gameId;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.authorId = authorId;
        this.coverImage = coverImage;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getGameId() { return gameId; }
    public void setGameId(int gameId) { this.gameId = gameId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    @Override
    public String toString() { return title; }
}