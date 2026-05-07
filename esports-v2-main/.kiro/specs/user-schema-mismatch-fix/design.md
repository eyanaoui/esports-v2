# User Schema Mismatch Bugfix Design

## Overview

The Java application's User model and UserDAO are fundamentally misaligned with the actual database schema. The code references columns that don't exist (`username`, `role`) while the database uses different column names (`first_name`, `last_name`, `roles` as JSON array). Additionally, the database contains numerous fields that the Java model doesn't map (phone, address, profile_image, sentiment analysis fields, capture metadata, blocking fields). This mismatch causes all CRUD operations to fail with SQL exceptions. The fix requires updating the User model to match the actual schema and modifying UserDAO to properly serialize/deserialize the JSON roles array.

## Glossary

- **Bug_Condition (C)**: Any CRUD operation (Create, Read, Update, Delete) on the User entity that references non-existent database columns
- **Property (P)**: CRUD operations should successfully execute using actual database columns and properly handle JSON serialization for roles
- **Preservation**: Database connection validation, error handling patterns, and SQL exception wrapping must remain unchanged
- **UserDAO**: The Data Access Object in `src/main/java/com/esports/dao/UserDAO.java` that performs SQL operations on the user table
- **User Model**: The entity class in `src/main/java/com/esports/models/User.java` representing user data
- **roles JSON Array**: Database column storing user roles as JSON array format: `["ROLE_USER"]` or `["ROLE_ADMIN"]`
- **Schema Mismatch**: Discrepancy between Java model field names and actual database column names

## Bug Details

### Bug Condition

The bug manifests when any CRUD operation is attempted on the User entity. The UserDAO methods reference database columns that don't exist (`username`, `role`), causing SQL exceptions. Additionally, the User model lacks fields for numerous database columns, preventing complete data mapping.

**Formal Specification:**
```
FUNCTION isBugCondition(operation)
  INPUT: operation of type CRUDOperation (getAll, add, update, delete)
  OUTPUT: boolean
  
  RETURN (operation.sqlQuery CONTAINS "username" OR operation.sqlQuery CONTAINS "role")
         AND NOT databaseSchemaContains("username")
         AND NOT databaseSchemaContains("role")
         AND databaseSchemaContains("first_name")
         AND databaseSchemaContains("last_name")
         AND databaseSchemaContains("roles")
END FUNCTION
```

### Examples

- **getAll() fails**: SQL query `SELECT * FROM user` succeeds, but ResultSet mapping fails when trying to read `rs.getString("username")` - column doesn't exist
- **add() fails**: SQL INSERT `INSERT INTO user (username, email, password, role, created_at) VALUES (?, ?, ?, ?, ?)` fails - columns `username` and `role` don't exist
- **update() fails**: SQL UPDATE `UPDATE user SET username=?, email=?, password=?, role=? WHERE id=?` fails - columns `username` and `role` don't exist
- **Missing data mapping**: Database contains `phone`, `address`, `profile_image`, sentiment fields, capture metadata, and blocking fields, but User model doesn't map these, causing data loss

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Database connection validation must continue to throw RuntimeException with descriptive message if connection is null
- SQL exception handling must continue to wrap SQLException in RuntimeException with descriptive error messages
- Operations on id, email, password, and created_at fields must continue to work correctly (these columns exist in both model and database)

**Scope:**
All database connection management, error handling patterns, and exception wrapping logic should be completely unaffected by this fix. This includes:
- Connection null checks before SQL execution
- Try-catch blocks wrapping SQL operations
- RuntimeException wrapping with descriptive messages
- Console logging of success/error messages

## Hypothesized Root Cause

Based on the bug description and code analysis, the root causes are:

1. **Column Name Mismatch**: The Java code uses `username` and `role` columns, but the actual database schema uses `first_name`, `last_name`, and `roles` (JSON array)
   - UserDAO.getAll() tries to read `rs.getString("username")` which doesn't exist
   - UserDAO.add() and update() try to insert/update `username` and `role` columns which don't exist

2. **Data Type Mismatch**: The code treats `role` as a simple enum string, but the database stores `roles` as a JSON array
   - Database expects: `["ROLE_USER"]` or `["ROLE_ADMIN"]`
   - Code provides: `"USER"` or `"ADMIN"` (enum name as string)

