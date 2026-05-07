# OAuth UI Implementation Guide

## Overview

This document describes the OAuth 2.0 UI components implemented for the Advanced Features Suite. The implementation provides a complete user interface for Google OAuth authentication, including sign-in, account management, and disconnection.

## Components Implemented

### 1. SignatureOAuthController
**Location:** `src/main/java/com/esports/controllers/SignatureOAuthController.java`

**Purpose:** Manages the complete OAuth 2.0 authentication flow.

**Key Features:**
- Initiates OAuth authorization with Google
- Handles OAuth callbacks and token exchange
- Links or creates user accounts based on Google profile
- Manages OAuth token storage
- Handles Google account disconnection
- Provides user-friendly error messages

**Key Methods:**
- `handleGoogleSignIn()` - Initiates OAuth flow by opening browser to Google authorization page
- `handleOAuthCallback(code, state)` - Processes OAuth callback after user authorization
- `linkOrCreateUser(profile)` - Links Google account to existing user or creates new user
- `handleDisconnectGoogle()` - Disconnects Google account and revokes tokens

### 2. OAuthCallbackServer
**Location:** `src/main/java/com/esports/services/OAuthCallbackServer.java`

**Purpose:** Simple HTTP server to handle OAuth callbacks from Google.

**Key Features:**
- Listens on localhost:8080 for OAuth callbacks
- Extracts authorization code and state from callback URL
- Validates callback parameters
- Displays success/error pages in browser
- Delegates to SignatureOAuthController for processing

**Configuration:**
- Port: 8080
- Callback path: `/oauth/callback`
- Redirect URI: `http://localhost:8080/oauth/callback`

### 3. Login Screen Integration
**Location:** `src/main/resources/views/login.fxml`

**Changes:**
- Added "Sign in with Google" button below traditional login
- Added visual divider with "OR" text
- Styled button to match Google's branding guidelines

**Button Features:**
- Google-style white background with border
- Lock icon emoji for visual identification
- Opens browser to Google authorization page on click

### 4. User Settings Page
**Location:** `src/main/resources/views/user/user-settings.fxml`

**Purpose:** Comprehensive account settings page for managing authentication methods.

**Sections:**
1. **Profile Information**
   - Displays user name, email, and role
   - Shows profile picture from Google (if connected)
   - Circular profile picture with fallback placeholder

2. **Authentication Methods**
   - Password Authentication status
   - Google Account connection status
   - Signature Authentication status (placeholder)
   - Action buttons for each method

3. **Danger Zone**
   - Account deletion option (placeholder)

### 5. UserSettingsController
**Location:** `src/main/java/com/esports/controllers/user/UserSettingsController.java`

**Purpose:** Controller for user settings page.

**Key Features:**
- Loads and displays user profile information
- Shows authentication method status
- Handles Google account connection/disconnection
- Validates that users maintain at least one authentication method
- Displays profile picture from Google
- Provides confirmation dialogs for destructive actions

**Key Methods:**
- `loadUserProfile()` - Loads and displays user information
- `handleGoogleAction()` - Toggles Google connection based on current state
- `handleConnectGoogle()` - Initiates OAuth flow to connect Google
- `handleDisconnectGoogle()` - Disconnects Google with validation and confirmation

### 6. Navigation Integration
**Location:** `src/main/java/com/esports/controllers/user/GameBrowseController.java`

**Changes:**
- Added "Settings" button to navigation bar
- Button navigates to user settings page
- Maintains theme consistency (dark/light mode)

## User Flow

### Sign In with Google (New User)
1. User clicks "Sign in with Google" on login screen
2. Browser opens to Google authorization page
3. User authorizes the application
4. Google redirects to callback server with authorization code
5. Application exchanges code for tokens
6. Application fetches user profile from Google
7. New user account is created with Google profile information
8. User is logged in and navigated to appropriate dashboard

### Sign In with Google (Existing User)
1. User clicks "Sign in with Google" on login screen
2. Browser opens to Google authorization page
3. User authorizes the application
4. Google redirects to callback server with authorization code
5. Application exchanges code for tokens
6. Application fetches user profile from Google
7. Google account is linked to existing user account (matched by email)
8. User is logged in and navigated to appropriate dashboard

### Connect Google Account (From Settings)
1. User navigates to Settings page
2. User clicks "Connect Google" button
3. Browser opens to Google authorization page
4. User authorizes the application
5. Google account is linked to current user
6. Settings page updates to show connected status
7. Profile picture is displayed if available

### Disconnect Google Account
1. User navigates to Settings page
2. User clicks "Disconnect Google" button
3. System validates user has password authentication enabled
4. Confirmation dialog is displayed
5. User confirms disconnection
6. OAuth tokens are revoked with Google
7. Tokens are deleted from database
8. Google ID and profile picture are removed from user
9. Settings page updates to show disconnected status

## Security Features

### CSRF Protection
- State parameter generated for each OAuth flow
- State validated on callback to prevent CSRF attacks
- State stored in session and cleared after use

### PKCE (Proof Key for Code Exchange)
- Code verifier generated for each OAuth flow
- Code challenge sent to Google in authorization URL
- Code verifier used to exchange authorization code for tokens
- Prevents authorization code interception attacks

