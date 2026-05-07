package com.esports.models;

import java.time.LocalDateTime;

public class Team {
    private int id;
    private String name;
    private String logo;
    private String description;
    private int captain_id;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    public Team() {}

    // Constructor for fetching from Database
    public Team(int id, String name, String logo, String description, int captain_id, LocalDateTime created_at, LocalDateTime updated_at) {
        this.id = id;
        this.name = name;
        this.logo = logo;
        this.description = description;
        this.captain_id = captain_id;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }

    // Constructor for adding a new Team (Timestamps handled by DB)
    public Team(String name, String logo, String description, int captain_id) {
        this.name = name;
        this.logo = logo;
        this.description = description;
        this.captain_id = captain_id;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getCaptain_id() { return captain_id; }
    public void setCaptain_id(int captain_id) { this.captain_id = captain_id; }
    public LocalDateTime getCreated_at() { return created_at; }
    public void setCreated_at(LocalDateTime created_at) { this.created_at = created_at; }
    public LocalDateTime getUpdated_at() { return updated_at; }
    public void setUpdated_at(LocalDateTime updated_at) { this.updated_at = updated_at; }

    @Override
    public String toString() {
        return "Team{" + "nom='" + name + "', description='" + description + "'}";
    }

}