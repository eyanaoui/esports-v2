package com.esports.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class TeamTest {

    private Team team;

    @BeforeEach
    void setUp() {
        // Arrange
        team = new Team("Team Liquid", "liquid_logo.png", "Pro gaming team", 5);
    }

    @Test
    @DisplayName("Should correctly set and get team details")
    void testTeamDetails() {
        // Act & Assert
        assertEquals("Team Liquid", team.getName());
        assertEquals("liquid_logo.png", team.getLogo());
        assertEquals("Pro gaming team", team.getDescription());
    }

    @Test
    @DisplayName("Should validate captain ID is assigned")
    void testCaptainId() {
        // Assert
        assertEquals(5, team.getCaptain_id(), "Captain ID should be correctly assigned.");
        assertTrue(team.getCaptain_id() > 0, "Captain ID must be a positive integer.");
    }

    @Test
    @DisplayName("Should update team name correctly")
    void testUpdateName() {
        // Act
        team.setName("Navi");
        // Assert
        assertEquals("Navi", team.getName());
    }
}