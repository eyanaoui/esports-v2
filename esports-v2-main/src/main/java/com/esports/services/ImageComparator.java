package com.esports.services;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Service for comparing signature images using histogram correlation.
 * 
 * This service implements a histogram-based image comparison algorithm to
 * calculate similarity scores between two signature images. The algorithm:
 * 1. Normalizes both images to 400x200 pixels
 * 2. Converts to grayscale
 * 3. Calculates pixel intensity histograms (256 bins)
 * 4. Computes correlation coefficient between histograms
 * 5. Returns similarity score (0-100)
 * 
 * A similarity score of 75 or higher indicates a match.
 * 
 * Requirements: 2.6, 2.7, 2.8
 */
public class ImageComparator {
    
    private static final int NORMALIZED_WIDTH = 400;
    private static final int NORMALIZED_HEIGHT = 200;
    private static final int HISTOGRAM_BINS = 256;
    
    /**
     * Calculate similarity between two signature images.
     * 
     * This is the main orchestration method that coordinates the entire
     * comparison process: normalization, grayscale conversion, histogram
     * calculation, and correlation computation.
     * 
     * @param img1 First signature image
     * @param img2 Second signature image
     * @return Similarity score from 0 to 100 (100 = identical)
     * @throws IllegalArgumentException if either image is null
     * 
     * Requirement 2.6: Calculate similarity score using image comparison
     * Requirement 2.7: Return score between 0-100
     */
    public double calculateSimilarity(BufferedImage img1, BufferedImage img2) {
        if (img1 == null || img2 == null) {
            throw new IllegalArgumentException("Images cannot be null");
        }
        
        // Step 1: Normalize both images to same dimensions
        BufferedImage normalized1 = normalizeImage(img1);
        BufferedImage normalized2 = normalizeImage(img2);
        
        // Step 2: Convert to grayscale
        BufferedImage gray1 = toGrayscale(normalized1);
        BufferedImage gray2 = toGrayscale(normalized2);
        
        // Step 3: Calculate histograms
        int[] histogram1 = calculateHistogram(gray1);
        int[] histogram2 = calculateHistogram(gray2);
        
        // Step 4: Compare histograms using correlation
        double correlation = compareHistograms(histogram1, histogram2);
        
        // Step 5: Convert correlation to similarity score (0-100)
        // Correlation ranges from -1 to 1, we map it to 0-100
        // Negative correlations are treated as 0 similarity
        double similarity = Math.max(0, correlation * 100);
        
        return similarity;
    }
    
    /**
     * Normalize image to standard dimensions (400x200 pixels).
     * 
     * Resizes the input image to a fixed size to ensure consistent
     * comparison regardless of original image dimensions.
     * 
     * @param image The image to normalize
     * @return Normalized image of size 400x200
     * 
     * Requirement 2.6: Normalize images to 400x200 pixels
     */
    BufferedImage normalizeImage(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }
        
        // Create new image with target dimensions
        BufferedImage normalized = new BufferedImage(
            NORMALIZED_WIDTH, 
            NORMALIZED_HEIGHT, 
            BufferedImage.TYPE_INT_ARGB
        );
        
        // Draw scaled image
        Graphics2D g2d = normalized.createGraphics();
        
        // Use high-quality rendering hints for better scaling
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
                            RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(image, 0, 0, NORMALIZED_WIDTH, NORMALIZED_HEIGHT, null);
        g2d.dispose();
        
        return normalized;
    }
    
    /**
     * Convert image to grayscale.
     * 
     * Converts a color image to grayscale by calculating the luminance
     * of each pixel using the standard formula:
     * Gray = 0.299*R + 0.587*G + 0.114*B
     * 
     * @param image The image to convert
     * @return Grayscale version of the image
     * 
     * Requirement 2.6: Convert to grayscale for comparison
     */
    BufferedImage toGrayscale(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        BufferedImage grayscale = new BufferedImage(
            width, 
            height, 
            BufferedImage.TYPE_BYTE_GRAY
        );
        
        // Convert each pixel to grayscale
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                
                // Extract RGB components
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // Calculate grayscale value using luminance formula
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                
                // Create grayscale RGB value
                int grayRgb = (gray << 16) | (gray << 8) | gray;
                
                grayscale.setRGB(x, y, grayRgb);
            }
        }
        
        return grayscale;
    }
    
    /**
     * Calculate pixel intensity histogram for a grayscale image.
     * 
     * Creates a histogram with 256 bins (one for each possible grayscale value)
     * counting the frequency of each intensity level in the image.
     * 
     * @param image The grayscale image
     * @return Array of 256 integers representing histogram bins
     * 
     * Requirement 2.6: Calculate pixel intensity histogram
     */
    int[] calculateHistogram(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }
        
        int width = image.getWidth();
        int height = image.getHeight();
        int[] histogram = new int[HISTOGRAM_BINS];
        
        // Count frequency of each intensity value
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                
                // Extract grayscale value (all RGB components are equal in grayscale)
                int intensity = rgb & 0xFF;
                
                histogram[intensity]++;
            }
        }
        
        return histogram;
    }
    
    /**
     * Compare two histograms using correlation coefficient.
     * 
     * Calculates the Pearson correlation coefficient between two histograms.
     * The correlation coefficient ranges from -1 (perfect negative correlation)
     * to +1 (perfect positive correlation), with 0 indicating no correlation.
     * 
     * Formula: correlation = covariance(H1, H2) / (stddev(H1) * stddev(H2))
     * 
     * @param hist1 First histogram
     * @param hist2 Second histogram
     * @return Correlation coefficient between -1 and 1
     * @throws IllegalArgumentException if histograms are null or different sizes
     * 
     * Requirement 2.6: Compare histograms using correlation coefficient
     * Requirement 2.8: Ensure deterministic and symmetric comparison
     */
    double compareHistograms(int[] hist1, int[] hist2) {
        if (hist1 == null || hist2 == null) {
            throw new IllegalArgumentException("Histograms cannot be null");
        }
        if (hist1.length != hist2.length) {
            throw new IllegalArgumentException("Histograms must have same length");
        }
        
        int n = hist1.length;
        
        // Calculate means
        double mean1 = 0;
        double mean2 = 0;
        for (int i = 0; i < n; i++) {
            mean1 += hist1[i];
            mean2 += hist2[i];
        }
        mean1 /= n;
        mean2 /= n;
        
        // Calculate covariance and standard deviations
        double covariance = 0;
        double variance1 = 0;
        double variance2 = 0;
        
        for (int i = 0; i < n; i++) {
            double diff1 = hist1[i] - mean1;
            double diff2 = hist2[i] - mean2;
            
            covariance += diff1 * diff2;
            variance1 += diff1 * diff1;
            variance2 += diff2 * diff2;
        }
        
        // Calculate standard deviations
        double stddev1 = Math.sqrt(variance1);
        double stddev2 = Math.sqrt(variance2);
        
        // Handle edge case: if either stddev is 0, images are uniform
        if (stddev1 == 0 || stddev2 == 0) {
            // If both are uniform with same mean, they're identical
            if (stddev1 == 0 && stddev2 == 0 && Math.abs(mean1 - mean2) < 0.001) {
                return 1.0;
            }
            // Otherwise, they're completely different
            return 0.0;
        }
        
        // Calculate correlation coefficient
        double correlation = covariance / (stddev1 * stddev2);
        
        // Clamp to [-1, 1] range to handle floating-point errors
        correlation = Math.max(-1.0, Math.min(1.0, correlation));
        
        return correlation;
    }
}
