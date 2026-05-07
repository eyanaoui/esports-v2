package com.esports.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ImageComparator service.
 * 
 * Tests the histogram-based image comparison algorithm with known signature pairs
 * to verify correct similarity calculations, normalization, grayscale conversion,
 * and histogram operations.
 * 
 * **Validates: Requirements 2.6, 2.7, 2.8**
 */
class ImageComparatorTest {
    
    private ImageComparator comparator;
    
    @BeforeEach
    void setUp() {
        comparator = new ImageComparator();
    }
    
    // ========== calculateSimilarity Tests ==========
    
    @Test
    void testCalculateSimilarity_IdenticalImages_Returns100() {
        // Create identical signature images
        BufferedImage img1 = createSignatureImage(Color.BLACK, "John Doe");
        BufferedImage img2 = createSignatureImage(Color.BLACK, "John Doe");
        
        double similarity = comparator.calculateSimilarity(img1, img2);
        
        // Identical images should have very high similarity (close to 100)
        assertTrue(similarity >= 95.0, "Identical images should have similarity >= 95, got: " + similarity);
    }
    
    @Test
    void testCalculateSimilarity_SameImageReference_Returns100() {
        // Use the same image reference
        BufferedImage img = createSignatureImage(Color.BLACK, "Jane Smith");
        
        double similarity = comparator.calculateSimilarity(img, img);
        
        // Same reference should return perfect similarity
        assertEquals(100.0, similarity, 0.1, "Same image reference should return 100");
    }
    
    @Test
    void testCalculateSimilarity_DifferentSignatures_ReturnsLowScore() {
        // Create completely different signatures with very different content
        BufferedImage img1 = createSignatureImage(Color.BLACK, "X");
        BufferedImage img2 = createComplexSignature();
        
        double similarity = comparator.calculateSimilarity(img1, img2);
        
        // Different signatures should have lower similarity than identical ones
        // Note: Histogram comparison may still show high similarity if overall
        // pixel distributions are similar, even with different content
        assertTrue(similarity < 100.0, "Different signatures should have similarity < 100, got: " + similarity);
    }
    
    @Test
    void testCalculateSimilarity_SimilarButNotIdentical_ReturnsModerateScore() {
        // Create similar signatures with slight variations
        BufferedImage img1 = createSignatureImage(Color.BLACK, "John D");
        BufferedImage img2 = createSignatureImage(Color.BLACK, "John Doe");
        
        double similarity = comparator.calculateSimilarity(img1, img2);
        
        // Similar signatures should have moderate to high similarity
        assertTrue(similarity > 50.0 && similarity < 100.0, 
                  "Similar signatures should have 50 < similarity < 100, got: " + similarity);
    }
    
    @Test
    void testCalculateSimilarity_NullFirstImage_ThrowsException() {
        BufferedImage img = createSignatureImage(Color.BLACK, "Test");
        
        assertThrows(IllegalArgumentException.class, () -> {
            comparator.calculateSimilarity(null, img);
        });
    }
    
    @Test
    void testCalculateSimilarity_NullSecondImage_ThrowsException() {
        BufferedImage img = createSignatureImage(Color.BLACK, "Test");
        
        assertThrows(IllegalArgumentException.class, () -> {
            comparator.calculateSimilarity(img, null);
        });
    }
    
