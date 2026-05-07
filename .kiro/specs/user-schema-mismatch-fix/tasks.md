# Implementation Plan

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - CRUD Operations Schema Mismatch
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists (SQL exceptions due to column name mismatch)
  - **Scoped PBT Approach**: For deterministic bugs, scope the property to the concrete failing case(s) to ensure reproducibility
  - Test that UserDAO.getAll() throws SQLException mentioning "username" column not found
  - Test that UserDAO.add() throws SQLException mentioning "username" or "role" column not found
  - Test that UserDAO.update() throws SQLException mentioning "username" or "role" column not found
  - Test that ResultSet mapping fails when trying to read non-existent "username" column
  - The test assertions should match the Expected Behavior Properties from design (successful CRUD operations with correct schema)
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the bug exists)
  - Document counterexamples found to understand root cause (specific SQLException messages)
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Error Handling and Connection Validation
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for error handling scenarios (null connection, SQL errors)
  - Write property-based tests capturing observed error handling patterns from Preservation Requirements
  - Test that null connection throws RuntimeException with message "Database connection is null"
  - Test that SQL errors are wrapped in RuntimeException with descriptive messages
  - Test that operations on id, email, password, created_at fields continue to work correctly
  - Property-based testing generates many test cases for stronger guarantees
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 3. Fix User schema mismatch

  - [x] 3.1 Update User model to match database schema
    - Replace username field with firstName and lastName fields
    - Replace single role field with roles List<UserRole>
    - Add missing database fields: phone, address, profileImage, detectedSentiment, sentimentScore, detectedAge, ageConfidence, captureSource, captureTimestamp, captureVerified, isBlocked, blockedAt, blockExpiresAt, blockReason
    - Update constructor to accept firstName and lastName instead of username
    - Update getters and setters for all new fields
    - Update toString() method to return firstName + " " + lastName
    - _Bug_Condition: isBugCondition(operation) where operation.sqlQuery CONTAINS "username" OR "role" AND NOT databaseSchemaContains("username")_
    - _Expected_Behavior: CRUD operations successfully execute using actual database columns (first_name, last_name, roles as JSON)_
    - _Preservation: Operations on id, email, password, created_at fields continue to work correctly_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 3.1_

  - [x] 3.2 Add JSON serialization helper methods to UserDAO
    - Add private method serializeRoles(List<UserRole> roles) that converts List to JSON array string format ["ROLE_USER"]
    - Add private method deserializeRoles(String rolesJson) that parses JSON array string to List<UserRole>
    - Handle edge cases: empty roles list, null values, multiple roles
    - _Bug_Condition: Code treats role as simple enum string instead of JSON array_
    - _Expected_Behavior: Roles properly serialized/deserialized as JSON array format_
    - _Preservation: Error handling patterns remain unchanged_
    - _Requirements: 2.5_

  - [x] 3.3 Update UserDAO.getAll() method
    - Keep SQL query as "SELECT * FROM user"
    - Update ResultSet mapping to use first_name and last_name columns instead of username
    - Update ResultSet mapping to deserialize roles JSON array using deserializeRoles() helper
    - Add ResultSet mappings for all additional fields (phone, address, profileImage, etc.)
    - _Bug_Condition: getAll() tries to read rs.getString("username") which doesn't exist_
    - _Expected_Behavior: getAll() successfully retrieves users with correct column mapping_
    - _Preservation: Database connection validation and SQLException wrapping remain unchanged_
    - _Requirements: 2.1, 2.4, 2.6, 3.2, 3.3_

  - [x] 3.4 Update UserDAO.add() method
    - Update SQL INSERT column list from (username, email, password, role, created_at) to (first_name, last_name, email, password, roles, created_at, phone, address, profile_image, ...)
    - Update parameter bindings to use user.getFirstName() and user.getLastName() instead of user.getUsername()
    - Add JSON serialization for roles using serializeRoles() helper
    - Add parameter bindings for all new fields
    - _Bug_Condition: add() references non-existent username and role columns_
    - _Expected_Behavior: add() successfully inserts user with JSON roles serialization_
    - _Preservation: SQLException wrapping and console logging remain unchanged_
    - _Requirements: 2.2, 2.5, 2.6, 3.2, 3.3_

  - [x] 3.5 Update UserDAO.update() method
    - Update SQL UPDATE SET clause from (username=?, email=?, password=?, role=?) to (first_name=?, last_name=?, email=?, password=?, roles=?, phone=?, address=?, ...)
    - Update parameter bindings to use user.getFirstName() and user.getLastName() instead of user.getUsername()
    - Add JSON serialization for roles using serializeRoles() helper
    - Add parameter bindings for all new fields
    - _Bug_Condition: update() references non-existent username and role columns_
    - _Expected_Behavior: update() successfully updates user with JSON roles serialization_
    - _Preservation: SQLException wrapping and console logging remain unchanged_
    - _Requirements: 2.3, 2.5, 2.6, 3.2, 3.3_

  - [x] 3.6 Update UserFormController to use new User model fields
    - Replace usernameField with firstNameField and lastNameField in FXML annotations
    - Update setUser() method to use user.getFirstName() and user.getLastName()
    - Update handleSave() method to create User with firstName and lastName
    - Update validation logic to validate firstName and lastName instead of username
    - Update closeWindow() method to reference firstNameField instead of usernameField
    - _Bug_Condition: Controller references username field that no longer exists in User model_
    - _Expected_Behavior: Controller works with firstName and lastName fields_
    - _Preservation: Validation rules and error handling remain unchanged_
    - _Requirements: 2.1, 2.2, 2.3, 3.4, 3.5_

  - [x] 3.7 Update user-form.fxml UI
    - Replace username TextField with firstNameField and lastNameField TextFields
    - Add labels "First Name:" and "Last Name:"
    - Update fx:id references to match controller field names
    - Optionally add fields for phone, address, profileImage if needed in UI
    - _Bug_Condition: FXML references username field that no longer exists_
    - _Expected_Behavior: FXML displays firstName and lastName fields_
    - _Preservation: Form layout and validation behavior remain consistent_
    - _Requirements: 2.1, 2.2, 2.3, 3.5_

  - [x] 3.8 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - CRUD Operations Use Correct Schema
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - Verify UserDAO.getAll() successfully retrieves users without SQLException
    - Verify UserDAO.add() successfully inserts users without SQLException
    - Verify UserDAO.update() successfully updates users without SQLException
    - Verify ResultSet mapping works with first_name, last_name, and roles columns
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x] 3.9 Verify preservation tests still pass
    - **Property 2: Preservation** - Error Handling and Connection Validation
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm null connection still throws RuntimeException with correct message
    - Confirm SQL errors still wrapped in RuntimeException with descriptive messages
    - Confirm operations on id, email, password, created_at fields still work correctly
    - Confirm all tests still pass after fix (no regressions)

- [x] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
