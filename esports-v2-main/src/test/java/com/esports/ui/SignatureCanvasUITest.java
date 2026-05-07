package com.esports.ui;

import com.esports.components.SignatureCanvas;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI tests for SignatureCanvas component.
 * 
 * Tests signature drawing functionality, canvas clearing,
 * and image capture capabilities.
 * 
 * Sub-task 11.1: Write UI tests for signature canvas
 */
@ExtendWith(ApplicationExtension.class)
public class SignatureCanvasUITest {
    
    private SignatureCanvas signatureCanvas;
    
    @Start
    public void start(Stage stage) {
        signatureCanvas = new SignatureCanvas(400, 200);
        StackPane root = new StackPane(signatureCanvas);
        Scene scene = new Scene(root, 500, 300);
        stage.setScene(scene);
        stage.show();
    }
    
    /**
     * Test that canvas initializes with correct dimensions.
     * 
     * Requirement 2.1: Canvas should be 400x200 pixels
     */
    @Test
    public void testCanvasInitialization() {
        assertEquals(400, signatureCanvas.getWidth(), "Canvas width should be 400 pixels");
        assertEquals(200, signatureCanvas.getHeight(), "Canvas height should be 200 pixels");
    }
    
    /**
     * Test that canvas starts with no content.
     */
    @Test
    public void testCanvasStartsEmpty() {
        WaitForAsyncUtils.waitForFxEvents();
        
        AtomicBoolean hasContent = new AtomicBoolean(true);
        Platform.runLater(() -> hasContent.set(signatureCanvas.hasContent()));
        WaitForAsyncUtils.waitForFxEvents();
        
        assertFalse(hasContent.get(), "Canvas should start with no content");
    }
    
    /**
     * Test that drawing on canvas creates content.
     * 
     * Requirement 2.2: Capture signature with mouse input
     */
    @Test
    public void testDrawingCreatesContent(FxRobot robot) {
        // Draw a line on the canvas
        robot.clickOn(signatureCanvas)
             .drag(100, 100)
             .dropTo(300, 150);
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify canvas has content
        AtomicBoolean hasContent = new AtomicBoolean(false);
        Platform.runLater(() -> hasContent.set(signatureCanvas.hasContent()));
        WaitForAsyncUtils.waitForFxEvents();
        
        assertTrue(hasContent.get(), "Canvas should have content after drawing");
    }
    
    /**
     * Test that clearing canvas removes all content.
     * 
     * Requirement 2.2: Provide clear functionality
     */
    @Test
    public void testClearingCanvas(FxRobot robot) {
        // Draw on canvas
        robot.clickOn(signatureCanvas)
             .drag(100, 100)
             .dropTo(300, 150);
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify content exists
        AtomicBoolean hasContentBefore = new AtomicBoolean(false);
        Platform.runLater(() -> hasContentBefore.set(signatureCanvas.hasContent()));
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(hasContentBefore.get(), "Canvas should have content after drawing");
        
        // Clear canvas
        Platform.runLater(() -> signatureCanvas.clear());
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify content is removed
        AtomicBoolean hasContentAfter = new AtomicBoolean(true);
        Platform.runLater(() -> hasContentAfter.set(signatureCanvas.hasContent()));
        WaitForAsyncUtils.waitForFxEvents();
        assertFalse(hasContentAfter.get(), "Canvas should be empty after clearing");
    }
    
    /**
     * Test that signature image can be captured.
     */
    @Test
    public void testGetSignatureImage(FxRobot robot) {
        // Draw on canvas
        robot.clickOn(signatureCanvas)
             .drag(100, 100)
             .dropTo(300, 150);
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Get signature image
        AtomicReference<BufferedImage> imageRef = new AtomicReference<>();
        Platform.runLater(() -> imageRef.set(signatureCanvas.getSignatureImage()));
        WaitForAsyncUtils.waitForFxEvents();
        
        BufferedImage image = imageRef.get();
        
        // Verify image properties
        assertNotNull(image, "Signature image should not be null");
        assertEquals(400, image.getWidth(), "Image width should match canvas width");
        assertEquals(200, image.getHeight(), "Image height should match canvas height");
        assertEquals(BufferedImage.TYPE_INT_ARGB, image.getType(), "Image should have ARGB type");
    }
    
