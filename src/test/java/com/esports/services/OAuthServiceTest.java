package com.esports.services;

import com.esports.dao.OAuthTokenDAO;
import com.esports.models.GoogleUserProfile;
import com.esports.models.OAuthTokens;
import okhttp3.*;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OAuthService.
 * 
 * Tests OAuth 2.0 flow with mocked Google API responses.
 * 
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.9, 1.10, 7.3, 7.4, 7.5, 7.6, 7.7, 7.9**
 */
@DisplayName("OAuthService Unit Tests")
class OAuthServiceTest {
    
    private OAuthService oauthService;
    private OAuthTokenDAO mockTokenDAO;
    private EncryptionService encryptionService;
    private OkHttpClient mockHttpClient;
    private Call mockCall;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create real encryption service for testing
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256, new SecureRandom());
        SecretKey testKey = keyGen.generateKey();
        encryptionService = new EncryptionService(testKey);
        
        // Create mocks
        mockTokenDAO = mock(OAuthTokenDAO.class);
        mockHttpClient = mock(OkHttpClient.class);
        mockCall = mock(Call.class);
        
        // Create service with mocked dependencies
        oauthService = new OAuthService(mockTokenDAO, encryptionService, mockHttpClient);
    }
    
    // ========== generateAuthorizationUrl Tests ==========
    
    @Test
    @DisplayName("Generate authorization URL - should include all required parameters")
    void testGenerateAuthorizationUrl_ValidInputs_ReturnsUrlWithAllParameters() {
        String state = "test-state-123";
        String codeVerifier = "test-verifier-456";
        
        String url = oauthService.generateAuthorizationUrl(state, codeVerifier);
        
        assertNotNull(url);
        assertTrue(url.startsWith("https://accounts.google.com/o/oauth2/v2/auth"));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("state=" + state));
        assertTrue(url.contains("code_challenge="));
        assertTrue(url.contains("code_challenge_method=S256"));
        assertTrue(url.contains("access_type=offline"));
        assertTrue(url.contains("prompt=consent"));
    }
    
    @Test
    @DisplayName("Generate authorization URL with null state - should throw exception")
    void testGenerateAuthorizationUrl_NullState_ThrowsException() {
        String codeVerifier = "test-verifier";
        
        assertThrows(IllegalArgumentException.class, () -> {
            oauthService.generateAuthorizationUrl(null, codeVerifier);
        });
    }
    
    @Test
    @DisplayName("Generate authorization URL with empty state - should throw exception")
    void testGenerateAuthorizationUrl_EmptyState_ThrowsException() {
        String codeVerifier = "test-verifier";
        
        assertThrows(IllegalArgumentException.class, () -> {
            oauthService.generateAuthorizationUrl("", codeVerifier);
        });
    }
    
    @Test
    @DisplayName("Generate authorization URL with null code verifier - should throw exception")
    void testGenerateAuthorizationUrl_NullCodeVerifier_ThrowsException() {
        String state = "test-state";
        
        assertThrows(IllegalArgumentException.class, () -> {
            oauthService.generateAuthorizationUrl(state, null);
        });
    }
    
    @Test
    @DisplayName("Generate authorization URL with empty code verifier - should throw exception")
    void testGenerateAuthorizationUrl_EmptyCodeVerifier_ThrowsException() {
        String state = "test-state";
        
        assertThrows(IllegalArgumentException.class, () -> {
            oauthService.generateAuthorizationUrl(state, "");
        });
    }
    
    // ========== exchangeCodeForTokens Tests ==========
    
    @Test
    @DisplayName("Exchange code for tokens - successful exchange returns tokens")
    void testExchangeCodeForTokens_ValidCode_ReturnsTokens() throws IOException {
        String code = "test-auth-code";
        String codeVerifier = "test-verifier";
        
        // Mock successful token response
        String responseJson = new JSONObject()
                .put("access_token", "test-access-token")
                .put("refresh_token", "test-refresh-token")
                .put("expires_in", 3600)
                .toString();
        
        ResponseBody mockResponseBody = ResponseBody.create(responseJson, MediaType.parse("application/json"));
        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("https://oauth2.googleapis.com/token").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(mockResponseBody)
                .build();
        
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        
        OAuthTokens tokens = oauthService.exchangeCodeForTokens(code, codeVerifier);
        
        assertNotNull(tokens);
        assertNotNull(tokens.getEncryptedAccessToken());
        assertNotNull(tokens.getEncryptedRefreshToken());
        assertNotNull(tokens.getExpiresAt());
        
        // Verify tokens can be decrypted
        String decryptedAccessToken = encryptionService.decrypt(tokens.getEncryptedAccessToken());
        assertEquals("test-access-token", decryptedAccessToken);
    }
    
    @Test
    @DisplayName("Exchange code for tokens with null code - should throw exception")
    void testExchangeCodeForTokens_NullCode_ThrowsException() {
        String codeVerifier = "test-verifier";
        
        assertThrows(IllegalArgumentException.class, () -> {
            oauthService.exchangeCodeForTokens(null, codeVerifier);
        });
    }
    
    @Test
    @DisplayName("Exchange code for tokens with empty code - should throw exception")
    void testExchangeCodeForTokens_EmptyCode_ThrowsException() {
        String codeVerifier = "test-verifier";
        
        assertThrows(IllegalArgumentException.class, () -> {
            oauthService.exchangeCodeForTokens("", codeVerifier);
        });
    }
    
    @Test
    @DisplayName("Exchange code for tokens with null code verifier - should throw exception")
    void testExchangeCodeForTokens_NullCodeVerifier_ThrowsException() {
        String code = "test-code";
        
        assertThrows(IllegalArgumentException.class, () -> {
            oauthService.exchangeCodeForTokens(code, null);
        });
    }
    
    @Test
    @DisplayName("Exchange code for tokens with invalid code - should throw exception")
    void testExchangeCodeForTokens_InvalidCode_ThrowsException() throws IOException {
        String code = "invalid-code";
        String codeVerifier = "test-verifier";
        
        // Mock error response
        String errorJson = new JSONObject()
                .put("error", "invalid_grant")
                .put("error_description", "Invalid authorization code")
                .toString();
        
        ResponseBody mockResponseBody = ResponseBody.create(errorJson, MediaType.parse("application/json"));
        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("https://oauth2.googleapis.com/token").build())
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Bad Request")
                .body(mockResponseBody)
                .build();
        
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        
        assertThrows(RuntimeException.class, () -> {
            oauthService.exchangeCodeForTokens(code, codeVerifier);
        });
    }
    
    @Test
    @DisplayName("Exchange code for tokens with network error - should throw exception")
    void testExchangeCodeForTokens_NetworkError_ThrowsException() throws IOException {
        String code = "test-code";
        String codeVerifier = "test-verifier";
        
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenThrow(new IOException("Network error"));
        
        assertThrows(RuntimeException.class, () -> {
            oauthService.exchangeCodeForTokens(code, codeVerifier);
        });
    }
    
    // ========== refreshAccessToken Tests ==========
    
    @Test
    @DisplayName("Refresh access token - successful refresh returns new token")
    void testRefreshAccessToken_ValidRefreshToken_ReturnsNewAccessToken() throws IOException {
        String refreshToken = encryptionService.encrypt("test-refresh-token");
        
        // Mock successful refresh response
        String responseJson = new JSONObject()
                .put("access_token", "new-access-token")
                .put("expires_in", 3600)
                .toString();
        
        ResponseBody mockResponseBody = ResponseBody.create(responseJson, MediaType.parse("application/json"));
        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("https://oauth2.googleapis.com/token").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(mockResponseBody)
                .build();
        
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        
        String newAccessToken = oauthService.refreshAccessToken(refreshToken);
        
        assertNotNull(newAccessToken);
        
        // Verify new token can be decrypted
        String decryptedAccessToken = encryptionService.decrypt(newAccessToken);
        assertEquals("new-access-token", decryptedAccessToken);
    }
    
    @Test
    @DisplayName("Refresh access token with null refresh token - should throw exception")
    void testRefreshAccessToken_NullRefreshToken_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            oauthService.refreshAccessToken(null);
        });
    }
    
    @Test
    @DisplayName("Refresh access token with empty refresh token - should throw exception")
    void testRefreshAccessToken_EmptyRefreshToken_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            oauthService.refreshAccessToken("");
        });
    }
    
    @Test
    @DisplayName("Refresh access token with invalid refresh token - should throw exception")
    void testRefreshAccessToken_InvalidRefreshToken_ThrowsException() throws IOException {
        String refreshToken = encryptionService.encrypt("invalid-refresh-token");
        
        // Mock error response
        String errorJson = new JSONObject()
                .put("error", "invalid_grant")
                .put("error_description", "Invalid refresh token")
                .toString();
        
        ResponseBody mockResponseBody = ResponseBody.create(errorJson, MediaType.parse("application/json"));
        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("https://oauth2.googleapis.com/token").build())
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Bad Request")
                .body(mockResponseBody)
                .build();
        
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        
        assertThrows(RuntimeException.class, () -> {
            oauthService.refreshAccessToken(refreshToken);
        });
    }
    
    // ========== revokeTokens Tests ==========
    
    @Test
    @DisplayName("Revoke tokens - successful revocation completes without error")
    void testRevokeTokens_ValidAccessToken_CompletesSuccessfully() throws IOException {
        String accessToken = encryptionService.encrypt("test-access-token");
        
        // Mock successful revoke response
        ResponseBody mockResponseBody = ResponseBody.create("", MediaType.parse("application/json"));
        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("https://oauth2.googleapis.com/revoke").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(mockResponseBody)
                .build();
        
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        
        assertDoesNotThrow(() -> {
            oauthService.revokeTokens(accessToken);
        });
    }
    
    @Test
    @DisplayName("Revoke tokens with null access token - should throw exception")
    void testRevokeTokens_NullAccessToken_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            oauthService.revokeTokens(null);
        });
    }
    
    @Test
    @DisplayName("Revoke tokens with empty access token - should throw exception")
    void testRevokeTokens_EmptyAccessToken_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            oauthService.revokeTokens("");
        });
    }
    
    @Test
    @DisplayName("Revoke tokens with invalid token - should throw exception")
    void testRevokeTokens_InvalidToken_ThrowsException() throws IOException {
        String accessToken = encryptionService.encrypt("invalid-token");
        
        // Mock error response
        ResponseBody mockResponseBody = ResponseBody.create("", MediaType.parse("application/json"));
        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("https://oauth2.googleapis.com/revoke").build())
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Bad Request")
                .body(mockResponseBody)
                .build();
        
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        
        assertThrows(RuntimeException.class, () -> {
            oauthService.revokeTokens(accessToken);
        });
    }
    
    // ========== fetchUserProfile Tests ==========
    
    @Test
    @DisplayName("Fetch user profile - successful fetch returns profile")
    void testFetchUserProfile_ValidAccessToken_ReturnsProfile() throws IOException {
        String accessToken = encryptionService.encrypt("test-access-token");
        
        // Mock successful profile response
        String responseJson = new JSONObject()
                .put("id", "123456789")
                .put("email", "test@example.com")
                .put("name", "Test User")
                .put("picture", "https://example.com/photo.jpg")
                .put("verified_email", true)
                .toString();
        
        ResponseBody mockResponseBody = ResponseBody.create(responseJson, MediaType.parse("application/json"));
        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("https://www.googleapis.com/oauth2/v2/userinfo").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(mockResponseBody)
                .build();
        
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        
        GoogleUserProfile profile = oauthService.fetchUserProfile(accessToken);
        
        assertNotNull(profile);
        assertEquals("123456789", profile.getGoogleId());
        assertEquals("test@example.com", profile.getEmail());
        assertEquals("Test User", profile.getName());
        assertEquals("https://example.com/photo.jpg", profile.getProfilePictureUrl());
        assertTrue(profile.isEmailVerified());
    }
    
    @Test
    @DisplayName("Fetch user profile with null access token - should throw exception")
    void testFetchUserProfile_NullAccessToken_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            oauthService.fetchUserProfile(null);
        });
    }
    
    @Test
    @DisplayName("Fetch user profile with empty access token - should throw exception")
    void testFetchUserProfile_EmptyAccessToken_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            oauthService.fetchUserProfile("");
        });
    }
    
    @Test
    @DisplayName("Fetch user profile with invalid token - should throw exception")
    void testFetchUserProfile_InvalidToken_ThrowsException() throws IOException {
        String accessToken = encryptionService.encrypt("invalid-token");
        
        // Mock error response
        String errorJson = new JSONObject()
                .put("error", "invalid_token")
                .toString();
        
        ResponseBody mockResponseBody = ResponseBody.create(errorJson, MediaType.parse("application/json"));
        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("https://www.googleapis.com/oauth2/v2/userinfo").build())
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .body(mockResponseBody)
                .build();
        
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        
        assertThrows(RuntimeException.class, () -> {
            oauthService.fetchUserProfile(accessToken);
        });
    }
    
    @Test
    @DisplayName("Fetch user profile with minimal data - should handle optional fields")
    void testFetchUserProfile_MinimalData_HandlesOptionalFields() throws IOException {
        String accessToken = encryptionService.encrypt("test-access-token");
        
        // Mock response with only required fields
        String responseJson = new JSONObject()
                .put("id", "123456789")
                .put("email", "test@example.com")
                .toString();
        
        ResponseBody mockResponseBody = ResponseBody.create(responseJson, MediaType.parse("application/json"));
        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("https://www.googleapis.com/oauth2/v2/userinfo").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(mockResponseBody)
                .build();
        
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        
        GoogleUserProfile profile = oauthService.fetchUserProfile(accessToken);
        
        assertNotNull(profile);
        assertEquals("123456789", profile.getGoogleId());
        assertEquals("test@example.com", profile.getEmail());
        assertEquals("", profile.getName());
        assertEquals("", profile.getProfilePictureUrl());
        assertFalse(profile.isEmailVerified());
    }
    
    // ========== validateState Tests ==========
    
    @Test
    @DisplayName("Validate state - matching states return true")
    void testValidateState_MatchingStates_ReturnsTrue() {
        String state = "test-state-123";
        
        boolean result = oauthService.validateState(state, state);
        
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Validate state - different states return false")
    void testValidateState_DifferentStates_ReturnsFalse() {
        String receivedState = "state-1";
        String expectedState = "state-2";
        
        boolean result = oauthService.validateState(receivedState, expectedState);
        
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Validate state with null received state - returns false")
    void testValidateState_NullReceivedState_ReturnsFalse() {
        String expectedState = "test-state";
        
        boolean result = oauthService.validateState(null, expectedState);
        
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Validate state with null expected state - returns false")
    void testValidateState_NullExpectedState_ReturnsFalse() {
        String receivedState = "test-state";
        
        boolean result = oauthService.validateState(receivedState, null);
        
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Validate state with both null - returns false")
    void testValidateState_BothNull_ReturnsFalse() {
        boolean result = oauthService.validateState(null, null);
        
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Validate state with empty strings - returns true")
    void testValidateState_EmptyStrings_ReturnsTrue() {
        boolean result = oauthService.validateState("", "");
        
        assertTrue(result);
    }
    
    // ========== Helper Method Tests ==========
    
    @Test
    @DisplayName("Generate state - returns non-null non-empty string")
    void testGenerateState_ReturnsValidString() {
        String state = oauthService.generateState();
        
        assertNotNull(state);
        assertFalse(state.isEmpty());
    }
    
    @Test
    @DisplayName("Generate state - produces unique values")
    void testGenerateState_ProducesUniqueValues() {
        String state1 = oauthService.generateState();
        String state2 = oauthService.generateState();
        
        assertNotEquals(state1, state2);
    }
    
    @Test
    @DisplayName("Generate code verifier - returns non-null non-empty string")
    void testGenerateCodeVerifier_ReturnsValidString() {
        String verifier = oauthService.generateCodeVerifier();
        
        assertNotNull(verifier);
        assertFalse(verifier.isEmpty());
    }
    
    @Test
    @DisplayName("Generate code verifier - produces unique values")
    void testGenerateCodeVerifier_ProducesUniqueValues() {
        String verifier1 = oauthService.generateCodeVerifier();
        String verifier2 = oauthService.generateCodeVerifier();
        
        assertNotEquals(verifier1, verifier2);
    }
}
