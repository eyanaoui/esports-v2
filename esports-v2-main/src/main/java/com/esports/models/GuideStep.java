package com.esports.models;

public class GuideStep {
    private int id;
    private int guideId;
    private String title;
    private String content;
    private int stepOrder;
    private String image;
    private String videoUrl;

    public GuideStep() {}

    public GuideStep(int guideId, String title, String content, int stepOrder, String image, String videoUrl) {
        this.guideId = guideId;
        this.title = title;
        this.content = content;
        this.stepOrder = stepOrder;
        this.image = image;
        this.videoUrl = videoUrl;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getGuideId() { return guideId; }
    public void setGuideId(int guideId) { this.guideId = guideId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    @Override
    public String toString() { return title; }
}