    /**
     * Test that signature image has transparent background.
     */
    @Test
    public void testSignatureImageTransparentBackground(FxRobot robot) {
        // Draw a small signature
        robot.clickOn(signatureCanvas)
             .drag(200, 100)
             .dropTo(250, 120);
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Get signature image
        AtomicReference<BufferedImage> imageRef = new AtomicReference<>();
        Platform.runLater(() -> imageRef.set(signatureCanvas.getSignatureImage()));
        WaitForAsyncUtils.waitForFxEvents();
        
        BufferedImage image = imageRef.get();
        
        // Check corners are transparent (should be background)
        int topLeft = image.getRGB(0, 0);
        int topRight = image.getRGB(399, 0);
        int bottomLeft = image.getRGB(0, 199);
        int bottomRight = image.getRGB(399, 199);
        
        // Extract alpha channel
        int alphaTopLeft = (topLeft >> 24) & 0xFF;
        int alphaTopRight = (topRight >> 24) & 0xFF;
        int alphaBottomLeft = (bottomLeft >> 24) & 0xFF;
        int alphaBottomRight = (bottomRight >> 24) & 0xFF;
        
        // Verify corners are transparent
        assertEquals(0, alphaTopLeft, "Top-left corner should be transparent");
        assertEquals(0, alphaTopRight, "Top-right corner should be transparent");
        assertEquals(0, alphaBottomLeft, "Bottom-left corner should be transparent");
        assertEquals(0, alphaBottomRight, "Bottom-right corner should be transparent");
    }
    
    /**
     * Test that multiple drawing strokes are captured.
     */
    @Test
    public void testMultipleDrawingStrokes(FxRobot robot) {
        // Draw first stroke
        robot.clickOn(signatureCanvas)
             .drag(50, 50)
             .dropTo(150, 50);
        
        // Draw second stroke
        robot.clickOn(signatureCanvas)
             .drag(50, 100)
             .dropTo(150, 100);
        
        // Draw third stroke
        robot.clickOn(signatureCanvas)
             .drag(50, 150)
             .dropTo(150, 150);
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify canvas has content
        AtomicBoolean hasContent = new AtomicBoolean(false);
        Platform.runLater(() -> hasContent.set(signatureCanvas.hasContent()));
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(hasContent.get(), "Canvas should have content from multiple strokes");
        
        // Get signature image
        AtomicReference<BufferedImage> imageRef = new AtomicReference<>();
        Platform.runLater(() -> imageRef.set(signatureCanvas.getSignatureImage()));
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(imageRef.get(), "Signature image should capture all strokes");
    }
    
    /**
     * Test that canvas can be reused after clearing.
     */
    @Test
    public void testCanvasReuseAfterClear(FxRobot robot) {
        // Draw, clear, draw again
        robot.clickOn(signatureCanvas)
             .drag(100, 100)
             .dropTo(200, 100);
        
        WaitForAsyncUtils.waitForFxEvents();
        
        Platform.runLater(() -> signatureCanvas.clear());
        WaitForAsyncUtils.waitForFxEvents();
        
        robot.clickOn(signatureCanvas)
             .drag(150, 150)
             .dropTo(250, 150);
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify canvas has content from second drawing
        AtomicBoolean hasContent = new AtomicBoolean(false);
        Platform.runLater(() -> hasContent.set(signatureCanvas.hasContent()));
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(hasContent.get(), "Canvas should have content after redrawing");
    }
    
    /**
     * Test that very small drawings are detected.
     */
    @Test
    public void testSmallDrawingDetection(FxRobot robot) {
        // Draw a very small signature (just a few pixels)
        robot.clickOn(signatureCanvas)
             .drag(200, 100)
             .dropTo(205, 105);
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Should still have content
        AtomicBoolean hasContent = new AtomicBoolean(false);
        Platform.runLater(() -> hasContent.set(signatureCanvas.hasContent()));
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(hasContent.get(), "Canvas should detect even small drawings");
    }
    
    /**
     * Test that canvas handles edge drawing (near borders).
     */
    @Test
    public void testEdgeDrawing(FxRobot robot) {
        // Draw near top edge
        robot.clickOn(signatureCanvas)
             .drag(10, 5)
             .dropTo(100, 5);
        
        // Draw near bottom edge
        robot.clickOn(signatureCanvas)
             .drag(10, 195)
             .dropTo(100, 195);
        
        // Draw near left edge
        robot.clickOn(signatureCanvas)
             .drag(5, 50)
             .dropTo(5, 150);
        
        // Draw near right edge
        robot.clickOn(signatureCanvas)
             .drag(395, 50)
             .dropTo(395, 150);
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify all edge drawings are captured
        AtomicBoolean hasContent = new AtomicBoolean(false);
        Platform.runLater(() -> hasContent.set(signatureCanvas.hasContent()));
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(hasContent.get(), "Canvas should handle edge drawings");
    }
}
