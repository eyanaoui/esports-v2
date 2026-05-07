package com.esports.services;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for Signature Authentication Consistency.
 * 
 * **Validates: Requirements 2.6, 2.7, 2.8**
 * 
 * Property 2: Signature Authentication Consistency
 * 
 * For all signature authentication attempts, if a user draws a signature S1 and it is stored,
 * then any subsequent signature S2 drawn by the same user SHALL produce a Similarity_Score
 * that is deterministic (same S2 always produces same score) and symmetric (comparing S1 to S2
 * equals comparing S2 to S1).
 * 
 * This property test verifies:
 * 1. Determinism: calculateSimilarity(S1, S2) always returns the same score
 * 2. Symmetry: calculateSimilarity(S1, S2) == calculateSimilarity(S2, S1)
 * 3. Score range: Similarity score is always between 0 and 100
 * 4. Identity: calculateSimilarity(S, S) == 100 (a signature compared to itself is identical)
 */
@PropertyDefaults(tries = 100)
class SignatureAuthenticationConsistencyPropertyTest {
    
    private final ImageComparator comparator = new ImageComparator();
    
    /**
     * Property: Determinism
     * 
     * For any two signatures S1 and S2, calling calculateSimilarity(S1, S2) multiple times
     * should always return the same score.
     */
    @Property
    void signatureComparisonIsDeterministic(
            @ForAll("signatures") BufferedImage sig1,
            @ForAll("signatures") BufferedImage sig2) {
        
        // Calculate similarity multiple times
        double score1 = comparator.calculateSimilarity(sig1, sig2);
        double score2 = comparator.calculateSimilarity(sig1, sig2);
        double score3 = comparator.calculateSimilarity(sig1, sig2);
        
        // All scores should be identical
        assertEquals(score1, score2, 0.0001, "Similarity calculation should be deterministic");
        assertEquals(score2, score3, 0.0001, "Similarity calculation should be deterministic");
    }
    
    /**
     * Property: Symmetry
     * 
     * For any two signatures S1 and S2, calculateSimilarity(S1, S2) should equal
     * calculateSimilarity(S2, S1).
     */
    @Property
    void signatureComparisonIsSymmetric(
            @ForAll("signatures") BufferedImage sig1,
            @ForAll("signatures") BufferedImage sig2) {
        
        double score12 = comparator.calculateSimilarity(sig1, sig2);
        double score21 = comparator.calculateSimilarity(sig2, sig1);
        
        // Scores should be equal (within floating-point tolerance)
        assertEquals(score12, score21, 0.0001, 
                    "Similarity should be symmetric: sim(A,B) == sim(B,A)");
    }
    
    /**
     * Property: Score Range
     * 
     * For any two signatures, the similarity score should always be between 0 and 100.
     */
    @Property
    void similarityScoreIsInValidRange(
            @ForAll("signatures") BufferedImage sig1,
            @ForAll("signatures") BufferedImage sig2) {
        
        double score = comparator.calculateSimilarity(sig1, sig2);
        
        assertTrue(score >= 0.0, "Similarity score should be >= 0, got: " + score);
        assertTrue(score <= 100.0, "Similarity score should be <= 100, got: " + score);
    }
    
    /**
     * Property: Identity
     * 
     * A signature compared to itself should always have a similarity score of 100.
     */
    @Property
    void signatureComparedToItselfIsIdentical(
            @ForAll("signatures") BufferedImage signature) {
        
        double score = comparator.calculateSimilarity(signature, signature);
        
        assertEquals(100.0, score, 0.1, 
                    "A signature compared to itself should have similarity 100");
    }
    
    /**
     * Property: Identical Content Has High Similarity
     * 
     * Two signatures with identical content (even if different object instances)
     * should have very high similarity (>= 95).
     */
    @Property
    void identicalSignaturesHaveHighSimilarity(
            @ForAll @IntRange(min = 0, max = 255) int red,
            @ForAll @IntRange(min = 0, max = 255) int green,
            @ForAll @IntRange(min = 0, max = 255) int blue,
            @ForAll("signatureTexts") String text) {
        
        Color color = new Color(red, green, blue);
        
        // Create two signatures with identical content
        BufferedImage sig1 = createSignature(color, text);
        BufferedImage sig2 = createSignature(color, text);
        
        double score = comparator.calculateSimilarity(sig1, sig2);
        
        assertTrue(score >= 95.0, 
                  "Identical signatures should have similarity >= 95, got: " + score);
    }
    
