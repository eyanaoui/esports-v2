package com.esports;

import com.esports.dao.OAuthTokenDAO;
import com.esports.dao.UserDAO;
import com.esports.models.GoogleUserProfile;
import com.esports.models.OAuthTokens;
import com.esports.models.User;
import com.esports.models.UserRole;
import com.esports.services.OAuthService;
import net.jqwik.api.*;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mockito;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for multi-method authentication equivalence.
 * 
 * Tests that successful authentication via different methods (password, OAuth)
 * produces equivalent results in terms of session tokens, permissions, and
 * feature access.
 * 
 * **Validates: Property 6: Multi-Method Authentication Equivalence**
 * **Validates: Requirements 1.3, 1.4, 1.5, 1.6, 1.7, 1.8**
 */
class MultiAuthenticationEquivalencePropertyTest {
    
    /**
     * Property 6: Multi-Method Authentication Equivalence
     * 
     * For all User_Accounts with multiple authentication methods enabled, 
     * successful authentication via any method SHALL result in equivalent 
     * Session_Tokens with identical permissions and expiration times, and 
     * SHALL grant access to the same application features.
     */
    @Property(tries = 50)
    @Label("Property 6: Multi-auth equivalence - OAuth and password auth return same user")
    void oauthAndPasswordAuthReturnSameUser(
            @ForAll("validEmails") String email,
            @ForAll("validPasswords") String password,
            @ForAll("validNames") String firstName,
            @ForAll("validNames") String lastName) {
        
        // Setup: Create a user with both password and OAuth authentication
        UserDAO mockUserDAO = mock(UserDAO.class);
        OAuthService mockOAuthService = mock(OAuthService.class);
        OAuthTokenDAO mockOAuthTokenDAO = mock(OAuthTokenDAO.class);
        
        // Create user with password
        User user = createTestUser(email, password, firstName, lastName);
        user.setGoogleId("google-id-123");
        user.setProfilePictureUrl("https://example.com/picture.jpg");
        
        // Mock password authentication
        when(mockUserDAO.findByEmail(email)).thenReturn(user);
        when(mockUserDAO.updateLastLogin(anyInt())).thenReturn(true);
        
        // Mock OAuth authentication
        GoogleUserProfile profile = new GoogleUserProfile();
        profile.setGoogleId("google-id-123");
        profile.setEmail(email);
        profile.setName(firstName + " " + lastName);
        profile.setProfilePictureUrl("https://example.com/picture.jpg");
        profile.setEmailVerified(true);
        
        when(mockOAuthService.fetchUserProfile(anyString())).thenReturn(profile);
        when(mockUserDAO.findByGoogleId("google-id-123")).thenReturn(user);
        when(mockUserDAO.update(any(User.class))).thenReturn(true);
        when(mockOAuthTokenDAO.saveTokens(anyInt(), anyString(), anyString(), any(Timestamp.class))).thenReturn(true);
        
        AuthenticationService authService = new AuthenticationService(mockUserDAO, mockOAuthService, mockOAuthTokenDAO);
        
        // When: Authenticate via password
        User passwordAuthUser = authService.authenticate(email, password);
        
        // When: Authenticate via OAuth
        String accessToken = "encrypted-access-token";
        String refreshToken = "encrypted-refresh-token";
        Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusHours(1));
        User oauthAuthUser = authService.authenticateWithOAuth(accessToken, refreshToken, expiresAt);
        
