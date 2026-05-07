package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.GuideStep;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GuideStepDAO {

    private Connection con = DatabaseConnection.getInstance().getConnection();

    public List<GuideStep> getByGuideId(int guideId) {
        List<GuideStep> steps = new ArrayList<>();
        String sql = "SELECT * FROM guide_step WHERE guide_id=? ORDER BY step_order";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, guideId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                GuideStep s = new GuideStep();
                s.setId(rs.getInt("id"));
                s.setGuideId(rs.getInt("guide_id"));
                s.setTitle(rs.getString("title"));
                s.setContent(rs.getString("content"));
                s.setStepOrder(rs.getInt("step_order"));
                s.setImage(rs.getString("image"));
                s.setVideoUrl(rs.getString("video_url"));
                steps.add(s);
            }
        } catch (SQLException e) {
        }
        return steps;
    }

    public void add(GuideStep s) {
        String sql = "INSERT INTO guide_step (guide_id, title, content, step_order, image, video_url) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, s.getGuideId());
            ps.setString(2, s.getTitle());
            ps.setString(3, s.getContent());
            ps.setInt(4, s.getStepOrder());
            ps.setString(5, s.getImage());
            ps.setString(6, s.getVideoUrl());
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public void update(GuideStep s) {
        String sql = "UPDATE guide_step SET title=?, content=?, step_order=?, image=?, video_url=? WHERE id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, s.getTitle());
            ps.setString(2, s.getContent());
            ps.setInt(3, s.getStepOrder());
            ps.setString(4, s.getImage());
            ps.setString(5, s.getVideoUrl());
            ps.setInt(6, s.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM guide_step WHERE id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public List<GuideStep> getAll() {
        List<GuideStep> steps = new ArrayList<>();
        String sql = "SELECT * FROM guide_step ORDER BY guide_id, step_order";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                GuideStep s = new GuideStep();
                s.setId(rs.getInt("id"));
                s.setGuideId(rs.getInt("guide_id"));
                s.setTitle(rs.getString("title"));
                s.setContent(rs.getString("content"));
                s.setStepOrder(rs.getInt("step_order"));
                s.setImage(rs.getString("image"));
                s.setVideoUrl(rs.getString("video_url"));
                steps.add(s);
            }
        } catch (SQLException e) {
        }
        return steps;
    }
}