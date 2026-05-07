package com.esports.controllers.admin;

import com.esports.dao.GameDAO;
import com.esports.dao.GuideDAO;
import com.esports.dao.GuideRatingDAO;
import com.esports.db.DatabaseConnection;
import com.esports.models.Game;
import com.esports.models.Guide;
import com.esports.models.GuideRating;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class StatsController {

    @FXML private Label totalGamesLabel, totalGuidesLabel, totalRatingsLabel, avgRatingLabel;
    @FXML private BarChart<String, Number> guidesPerGameChart;
    @FXML private PieChart difficultyChart;
    @FXML private TableView<GuideStats> leaderboardTable;
    @FXML private TableColumn<GuideStats, Number> colRank, colAvgRating, colTotal;
    @FXML private TableColumn<GuideStats, String> colTitle, colGame;

    private GameDAO gameDAO     = new GameDAO();
    private GuideDAO guideDAO   = new GuideDAO();
    private GuideRatingDAO ratingDAO = new GuideRatingDAO();
    private Connection con = DatabaseConnection.getInstance().getConnection();

    @FXML
    public void initialize() {
        loadMetricCards();
        loadGuidesPerGameChart();
        loadDifficultyChart();
        loadLeaderboard();
    }

    private void loadMetricCards() {
        int totalGames   = gameDAO.getAll().size();
        int totalGuides  = guideDAO.getAll().size();
        int totalRatings = ratingDAO.getAll().size();

        double avgRating = ratingDAO.getAll().stream()
                .mapToInt(GuideRating::getRatingValue)
                .average()
                .orElse(0.0);

        totalGamesLabel.setText(String.valueOf(totalGames));
        totalGuidesLabel.setText(String.valueOf(totalGuides));
        totalRatingsLabel.setText(String.valueOf(totalRatings));
        avgRatingLabel.setText(String.format("%.1f", avgRating));
    }

    private void loadGuidesPerGameChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Guides");
        for (Game game : gameDAO.getAll()) {
            int count = guideDAO.getByGameId(game.getId()).size();
            series.getData().add(new XYChart.Data<>(game.getName(), count));
        }
        guidesPerGameChart.getData().clear();
        guidesPerGameChart.getData().add(series);
    }

    private void loadDifficultyChart() {
        long easy   = guideDAO.getAll().stream().filter(g -> "Easy".equals(g.getDifficulty())).count();
        long medium = guideDAO.getAll().stream().filter(g -> "Medium".equals(g.getDifficulty())).count();
        long hard   = guideDAO.getAll().stream().filter(g -> "Hard".equals(g.getDifficulty())).count();

        difficultyChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Easy",   easy),
                new PieChart.Data("Medium", medium),
                new PieChart.Data("Hard",   hard)
        ));
    }

    private void loadLeaderboard() {
        colRank.setCellValueFactory(c -> new SimpleIntegerProperty(
                leaderboardTable.getItems().indexOf(c.getValue()) + 1));
        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().title));
        colGame.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().game));
        colAvgRating.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().avgRating));
        colTotal.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().totalRatings));

        try {
            String sql = """
                SELECT g.title, ga.name as game_name,
                       ROUND(AVG(gr.rating_value), 2) as avg_rating,
                       COUNT(gr.id) as total_ratings
                FROM guide g
                JOIN game ga ON g.game_id = ga.id
                JOIN guide_rating gr ON gr.guide_id = g.id
                GROUP BY g.id
                ORDER BY avg_rating DESC
                LIMIT 10
                """;
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            var list = FXCollections.<GuideStats>observableArrayList();
            while (rs.next()) {
                list.add(new GuideStats(
                        rs.getString("title"),
                        rs.getString("game_name"),
                        rs.getDouble("avg_rating"),
                        rs.getInt("total_ratings")
                ));
            }
            leaderboardTable.setItems(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // inner class for leaderboard data
    public static class GuideStats {
        String title, game;
        double avgRating;
        int totalRatings;

        GuideStats(String title, String game, double avgRating, int totalRatings) {
            this.title = title;
            this.game = game;
            this.avgRating = avgRating;
            this.totalRatings = totalRatings;
        }
    }
}