    /**
     * Property: Normalization Consistency
     * 
     * Signatures of different sizes but same content should have high similarity
     * after normalization.
     */
    @Property
    void normalizationMaintainsContentSimilarity(
            @ForAll @IntRange(min = 100, max = 1000) int width1,
            @ForAll @IntRange(min = 50, max = 500) int height1,
            @ForAll @IntRange(min = 100, max = 1000) int width2,
            @ForAll @IntRange(min = 50, max = 500) int height2,
            @ForAll("signatureTexts") String text) {
        
        // Create signatures with same content but different sizes
        BufferedImage sig1 = createSignature(width1, height1, Color.BLACK, text);
        BufferedImage sig2 = createSignature(width2, height2, Color.BLACK, text);
        
        double score = comparator.calculateSimilarity(sig1, sig2);
        
        // Should have high similarity despite different sizes
        assertTrue(score >= 90.0, 
                  "Same content with different sizes should have high similarity, got: " + score);
    }
    
    /**
     * Property: Transitivity of High Similarity
     * 
     * If S1 is very similar to S2 (score >= 95) and S2 is very similar to S3 (score >= 95),
     * then S1 should be reasonably similar to S3 (score >= 85).
     * 
     * Note: This is a weaker form of transitivity appropriate for similarity measures.
     */
    @Property
    void highSimilarityIsTransitive(
            @ForAll("signatureTexts") String text) {
        
        // Create three very similar signatures (same text, slightly different rendering)
        BufferedImage sig1 = createSignature(Color.BLACK, text);
        BufferedImage sig2 = createSignature(Color.BLACK, text);
        BufferedImage sig3 = createSignature(Color.BLACK, text);
        
        double score12 = comparator.calculateSimilarity(sig1, sig2);
        double score23 = comparator.calculateSimilarity(sig2, sig3);
        double score13 = comparator.calculateSimilarity(sig1, sig3);
        
        // If both pairs are very similar, the third pair should also be similar
        if (score12 >= 95.0 && score23 >= 95.0) {
            assertTrue(score13 >= 85.0, 
                      "Transitivity: if sim(A,B)>=95 and sim(B,C)>=95, then sim(A,C)>=85, got: " + score13);
        }
    }
    
    /**
     * Property: Grayscale Conversion Preserves Similarity
     * 
     * Converting signatures to grayscale before comparison should not drastically
     * change the similarity score compared to comparing the original images.
     */
    @Property
    void grayscaleConversionPreservesSimilarity(
            @ForAll("signatures") BufferedImage sig1,
            @ForAll("signatures") BufferedImage sig2) {
        
        // Compare original signatures
        double originalScore = comparator.calculateSimilarity(sig1, sig2);
        
        // Convert to grayscale manually and compare
        BufferedImage gray1 = comparator.toGrayscale(sig1);
        BufferedImage gray2 = comparator.toGrayscale(sig2);
        double grayScore = comparator.calculateSimilarity(gray1, gray2);
        
        // Scores should be very close (within 5%)
        double difference = Math.abs(originalScore - grayScore);
        assertTrue(difference <= 5.0, 
                  "Grayscale conversion should not drastically change similarity, difference: " + difference);
    }
    
    // ========== Arbitraries (Generators) ==========
    
    /**
     * Generate random signature images for property testing.
     */
    @Provide
    Arbitrary<BufferedImage> signatures() {
        return Combinators.combine(
            Arbitraries.integers().between(0, 255),  // red
            Arbitraries.integers().between(0, 255),  // green
            Arbitraries.integers().between(0, 255),  // blue
            signatureTexts()
        ).as((r, g, b, text) -> {
            Color color = new Color(r, g, b);
            return createSignature(color, text);
        });
    }
    
    /**
     * Generate random signature text strings.
     */
    @Provide
    Arbitrary<String> signatureTexts() {
        return Arbitraries.oneOf(
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
            Arbitraries.of("John Doe", "Jane Smith", "Alice Johnson", "Bob Williams", 
                          "Charlie Brown", "Diana Prince", "Eve Anderson", "Frank Miller")
        );
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Create a signature image with default size (400x200).
     */
    private BufferedImage createSignature(Color color, String text) {
        return createSignature(400, 200, color, text);
    }
    
    /**
     * Create a signature image with specified size.
     */
    private BufferedImage createSignature(int width, int height, Color color, String text) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // Enable anti-aliasing for better rendering
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // White background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        
        // Draw signature text
        g.setColor(color);
        int fontSize = Math.max(12, Math.min(width / 15, height / 5));
        g.setFont(new Font("Serif", Font.ITALIC, fontSize));
        
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = Math.max(10, (width - textWidth) / 2);
        int y = height / 2 + fm.getAscent() / 2;
        
        g.drawString(text, x, y);
        
        g.dispose();
        return img;
    }
}
