package com.esports.dao;

import com.esports.db.DatabaseConnection;
import com.esports.models.User;
import com.esports.models.UserRole;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private Connection con = DatabaseConnection.getInstance().getConnection();

    // JSON serialization helper methods
    private String serializeRoles(List<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return "[\"ROLE_USER\"]"; // Default role
        }
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < roles.size(); i++) {
            json.append("\"").append(roles.get(i).name()).append("\"");
            if (i < roles.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }

    private List<UserRole> deserializeRoles(String rolesJson) {
        List<UserRole> roles = new ArrayList<>();
        if (rolesJson == null || rolesJson.trim().isEmpty()) {
            roles.add(UserRole.USER); // Default role
            return roles;
        }
        // Simple JSON array parsing: ["ROLE_USER"] or ["ROLE_ADMIN"]
        String cleaned = rolesJson.replace("[", "").replace("]", "").replace("\"", "");
        String[] roleStrings = cleaned.split(",");
        for (String roleStr : roleStrings) {
            String trimmed = roleStr.trim();
            if (!trimmed.isEmpty()) {
                try {
                    roles.add(UserRole.valueOf(trimmed));
                } catch (IllegalArgumentException e) {
                    System.out.println("[WARNING] Unknown role: " + trimmed + ", defaulting to USER");
                    roles.add(UserRole.USER);
                }
            }
        }
        if (roles.isEmpty()) {
            roles.add(UserRole.USER); // Default if parsing failed
        }
        return roles;
    }

    public List<User> getAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setFirstName(rs.getString("first_name"));
                u.setLastName(rs.getString("last_name"));
                u.setEmail(rs.getString("email"));
                u.setPhone(rs.getString("phone"));
                u.setAddress(rs.getString("address"));
                u.setRoles(deserializeRoles(rs.getString("roles")));
                u.setPassword(rs.getString("password"));
                u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                u.setProfileImage(rs.getString("profile_image"));
                u.setDetectedSentiment(rs.getString("detected_sentiment"));
                u.setSentimentScore(rs.getObject("sentiment_score") != null ? rs.getDouble("sentiment_score") : null);
                u.setDetectedAge(rs.getObject("detected_age") != null ? rs.getInt("detected_age") : null);
                u.setAgeConfidence(rs.getObject("age_confidence") != null ? rs.getDouble("age_confidence") : null);
                u.setCaptureSource(rs.getString("capture_source"));
                Timestamp captureTs = rs.getTimestamp("capture_timestamp");
                u.setCaptureTimestamp(captureTs != null ? captureTs.toLocalDateTime() : null);
                u.setCaptureVerified(rs.getObject("capture_verified") != null ? rs.getBoolean("capture_verified") : null);
                u.setIsBlocked(rs.getBoolean("is_blocked"));
                Timestamp blockedAtTs = rs.getTimestamp("blocked_at");
                u.setBlockedAt(blockedAtTs != null ? blockedAtTs.toLocalDateTime() : null);
                Timestamp blockExpiresTs = rs.getTimestamp("block_expires_at");
                u.setBlockExpiresAt(blockExpiresTs != null ? blockExpiresTs.toLocalDateTime() : null);
                u.setBlockReason(rs.getString("block_reason"));
                u.setGoogleId(rs.getString("google_id"));
                u.setProfilePictureUrl(rs.getString("profile_picture_url"));
                u.setPreferredAuthMethod(rs.getString("preferred_auth_method"));
                Timestamp lastLoginTs = rs.getTimestamp("last_login");
                u.setLastLogin(lastLoginTs != null ? lastLoginTs.toLocalDateTime() : null);
                users.add(u);
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] getAll error: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve users from database: " + e.getMessage(), e);
        }
        return users;
    }

    public boolean add(User u) {
        String sql = "INSERT INTO user (first_name, last_name, email, phone, address, roles, password, created_at, " +
                     "profile_image, detected_sentiment, sentiment_score, detected_age, age_confidence, " +
                     "capture_source, capture_timestamp, capture_verified, is_blocked, blocked_at, " +
                     "block_expires_at, block_reason, google_id, profile_picture_url, preferred_auth_method, last_login) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, u.getFirstName());
            ps.setString(2, u.getLastName());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPhone());
            ps.setString(5, u.getAddress());
            ps.setString(6, serializeRoles(u.getRoles()));
            ps.setString(7, u.getPassword());
            ps.setTimestamp(8, Timestamp.valueOf(u.getCreatedAt() != null ? u.getCreatedAt() : LocalDateTime.now()));
            ps.setString(9, u.getProfileImage());
            ps.setString(10, u.getDetectedSentiment());
            ps.setObject(11, u.getSentimentScore());
            ps.setObject(12, u.getDetectedAge());
            ps.setObject(13, u.getAgeConfidence());
            ps.setString(14, u.getCaptureSource());
            ps.setTimestamp(15, u.getCaptureTimestamp() != null ? Timestamp.valueOf(u.getCaptureTimestamp()) : null);
            ps.setObject(16, u.getCaptureVerified());
            ps.setBoolean(17, u.getIsBlocked() != null ? u.getIsBlocked() : false);
            ps.setTimestamp(18, u.getBlockedAt() != null ? Timestamp.valueOf(u.getBlockedAt()) : null);
            ps.setTimestamp(19, u.getBlockExpiresAt() != null ? Timestamp.valueOf(u.getBlockExpiresAt()) : null);
            ps.setString(20, u.getBlockReason());
            ps.setString(21, u.getGoogleId());
            ps.setString(22, u.getProfilePictureUrl());
            ps.setString(23, u.getPreferredAuthMethod());
            ps.setTimestamp(24, u.getLastLogin() != null ? Timestamp.valueOf(u.getLastLogin()) : null);
            int rowsAffected = ps.executeUpdate();
            System.out.println("[SUCCESS] User added!");
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] add error: " + e.getMessage());
            throw new RuntimeException("Failed to add user to database: " + e.getMessage(), e);
        }
    }

    public boolean update(User u) {
        String sql = "UPDATE user SET first_name=?, last_name=?, email=?, phone=?, address=?, roles=?, password=?, " +
                     "profile_image=?, detected_sentiment=?, sentiment_score=?, detected_age=?, age_confidence=?, " +
                     "capture_source=?, capture_timestamp=?, capture_verified=?, is_blocked=?, blocked_at=?, " +
                     "block_expires_at=?, block_reason=?, google_id=?, profile_picture_url=?, preferred_auth_method=?, last_login=? WHERE id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, u.getFirstName());
            ps.setString(2, u.getLastName());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPhone());
            ps.setString(5, u.getAddress());
            ps.setString(6, serializeRoles(u.getRoles()));
            ps.setString(7, u.getPassword());
            ps.setString(8, u.getProfileImage());
            ps.setString(9, u.getDetectedSentiment());
            ps.setObject(10, u.getSentimentScore());
            ps.setObject(11, u.getDetectedAge());
            ps.setObject(12, u.getAgeConfidence());
            ps.setString(13, u.getCaptureSource());
            ps.setTimestamp(14, u.getCaptureTimestamp() != null ? Timestamp.valueOf(u.getCaptureTimestamp()) : null);
            ps.setObject(15, u.getCaptureVerified());
            ps.setBoolean(16, u.getIsBlocked() != null ? u.getIsBlocked() : false);
            ps.setTimestamp(17, u.getBlockedAt() != null ? Timestamp.valueOf(u.getBlockedAt()) : null);
            ps.setTimestamp(18, u.getBlockExpiresAt() != null ? Timestamp.valueOf(u.getBlockExpiresAt()) : null);
            ps.setString(19, u.getBlockReason());
            ps.setString(20, u.getGoogleId());
            ps.setString(21, u.getProfilePictureUrl());
            ps.setString(22, u.getPreferredAuthMethod());
            ps.setTimestamp(23, u.getLastLogin() != null ? Timestamp.valueOf(u.getLastLogin()) : null);
            ps.setInt(24, u.getId());
            int rowsAffected = ps.executeUpdate();
            System.out.println("[SUCCESS] User updated!");
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] update error: " + e.getMessage());
            throw new RuntimeException("Failed to update user in database: " + e.getMessage(), e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM user WHERE id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            int rowsAffected = ps.executeUpdate();
            System.out.println("[SUCCESS] User deleted!");
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] delete error: " + e.getMessage());
            throw new RuntimeException("Failed to delete user from database: " + e.getMessage(), e);
        }
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setFirstName(rs.getString("first_name"));
                u.setLastName(rs.getString("last_name"));
                u.setEmail(rs.getString("email"));
                u.setPhone(rs.getString("phone"));
                u.setAddress(rs.getString("address"));
                u.setRoles(deserializeRoles(rs.getString("roles")));
                u.setPassword(rs.getString("password"));
                u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                u.setProfileImage(rs.getString("profile_image"));
                u.setDetectedSentiment(rs.getString("detected_sentiment"));
                u.setSentimentScore(rs.getObject("sentiment_score") != null ? rs.getDouble("sentiment_score") : null);
                u.setDetectedAge(rs.getObject("detected_age") != null ? rs.getInt("detected_age") : null);
                u.setAgeConfidence(rs.getObject("age_confidence") != null ? rs.getDouble("age_confidence") : null);
                u.setCaptureSource(rs.getString("capture_source"));
                Timestamp captureTs = rs.getTimestamp("capture_timestamp");
                u.setCaptureTimestamp(captureTs != null ? captureTs.toLocalDateTime() : null);
                u.setCaptureVerified(rs.getObject("capture_verified") != null ? rs.getBoolean("capture_verified") : null);
                u.setIsBlocked(rs.getBoolean("is_blocked"));
                Timestamp blockedAtTs = rs.getTimestamp("blocked_at");
                u.setBlockedAt(blockedAtTs != null ? blockedAtTs.toLocalDateTime() : null);
                Timestamp blockExpiresTs = rs.getTimestamp("block_expires_at");
                u.setBlockExpiresAt(blockExpiresTs != null ? blockExpiresTs.toLocalDateTime() : null);
                u.setBlockReason(rs.getString("block_reason"));
                u.setGoogleId(rs.getString("google_id"));
                u.setProfilePictureUrl(rs.getString("profile_picture_url"));
                u.setPreferredAuthMethod(rs.getString("preferred_auth_method"));
                Timestamp lastLoginTs = rs.getTimestamp("last_login");
                u.setLastLogin(lastLoginTs != null ? lastLoginTs.toLocalDateTime() : null);
                return u;
            }
            return null;
        } catch (SQLException e) {
            System.out.println("[ERROR] findByEmail error: " + e.getMessage());
            throw new RuntimeException("Failed to find user by email: " + e.getMessage(), e);
        }
    }

    /**
     * Find a user by their Google ID.
     * 
     * @param googleId The Google OAuth user identifier
     * @return User object if found, null otherwise
     * @throws RuntimeException if database operation fails
     * 
     * Requirement 1.4: Link Google account to existing user account
     */
    public User findByGoogleId(String googleId) {
        String sql = "SELECT * FROM user WHERE google_id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, googleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setFirstName(rs.getString("first_name"));
                u.setLastName(rs.getString("last_name"));
                u.setEmail(rs.getString("email"));
                u.setPhone(rs.getString("phone"));
                u.setAddress(rs.getString("address"));
                u.setRoles(deserializeRoles(rs.getString("roles")));
                u.setPassword(rs.getString("password"));
                u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                u.setProfileImage(rs.getString("profile_image"));
                u.setDetectedSentiment(rs.getString("detected_sentiment"));
                u.setSentimentScore(rs.getObject("sentiment_score") != null ? rs.getDouble("sentiment_score") : null);
                u.setDetectedAge(rs.getObject("detected_age") != null ? rs.getInt("detected_age") : null);
                u.setAgeConfidence(rs.getObject("age_confidence") != null ? rs.getDouble("age_confidence") : null);
                u.setCaptureSource(rs.getString("capture_source"));
                Timestamp captureTs = rs.getTimestamp("capture_timestamp");
                u.setCaptureTimestamp(captureTs != null ? captureTs.toLocalDateTime() : null);
                u.setCaptureVerified(rs.getObject("capture_verified") != null ? rs.getBoolean("capture_verified") : null);
                u.setIsBlocked(rs.getBoolean("is_blocked"));
                Timestamp blockedAtTs = rs.getTimestamp("blocked_at");
                u.setBlockedAt(blockedAtTs != null ? blockedAtTs.toLocalDateTime() : null);
                Timestamp blockExpiresTs = rs.getTimestamp("block_expires_at");
                u.setBlockExpiresAt(blockExpiresTs != null ? blockExpiresTs.toLocalDateTime() : null);
                u.setBlockReason(rs.getString("block_reason"));
                u.setGoogleId(rs.getString("google_id"));
                u.setProfilePictureUrl(rs.getString("profile_picture_url"));
                u.setPreferredAuthMethod(rs.getString("preferred_auth_method"));
                Timestamp lastLoginTs = rs.getTimestamp("last_login");
                u.setLastLogin(lastLoginTs != null ? lastLoginTs.toLocalDateTime() : null);
                return u;
            }
            return null;
        } catch (SQLException e) {
            System.out.println("[ERROR] findByGoogleId error: " + e.getMessage());
            throw new RuntimeException("Failed to find user by Google ID: " + e.getMessage(), e);
        }
    }

    /**
     * Update the last login timestamp for a user.
     * 
     * @param userId The user ID to update
     * @return true if update was successful, false otherwise
     * @throws RuntimeException if database operation fails
     * 
     * Requirement 1.7: Track user login activity
     */
    public boolean updateLastLogin(int userId) {
        String sql = "UPDATE user SET last_login = ? WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, userId);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] updateLastLogin error: " + e.getMessage());
            throw new RuntimeException("Failed to update last login: " + e.getMessage(), e);
        }
    }
}
