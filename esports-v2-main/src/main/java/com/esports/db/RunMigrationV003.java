package com.esports.db;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Simple script to run migration V003.
 * This adds phone_number column and password_reset_tokens table.
 */
public class RunMigrationV003 {
    
    public static void main(String[] args) {
        System.out.println("=== Running Migration V003 ===\n");
        
        Connection conn = null;
        Statement stmt = null;
        
        try {
            // Get database connection
            conn = DatabaseConnection.getInstance().getConnection();
            stmt = conn.createStatement();
            
            // Step 1: Add phone_number column to user table
            System.out.println("[1/2] Adding phone_number column to user table...");
            try {
                stmt.execute("ALTER TABLE user ADD COLUMN phone_number VARCHAR(20) AFTER email");
                System.out.println("      [SUCCESS] phone_number column added");
            } catch (Exception e) {
                if (e.getMessage().contains("Duplicate column")) {
                    System.out.println("      [SKIP] phone_number column already exists");
                } else {
                    throw e;
                }
            }
            
            // Step 2: Create password_reset_tokens table
            System.out.println("\n[2/2] Creating password_reset_tokens table...");
            String createTableSQL = 
                "CREATE TABLE IF NOT EXISTS password_reset_tokens (" +
                "    id INT AUTO_INCREMENT PRIMARY KEY," +
                "    user_id INT NOT NULL," +
                "    token VARCHAR(6) NOT NULL," +
                "    expires_at TIMESTAMP NOT NULL," +
                "    used BOOLEAN DEFAULT FALSE," +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE," +
                "    INDEX idx_token (token)," +
                "    INDEX idx_user_id (user_id)," +
                "    INDEX idx_expires_at (expires_at)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            
            stmt.execute(createTableSQL);
            System.out.println("      [SUCCESS] password_reset_tokens table created");
            
            System.out.println("\n=== Migration V003 Completed Successfully! ===");
            System.out.println("The SMS password reset feature is now ready to use.");
            
        } catch (Exception e) {
            System.err.println("\n[ERROR] Migration failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (Exception e) {
                System.err.println("[WARNING] Failed to close statement: " + e.getMessage());
            }
        }
    }
}
