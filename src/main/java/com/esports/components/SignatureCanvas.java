package com.esports.components;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.embed.swing.SwingFXUtils;

import java.awt.image.BufferedImage;

/**
 * Custom JavaFX Canvas component for signature capture.
 * 
 * Provides drawing functionality with mouse/touch input and
 * conversion to BufferedImage for authentication.
 * 
 * Requirements: 2.1, 2.2
 */
public class SignatureCanvas extends Canvas {
    
    private final GraphicsContext gc;
    private boolean isDrawing = false;
    private double lastX;
    private double lastY;
    
    /**
     * Create signature canvas with specified dimensions.
     * 
     * @param width Canvas width in pixels
     * @param height Canvas height in pixels
     * 
     * Requirement 2.1: Display canvas of at least 400x200 pixels
     */
    public SignatureCanvas(double width, double height) {
        super(width, height);
        this.gc = getGraphicsContext2D();
        
        // Initialize canvas with white background
        clear();
        
        // Configure drawing style
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2.0);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        
        // Set up mouse event handlers
        setupMouseHandlers();
    }
    
    /**
     * Set up mouse event handlers for drawing.
     * 
     * Requirement 2.2: Capture signature with mouse input
     */
    private void setupMouseHandlers() {
        // Mouse pressed - start drawing
        setOnMousePressed(this::handleMousePressed);
        
        // Mouse dragged - draw line
        setOnMouseDragged(this::handleMouseDragged);
        
        // Mouse released - stop drawing
        setOnMouseReleased(this::handleMouseReleased);
    }
    
    /**
     * Handle mouse pressed event - start drawing.
     */
    private void handleMousePressed(MouseEvent event) {
        isDrawing = true;
        lastX = event.getX();
        lastY = event.getY();
        
        // Draw a small dot at the starting point
        gc.fillOval(lastX - 1, lastY - 1, 2, 2);
    }
    
    /**
     * Handle mouse dragged event - draw line from last position.
     */
    private void handleMouseDragged(MouseEvent event) {
        if (isDrawing) {
            double currentX = event.getX();
            double currentY = event.getY();
            
            // Draw line from last position to current position
            gc.strokeLine(lastX, lastY, currentX, currentY);
            
            // Update last position
            lastX = currentX;
            lastY = currentY;
        }
    }
    
    /**
     * Handle mouse released event - stop drawing.
     */
    private void handleMouseReleased(MouseEvent event) {
        isDrawing = false;
    }
    
    /**
     * Clear the canvas (reset to white background).
     */
    public void clear() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, getWidth(), getHeight());
    }
    
    /**
     * Get the signature as a BufferedImage.
     * 
     * Converts the JavaFX canvas to a BufferedImage with transparent
     * background for signature authentication.
     * 
     * @return BufferedImage representation of the signature
     */
    public BufferedImage getSignatureImage() {
        // Create snapshot of canvas (must be on FX thread)
        WritableImage snapshot = new WritableImage((int) getWidth(), (int) getHeight());
        snapshot(null, snapshot);
        
        // Convert to BufferedImage
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);
        
        // Convert white background to transparent
        BufferedImage transparentImage = new BufferedImage(
            bufferedImage.getWidth(),
            bufferedImage.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );
        
        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                int rgb = bufferedImage.getRGB(x, y);
                
                // Check if pixel is white (background)
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                
                if (red > 250 && green > 250 && blue > 250) {
                    // Make white pixels transparent
                    transparentImage.setRGB(x, y, 0x00000000);
                } else {
                    // Keep non-white pixels
                    transparentImage.setRGB(x, y, rgb);
                }
            }
        }
        
        return transparentImage;
    }
    
    /**
     * Check if canvas has any drawn content.
     * 
     * Note: This method should be called from the JavaFX Application Thread.
     * 
     * @return true if canvas has been drawn on
     */
    public boolean hasContent() {
        // Get canvas pixels directly from GraphicsContext
        WritableImage snapshot = new WritableImage((int) getWidth(), (int) getHeight());
        snapshot(null, snapshot);
        
        // Check for non-white pixels
        for (int y = 0; y < (int) getHeight(); y++) {
            for (int x = 0; x < (int) getWidth(); x++) {
                int argb = snapshot.getPixelReader().getArgb(x, y);
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;
                
                // If pixel is not white, we have content
                if (red < 250 || green < 250 || blue < 250) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
