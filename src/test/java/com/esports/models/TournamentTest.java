package com.esports.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class TournamentTest {

    private Tournament tournament;

    @BeforeEach
    void setUp() {
        tournament = new Tournament();
    }

    @Test
    @DisplayName("Should correctly set and get tournament name")
    void testTournamentName() {
        String name = "Elite Cup 2026";
        tournament.setName(name);
        assertEquals(name, tournament.getName(), "The name should match the assigned value.");
    }

    @Test
    @DisplayName("Should validate max teams is positive")
    void testMaxTeamsPositive() {
        tournament.setMax_teams(16);
        assertTrue(tournament.getMax_teams() > 0, "Tournament should allow a positive number of teams.");
    }

    @Test
    @DisplayName("Should handle registration deadline logic")
    void testRegistrationDeadline() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime deadline = LocalDateTime.of(2026, 5, 25, 23, 59);

        tournament.setStart_date(start);
        tournament.setRegistration_deadline(deadline);

        // Logic check: Deadline must be before start
        assertTrue(tournament.getRegistration_deadline().isBefore(tournament.getStart_date()),
                "The registration deadline must occur before the tournament starts.");
    }

    @Test
    @DisplayName("Should correctly identify tournament status")
    void testStatusAssignment() {
        tournament.setStatus("Ongoing");
        assertEquals("Ongoing", tournament.getStatus());
    }
}