3. **Incomplete Model Mapping**: The User model only has 6 fields (id, username, email, password, role, createdAt), but the database has 21 columns
   - Missing fields: first_name, last_name, phone, address, profile_image, detected_sentiment, sentiment_score, detected_age, age_confidence, capture_source, capture_timestamp, capture_verified, is_blocked, blocked_at, block_expires_at, block_reason
   - This causes data loss when reading from database

4. **No JSON Serialization Logic**: The code lacks logic to serialize/deserialize the roles JSON array
   - Need to convert between `List<UserRole>` in Java and `["ROLE_USER"]` JSON format in database

## Correctness Properties

Property 1: Bug Condition - CRUD Operations Use Correct Schema

_For any_ CRUD operation (getAll, add, update, delete) on the User entity, the fixed UserDAO SHALL use actual database column names (first_name, last_name, roles) instead of non-existent columns (username, role), and SHALL properly serialize roles as JSON array format.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6**

Property 2: Preservation - Error Handling and Connection Validation

_For any_ database operation that encounters null connection or SQL errors, the fixed code SHALL produce exactly the same error handling behavior as the original code, preserving RuntimeException wrapping with descriptive messages and connection validation logic.

**Validates: Requirements 3.1, 3.2, 3.3**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

**File**: `src/main/java/com/esports/models/User.java`

**Specific Changes**:
1. **Replace username field with first_name and last_name**:
   - Remove: `private String username;`
   - Add: `private String firstName;` and `private String lastName;`
   - Update getters/setters and constructor

2. **Replace single role with roles list**:
   - Remove: `private UserRole role;`
   - Add: `private List<UserRole> roles;`
   - Update getters/setters

3. **Add missing database fields**:
   - Add: `private String phone;`
   - Add: `private String address;`
   - Add: `private String profileImage;`
   - Add: `private String detectedSentiment;`
   - Add: `private Double sentimentScore;`
   - Add: `private Integer detectedAge;`
   - Add: `private Double ageConfidence;`
   - Add: `private String captureSource;`
   - Add: `private LocalDateTime captureTimestamp;`
   - Add: `private Boolean captureVerified;`
   - Add: `private Boolean isBlocked;`
   - Add: `private LocalDateTime blockedAt;`
   - Add: `private LocalDateTime blockExpiresAt;`
   - Add: `private String blockReason;`
   - Add corresponding getters/setters

4. **Update toString() method**:
   - Change from: `return username;`
   - To: `return firstName + " " + lastName;`

**File**: `src/main/java/com/esports/dao/UserDAO.java`

**Specific Changes**:
1. **Update getAll() SQL query and ResultSet mapping**:
   - Keep: `SELECT * FROM user`
   - Change ResultSet mapping:
     - Replace: `u.setUsername(rs.getString("username"));`
     - With: `u.setFirstName(rs.getString("first_name")); u.setLastName(rs.getString("last_name"));`
     - Replace: `u.setRole(UserRole.valueOf(rs.getString("role")));`
     - With: JSON deserialization logic to parse `roles` array into `List<UserRole>`
   - Add mappings for all additional fields (phone, address, profile_image, etc.)

2. **Update add() SQL INSERT**:
   - Change column list from: `(username, email, password, role, created_at)`
   - To: `(first_name, last_name, email, password, roles, created_at, phone, address, profile_image, ...)`
   - Add JSON serialization logic to convert `List<UserRole>` to JSON array string
   - Add parameter bindings for all new fields

3. **Update update() SQL UPDATE**:
   - Change SET clause from: `username=?, email=?, password=?, role=?`
   - To: `first_name=?, last_name=?, email=?, password=?, roles=?, phone=?, address=?, ...`
   - Add JSON serialization logic for roles
   - Add parameter bindings for all new fields

4. **Add JSON helper methods**:
   - Add: `private String serializeRoles(List<UserRole> roles)` - converts list to JSON array string
   - Add: `private List<UserRole> deserializeRoles(String rolesJson)` - parses JSON array to list
   - Implementation can use simple string manipulation or a JSON library

**File**: `src/main/java/com/esports/controllers/admin/UserFormController.java`

**Specific Changes**:
1. **Update form fields**:
   - Replace: `@FXML private TextField usernameField;`
   - With: `@FXML private TextField firstNameField, lastNameField;`

2. **Update setUser() method**:
   - Replace: `usernameField.setText(user.getUsername());`
   - With: `firstNameField.setText(user.getFirstName()); lastNameField.setText(user.getLastName());`

