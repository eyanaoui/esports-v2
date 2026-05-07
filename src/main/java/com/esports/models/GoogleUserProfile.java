package com.esports.models;

/**
 * Domain model representing a Google user profile retrieved via OAuth 2.0.
 * 
 * This model contains user information obtained from Google's UserInfo API
 * after successful OAuth authentication. Used to create or link user accounts.
 * 
 * Requirements: 1.3, 1.4, 1.6
 */
public class GoogleUserProfile {
    private String googleId;
    private String email;
    private String name;
    private String profilePictureUrl;
    private boolean emailVerified;

    // Constructors
    public GoogleUserProfile() {
    }

    public GoogleUserProfile(String googleId, String email, String name, String profilePictureUrl, boolean emailVerified) {
        this.googleId = googleId;
        this.email = email;
        this.name = name;
        this.profilePictureUrl = profilePictureUrl;
        this.emailVerified = emailVerified;
    }

    // Getters and Setters
    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @Override
    public String toString() {
        return "GoogleUserProfile{" +
                "googleId='" + googleId + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", profilePictureUrl='" + profilePictureUrl + '\'' +
                ", emailVerified=" + emailVerified +
                '}';
    }
}
