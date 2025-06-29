package com.javadev.mcp.protocol.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Component
public class StdioTransport {
    
    private static final Logger logger = LoggerFactory.getLogger(StdioTransport.class);
    
    private final ObjectMapper objectMapper;
    private final BufferedReader reader;
    private final PrintWriter writer;
    
    public StdioTransport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.writer = new PrintWriter(System.out, true);
    }
    
    public void start(Function<JsonNode, CompletableFuture<Map<String, Object>>> messageHandler) {
        logger.info("Starting STDIO transport...");
        
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    JsonNode message = objectMapper.readTree(line);
                    logger.debug("Received message: {}", message);
                    
                    CompletableFuture<Map<String, Object>> response = messageHandler.apply(message);
                    
                    response.thenAccept(result -> {
                        try {
                            String responseJson = objectMapper.writeValueAsString(result);
                            writer.println(responseJson);
                            logger.debug("Sent response: {}", responseJson);
                        } catch (Exception e) {
                            logger.error("Error serializing response", e);
                            sendError(message.get("id"), "Error serializing response");
                        }
                    }).exceptionally(throwable -> {
                        logger.error("Error processing message", throwable);
                        sendError(message.get("id"), "Error processing message");
                        return null;
                    });
                    
                } catch (Exception e) {
                    logger.error("Error parsing message: {}", line, e);
                    sendError(null, "Error parsing message");
                }
            }
        } catch (IOException e) {
            logger.error("Error reading from stdin", e);
        }
        
        logger.info("STDIO transport stopped");
    }
    
    private void sendError(JsonNode id, String message) {
        try {
            Map<String, Object> error = Map.of(
                "jsonrpc", "2.0",
                "id", id != null ? id : null,
                "error", Map.of(
                    "code", -32603,
                    "message", message
                )
            );
            
            String errorJson = objectMapper.writeValueAsString(error);
            writer.println(errorJson);
            logger.debug("Sent error: {}", errorJson);
        } catch (Exception e) {
            logger.error("Error sending error response", e);
        }
    }
}