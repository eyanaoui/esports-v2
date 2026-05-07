package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.Tournament;
import java.sql.*;
import java.util.*;

public class TournamentDAO {
    private final Connection con = DatabaseConnection.getInstance().getConnection();

    public void add(Tournament t) {
        String sql = "INSERT INTO tournament (name, game, start_date, end_date, registration_deadline, status, description, format, max_teams, prize, organizer_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getGame());
            ps.setString(3, t.getStartDate());
            ps.setString(4, t.getEndDate());
            ps.setString(5, t.getRegistrationDeadline());
            ps.setString(6, t.getStatus());
            ps.setString(7, t.getDescription());
            ps.setString(8, t.getFormat());
            ps.setInt(9, t.getMaxTeams());
            ps.setString(10, t.getPrize());
            ps.setInt(11, 3);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void update(Tournament t) {
        String sql = "UPDATE tournament SET name=?, game=?, start_date=?, status=?, description=?, format=?, max_teams=?, prize=? WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getGame());
            ps.setString(3, t.getStartDate());
            ps.setString(4, t.getStatus());
            ps.setString(5, t.getDescription());
            ps.setString(6, t.getFormat());
            ps.setInt(7, t.getMaxTeams());
            ps.setString(8, t.getPrize());
            ps.setInt(9, t.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    public void delete(int id) {
        String sql = "DELETE FROM tournament WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    public List<Tournament> getAll() {
        List<Tournament> list = new ArrayList<>();
        String sql = "SELECT * FROM tournament ORDER BY id DESC";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Tournament t = new Tournament();
                t.setId(rs.getInt("id"));
                t.setName(rs.getString("name"));
                t.setGame(rs.getString("game"));
                t.setStartDate(rs.getString("start_date"));
                t.setStatus(rs.getString("status"));
                t.setDescription(rs.getString("description"));
                t.setFormat(rs.getString("format"));
                t.setMaxTeams(rs.getInt("max_teams"));
                t.setPrize(rs.getString("prize"));
                list.add(t);
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public List<Map<String, Object>> getTournamentTeamStats(int tournamentId) {
        List<Map<String, Object>> analysisData = new ArrayList<>();
        String sql = "SELECT ts.elo_rating, t.name as team_name FROM team_stats ts " +
                "JOIN team t ON ts.team_id = t.id " +
                "JOIN tournament_teams tt ON t.id = tt.team_id " +
                "WHERE tt.tournament_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, tournamentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("name", rs.getString("team_name"));
                    stats.put("elo", rs.getDouble("elo_rating"));
                    analysisData.add(stats);
                }
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return analysisData;
    }
}