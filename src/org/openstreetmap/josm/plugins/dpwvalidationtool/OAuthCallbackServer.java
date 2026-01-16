package org.openstreetmap.josm.plugins.dpwvalidationtool;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.openstreetmap.josm.tools.Logging;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Local HTTP server to handle OAuth 2.0 callbacks.
 * Listens on localhost for the redirect from the OAuth provider.
 * 
 * @version 3.4.0
 * @since 3.4.0
 */
public class OAuthCallbackServer {
    
    private static final int DEFAULT_PORT = 8111; // Same as JOSM remote control
    private static final int ALTERNATE_PORT = 8112;
    private static final String CALLBACK_PATH = "/oauth/callback";
    
    private HttpServer server;
    private int port;
    private CompletableFuture<OAuthCallback> callbackFuture;
    
    /**
     * OAuth callback result
     */
    public static class OAuthCallback {
        public final String code;
        public final String state;
        public final String error;
        public final String errorDescription;
        
        public OAuthCallback(String code, String state, String error, String errorDescription) {
            this.code = code;
            this.state = state;
            this.error = error;
            this.errorDescription = errorDescription;
        }
        
        public boolean isSuccess() {
            return error == null && code != null && !code.isEmpty();
        }
        
        public boolean isError() {
            return error != null && !error.isEmpty();
        }
    }
    
    /**
     * Start the callback server and wait for OAuth redirect
     * 
     * @param expectedState The state parameter sent in the authorization request
     * @return CompletableFuture that completes when callback is received
     */
    public CompletableFuture<OAuthCallback> startAndWaitForCallback(String expectedState) throws IOException {
        callbackFuture = new CompletableFuture<>();
        
        // Try default port first, then alternate
        try {
            start(DEFAULT_PORT, expectedState);
        } catch (IOException e) {
            Logging.info("DPW OAuth: Port " + DEFAULT_PORT + " in use, trying " + ALTERNATE_PORT);
            start(ALTERNATE_PORT, expectedState);
        }
        
        Logging.info("DPW OAuth: Callback server started on http://localhost:" + port + CALLBACK_PATH);
        
        // Auto-stop after 5 minutes if no callback received
        CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES).execute(() -> {
            if (!callbackFuture.isDone()) {
                callbackFuture.completeExceptionally(new IOException("OAuth callback timeout - no response received"));
                stop();
            }
        });
        
        return callbackFuture;
    }
    
    /**
     * Start the HTTP server on specified port
     */
    private void start(int port, String expectedState) throws IOException {
        this.port = port;
        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        
        server.createContext(CALLBACK_PATH, new CallbackHandler(expectedState));
        server.setExecutor(null); // Use default executor
        server.start();
    }
    
    /**
     * Stop the callback server
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            Logging.info("DPW OAuth: Callback server stopped");
        }
    }
    
    /**
     * Get the callback URL for this server
     */
    public String getCallbackUrl() {
        return "http://localhost:" + port + CALLBACK_PATH;
    }
    
    /**
     * Get the port the server is listening on
     */
    public int getPort() {
        return port;
    }
    
    /**
     * HTTP handler for OAuth callbacks
     */
    private class CallbackHandler implements HttpHandler {
        private final String expectedState;
        
        public CallbackHandler(String expectedState) {
            this.expectedState = expectedState;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Logging.info("DPW OAuth: Received callback request");
            
            try {
                URI requestUri = exchange.getRequestURI();
                Map<String, String> params = parseQueryParams(requestUri.getQuery());
                
                String code = params.get("code");
                String state = params.get("state");
                String error = params.get("error");
                String errorDescription = params.get("error_description");
                
                Logging.debug("DPW OAuth: Callback params - code=" + (code != null ? "present" : "null") + 
                             ", state=" + state + ", error=" + error);
                
                // Validate state parameter (CSRF protection)
                if (state == null || !state.equals(expectedState)) {
                    Logging.error("DPW OAuth: State mismatch! Expected: " + expectedState + ", Got: " + state);
                    sendErrorResponse(exchange, "Invalid state parameter - possible CSRF attack");
                    callbackFuture.completeExceptionally(new SecurityException("State parameter mismatch"));
                    return;
                }
                
                OAuthCallback callback = new OAuthCallback(code, state, error, errorDescription);
                
                if (callback.isSuccess()) {
                    sendSuccessResponse(exchange);
                    callbackFuture.complete(callback);
                } else if (callback.isError()) {
                    sendErrorResponse(exchange, error + ": " + errorDescription);
                    callbackFuture.complete(callback); // Complete with error info
                } else {
                    sendErrorResponse(exchange, "Missing authorization code");
                    callbackFuture.completeExceptionally(new IOException("Missing authorization code"));
                }
                
            } catch (Exception e) {
                Logging.error("DPW OAuth: Error handling callback: " + e.getMessage());
                sendErrorResponse(exchange, "Internal error: " + e.getMessage());
                callbackFuture.completeExceptionally(e);
            } finally {
                // Stop server after handling callback
                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(this::stopServer);
            }
        }
        
        private void stopServer() {
            stop();
        }
    }
    
    /**
     * Send success HTML response to browser
     */
    private void sendSuccessResponse(HttpExchange exchange) throws IOException {
        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset='UTF-8'>\n" +
                "    <title>Authentication Successful</title>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f0f8ff; }\n" +
                "        .container { max-width: 600px; margin: 0 auto; background: white; padding: 40px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n" +
                "        h1 { color: #28a745; }\n" +
                "        p { color: #666; font-size: 16px; }\n" +
                "        .icon { font-size: 64px; color: #28a745; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class='container'>\n" +
                "        <div class='icon'>✅</div>\n" +
                "        <h1>Authentication Successful!</h1>\n" +
                "        <p>You have successfully authenticated with the OSM server.</p>\n" +
                "        <p>You can close this window and return to JOSM.</p>\n" +
                "        <p><small>DPW Validation Tool v3.4.0</small></p>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
        
        sendResponse(exchange, 200, html);
    }
    
    /**
     * Send error HTML response to browser
     */
    private void sendErrorResponse(HttpExchange exchange, String errorMessage) throws IOException {
        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset='UTF-8'>\n" +
                "    <title>Authentication Failed</title>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #fff5f5; }\n" +
                "        .container { max-width: 600px; margin: 0 auto; background: white; padding: 40px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n" +
                "        h1 { color: #dc3545; }\n" +
                "        p { color: #666; font-size: 16px; }\n" +
                "        .icon { font-size: 64px; color: #dc3545; }\n" +
                "        .error { background: #f8d7da; border: 1px solid #f5c6cb; padding: 15px; border-radius: 5px; margin: 20px 0; color: #721c24; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class='container'>\n" +
                "        <div class='icon'>❌</div>\n" +
                "        <h1>Authentication Failed</h1>\n" +
                "        <div class='error'>" + escapeHtml(errorMessage) + "</div>\n" +
                "        <p>Please close this window and try again in JOSM.</p>\n" +
                "        <p><small>DPW Validation Tool v3.4.0</small></p>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
        
        sendResponse(exchange, 400, html);
    }
    
    /**
     * Send HTTP response
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String html) throws IOException {
        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
    
    /**
     * Parse query parameters from URL
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        
        if (query == null || query.isEmpty()) {
            return params;
        }
        
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                
                try {
                    // URL decode
                    value = java.net.URLDecoder.decode(value, StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    // Keep original value if decode fails
                }
                
                params.put(key, value);
            }
        }
        
        return params;
    }
    
    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
