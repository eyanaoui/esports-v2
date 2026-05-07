package com.esports.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utility to inspect the current database schema.
 */
public class DatabaseInspector {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Database Schema Inspector");
        System.out.println("=".repeat(80));
        
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            
            System.out.println("\nDatabase: " + metaData.getDatabaseProductName());
            System.out.println("Version: " + metaData.getDatabaseProductVersion());
            System.out.println("URL: " + metaData.getURL());
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("Existing Tables:");
            System.out.println("=".repeat(80));
            
            ResultSet tables = metaData.getTables("esports_db", null, "%", new String[]{"TABLE"});
            
            boolean hasTable = false;
            while (tables.next()) {
                hasTable = true;
                String tableName = tables.getString("TABLE_NAME");
                System.out.println("  - " + tableName);
            }
            
            if (!hasTable) {
                System.out.println("  (No tables found in database)");
            }
            
            tables.close();
            
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to inspect database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