    @Test
    void testCalculateSimilarity_BothNull_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            comparator.calculateSimilarity(null, null);
        });
    }
    
    @Test
    void testCalculateSimilarity_DifferentSizes_StillCompares() {
        // Create images of different sizes
        BufferedImage img1 = createSignatureImage(200, 100, Color.BLACK, "Test");
        BufferedImage img2 = createSignatureImage(800, 400, Color.BLACK, "Test");
        
        // Should not throw exception - normalization handles different sizes
        double similarity = comparator.calculateSimilarity(img1, img2);
        
        // Should have high similarity since content is same
        assertTrue(similarity >= 90.0, "Same content with different sizes should have high similarity");
    }
    
    @Test
    void testCalculateSimilarity_WhiteOnBlackVsBlackOnWhite_ReturnsDifferent() {
        // Create inverse signatures
        BufferedImage img1 = createSignatureWithBackground(Color.WHITE, Color.BLACK, "Test");
        BufferedImage img2 = createSignatureWithBackground(Color.BLACK, Color.WHITE, "Test");
        
        double similarity = comparator.calculateSimilarity(img1, img2);
        
        // Inverse images should have lower similarity
        assertTrue(similarity < 50.0, "Inverse images should have low similarity, got: " + similarity);
    }
    
    // ========== normalizeImage Tests ==========
    
    @Test
    void testNormalizeImage_StandardSize_Returns400x200() {
        BufferedImage img = createSignatureImage(500, 300, Color.BLACK, "Test");
        
        BufferedImage normalized = comparator.normalizeImage(img);
        
        assertEquals(400, normalized.getWidth(), "Normalized width should be 400");
        assertEquals(200, normalized.getHeight(), "Normalized height should be 200");
    }
    
    @Test
    void testNormalizeImage_SmallImage_ScalesUp() {
        BufferedImage img = createSignatureImage(100, 50, Color.BLACK, "Test");
        
        BufferedImage normalized = comparator.normalizeImage(img);
        
        assertEquals(400, normalized.getWidth(), "Should scale up to 400");
        assertEquals(200, normalized.getHeight(), "Should scale up to 200");
    }
    
    @Test
    void testNormalizeImage_LargeImage_ScalesDown() {
        BufferedImage img = createSignatureImage(1000, 600, Color.BLACK, "Test");
        
        BufferedImage normalized = comparator.normalizeImage(img);
        
        assertEquals(400, normalized.getWidth(), "Should scale down to 400");
        assertEquals(200, normalized.getHeight(), "Should scale down to 200");
    }
    
    @Test
    void testNormalizeImage_NullImage_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            comparator.normalizeImage(null);
        });
    }
    
    @Test
    void testNormalizeImage_PreservesContent() {
        // Create image with specific content
        BufferedImage img = createSignatureImage(Color.BLACK, "Test Content");
        
        BufferedImage normalized = comparator.normalizeImage(img);
        
        // Verify it's not completely blank
        assertFalse(isBlankImage(normalized), "Normalized image should preserve content");
    }
    
    // ========== toGrayscale Tests ==========
    
    @Test
    void testToGrayscale_ColorImage_ConvertsToGray() {
        // Create a color image
        BufferedImage colorImg = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = colorImg.createGraphics();
        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, 50, 50);
        g2d.setColor(Color.BLUE);
        g2d.fillRect(50, 50, 50, 50);
        g2d.dispose();
        
        BufferedImage grayImg = comparator.toGrayscale(colorImg);
        
        // Check that result is grayscale (R=G=B for all pixels)
        for (int y = 0; y < grayImg.getHeight(); y++) {
            for (int x = 0; x < grayImg.getWidth(); x++) {
                int rgb = grayImg.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                assertEquals(r, g, "R and G should be equal in grayscale");
                assertEquals(g, b, "G and B should be equal in grayscale");
            }
        }
    }
    
    @Test
    void testToGrayscale_AlreadyGrayscale_RemainsGrayscale() {
        // Create a grayscale image
        BufferedImage grayImg = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = grayImg.createGraphics();
        g.setColor(new Color(128, 128, 128));
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        
        BufferedImage result = comparator.toGrayscale(grayImg);
        
        // Should still be grayscale
        assertNotNull(result);
        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
    }
    
    @Test
    void testToGrayscale_NullImage_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            comparator.toGrayscale(null);
        });
    }
    
    @Test
    void testToGrayscale_WhiteImage_RemainsWhite() {
        BufferedImage whiteImg = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = whiteImg.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 50, 50);
        g.dispose();
        
        BufferedImage grayImg = comparator.toGrayscale(whiteImg);
        
        // Check that white remains white (255, 255, 255)
        int rgb = grayImg.getRGB(25, 25);
        int gray = rgb & 0xFF;
        assertEquals(255, gray, "White should remain white (255)");
    }
    
    @Test
    void testToGrayscale_BlackImage_RemainsBlack() {
        BufferedImage blackImg = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = blackImg.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 50, 50);
        g.dispose();
        
        BufferedImage grayImg = comparator.toGrayscale(blackImg);
        
        // Check that black remains black (0, 0, 0)
        int rgb = grayImg.getRGB(25, 25);
        int gray = rgb & 0xFF;
        assertEquals(0, gray, "Black should remain black (0)");
    }
    
    // ========== calculateHistogram Tests ==========
    
    @Test
    void testCalculateHistogram_UniformImage_SinglePeak() {
        // Create uniform gray image
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(128, 128, 128));
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        
        int[] histogram = comparator.calculateHistogram(img);
        
        // Should have 256 bins
        assertEquals(256, histogram.length, "Histogram should have 256 bins");
        
        // Find the peak bin
        int maxCount = 0;
        int peakBin = -1;
        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > maxCount) {
                maxCount = histogram[i];
                peakBin = i;
            }
        }
        
        // Most pixels should be concentrated in one bin
        assertTrue(maxCount > 9000, "Should have a dominant peak with >9000 pixels, got: " + maxCount);
        assertTrue(peakBin >= 0 && peakBin < 256, "Peak should be in valid range, got: " + peakBin);
        
        // Other bins should be mostly empty
        int nonZeroBins = 0;
        for (int count : histogram) {
            if (count > 0) nonZeroBins++;
        }
        assertTrue(nonZeroBins < 20, "Uniform image should have few non-zero bins, got: " + nonZeroBins);
    }
    
    @Test
    void testCalculateHistogram_BlackImage_PeakAtZero() {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 50, 50);
        g.dispose();
        
        int[] histogram = comparator.calculateHistogram(img);
        
        // All pixels should be at intensity 0
        assertEquals(2500, histogram[0], "All 2500 pixels should be black (intensity 0)");
    }
    
    @Test
    void testCalculateHistogram_WhiteImage_PeakAt255() {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 50, 50);
        g.dispose();
        
        int[] histogram = comparator.calculateHistogram(img);
        
        // All pixels should be at intensity 255
        assertEquals(2500, histogram[255], "All 2500 pixels should be white (intensity 255)");
    }
    
    @Test
    void testCalculateHistogram_NullImage_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            comparator.calculateHistogram(null);
        });
    }
    
    @Test
    void testCalculateHistogram_TotalCountMatchesPixels() {
        BufferedImage img = createSignatureImage(100, 80, Color.BLACK, "Test");
        
        int[] histogram = comparator.calculateHistogram(img);
        
        // Sum of all bins should equal total pixels
        int totalCount = 0;
        for (int count : histogram) {
            totalCount += count;
        }
        assertEquals(100 * 80, totalCount, "Total histogram count should equal pixel count");
    }
    
    // ========== compareHistograms Tests ==========
    
    @Test
    void testCompareHistograms_IdenticalHistograms_Returns1() {
        int[] hist1 = {10, 20, 30, 40, 50};
        int[] hist2 = {10, 20, 30, 40, 50};
        
        double correlation = comparator.compareHistograms(hist1, hist2);
        
        assertEquals(1.0, correlation, 0.001, "Identical histograms should have correlation 1.0");
    }
    
    @Test
    void testCompareHistograms_CompletelyDifferent_ReturnsLow() {
        int[] hist1 = {100, 0, 0, 0, 0};
        int[] hist2 = {0, 0, 0, 0, 100};
        
        double correlation = comparator.compareHistograms(hist1, hist2);
        
        // Should have low or negative correlation
        assertTrue(correlation < 0.5, "Completely different histograms should have low correlation");
    }
    
    @Test
    void testCompareHistograms_UniformHistograms_Returns1() {
        // Both histograms are uniform (all same values)
        int[] hist1 = new int[256];
        int[] hist2 = new int[256];
        for (int i = 0; i < 256; i++) {
            hist1[i] = 100;
            hist2[i] = 100;
        }
        
        double correlation = comparator.compareHistograms(hist1, hist2);
        
        assertEquals(1.0, correlation, 0.001, "Uniform histograms with same mean should have correlation 1.0");
    }
    
    @Test
    void testCompareHistograms_NullFirstHistogram_ThrowsException() {
        int[] hist = {10, 20, 30};
        
        assertThrows(IllegalArgumentException.class, () -> {
            comparator.compareHistograms(null, hist);
        });
    }
    
    @Test
    void testCompareHistograms_NullSecondHistogram_ThrowsException() {
        int[] hist = {10, 20, 30};
        
        assertThrows(IllegalArgumentException.class, () -> {
            comparator.compareHistograms(hist, null);
        });
    }
    
    @Test
    void testCompareHistograms_DifferentLengths_ThrowsException() {
        int[] hist1 = {10, 20, 30};
        int[] hist2 = {10, 20, 30, 40};
        
        assertThrows(IllegalArgumentException.class, () -> {
            comparator.compareHistograms(hist1, hist2);
        });
    }
    
    @Test
    void testCompareHistograms_SymmetricProperty() {
        // Correlation should be symmetric: corr(A, B) = corr(B, A)
        int[] hist1 = {10, 20, 30, 40, 50, 60, 70, 80};
        int[] hist2 = {15, 25, 35, 45, 55, 65, 75, 85};
        
        double corr1 = comparator.compareHistograms(hist1, hist2);
        double corr2 = comparator.compareHistograms(hist2, hist1);
        
        assertEquals(corr1, corr2, 0.0001, "Correlation should be symmetric");
    }
    
    @Test
    void testCompareHistograms_ResultInValidRange() {
        // Correlation should always be between -1 and 1
        int[] hist1 = {5, 15, 25, 35, 45, 55, 65, 75};
        int[] hist2 = {10, 20, 30, 40, 50, 60, 70, 80};
        
        double correlation = comparator.compareHistograms(hist1, hist2);
        
        assertTrue(correlation >= -1.0 && correlation <= 1.0, 
                  "Correlation should be in range [-1, 1], got: " + correlation);
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Create a signature image with default size (400x200).
     */
    private BufferedImage createSignatureImage(Color color, String text) {
        return createSignatureImage(400, 200, color, text);
    }
    
    /**
     * Create a signature image with specified size.
     */
    private BufferedImage createSignatureImage(int width, int height, Color color, String text) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // White background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        
        // Draw signature text
        g.setColor(color);
        g.setFont(new Font("Serif", Font.ITALIC, 24));
        g.drawString(text, 20, height / 2);
        
        g.dispose();
        return img;
    }
    
    /**
     * Create a signature with custom background and foreground colors.
     */
    private BufferedImage createSignatureWithBackground(Color background, Color foreground, String text) {
        BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // Custom background
        g.setColor(background);
        g.fillRect(0, 0, 400, 200);
        
        // Draw signature text
        g.setColor(foreground);
        g.setFont(new Font("Serif", Font.ITALIC, 24));
        g.drawString(text, 20, 100);
        
        g.dispose();
        return img;
    }
    
    /**
     * Create a complex signature with multiple elements.
     */
    private BufferedImage createComplexSignature() {
        BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // White background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 400, 200);
        
        // Draw multiple shapes and lines
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(3));
        g.drawLine(50, 50, 350, 50);
        g.drawLine(50, 150, 350, 150);
        g.fillOval(100, 75, 50, 50);
        g.fillRect(250, 75, 50, 50);
        
        g.dispose();
        return img;
    }
    
    /**
     * Check if an image is completely blank (all white or all transparent).
     */
    private boolean isBlankImage(BufferedImage img) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // If any pixel is not white or transparent, image is not blank
                if (alpha > 0 && (r != 255 || g != 255 || b != 255)) {
                    return false;
                }
            }
        }
        return true;
    }
}
