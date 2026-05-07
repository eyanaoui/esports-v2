package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.Team;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TeamDAO {
    private Connection con = DatabaseConnection.getInstance().getConnection();

    public List<Team> getAll() {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM team";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Team t = new Team();
                t.setId(rs.getInt("id"));
                t.setName(rs.getString("name"));
                t.setLogo(rs.getString("logo"));
                t.setDescription(rs.getString("description"));
                t.setCaptain_id(rs.getInt("captain_id"));
                if (rs.getTimestamp("created_at") != null)
                    t.setCreated_at(rs.getTimestamp("created_at").toLocalDateTime());
                if (rs.getTimestamp("updated_at") != null)
                    t.setUpdated_at(rs.getTimestamp("updated_at").toLocalDateTime());
                teams.add(t);
            }
        } catch (SQLException e) {
            System.out.println("❌ getAll error: " + e.getMessage());
        }
        return teams;
    }

    public void add(Team t) {
        String sql = "INSERT INTO team (name, logo, description, captain_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, t.getName());
            ps.setString(2, t.getLogo());
            ps.setString(3, t.getDescription());
            ps.setInt(4, t.getCaptain_id());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            System.out.println("✅ Team added!");
        } catch (SQLException e) {
            System.out.println("❌ add error: " + e.getMessage());
        }
    }

    public void update(Team t) {
        String sql = "UPDATE team SET name=?, logo=?, description=?, captain_id=?, updated_at=? WHERE id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, t.getName());
            ps.setString(2, t.getLogo());
            ps.setString(3, t.getDescription());
            ps.setInt(4, t.getCaptain_id());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(6, t.getId());
            ps.executeUpdate();
            System.out.println("✅ Team updated!");
        } catch (SQLException e) {
            System.out.println("❌ update error: " + e.getMessage());
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM team WHERE id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Team deleted!");
        } catch (SQLException e) {
            System.out.println("❌ delete error: " + e.getMessage());
        }
    }
}