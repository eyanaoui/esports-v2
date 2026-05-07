# Bugfix Requirements Document

## Introduction

The Java application's User model and UserDAO are misaligned with the actual database schema, causing all CRUD operations (Create, Read, Update, Delete) to fail. The Java code references columns that don't exist in the database (`username`, `role`) and is missing mappings for numerous fields that do exist in the database (`first_name`, `last_name`, `roles` as JSON array, and additional fields like `phone`, `address`, `profile_image`, sentiment analysis fields, capture metadata, and blocking fields). This schema mismatch results in SQL exceptions and prevents any user management functionality from working.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN the application attempts to retrieve all users via `UserDAO.getAll()` THEN the system throws SQLException because the SQL query references non-existent column `username`

1.2 WHEN the application attempts to add a new user via `UserDAO.add()` THEN the system throws SQLException because the SQL INSERT references non-existent columns `username` and `role`

1.3 WHEN the application attempts to update a user via `UserDAO.update()` THEN the system throws SQLException because the SQL UPDATE references non-existent columns `username` and `role`

1.4 WHEN the application attempts to map database results to User model THEN the system throws SQLException because it tries to read non-existent column `username` from ResultSet

1.5 WHEN the application attempts to store role information THEN the system fails because it treats `role` as a simple enum string instead of a JSON array `roles`

1.6 WHEN the application retrieves user data THEN the system ignores all additional database fields (phone, address, profile_image, detected_sentiment, sentiment_score, detected_age, age_confidence, capture_source, capture_timestamp, capture_verified, is_blocked, blocked_at, block_expires_at, block_reason)

### Expected Behavior (Correct)

2.1 WHEN the application attempts to retrieve all users via `UserDAO.getAll()` THEN the system SHALL successfully execute SQL query using actual database columns (`first_name`, `last_name`, `roles` as JSON)

2.2 WHEN the application attempts to add a new user via `UserDAO.add()` THEN the system SHALL successfully execute SQL INSERT using actual database columns and properly serialize `roles` as JSON array

2.3 WHEN the application attempts to update a user via `UserDAO.update()` THEN the system SHALL successfully execute SQL UPDATE using actual database columns and properly serialize `roles` as JSON array

2.4 WHEN the application attempts to map database results to User model THEN the system SHALL successfully read `first_name` and `last_name` columns and deserialize `roles` JSON array

2.5 WHEN the application stores role information THEN the system SHALL serialize roles as JSON array format (e.g., `["ROLE_USER"]`, `["ROLE_ADMIN"]`)

2.6 WHEN the application retrieves user data THEN the system SHALL map all additional database fields to corresponding User model properties (phone, address, profile_image, detected_sentiment, sentiment_score, detected_age, age_confidence, capture_source, capture_timestamp, capture_verified, is_blocked, blocked_at, block_expires_at, block_reason)

### Unchanged Behavior (Regression Prevention)

3.1 WHEN the application accesses user ID, email, password, and created_at fields THEN the system SHALL CONTINUE TO work correctly as these columns exist in both the model and database

3.2 WHEN the application performs database connection validation THEN the system SHALL CONTINUE TO throw RuntimeException with descriptive message if connection is null

3.3 WHEN the application encounters SQL errors THEN the system SHALL CONTINUE TO wrap SQLException in RuntimeException with descriptive error messages

3.4 WHEN UserFormController validates user inputs THEN the system SHALL CONTINUE TO enforce validation rules (though field names may change from username to first_name/last_name)

3.5 WHEN the application displays user information in the UI THEN the system SHALL CONTINUE TO show user data in tables and forms (though the displayed fields will now include first_name/last_name instead of username)
