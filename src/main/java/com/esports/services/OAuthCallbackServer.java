package com.esports.services;

import com.esports.controllers.SignatureOAuthController;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple HTTP server to handle OAuth 2.0 callbacks.
 * 
 * This server listens on localhost:8080 for OAuth callbacks from Google.
 * When a callback is received, it extracts the authorization code and state,
 * then delegates to the SignatureOAuthController for processing.
 * 
 * Requirements: 1.1, 1.2, 7.7
 */
public class OAuthCallbackServer {
    
    private static final int PORT = 8080;
    private static final String CALLBACK_PATH = "/oauth/callback";
    
    private HttpServer server;
    private SignatureOAuthController oauthController;
    
    /**
     * Initialize the callback server.
     * 
     * @param oauthController Controller to handle OAuth callbacks
     */
    public OAuthCallbackServer(SignatureOAuthController oauthController) {
        this.oauthController = oauthController;
    }
    
    /**
     * Start the HTTP server to listen for OAuth callbacks.
     * 
     * @throws IOException if server cannot be started
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext(CALLBACK_PATH, new OAuthCallbackHandler());
        server.setExecutor(null); // Use default executor
        server.start();
        System.out.println("OAuth callback server started on port " + PORT);
    }
    
    /**
     * Stop the HTTP server.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("OAuth callback server stopped");
        }
    }
    
    /**
     * HTTP handler for OAuth callbacks.
     */
    private class OAuthCallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Parse query parameters
                URI requestURI = exchange.getRequestURI();
                Map<String, String> params = parseQueryParams(requestURI.getQuery());
                
                String code = params.get("code");
                String state = params.get("state");
                String error = params.get("error");
                
                // Check for errors
                if (error != null) {
                    sendErrorResponse(exchange, "Authentication failed: " + error);
                    return;
                }
                
                // Validate required parameters
                if (code == null || state == null) {
                    sendErrorResponse(exchange, "Missing required parameters");
                    return;
                }
                
                // Send success response to browser
                sendSuccessResponse(exchange);
                
                // Process OAuth callback in background
                new Thread(() -> {
                    try {
                        oauthController.handleOAuthCallback(code, state);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                
            } catch (Exception e) {
                e.printStackTrace();
                sendErrorResponse(exchange, "Internal server error");
            }
        }
        
        /**
         * Parse query parameters from URL.
         */
        private Map<String, String> parseQueryParams(String query) {
            Map<String, String> params = new HashMap<>();
            if (query != null && !query.isEmpty()) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=", 2);
                    if (keyValue.length == 2) {
                        params.put(keyValue[0], keyValue[1]);
                    }
                }
            }
            return params;
        }
        
        /**
         * Send success response to browser.
         */
        private void sendSuccessResponse(HttpExchange exchange) throws IOException {
            String response = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "<title>Authentication Successful</title>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }" +
                    ".container { background: white; padding: 40px; border-radius: 20px; max-width: 500px; margin: 0 auto; box-shadow: 0 10px 30px rgba(0,0,0,0.3); }" +
                    "h1 { color: #667eea; margin-bottom: 20px; }" +
                    "p { color: #666; font-size: 16px; }" +
                    ".icon { font-size: 64px; margin-bottom: 20px; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<div class='icon'>✓</div>" +
                    "<h1>Authentication Successful!</h1>" +
                    "<p>You have successfully signed in with Google.</p>" +
                    "<p>You can close this window and return to the application.</p>" +
                    "</div>" +
                    "</body>" +
                    "</html>";
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
        
        /**
         * Send error response to browser.
         */
        private void sendErrorResponse(HttpExchange exchange, String errorMessage) throws IOException {
            String response = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "<title>Authentication Failed</title>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }" +
                    ".container { background: white; padding: 40px; border-radius: 20px; max-width: 500px; margin: 0 auto; box-shadow: 0 10px 30px rgba(0,0,0,0.3); }" +
                    "h1 { color: #e74c3c; margin-bottom: 20px; }" +
                    "p { color: #666; font-size: 16px; }" +
                    ".icon { font-size: 64px; margin-bottom: 20px; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<div class='icon'>✗</div>" +
                    "<h1>Authentication Failed</h1>" +
                    "<p>" + errorMessage + "</p>" +
                    "<p>Please close this window and try again.</p>" +
                    "</div>" +
                    "</body>" +
                    "</html>";
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(400, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
