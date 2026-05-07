package com.esports.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Utility class to run database migrations.
 */
public class MigrationRunner {
    
    /**
     * Execute a migration SQL file.
     * 
     * @param migrationFile The migration file path (e.g., "db/migrations/V003__add_phone_number_and_password_reset.sql")
     * @return true if migration executed successfully
     */
    public static boolean executeMigration(String migrationFile) {
        Connection conn = null;
        Statement stmt = null;
        
        try {
            // Get database connection
            conn = DatabaseConnection.getInstance().getConnection();
            
            // Read SQL file from resources
            InputStream is = MigrationRunner.class.getClassLoader().getResourceAsStream(migrationFile);
            if (is == null) {
                System.err.println("[ERROR] Migration file not found: " + migrationFile);
                return false;
            }
            
            String sql = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .collect(Collectors.joining("\n"));
            
            // Split SQL into individual statements (separated by semicolons)
            String[] statements = sql.split(";");
            
            stmt = conn.createStatement();
            
            // Execute each statement
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    System.out.println("[MIGRATION] Executing: " + trimmed.substring(0, Math.min(50, trimmed.length())) + "...");
                    stmt.execute(trimmed);
                }
            }
            
            System.out.println("[SUCCESS] Migration executed successfully: " + migrationFile);
            return true;
            
        } catch (Exception e) {
            System.err.println("[ERROR] Migration failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (Exception e) {
                System.err.println("[WARNING] Failed to close statement: " + e.getMessage());
            }
        }
    }
    
    /**
     * Main method to run migrations manually.
     */
    public static void main(String[] args) {
        System.out.println("=== Database Migration Runner ===");
        
        // Execute V003 migration
        boolean success = executeMigration("db/migrations/V003__add_phone_number_and_password_reset.sql");
        
        if (success) {
            System.out.println("\n[SUCCESS] Migration V003 completed successfully!");
            System.out.println("   - Added phone_number column to user table");
            System.out.println("   - Created password_reset_tokens table");
        } else {
            System.err.println("\n[ERROR] Migration V003 failed!");
            System.err.println("   Please check the error messages above.");
        }
    }
}