3. **Update handleSave() method**:
   - Replace: `new User(usernameField.getText().trim(), ...)`
   - With: `new User(firstNameField.getText().trim(), lastNameField.getText().trim(), ...)`
   - Update role handling to work with List<UserRole>

4. **Update validation**:
   - Replace username validation with first_name and last_name validation
   - Add validation for new required fields if needed

5. **Update closeWindow() method**:
   - Replace: `((Stage) usernameField.getScene().getWindow()).close();`
   - With: `((Stage) firstNameField.getScene().getWindow()).close();`

**File**: `src/main/resources/views/admin/user-form.fxml`

**Specific Changes**:
1. **Update form UI**:
   - Replace username TextField with two TextFields: firstNameField and lastNameField
   - Add labels "First Name:" and "Last Name:"
   - Optionally add fields for phone, address, profile_image if needed in UI

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on unfixed code (SQL exceptions), then verify the fix works correctly and preserves existing error handling behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm that SQL operations fail due to schema mismatch.

**Test Plan**: Write tests that attempt each CRUD operation and capture the SQLException messages. Run these tests on the UNFIXED code to observe failures and confirm the root cause is column name mismatch.

**Test Cases**:
1. **getAll() Test**: Call UserDAO.getAll() and expect SQLException mentioning "username" column not found (will fail on unfixed code)
2. **add() Test**: Call UserDAO.add() with a new user and expect SQLException mentioning "username" or "role" column not found (will fail on unfixed code)
3. **update() Test**: Call UserDAO.update() with modified user and expect SQLException mentioning "username" or "role" column not found (will fail on unfixed code)
4. **ResultSet Mapping Test**: Manually execute `SELECT * FROM user` and try to read "username" column - expect SQLException (will fail on unfixed code)

**Expected Counterexamples**:
- SQLException: "Column 'username' not found"
- SQLException: "Unknown column 'username' in 'field list'"
- SQLException: "Column 'role' not found"
- Possible causes: column name mismatch, missing JSON serialization logic

### Fix Checking

**Goal**: Verify that for all CRUD operations where the bug condition holds, the fixed UserDAO produces the expected behavior (successful execution with correct schema).

**Pseudocode:**
```
FOR ALL operation IN [getAll, add, update, delete] DO
  result := operation_fixed()
  ASSERT result.success = true
  ASSERT result.usesCorrectColumns = true
  ASSERT result.rolesSerializedAsJSON = true
END FOR
```

### Preservation Checking

**Goal**: Verify that for all error handling scenarios (null connection, SQL errors), the fixed code produces the same error handling behavior as the original code.

**Pseudocode:**
```
FOR ALL errorScenario IN [nullConnection, sqlError] DO
  ASSERT errorHandling_original(errorScenario) = errorHandling_fixed(errorScenario)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many test cases automatically across different error scenarios
- It catches edge cases in error handling that manual unit tests might miss
- It provides strong guarantees that error handling behavior is unchanged

**Test Plan**: Observe error handling behavior on UNFIXED code first (connection validation, exception wrapping), then write property-based tests capturing that behavior.

**Test Cases**:
1. **Null Connection Preservation**: Verify that null connection still throws RuntimeException with message "Database connection is null"
2. **SQLException Wrapping Preservation**: Verify that SQL errors are still wrapped in RuntimeException with descriptive messages
3. **Console Logging Preservation**: Verify that success/error console messages still appear
4. **Return Value Preservation**: Verify that boolean return values (true/false) still work correctly

### Unit Tests

- Test UserDAO.getAll() successfully retrieves users with correct column mapping
- Test UserDAO.add() successfully inserts user with JSON roles serialization
- Test UserDAO.update() successfully updates user with JSON roles serialization
- Test UserDAO.delete() continues to work (no schema changes for this operation)
- Test JSON serialization: List<UserRole> to `["ROLE_USER"]` format
- Test JSON deserialization: `["ROLE_ADMIN"]` to List<UserRole>
- Test edge cases: empty roles list, multiple roles, null handling

### Property-Based Tests

- Generate random User objects and verify add() then getAll() returns same data
- Generate random role combinations and verify JSON serialization round-trips correctly
- Generate random error scenarios and verify error handling matches original behavior
- Test that all 21 database columns are properly mapped in both directions

### Integration Tests

- Test full user creation flow: UI form → UserFormController → UserDAO → Database
- Test full user update flow with schema changes
- Test user listing in admin dashboard with new column names
- Test that existing users in database (if any) can be read correctly with new mapping
