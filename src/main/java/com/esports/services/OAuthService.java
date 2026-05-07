package com.esports.services;

import com.esports.dao.OAuthTokenDAO;
import com.esports.models.GoogleUserProfile;
import com.esports.models.OAuthTokens;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Base64;

/**
 * Service for managing Google OAuth 2.0 authentication flow with PKCE support.
 * 
 * This service orchestrates the complete OAuth 2.0 flow including:
 * - Authorization URL generation with PKCE (Proof Key for Code Exchange)
 * - Token exchange after user authorization
 * - Token refresh when access tokens expire
 * - Token revocation on account disconnect
 * - User profile fetching from Google APIs
 * - CSRF protection via state parameter validation
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.9, 1.10, 7.3, 7.4, 7.5, 7.6, 7.7, 7.9
 */
public class OAuthService {
    
    // Google OAuth 2.0 endpoints
    private static final String AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String REVOKE_ENDPOINT = "https://oauth2.googleapis.com/revoke";
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v2/userinfo";
    
    // OAuth configuration (loaded from oauth.properties)
    private final String CLIENT_ID;
    private final String CLIENT_SECRET;
    private final String REDIRECT_URI;
    private final String SCOPE;
    private final boolean enabled;
    
    private final OAuthTokenDAO oauthTokenDAO;
    private final EncryptionService encryptionService;
    private final OkHttpClient httpClient;
    
    /**
     * Initialize the OAuth service with required dependencies.
     */
    public OAuthService() {
        // Load OAuth configuration from properties file
        java.util.Properties props = new java.util.Properties();
        String loadedClientId = null;
        String loadedClientSecret = null;
        String loadedRedirectUri = null;
        String loadedScope = null;
        boolean loadedEnabled = false;
        
        try (java.io.InputStream input = getClass().getClassLoader().getResourceAsStream("oauth.properties")) {
            if (input == null) {
                System.err.println("[OAuth] Warning: oauth.properties file not found. OAuth service will be disabled.");
            } else {
                props.load(input);
                
                loadedClientId = props.getProperty("google.client.id");
                loadedClientSecret = props.getProperty("google.client.secret");
                loadedRedirectUri = props.getProperty("google.redirect.uri");
                loadedScope = props.getProperty("google.scope");
                
                // Validate configuration
                if (loadedClientId == null || loadedClientId.contains("YOUR_CLIENT_ID")) {
                    System.err.println("[OAuth] Warning: Google OAuth Client ID not configured. OAuth service will be disabled.");
                } else if (loadedClientSecret == null || loadedClientSecret.contains("YOUR_CLIENT_SECRET")) {
                    System.err.println("[OAuth] Warning: Google OAuth Client Secret not configured. OAuth service will be disabled.");
                } else {
                    loadedEnabled = true;
                }
            }
            
        } catch (IOException e) {
            System.err.println("[OAuth] Warning: Failed to load OAuth configuration. OAuth service will be disabled.");
            e.printStackTrace();
        }
        
        // Assign to final fields
        this.CLIENT_ID = loadedClientId;
        this.CLIENT_SECRET = loadedClientSecret;
        this.REDIRECT_URI = loadedRedirectUri;
        this.SCOPE = loadedScope;
        this.enabled = loadedEnabled;
        
        this.oauthTokenDAO = new OAuthTokenDAO();
        this.encryptionService = new EncryptionService();
        this.httpClient = new OkHttpClient();
    }
    
    /**
     * Constructor for testing with dependency injection.
     */
    OAuthService(OAuthTokenDAO oauthTokenDAO, EncryptionService encryptionService, OkHttpClient httpClient) {
        this.CLIENT_ID = "test-client-id";
        this.CLIENT_SECRET = "test-client-secret";
        this.REDIRECT_URI = "http://localhost:8080/oauth/callback";
        this.SCOPE = "openid email profile";
        this.enabled = true;
        
        this.oauthTokenDAO = oauthTokenDAO;
        this.encryptionService = encryptionService;
        this.httpClient = httpClient;
    }
    
