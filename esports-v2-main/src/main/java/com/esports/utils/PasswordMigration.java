package com.esports.utils;

import com.esports.dao.UserDAO;
import com.esports.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

/**
 * One-time migration utility to hash existing plain text passwords in the database.
 * 
 * This utility should be run ONCE after deploying the password hashing fix to convert
 * all existing plain text passwords to BCrypt hashes.
 * 
 * WARNING: This assumes all existing passwords in the database are plain text.
 * If some passwords are already hashed, this will double-hash them (breaking authentication).
 */
public class PasswordMigration {
    
    public static void main(String[] args) {
        System.out.println("=== Password Migration Utility ===");
        System.out.println("This will hash all plain text passwords in the database.");
        System.out.println();
        
        UserDAO userDAO = new UserDAO();
        List<User> allUsers = userDAO.getAll();
        
        if (allUsers.isEmpty()) {
            System.out.println("No users found in database. Nothing to migrate.");
            return;
        }
        
        System.out.println("Found " + allUsers.size() + " user(s) in database.");
        System.out.println();
        
        int migratedCount = 0;
        int skippedCount = 0;
        
        for (User user : allUsers) {
            String currentPassword = user.getPassword();
            
            // Check if password is already a BCrypt hash
            if (isBcryptHash(currentPassword)) {
                System.out.println("SKIPPED: " + user.getEmail() + " (already hashed)");
                skippedCount++;
                continue;
            }
            
            // Hash the plain text password
            String hashedPassword = BCrypt.hashpw(currentPassword, BCrypt.gensalt());
            user.setPassword(hashedPassword);
            
            // Update in database
            boolean success = userDAO.update(user);
            
            if (success) {
                System.out.println("MIGRATED: " + user.getEmail() + " (password hashed)");
                migratedCount++;
            } else {
                System.out.println("FAILED: " + user.getEmail() + " (update failed)");
            }
        }
        
        System.out.println();
        System.out.println("=== Migration Complete ===");
        System.out.println("Migrated: " + migratedCount + " user(s)");
        System.out.println("Skipped: " + skippedCount + " user(s) (already hashed)");
        System.out.println("Total: " + allUsers.size() + " user(s)");
    }
    
    /**
     * Check if a password string is a BCrypt hash.
     * BCrypt hashes start with $2a$, $2b$, or $2y$ and are 60 characters long.
     */
    private static boolean isBcryptHash(String password) {
        if (password == null || password.length() != 60) {
            return false;
        }
        return password.matches("^\\$2[ayb]\\$\\d{2}\\$.{53}$");
    }
}