        // Then: Both methods should return the same user
        assertNotNull(passwordAuthUser, "Password authentication should succeed");
        assertNotNull(oauthAuthUser, "OAuth authentication should succeed");
        assertEquals(passwordAuthUser.getId(), oauthAuthUser.getId(), 
            "Both authentication methods should return the same user ID");
        assertEquals(passwordAuthUser.getEmail(), oauthAuthUser.getEmail(), 
            "Both authentication methods should return the same email");
        assertEquals(passwordAuthUser.getRoles(), oauthAuthUser.getRoles(), 
            "Both authentication methods should return the same roles");
    }
    
    @Property(tries = 50)
    @Label("Property 6: Multi-auth equivalence - OAuth creates user with correct roles")
    void oauthCreatesUserWithCorrectRoles(
            @ForAll("validEmails") String email,
            @ForAll("validNames") String firstName,
            @ForAll("validNames") String lastName) {
        
        // Setup: Mock dependencies for new user creation
        UserDAO mockUserDAO = mock(UserDAO.class);
        OAuthService mockOAuthService = mock(OAuthService.class);
        OAuthTokenDAO mockOAuthTokenDAO = mock(OAuthTokenDAO.class);
        
        // Mock OAuth profile fetch
        GoogleUserProfile profile = new GoogleUserProfile();
        profile.setGoogleId("new-google-id-" + email.hashCode());
        profile.setEmail(email);
        profile.setName(firstName + " " + lastName);
        profile.setProfilePictureUrl("https://example.com/picture.jpg");
        profile.setEmailVerified(true);
        
        when(mockOAuthService.fetchUserProfile(anyString())).thenReturn(profile);
        when(mockUserDAO.findByGoogleId(anyString())).thenReturn(null);
        when(mockUserDAO.findByEmail(email)).thenReturn(null).thenReturn(createNewUser(email, firstName, lastName, profile.getGoogleId()));
        when(mockUserDAO.add(any(User.class))).thenReturn(true);
        when(mockOAuthTokenDAO.saveTokens(anyInt(), anyString(), anyString(), any(Timestamp.class))).thenReturn(true);
        
        AuthenticationService authService = new AuthenticationService(mockUserDAO, mockOAuthService, mockOAuthTokenDAO);
        
        // When: Authenticate via OAuth (creates new user)
        String accessToken = "encrypted-access-token";
        String refreshToken = "encrypted-refresh-token";
        Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusHours(1));
        User oauthUser = authService.authenticateWithOAuth(accessToken, refreshToken, expiresAt);
        
        // Then: User should have default USER role
        assertNotNull(oauthUser, "OAuth authentication should create new user");
        assertNotNull(oauthUser.getRoles(), "User should have roles assigned");
        assertTrue(oauthUser.getRoles().contains(UserRole.USER), 
            "OAuth-created user should have USER role");
        assertEquals(email, oauthUser.getEmail(), "User should have correct email");
        assertEquals(profile.getGoogleId(), oauthUser.getGoogleId(), "User should have Google ID");
    }
    
    @Property(tries = 50)
    @Label("Property 6: Multi-auth equivalence - OAuth links to existing user by email")
    void oauthLinksToExistingUserByEmail(
            @ForAll("validEmails") String email,
            @ForAll("validPasswords") String password,
            @ForAll("validNames") String firstName,
            @ForAll("validNames") String lastName) {
        
        // Setup: Create existing user without Google ID
        UserDAO mockUserDAO = mock(UserDAO.class);
        OAuthService mockOAuthService = mock(OAuthService.class);
        OAuthTokenDAO mockOAuthTokenDAO = mock(OAuthTokenDAO.class);
        
        User existingUser = createTestUser(email, password, firstName, lastName);
        existingUser.setGoogleId(null); // No Google ID yet
        
        // Mock OAuth profile fetch
        GoogleUserProfile profile = new GoogleUserProfile();
        profile.setGoogleId("new-google-id-" + email.hashCode());
        profile.setEmail(email);
        profile.setName(firstName + " " + lastName);
        profile.setProfilePictureUrl("https://example.com/picture.jpg");
        profile.setEmailVerified(true);
        
        when(mockOAuthService.fetchUserProfile(anyString())).thenReturn(profile);
        when(mockUserDAO.findByGoogleId(anyString())).thenReturn(null);
        when(mockUserDAO.findByEmail(email)).thenReturn(existingUser);
        when(mockUserDAO.update(any(User.class))).thenReturn(true);
        when(mockOAuthTokenDAO.saveTokens(anyInt(), anyString(), anyString(), any(Timestamp.class))).thenReturn(true);
        
        AuthenticationService authService = new AuthenticationService(mockUserDAO, mockOAuthService, mockOAuthTokenDAO);
        
        // When: Authenticate via OAuth
        String accessToken = "encrypted-access-token";
        String refreshToken = "encrypted-refresh-token";
        Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusHours(1));
        User linkedUser = authService.authenticateWithOAuth(accessToken, refreshToken, expiresAt);
        
        // Then: OAuth should link to existing user
        assertNotNull(linkedUser, "OAuth authentication should succeed");
        assertEquals(existingUser.getId(), linkedUser.getId(), 
            "OAuth should link to existing user by email");
        assertEquals(profile.getGoogleId(), linkedUser.getGoogleId(), 
            "User should now have Google ID linked");
        
        // Verify update was called to link Google account
        verify(mockUserDAO, times(1)).update(any(User.class));
    }
    
    @Property(tries = 50)
    @Label("Property 6: Multi-auth equivalence - OAuth stores profile picture URL")
    void oauthStoresProfilePictureUrl(
            @ForAll("validEmails") String email,
            @ForAll("validNames") String firstName,
            @ForAll("validNames") String lastName,
            @ForAll("validUrls") String pictureUrl) {
        
        // Setup
        UserDAO mockUserDAO = mock(UserDAO.class);
        OAuthService mockOAuthService = mock(OAuthService.class);
        OAuthTokenDAO mockOAuthTokenDAO = mock(OAuthTokenDAO.class);
        
        // Mock OAuth profile fetch
        GoogleUserProfile profile = new GoogleUserProfile();
        profile.setGoogleId("google-id-" + email.hashCode());
        profile.setEmail(email);
        profile.setName(firstName + " " + lastName);
        profile.setProfilePictureUrl(pictureUrl);
        profile.setEmailVerified(true);
        
        when(mockOAuthService.fetchUserProfile(anyString())).thenReturn(profile);
        when(mockUserDAO.findByGoogleId(anyString())).thenReturn(null);
        when(mockUserDAO.findByEmail(email)).thenReturn(null).thenReturn(createNewUser(email, firstName, lastName, profile.getGoogleId(), pictureUrl));
        when(mockUserDAO.add(any(User.class))).thenReturn(true);
        when(mockOAuthTokenDAO.saveTokens(anyInt(), anyString(), anyString(), any(Timestamp.class))).thenReturn(true);
        
        AuthenticationService authService = new AuthenticationService(mockUserDAO, mockOAuthService, mockOAuthTokenDAO);
        
        // When: Authenticate via OAuth
        String accessToken = "encrypted-access-token";
        String refreshToken = "encrypted-refresh-token";
        Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusHours(1));
        User oauthUser = authService.authenticateWithOAuth(accessToken, refreshToken, expiresAt);
        
        // Then: User should have profile picture URL stored
        assertNotNull(oauthUser, "OAuth authentication should succeed");
        assertEquals(pictureUrl, oauthUser.getProfilePictureUrl(), 
            "User should have profile picture URL from Google");
    }
    
    @Property(tries = 50)
    @Label("Property 6: Multi-auth equivalence - OAuth updates last login timestamp")
    void oauthUpdatesLastLoginTimestamp(
            @ForAll("validEmails") String email,
            @ForAll("validNames") String firstName,
            @ForAll("validNames") String lastName) {
        
        // Setup
        UserDAO mockUserDAO = mock(UserDAO.class);
        OAuthService mockOAuthService = mock(OAuthService.class);
        OAuthTokenDAO mockOAuthTokenDAO = mock(OAuthTokenDAO.class);
        
        User existingUser = createTestUser(email, "password123", firstName, lastName);
        existingUser.setGoogleId("google-id-123");
        existingUser.setLastLogin(null); // No previous login
        
        // Mock OAuth profile fetch
        GoogleUserProfile profile = new GoogleUserProfile();
        profile.setGoogleId("google-id-123");
        profile.setEmail(email);
        profile.setName(firstName + " " + lastName);
        profile.setProfilePictureUrl("https://example.com/picture.jpg");
        profile.setEmailVerified(true);
        
        when(mockOAuthService.fetchUserProfile(anyString())).thenReturn(profile);
        when(mockUserDAO.findByGoogleId("google-id-123")).thenReturn(existingUser);
        when(mockUserDAO.update(any(User.class))).thenReturn(true);
        when(mockOAuthTokenDAO.saveTokens(anyInt(), anyString(), anyString(), any(Timestamp.class))).thenReturn(true);
        
        AuthenticationService authService = new AuthenticationService(mockUserDAO, mockOAuthService, mockOAuthTokenDAO);
        
        // When: Authenticate via OAuth
        String accessToken = "encrypted-access-token";
        String refreshToken = "encrypted-refresh-token";
        Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusHours(1));
        User oauthUser = authService.authenticateWithOAuth(accessToken, refreshToken, expiresAt);
        
        // Then: Last login should be updated
        assertNotNull(oauthUser, "OAuth authentication should succeed");
        assertNotNull(oauthUser.getLastLogin(), "Last login timestamp should be set");
        
        // Verify update was called
        verify(mockUserDAO, times(1)).update(any(User.class));
    }
    
    @Property(tries = 50)
    @Label("Property 6: Multi-auth equivalence - OAuth stores tokens after authentication")
    void oauthStoresTokensAfterAuthentication(
            @ForAll("validEmails") String email,
            @ForAll("validNames") String firstName,
            @ForAll("validNames") String lastName) {
        
        // Setup
        UserDAO mockUserDAO = mock(UserDAO.class);
        OAuthService mockOAuthService = mock(OAuthService.class);
        OAuthTokenDAO mockOAuthTokenDAO = mock(OAuthTokenDAO.class);
        
        User existingUser = createTestUser(email, "password123", firstName, lastName);
        existingUser.setGoogleId("google-id-123");
        
        // Mock OAuth profile fetch
        GoogleUserProfile profile = new GoogleUserProfile();
        profile.setGoogleId("google-id-123");
        profile.setEmail(email);
        profile.setName(firstName + " " + lastName);
        profile.setProfilePictureUrl("https://example.com/picture.jpg");
        profile.setEmailVerified(true);
        
        when(mockOAuthService.fetchUserProfile(anyString())).thenReturn(profile);
        when(mockUserDAO.findByGoogleId("google-id-123")).thenReturn(existingUser);
        when(mockUserDAO.update(any(User.class))).thenReturn(true);
        when(mockOAuthTokenDAO.saveTokens(anyInt(), anyString(), anyString(), any(Timestamp.class))).thenReturn(true);
        
        AuthenticationService authService = new AuthenticationService(mockUserDAO, mockOAuthService, mockOAuthTokenDAO);
        
        // When: Authenticate via OAuth
        String accessToken = "encrypted-access-token";
        String refreshToken = "encrypted-refresh-token";
        Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusHours(1));
        User oauthUser = authService.authenticateWithOAuth(accessToken, refreshToken, expiresAt);
        
        // Then: Tokens should be stored
        assertNotNull(oauthUser, "OAuth authentication should succeed");
        
        // Verify tokens were saved
        verify(mockOAuthTokenDAO, times(1)).saveTokens(
            eq(existingUser.getId()), 
            eq(accessToken), 
            eq(refreshToken), 
            eq(expiresAt)
        );
    }
    
    // Arbitraries (generators) for test data
    
    @Provide
    Arbitrary<String> validEmails() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(3)
            .ofMaxLength(20)
            .map(s -> s.toLowerCase() + "@example.com");
    }
    
    @Provide
    Arbitrary<String> validPasswords() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(8)
            .ofMaxLength(20);
    }
    
    @Provide
    Arbitrary<String> validNames() {
        return Arbitraries.strings()
            .alpha()
            .ofMinLength(2)
            .ofMaxLength(20)
            .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase());
    }
    
    @Provide
    Arbitrary<String> validUrls() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(5)
            .ofMaxLength(20)
            .map(s -> "https://example.com/" + s + ".jpg");
    }
    
    // Helper methods
    
    private User createTestUser(String email, String password, String firstName, String lastName) {
        User user = new User();
        user.setId(email.hashCode() & 0x7FFFFFFF); // Positive ID from email hash
        user.setEmail(email);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCreatedAt(LocalDateTime.now());
        user.setIsBlocked(false);
        
        List<UserRole> roles = new ArrayList<>();
        roles.add(UserRole.USER);
        user.setRoles(roles);
        
        return user;
    }
    
    private User createNewUser(String email, String firstName, String lastName, String googleId) {
        return createNewUser(email, firstName, lastName, googleId, "https://example.com/default.jpg");
    }
    
    private User createNewUser(String email, String firstName, String lastName, String googleId, String pictureUrl) {
        User user = new User();
        user.setId(email.hashCode() & 0x7FFFFFFF);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setGoogleId(googleId);
        user.setProfilePictureUrl(pictureUrl);
        user.setPreferredAuthMethod("GOOGLE");
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        user.setIsBlocked(false);
        
        List<UserRole> roles = new ArrayList<>();
        roles.add(UserRole.USER);
        user.setRoles(roles);
        
        return user;
    }
}
