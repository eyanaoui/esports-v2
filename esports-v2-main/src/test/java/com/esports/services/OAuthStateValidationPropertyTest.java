package com.esports.services;

import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for OAuth state validation (CSRF protection).
 * 
 * Tests universal properties of state parameter validation to ensure
 * CSRF protection is robust across all possible inputs.
 * 
 * **Validates: Property 11: OAuth CSRF Protection**
 * **Validates: Requirements 1.10, 7.3**
 */
class OAuthStateValidationPropertyTest {
    
    /**
     * Property 11: OAuth CSRF Protection
     * 
     * For all OAuth authentication flows, if the Authentication_System generates 
     * state parameter S1 at the start of the flow, then the OAuth callback SHALL 
     * only succeed if the returned state parameter equals S1, and any callback 
     * with mismatched state SHALL be rejected with security error.
     */
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - identical states always validate")
    void identicalStatesAlwaysValidate(
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String state) {
        
        OAuthService oauthService = new OAuthService();
        
        // When the same state is used for both received and expected
        boolean result = oauthService.validateState(state, state);
        
        // Then validation should always succeed
        assertTrue(result, 
            "Identical state parameters should always validate successfully");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - different states always fail")
    void differentStatesAlwaysFail(
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String state1,
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String state2) {
        
        Assume.that(!state1.equals(state2));
        
        OAuthService oauthService = new OAuthService();
        
        // When different states are compared
        boolean result = oauthService.validateState(state1, state2);
        
        // Then validation should always fail
        assertFalse(result, 
            "Different state parameters should always fail validation");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - null states always fail")
    void nullStatesAlwaysFail(
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String state) {
        
        OAuthService oauthService = new OAuthService();
        
        // When either state is null
        boolean result1 = oauthService.validateState(null, state);
        boolean result2 = oauthService.validateState(state, null);
        boolean result3 = oauthService.validateState(null, null);
        
        // Then validation should always fail
        assertFalse(result1, "Null received state should fail validation");
        assertFalse(result2, "Null expected state should fail validation");
        assertFalse(result3, "Both null states should fail validation");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - validation is symmetric")
    void validationIsSymmetric(
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String state1,
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String state2) {
        
        OAuthService oauthService = new OAuthService();
        
        // When comparing states in both directions
        boolean result1 = oauthService.validateState(state1, state2);
        boolean result2 = oauthService.validateState(state2, state1);
        
        // Then results should be symmetric
        assertEquals(result1, result2, 
            "State validation should be symmetric: validateState(A, B) == validateState(B, A)");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - case sensitive comparison")
    void validationIsCaseSensitive(
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String state) {
        
        OAuthService oauthService = new OAuthService();
        
        String lowercase = state.toLowerCase();
        String uppercase = state.toUpperCase();
        
        Assume.that(!lowercase.equals(uppercase));
        
        // When comparing states with different cases
        boolean result = oauthService.validateState(lowercase, uppercase);
        
        // Then validation should fail (case sensitive)
        assertFalse(result, 
            "State validation should be case sensitive");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - whitespace matters")
    void validationConsidersWhitespace(
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String state) {
        
        OAuthService oauthService = new OAuthService();
        
        String withSpace = state + " ";
        String withTab = state + "\t";
        
        // When comparing states with different whitespace
        boolean result1 = oauthService.validateState(state, withSpace);
        boolean result2 = oauthService.validateState(state, withTab);
        
        // Then validation should fail (whitespace matters)
        assertFalse(result1, "State validation should consider trailing spaces");
        assertFalse(result2, "State validation should consider trailing tabs");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - generated states are unique")
    void generatedStatesAreUnique() {
        OAuthService oauthService = new OAuthService();
        
        // Generate multiple states
        String state1 = oauthService.generateState();
        String state2 = oauthService.generateState();
        String state3 = oauthService.generateState();
        
        // Then all states should be unique
        assertNotEquals(state1, state2, "Generated states should be unique");
        assertNotEquals(state2, state3, "Generated states should be unique");
        assertNotEquals(state1, state3, "Generated states should be unique");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - generated states are non-empty")
    void generatedStatesAreNonEmpty() {
        OAuthService oauthService = new OAuthService();
        
        // Generate a state
        String state = oauthService.generateState();
        
        // Then state should be non-null and non-empty
        assertNotNull(state, "Generated state should not be null");
        assertFalse(state.isEmpty(), "Generated state should not be empty");
        assertTrue(state.length() > 10, "Generated state should have sufficient entropy");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - generated states validate with themselves")
    void generatedStatesValidateWithThemselves() {
        OAuthService oauthService = new OAuthService();
        
        // Generate a state
        String state = oauthService.generateState();
        
        // When validating the state with itself
        boolean result = oauthService.validateState(state, state);
        
        // Then validation should succeed
        assertTrue(result, "Generated state should validate with itself");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - substring attacks fail")
    void substringAttacksFail(
            @ForAll @StringLength(min = 10, max = 100) @AlphaChars String state) {
        
        OAuthService oauthService = new OAuthService();
        
        // Create substring attacks
        String prefix = state.substring(0, state.length() / 2);
        String suffix = state.substring(state.length() / 2);
        
        // When comparing state with its substrings
        boolean result1 = oauthService.validateState(state, prefix);
        boolean result2 = oauthService.validateState(state, suffix);
        
        // Then validation should fail
        assertFalse(result1, "State validation should reject prefix substring attacks");
        assertFalse(result2, "State validation should reject suffix substring attacks");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - concatenation attacks fail")
    void concatenationAttacksFail(
            @ForAll @StringLength(min = 1, max = 50) @AlphaChars String state) {
        
        OAuthService oauthService = new OAuthService();
        
        // Create concatenation attacks
        String doubled = state + state;
        String withPrefix = "x" + state;
        String withSuffix = state + "x";
        
        // When comparing state with concatenated versions
        boolean result1 = oauthService.validateState(state, doubled);
        boolean result2 = oauthService.validateState(state, withPrefix);
        boolean result3 = oauthService.validateState(state, withSuffix);
        
        // Then validation should fail
        assertFalse(result1, "State validation should reject doubled state attacks");
        assertFalse(result2, "State validation should reject prefix concatenation attacks");
        assertFalse(result3, "State validation should reject suffix concatenation attacks");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - empty string edge case")
    void emptyStringEdgeCase() {
        OAuthService oauthService = new OAuthService();
        
        // When comparing empty strings
        boolean result = oauthService.validateState("", "");
        
        // Then validation should succeed (both are equal)
        assertTrue(result, "Empty strings should validate as equal");
        
        // But empty vs non-empty should fail
        boolean result2 = oauthService.validateState("", "non-empty");
        assertFalse(result2, "Empty string should not validate with non-empty string");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - special characters are preserved")
    void specialCharactersArePreserved(
            @ForAll @StringLength(min = 1, max = 50) String state) {
        
        OAuthService oauthService = new OAuthService();
        
        // When validating states with special characters
        boolean result = oauthService.validateState(state, state);
        
        // Then validation should succeed
        assertTrue(result, 
            "State validation should preserve special characters: " + state);
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - validation is deterministic")
    void validationIsDeterministic(
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String state1,
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String state2) {
        
        OAuthService oauthService = new OAuthService();
        
        // When validating the same states multiple times
        boolean result1 = oauthService.validateState(state1, state2);
        boolean result2 = oauthService.validateState(state1, state2);
        boolean result3 = oauthService.validateState(state1, state2);
        
        // Then all results should be identical
        assertEquals(result1, result2, "State validation should be deterministic");
        assertEquals(result2, result3, "State validation should be deterministic");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - code verifiers are unique")
    void codeVerifiersAreUnique() {
        OAuthService oauthService = new OAuthService();
        
        // Generate multiple code verifiers
        String verifier1 = oauthService.generateCodeVerifier();
        String verifier2 = oauthService.generateCodeVerifier();
        String verifier3 = oauthService.generateCodeVerifier();
        
        // Then all verifiers should be unique
        assertNotEquals(verifier1, verifier2, "Generated code verifiers should be unique");
        assertNotEquals(verifier2, verifier3, "Generated code verifiers should be unique");
        assertNotEquals(verifier1, verifier3, "Generated code verifiers should be unique");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - code verifiers are non-empty")
    void codeVerifiersAreNonEmpty() {
        OAuthService oauthService = new OAuthService();
        
        // Generate a code verifier
        String verifier = oauthService.generateCodeVerifier();
        
        // Then verifier should be non-null and non-empty
        assertNotNull(verifier, "Generated code verifier should not be null");
        assertFalse(verifier.isEmpty(), "Generated code verifier should not be empty");
        assertTrue(verifier.length() > 10, "Generated code verifier should have sufficient entropy");
    }
    
    @Property(tries = 100)
    @Label("Property 11: OAuth state validation - authorization URL contains state")
    void authorizationUrlContainsState(
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String state,
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String codeVerifier) {
        
        OAuthService oauthService = new OAuthService();
        
        // When generating authorization URL
        String url = oauthService.generateAuthorizationUrl(state, codeVerifier);
        
        // Then URL should contain the state parameter
        assertTrue(url.contains("state="), "Authorization URL should contain state parameter");
        assertTrue(url.contains(state), "Authorization URL should contain the actual state value");
    }
}
