package com.esports;

import com.esports.dao.OAuthTokenDAO;
import com.esports.dao.UserDAO;
import com.esports.models.GoogleUserProfile;
import com.esports.models.OAuthTokens;
import com.esports.models.User;
import com.esports.models.UserRole;
import com.esports.services.OAuthService;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for authenticating users via multiple methods.
 * 
 * Supports:
 * - Traditional email/password authentication with BCrypt
 * - Google OAuth 2.0 authentication
 * - Session token generation for authenticated users
 * 
 * Requirements: 1.3, 1.4, 1.5, 1.6, 1.7, 1.8
 */
public class AuthenticationService {
    
    private UserDAO userDAO;
    private OAuthService oauthService;
    private OAuthTokenDAO oauthTokenDAO;
    
    public AuthenticationService() {
        this.userDAO = new UserDAO();
        this.oauthService = new OAuthService();
        this.oauthTokenDAO = new OAuthTokenDAO();
    }
    
    /**
     * Constructor for testing with dependency injection.
     */
    AuthenticationService(UserDAO userDAO, OAuthService oauthService, OAuthTokenDAO oauthTokenDAO) {
        this.userDAO = userDAO;
        this.oauthService = oauthService;
        this.oauthTokenDAO = oauthTokenDAO;
    }
    
    /**
     * Authenticate user with email and password.
     * 
     * @param email User email address
     * @param password Plain text password
     * @return Authenticated User object, or null if authentication fails
     * 
     * Requirement 1.8: Support both Google OAuth and traditional email/password authentication
     */
    public User authenticate(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return null;
        }
        
        try {
            User user = userDAO.findByEmail(email);
            if (user == null) {
                return null;
            }
            
            if (verifyBcryptPassword(password, user.getPassword())) {
                // Update last login timestamp
                userDAO.updateLastLogin(user.getId());
                user.setLastLogin(LocalDateTime.now());
                return user;
            }
            
            return null;
        } catch (RuntimeException e) {
            System.out.println("[ERROR] Authentication error: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Authenticate user with Google OAuth.
     * 
     * This method handles the complete OAuth flow:
     * 1. Fetch user profile from Google using access token
     * 2. Check if user exists by Google ID or email
     * 3. Link Google account to existing user OR create new user
     * 4. Store OAuth tokens
     * 5. Generate session token
     * 
     * @param accessToken Encrypted OAuth access token
     * @param refreshToken Encrypted OAuth refresh token
     * @param expiresAt Token expiration timestamp
     * @return Authenticated User object
     * @throws RuntimeException if OAuth authentication fails
     * 
     * Requirement 1.3: Retrieve user profile from Google
     * Requirement 1.4: Link Google account to existing user account
     * Requirement 1.5: Create new user account from Google profile
     * Requirement 1.6: Store profile picture URL
     * Requirement 1.7: Generate session token for OAuth-authenticated users
     * Requirement 1.8: Support both Google OAuth and traditional authentication
     */
    public User authenticateWithOAuth(String accessToken, String refreshToken, java.sql.Timestamp expiresAt) {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be null or empty");
        }
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Refresh token cannot be null or empty");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiration timestamp cannot be null");
        }
        
        try {
            // Fetch user profile from Google
            GoogleUserProfile profile = oauthService.fetchUserProfile(accessToken);
            
            if (profile == null || profile.getEmail() == null || profile.getEmail().isEmpty()) {
                throw new RuntimeException("Failed to retrieve valid user profile from Google");
            }
            
            // Check if user already exists by Google ID
            User user = userDAO.findByGoogleId(profile.getGoogleId());
            
            if (user != null) {
                // User exists with this Google ID - update profile picture and last login
                user.setProfilePictureUrl(profile.getProfilePictureUrl());
                user.setLastLogin(LocalDateTime.now());
                userDAO.update(user);
                
                // Update OAuth tokens
                oauthTokenDAO.saveTokens(user.getId(), accessToken, refreshToken, expiresAt);
                
                System.out.println("[SUCCESS] OAuth authentication successful for existing user: " + user.getEmail());
                return user;
            }
            
            // Check if user exists by email (account linking)
            user = userDAO.findByEmail(profile.getEmail());
            
            if (user != null) {
                // Link Google account to existing user
                user.setGoogleId(profile.getGoogleId());
                user.setProfilePictureUrl(profile.getProfilePictureUrl());
                user.setLastLogin(LocalDateTime.now());
                userDAO.update(user);
                
                // Store OAuth tokens
                oauthTokenDAO.saveTokens(user.getId(), accessToken, refreshToken, expiresAt);
                
                System.out.println("[SUCCESS] Google account linked to existing user: " + user.getEmail());
                return user;
            }
            
            // Create new user from Google profile
            user = createUserFromGoogleProfile(profile);
            
            // Store OAuth tokens
            oauthTokenDAO.saveTokens(user.getId(), accessToken, refreshToken, expiresAt);
            
            System.out.println("[SUCCESS] New user created from Google profile: " + user.getEmail());
            return user;
            
        } catch (RuntimeException e) {
            System.out.println("[ERROR] OAuth authentication error: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Create a new user account from Google profile information.
     * 
     * @param profile Google user profile
     * @return Created User object with ID populated
     * @throws RuntimeException if user creation fails
     * 
     * Requirement 1.5: Create new user account from Google profile
     * Requirement 1.6: Store profile picture URL
     */
    private User createUserFromGoogleProfile(GoogleUserProfile profile) {
        User user = new User();
        
        // Parse name into first and last name
        String fullName = profile.getName() != null ? profile.getName() : "";
        String[] nameParts = fullName.split(" ", 2);
        user.setFirstName(nameParts.length > 0 ? nameParts[0] : "");
        user.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        
        user.setEmail(profile.getEmail());
        user.setGoogleId(profile.getGoogleId());
        user.setProfilePictureUrl(profile.getProfilePictureUrl());
        user.setPreferredAuthMethod("GOOGLE");
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        user.setIsBlocked(false);
        
        // Set default role
        List<UserRole> roles = new ArrayList<>();
        roles.add(UserRole.USER);
        user.setRoles(roles);
        
        // No password for OAuth-only users (set to null or empty)
        user.setPassword(null);
        
        // Add user to database
        boolean success = userDAO.add(user);
        if (!success) {
            throw new RuntimeException("Failed to create user from Google profile");
        }
        
        // Retrieve the created user to get the generated ID
        User createdUser = userDAO.findByEmail(profile.getEmail());
        if (createdUser == null) {
            throw new RuntimeException("Failed to retrieve created user");
        }
        
        return createdUser;
    }
    
    private boolean verifyBcryptPassword(String plainPassword, String hashedPassword) {
        try {
            // BCrypt.checkpw handles $2y$ format automatically
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            // Invalid hash format or verification error
            System.out.println("[WARNING] Password verification error: " + e.getMessage());
            return false;
        }
    }
}
