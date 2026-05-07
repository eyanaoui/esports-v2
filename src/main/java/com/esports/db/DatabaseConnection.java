package com.esports.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;

    private static final String URL =
            "jdbc:mysql://localhost:3306/esports_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";

    private static final String USER = "root";
    private static final String PASSWORD = "";

    private DatabaseConnection() {
        connect();
    }

    private void connect() {
        try {
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            System.out.println("❌ Error checking connection: " + e.getMessage());
            connect();
        }

        return connection;
    }
}