### Token Encryption
- Access tokens encrypted with AES-256-GCM before storage
- Refresh tokens encrypted with AES-256-GCM before storage
- Tokens decrypted only when needed for API calls

### Token Revocation
- Tokens revoked with Google on account disconnection
- Ensures Google account cannot be used after disconnection

### Authentication Method Validation
- Users cannot disconnect Google if no password is set
- Prevents users from locking themselves out of account
- Clear error messages guide users to set password first

## Error Handling

### Network Errors
- Displays user-friendly message: "Network error. Please check your internet connection."
- Logs detailed error for debugging

### Authorization Failures
- Displays user-friendly message: "Authentication failed. Please try again."
- Handles user cancellation gracefully

### Security Validation Failures
- Displays message: "Security validation failed. Please try again."
- Logs security events for monitoring

### Browser Opening Failures
- Displays authorization URL for manual navigation
- Provides fallback when Desktop API is unavailable

## Configuration

### OAuth Credentials
**Location:** `src/main/java/com/esports/services/OAuthService.java`

**Required Configuration:**
```java
private static final String CLIENT_ID = "YOUR_CLIENT_ID.apps.googleusercontent.com";
private static final String CLIENT_SECRET = "YOUR_CLIENT_SECRET";
private static final String REDIRECT_URI = "http://localhost:8080/oauth/callback";
```

**Setup Steps:**
1. Create project in Google Cloud Console
2. Enable Google+ API
3. Create OAuth 2.0 credentials
4. Add `http://localhost:8080/oauth/callback` as authorized redirect URI
5. Copy Client ID and Client Secret to OAuthService.java

### Callback Server
**Location:** `src/main/java/com/esports/Main.java`

**Automatic Startup:**
- Callback server starts automatically when application launches
- Listens on port 8080
- Stops automatically when application closes

## Testing

### Manual Testing Checklist

#### Sign In with Google (New User)
- [ ] Click "Sign in with Google" button
- [ ] Browser opens to Google authorization page
- [ ] Authorize application
- [ ] Redirected to success page in browser
- [ ] Logged in to application
- [ ] Profile picture displayed (if available)

#### Sign In with Google (Existing User)
- [ ] Create account with email/password
- [ ] Logout
- [ ] Click "Sign in with Google" with same email
- [ ] Google account linked to existing account
- [ ] Logged in successfully

#### Connect Google Account
- [ ] Login with email/password
- [ ] Navigate to Settings
- [ ] Click "Connect Google"
- [ ] Authorize in browser
- [ ] Settings page shows "Connected" status
- [ ] Profile picture displayed

#### Disconnect Google Account
- [ ] Login with Google
- [ ] Navigate to Settings
- [ ] Click "Disconnect Google"
- [ ] Confirm disconnection
- [ ] Settings page shows "Not connected" status
- [ ] Profile picture removed

#### Error Scenarios
- [ ] Cancel authorization in browser - shows error message
- [ ] Disconnect Google without password - shows error message
- [ ] Network failure - shows appropriate error message

## Requirements Satisfied

This implementation satisfies the following requirements from the Advanced Features Suite:

- **1.1** - Redirect to OAuth provider authorization page ✓
- **1.2** - Exchange authorization code for tokens ✓
- **1.3** - Retrieve user profile information ✓
- **1.4** - Link Google account to existing user ✓
- **1.5** - Create new user if email doesn't exist ✓
- **1.6** - Store profile picture URL ✓
- **1.9** - Display error messages on failure ✓
- **7.9** - Revoke tokens when disconnecting ✓
- **12.3** - Allow enabling/disabling authentication methods ✓
- **12.7** - Display active authentication methods ✓

## Future Enhancements

### Planned Features
1. **Automatic Token Refresh** - Refresh expired access tokens automatically
2. **Multiple OAuth Providers** - Support for GitHub, Microsoft, etc.
3. **Profile Picture Upload** - Allow users to upload custom profile pictures
4. **Two-Factor Authentication** - Add 2FA support for enhanced security
5. **Session Management** - View and manage active sessions

### Known Limitations
1. **Desktop Only** - Callback server requires localhost access
2. **Single Session** - Only one OAuth flow can be active at a time
3. **Manual Configuration** - OAuth credentials must be manually configured
4. **No Token Refresh UI** - Token refresh happens automatically but no UI feedback

## Troubleshooting

### "Failed to start OAuth callback server"
**Cause:** Port 8080 is already in use
**Solution:** Close other applications using port 8080 or change port in OAuthCallbackServer.java

### "Unable to open browser"
**Cause:** Desktop API not supported on this system
**Solution:** Manually copy authorization URL from error message and open in browser

### "Security validation failed"
**Cause:** State parameter mismatch (possible CSRF attack)
**Solution:** Try again - state is regenerated for each attempt

### "Cannot disconnect Google account"
**Cause:** No password authentication enabled
**Solution:** Set a password in account settings before disconnecting Google

### Profile picture not loading
**Cause:** Network issue or invalid URL
**Solution:** Check internet connection and try reconnecting Google account

## Support

For issues or questions about the OAuth UI implementation:
1. Check this documentation
2. Review error logs in console
3. Verify OAuth credentials are correctly configured
4. Ensure callback server is running on port 8080
5. Check Google Cloud Console for API quota limits
