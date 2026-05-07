package com.esports.models;

public class ForumUserScore {
    private int id;
    private int userId;
    private int score;
    private int messagesCount;
    private int bestAnswersCount;
    private int likesReceived;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getMessagesCount() { return messagesCount; }
    public void setMessagesCount(int messagesCount) { this.messagesCount = messagesCount; }
    public int getBestAnswersCount() { return bestAnswersCount; }
    public void setBestAnswersCount(int bestAnswersCount) { this.bestAnswersCount = bestAnswersCount; }
    public int getLikesReceived() { return likesReceived; }
    public void setLikesReceived(int likesReceived) { this.likesReceived = likesReceived; }
}