    /**
     * Check if OAuth service is enabled and configured.
     * 
     * @return true if OAuth service is ready to use
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Generate OAuth 2.0 authorization URL with PKCE support.
     * 
     * Creates a URL that redirects the user to Google's authorization page.
     * Implements PKCE (Proof Key for Code Exchange) for enhanced security.
     * 
     * @param state CSRF protection token (should be stored in session)
     * @param codeVerifier PKCE code verifier (should be stored in session)
     * @return Authorization URL to redirect user to
     * @throws IllegalArgumentException if state or codeVerifier is null/empty
     * @throws IllegalStateException if OAuth is not enabled
     * 
     * Requirement 1.1: Generate authorization URL for OAuth flow
     * Requirement 7.6: Implement PKCE for enhanced security
     * Requirement 1.10: Include state parameter for CSRF protection
     */
    public String generateAuthorizationUrl(String state, String codeVerifier) {
        if (!enabled) {
            throw new IllegalStateException("OAuth service is not enabled. Please configure oauth.properties");
        }
        if (state == null || state.isEmpty()) {
            throw new IllegalArgumentException("State parameter cannot be null or empty");
        }
        if (codeVerifier == null || codeVerifier.isEmpty()) {
            throw new IllegalArgumentException("Code verifier cannot be null or empty");
        }
        
        try {
            // Generate code challenge from code verifier (SHA-256 hash, Base64 URL-encoded)
            String codeChallenge = generateCodeChallenge(codeVerifier);
            
            // Build authorization URL with all required parameters
            StringBuilder url = new StringBuilder(AUTHORIZATION_ENDPOINT);
            url.append("?response_type=code");
            url.append("&client_id=").append(URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8));
            url.append("&redirect_uri=").append(URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8));
            url.append("&scope=").append(URLEncoder.encode(SCOPE, StandardCharsets.UTF_8));
            url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
            url.append("&code_challenge=").append(URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8));
            url.append("&code_challenge_method=S256");
            url.append("&access_type=offline"); // Request refresh token
            url.append("&prompt=consent"); // Force consent screen to get refresh token
            
            return url.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate authorization URL", e);
        }
    }
    
    /**
     * Exchange authorization code for OAuth tokens.
     * 
     * After user authorizes the application, Google redirects back with an
     * authorization code. This method exchanges that code for access and refresh tokens.
     * 
     * @param code Authorization code from Google
     * @param codeVerifier PKCE code verifier (must match the one used in authorization URL)
     * @return OAuthTokens containing encrypted access and refresh tokens
     * @throws IllegalArgumentException if code or codeVerifier is null/empty
     * @throws RuntimeException if token exchange fails
     * 
     * Requirement 1.2: Exchange authorization code for tokens
     * Requirement 7.6: Validate PKCE code verifier
     * Requirement 7.1: Store access token encrypted
     * Requirement 7.2: Store refresh token encrypted
     */
    public OAuthTokens exchangeCodeForTokens(String code, String codeVerifier) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Authorization code cannot be null or empty");
        }
        if (codeVerifier == null || codeVerifier.isEmpty()) {
            throw new IllegalArgumentException("Code verifier cannot be null or empty");
        }
        
        try {
            // Build token request
            RequestBody requestBody = new FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("code", code)
                    .add("client_id", CLIENT_ID)
                    .add("client_secret", CLIENT_SECRET)
                    .add("redirect_uri", REDIRECT_URI)
                    .add("code_verifier", codeVerifier) // PKCE verification
                    .build();
            
            Request request = new Request.Builder()
                    .url(TOKEN_ENDPOINT)
                    .post(requestBody)
                    .build();
            
            // Execute request
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error details";
                    throw new RuntimeException("Token exchange failed: " + response.code() + " - " + errorBody);
                }
                
                // Parse response
                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);
                
                String accessToken = json.getString("access_token");
                String refreshToken = json.optString("refresh_token", null);
                int expiresIn = json.getInt("expires_in");
                
                // Encrypt tokens before storage
                String encryptedAccessToken = encryptionService.encrypt(accessToken);
                String encryptedRefreshToken = refreshToken != null ? encryptionService.encrypt(refreshToken) : null;
                
                // Calculate expiration timestamp
                Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + (expiresIn * 1000L));
                
                // Create OAuthTokens object (userId will be set by caller)
                OAuthTokens tokens = new OAuthTokens();
                tokens.setEncryptedAccessToken(encryptedAccessToken);
                tokens.setEncryptedRefreshToken(encryptedRefreshToken);
                tokens.setExpiresAt(expiresAt);
                
                return tokens;
            }
        } catch (IOException e) {
            throw new RuntimeException("Network error during token exchange", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange code for tokens", e);
        }
    }
    
    /**
     * Refresh an expired access token using the refresh token.
     * 
     * When an access token expires, this method uses the refresh token to
     * obtain a new access token without requiring user re-authentication.
     * 
     * @param refreshToken The encrypted refresh token
     * @return New encrypted access token
     * @throws IllegalArgumentException if refreshToken is null/empty
     * @throws RuntimeException if token refresh fails
     * 
     * Requirement 7.4: Use refresh token to obtain new access token
     * Requirement 7.10: Update access token when refreshed
     */
    public String refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Refresh token cannot be null or empty");
        }
        
        try {
            // Decrypt refresh token
            String decryptedRefreshToken = encryptionService.decrypt(refreshToken);
            
            // Build refresh request
            RequestBody requestBody = new FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", decryptedRefreshToken)
                    .add("client_id", CLIENT_ID)
                    .add("client_secret", CLIENT_SECRET)
                    .build();
            
            Request request = new Request.Builder()
                    .url(TOKEN_ENDPOINT)
                    .post(requestBody)
                    .build();
            
            // Execute request
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error details";
                    throw new RuntimeException("Token refresh failed: " + response.code() + " - " + errorBody);
                }
                
                // Parse response
                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);
                
                String newAccessToken = json.getString("access_token");
                
                // Encrypt new access token
                return encryptionService.encrypt(newAccessToken);
            }
        } catch (IOException e) {
            throw new RuntimeException("Network error during token refresh", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh access token", e);
        }
    }
    
    /**
     * Revoke OAuth tokens with Google.
     * 
     * When a user disconnects their Google account, this method revokes
     * the tokens with Google's authorization server.
     * 
     * @param accessToken The encrypted access token to revoke
     * @throws IllegalArgumentException if accessToken is null/empty
     * @throws RuntimeException if token revocation fails
     * 
     * Requirement 7.9: Revoke tokens when user disconnects
     */
    public void revokeTokens(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be null or empty");
        }
        
        try {
            // Decrypt access token
            String decryptedAccessToken = encryptionService.decrypt(accessToken);
            
            // Build revoke request
            RequestBody requestBody = new FormBody.Builder()
                    .add("token", decryptedAccessToken)
                    .build();
            
            Request request = new Request.Builder()
                    .url(REVOKE_ENDPOINT)
                    .post(requestBody)
                    .build();
            
            // Execute request
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error details";
                    throw new RuntimeException("Token revocation failed: " + response.code() + " - " + errorBody);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Network error during token revocation", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to revoke tokens", e);
        }
    }
    
    /**
     * Fetch user profile information from Google.
     * 
     * Retrieves the user's Google profile including email, name, and profile picture.
     * Used to create or link user accounts after successful authentication.
     * 
     * @param accessToken The encrypted access token
     * @return GoogleUserProfile containing user information
     * @throws IllegalArgumentException if accessToken is null/empty
     * @throws RuntimeException if profile fetch fails
     * 
     * Requirement 1.3: Retrieve user profile from Google
     * Requirement 1.6: Store profile picture URL
     */
    public GoogleUserProfile fetchUserProfile(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be null or empty");
        }
        
        try {
            // Decrypt access token
            String decryptedAccessToken = encryptionService.decrypt(accessToken);
            
            // Build profile request
            Request request = new Request.Builder()
                    .url(USERINFO_ENDPOINT)
                    .header("Authorization", "Bearer " + decryptedAccessToken)
                    .get()
                    .build();
            
            // Execute request
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error details";
                    throw new RuntimeException("Profile fetch failed: " + response.code() + " - " + errorBody);
                }
                
                // Parse response
                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);
                
                GoogleUserProfile profile = new GoogleUserProfile();
                profile.setGoogleId(json.getString("id"));
                profile.setEmail(json.getString("email"));
                profile.setName(json.optString("name", ""));
                profile.setProfilePictureUrl(json.optString("picture", ""));
                profile.setEmailVerified(json.optBoolean("verified_email", false));
                
                return profile;
            }
        } catch (IOException e) {
            throw new RuntimeException("Network error during profile fetch", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user profile", e);
        }
    }
    
    /**
     * Validate OAuth state parameter for CSRF protection.
     * 
     * Compares the state parameter returned from Google with the expected
     * state that was stored in the session. This prevents CSRF attacks.
     * 
     * @param receivedState State parameter received from OAuth callback
     * @param expectedState State parameter stored in session
     * @return true if states match, false otherwise
     * 
     * Requirement 1.10: Validate state parameter for CSRF protection
     */
    public boolean validateState(String receivedState, String expectedState) {
        if (receivedState == null || expectedState == null) {
            return false;
        }
        
        // Use constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
            receivedState.getBytes(StandardCharsets.UTF_8),
            expectedState.getBytes(StandardCharsets.UTF_8)
        );
    }
    
    /**
     * Generate a secure random state parameter for CSRF protection.
     * 
     * @return Random state string (Base64 URL-encoded)
     */
    public String generateState() {
        SecureRandom random = new SecureRandom();
        byte[] stateBytes = new byte[32];
        random.nextBytes(stateBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);
    }
    
    /**
     * Generate a secure random PKCE code verifier.
     * 
     * @return Random code verifier string (Base64 URL-encoded)
     */
    public String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        byte[] verifierBytes = new byte[32];
        random.nextBytes(verifierBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes);
    }
    
    /**
     * Generate PKCE code challenge from code verifier.
     * 
     * @param codeVerifier The code verifier
     * @return SHA-256 hash of verifier, Base64 URL-encoded
     * @throws NoSuchAlgorithmException if SHA-256 is not available
     */
    private String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}
