package com.esports.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Generates a comprehensive schema verification report.
 */
public class SchemaVerificationReport {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("ADVANCED FEATURES SUITE - SCHEMA VERIFICATION REPORT");
        System.out.println("=".repeat(80));
        
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            
            System.out.println("\nDatabase Information:");
            System.out.println("  Product: " + metaData.getDatabaseProductName());
            System.out.println("  Version: " + metaData.getDatabaseProductVersion());
            System.out.println("  URL: " + metaData.getURL());
            
            // Verify new tables
            System.out.println("\n" + "=".repeat(80));
            System.out.println("NEW TABLES VERIFICATION");
            System.out.println("=".repeat(80));
            
            String[] newTables = {
                "oauth_tokens",
                "user_signatures",
                "user_bans",
                "audit_logs",
                "signature_auth_attempts",
                "scheduled_exports"
            };
            
            for (String tableName : newTables) {
                verifyTable(conn, tableName);
            }
            
            // Verify user table modifications
            System.out.println("\n" + "=".repeat(80));
            System.out.println("USER TABLE MODIFICATIONS VERIFICATION");
            System.out.println("=".repeat(80));
            
            String[] newColumns = {
                "google_id",
                "profile_picture_url",
                "preferred_auth_method",
                "is_banned",
                "last_login"
            };
            
            for (String column : newColumns) {
                verifyColumn(conn, "user", column);
            }
            
            // Verify indexes
            System.out.println("\n" + "=".repeat(80));
            System.out.println("INDEXES VERIFICATION");
            System.out.println("=".repeat(80));
            
            verifyIndexes(conn);
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("✓ SCHEMA VERIFICATION COMPLETE");
            System.out.println("=".repeat(80));
            
        } catch (Exception e) {
            System.out.println("\n[ERROR] Verification failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void verifyTable(Connection conn, String tableName) {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM " + tableName);
            rs.next();
            int count = rs.getInt("count");
            System.out.println("✓ Table '" + tableName + "' exists (rows: " + count + ")");
            rs.close();
        } catch (Exception e) {
            System.out.println("X Table '" + tableName + "' NOT FOUND");
        }
    }
    
    private static void verifyColumn(Connection conn, String tableName, String columnName) {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("DESCRIBE " + tableName);
            boolean found = false;
            String type = "";
            
            while (rs.next()) {
                if (rs.getString("Field").equals(columnName)) {
                    found = true;
                    type = rs.getString("Type");
                    break;
                }
            }
            
            if (found) {
                System.out.println("✓ Column '" + tableName + "." + columnName + "' exists (type: " + type + ")");
            } else {
                System.out.println("X Column '" + tableName + "." + columnName + "' NOT FOUND");
            }
            
            rs.close();
        } catch (Exception e) {
            System.out.println("X Error verifying column '" + tableName + "." + columnName + "'");
        }
    }
    
    private static void verifyIndexes(Connection conn) {
        String[] expectedIndexes = {
            "user.idx_user_google",
            "user.idx_user_banned",
            "user.idx_user_last_login",
            "oauth_tokens.idx_oauth_expires",
            "user_bans.idx_bans_user",
            "user_bans.idx_bans_admin",
            "audit_logs.idx_audit_action",
            "audit_logs.idx_audit_user",
            "audit_logs.idx_audit_admin",
            "signature_auth_attempts.idx_attempts_user_time",
            "scheduled_exports.idx_scheduled_next_run"
        };
        
        for (String indexSpec : expectedIndexes) {
            String[] parts = indexSpec.split("\\.");
            String tableName = parts[0];
            String indexName = parts[1];
            
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                    "SHOW INDEX FROM " + tableName + " WHERE Key_name = '" + indexName + "'"
                );
                
                if (rs.next()) {
                    System.out.println("✓ Index '" + indexName + "' exists on table '" + tableName + "'");
                } else {
                    System.out.println("X Index '" + indexName + "' NOT FOUND on table '" + tableName + "'");
                }
                
                rs.close();
            } catch (Exception e) {
                System.out.println("X Error verifying index '" + indexName + "' on table '" + tableName + "'");
            }
        }
    }
}
