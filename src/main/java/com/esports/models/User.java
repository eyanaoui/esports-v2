package com.esports.models;

import java.time.LocalDateTime;
import java.util.List;

public class User {
    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private List<UserRole> roles;
    private String password;
    private LocalDateTime createdAt;
    private String profileImage;
    private String detectedSentiment;
    private Double sentimentScore;
    private Integer detectedAge;
    private Double ageConfidence;
    private String captureSource;
    private LocalDateTime captureTimestamp;
    private Boolean captureVerified;
    private Boolean isBlocked;
    private LocalDateTime blockedAt;
    private LocalDateTime blockExpiresAt;
    private String blockReason;
    private String googleId;
    private String profilePictureUrl;
    private String preferredAuthMethod;
    private LocalDateTime lastLogin;

    public User() {}

    public User(String firstName, String lastName, String email, String password, List<UserRole> roles) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.createdAt = LocalDateTime.now();
        this.isBlocked = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public List<UserRole> getRoles() { return roles; }
    public void setRoles(List<UserRole> roles) { this.roles = roles; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public String getDetectedSentiment() { return detectedSentiment; }
    public void setDetectedSentiment(String detectedSentiment) { this.detectedSentiment = detectedSentiment; }

    public Double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(Double sentimentScore) { this.sentimentScore = sentimentScore; }

    public Integer getDetectedAge() { return detectedAge; }
    public void setDetectedAge(Integer detectedAge) { this.detectedAge = detectedAge; }

    public Double getAgeConfidence() { return ageConfidence; }
    public void setAgeConfidence(Double ageConfidence) { this.ageConfidence = ageConfidence; }

    public String getCaptureSource() { return captureSource; }
    public void setCaptureSource(String captureSource) { this.captureSource = captureSource; }

    public LocalDateTime getCaptureTimestamp() { return captureTimestamp; }
    public void setCaptureTimestamp(LocalDateTime captureTimestamp) { this.captureTimestamp = captureTimestamp; }

    public Boolean getCaptureVerified() { return captureVerified; }
    public void setCaptureVerified(Boolean captureVerified) { this.captureVerified = captureVerified; }

    public Boolean getIsBlocked() { return isBlocked; }
    public void setIsBlocked(Boolean isBlocked) { this.isBlocked = isBlocked; }

    public LocalDateTime getBlockedAt() { return blockedAt; }
    public void setBlockedAt(LocalDateTime blockedAt) { this.blockedAt = blockedAt; }

    public LocalDateTime getBlockExpiresAt() { return blockExpiresAt; }
    public void setBlockExpiresAt(LocalDateTime blockExpiresAt) { this.blockExpiresAt = blockExpiresAt; }

    public String getBlockReason() { return blockReason; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

    public String getPreferredAuthMethod() { return preferredAuthMethod; }
    public void setPreferredAuthMethod(String preferredAuthMethod) { this.preferredAuthMethod = preferredAuthMethod; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    @Override
    public String toString() { 
        return firstName + " " + lastName; 
    }
}
