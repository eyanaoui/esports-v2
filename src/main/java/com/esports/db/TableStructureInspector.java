package com.esports.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility to inspect table structure.
 */
public class TableStructureInspector {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Table Structure Inspector - 'user' table");
        System.out.println("=".repeat(80));
        
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            Statement stmt = conn.createStatement();
            
            // Show table structure
            ResultSet rs = stmt.executeQuery("DESCRIBE user");
            
            System.out.println("\nColumns in 'user' table:");
            System.out.println("-".repeat(80));
            System.out.printf("%-30s %-20s %-10s %-10s%n", "Field", "Type", "Null", "Key");
            System.out.println("-".repeat(80));
            
            while (rs.next()) {
                String field = rs.getString("Field");
                String type = rs.getString("Type");
                String nullable = rs.getString("Null");
                String key = rs.getString("Key");
                
                System.out.printf("%-30s %-20s %-10s %-10s%n", field, type, nullable, key);
            }
            
            rs.close();
            stmt.close();
            
            System.out.println("=".repeat(80));
            
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to inspect table: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
