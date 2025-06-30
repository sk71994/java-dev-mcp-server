import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Live Demo of Java Dev MCP Server
 * Run this in IntelliJ to see the MCP server in action
 */
public class McpDemo {
    
    public static void main(String[] args) {
        System.out.println("🚀 Java Dev MCP Server Live Demo");
        System.out.println("==================================");
        
        try {
            // Start the MCP server process
            ProcessBuilder pb = new ProcessBuilder("java", "-jar", "build/libs/java-dev-mcp-server-1.0.0.jar");
            pb.directory(new File(System.getProperty("user.dir")));
            Process mcpServer = pb.start();
            
            // Give the server time to start
            Thread.sleep(1000);
            
            // Demo 1: Initialize the server
            System.out.println("\n📋 Demo 1: Initialize MCP Server");
            sendMcpMessage(mcpServer, """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {
                      "tools": {}
                    },
                    "clientInfo": {
                      "name": "JavaDevDemo",
                      "version": "1.0.0"
                    }
                  }
                }
                """);
            
            Thread.sleep(500);
            
            // Demo 2: List available tools
            System.out.println("\n🔧 Demo 2: List Available Tools");
            sendMcpMessage(mcpServer, """
                {
                  "jsonrpc": "2.0",
                  "id": 2,
                  "method": "tools/list",
                  "params": {}
                }
                """);
            
            Thread.sleep(500);
            
            // Demo 3: Generate Spring Boot Controller
            System.out.println("\n🏗️  Demo 3: Generate Spring Boot Controller");
            sendMcpMessage(mcpServer, """
                {
                  "jsonrpc": "2.0",
                  "id": 3,
                  "method": "tools/call",
                  "params": {
                    "name": "spring-controller-generator",
                    "arguments": {
                      "entityName": "Product",
                      "packageName": "com.example.demo",
                      "includeCrud": true,
                      "useResponseEntity": true
                    }
                  }
                }
                """);
            
            Thread.sleep(1000);
            
            // Demo 4: Generate JPA Entity
            System.out.println("\n📦 Demo 4: Generate JPA Entity");
            sendMcpMessage(mcpServer, """
                {
                  "jsonrpc": "2.0",
                  "id": 4,
                  "method": "tools/call",
                  "params": {
                    "name": "jpa-entity-generator",
                    "arguments": {
                      "entityName": "Product",
                      "packageName": "com.example.demo.entity",
                      "fields": [
                        {
                          "name": "name",
                          "type": "String",
                          "required": true
                        },
                        {
                          "name": "price",
                          "type": "BigDecimal",
                          "required": true
                        },
                        {
                          "name": "description",
                          "type": "String",
                          "required": false
                        }
                      ],
                      "includeAuditFields": true
                    }
                  }
                }
                """);
            
            Thread.sleep(1000);
            
            // Demo 5: Analyze Java Class
            System.out.println("\n🔍 Demo 5: Analyze Java Class");
            sendMcpMessage(mcpServer, """
                {
                  "jsonrpc": "2.0",
                  "id": 5,
                  "method": "tools/call",
                  "params": {
                    "name": "java-class-analyzer",
                    "arguments": {
                      "filePath": "src/main/java/com/javadev/mcp/tools/SpringBootTools.java"
                    }
                  }
                }
                """);
            
            Thread.sleep(1000);
            
            System.out.println("\n✅ Demo Complete! MCP Server is working perfectly!");
            System.out.println("💡 The server generated Spring Boot controllers, JPA entities, and analyzed code!");
            
            // Clean shutdown
            mcpServer.destroy();
            mcpServer.waitFor(5, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void sendMcpMessage(Process mcpServer, String jsonMessage) {
        try {
            PrintWriter writer = new PrintWriter(mcpServer.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(mcpServer.getInputStream()));
            
            System.out.println("📤 Sending: " + jsonMessage.trim().replaceAll("\\s+", " "));
            writer.println(jsonMessage);
            writer.flush();
            
            // Read response
            String response = reader.readLine();
            if (response != null) {
                System.out.println("📥 Response: " + response.substring(0, Math.min(response.length(), 200)) + "...");
            }
        } catch (IOException e) {
            System.err.println("Communication error: " + e.getMessage());
        }
    }
}