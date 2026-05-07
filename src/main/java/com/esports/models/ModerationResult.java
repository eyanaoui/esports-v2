package com.esports.models;

public class ModerationResult {
    private String status;
    private double spamScore;
    private String reason;

    public ModerationResult() {}

    public ModerationResult(String status, double spamScore, String reason) {
        this.status = status;
        this.spamScore = spamScore;
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getSpamScore() {
        return spamScore;
    }

    public void setSpamScore(double spamScore) {
        this.spamScore = spamScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
