package com.javadev.mcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javadev.mcp.protocol.transport.StdioTransport;
import com.javadev.mcp.tools.CodeAnalysisTools;
import com.javadev.mcp.tools.SpringBootTools;
import com.javadev.mcp.resources.ProjectStructureResource;
import com.javadev.mcp.prompts.DevelopmentPrompts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class McpServer {
    
    private static final Logger logger = LoggerFactory.getLogger(McpServer.class);
    
    private final ObjectMapper objectMapper;
    private final StdioTransport transport;
    private final CodeAnalysisTools codeAnalysisTools;
    private final SpringBootTools springBootTools;
    private final ProjectStructureResource projectStructureResource;
    private final DevelopmentPrompts developmentPrompts;
    
    public McpServer(ObjectMapper objectMapper, 
                     StdioTransport transport,
                     CodeAnalysisTools codeAnalysisTools,
                     SpringBootTools springBootTools,
                     ProjectStructureResource projectStructureResource,
                     DevelopmentPrompts developmentPrompts) {
        this.objectMapper = objectMapper;
        this.transport = transport;
        this.codeAnalysisTools = codeAnalysisTools;
        this.springBootTools = springBootTools;
        this.projectStructureResource = projectStructureResource;
        this.developmentPrompts = developmentPrompts;
    }
    
    public void start() {
        logger.info("Starting Java Dev MCP Server...");
        transport.start(this::handleMessage);
    }
    
    private CompletableFuture<Map<String, Object>> handleMessage(JsonNode message) {
        try {
            String method = message.get("method").asText();
            JsonNode params = message.get("params");
            
            logger.debug("Handling method: {}", method);
            
            return switch (method) {
                case "initialize" -> handleInitialize(params);
                case "tools/list" -> handleToolsList();
                case "tools/call" -> handleToolsCall(params);
                case "resources/list" -> handleResourcesList();
                case "resources/read" -> handleResourcesRead(params);
                case "prompts/list" -> handlePromptsList();
                case "prompts/get" -> handlePromptsGet(params);
                default -> CompletableFuture.completedFuture(createErrorResponse("Unknown method: " + method));
            };
            
        } catch (Exception e) {
            logger.error("Error handling message: ", e);
            return CompletableFuture.completedFuture(createErrorResponse("Internal server error"));
        }
    }
    
    private CompletableFuture<Map<String, Object>> handleInitialize(JsonNode params) {
        Map<String, Object> response = new HashMap<>();
        response.put("protocolVersion", "2024-11-05");
        response.put("serverInfo", Map.of(
            "name", "Java Dev MCP Server",
            "version", "1.0.0"
        ));
        
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("tools", Map.of("listChanged", true));
        capabilities.put("resources", Map.of("subscribe", true, "listChanged", true));
        capabilities.put("prompts", Map.of("listChanged", true));
        
        response.put("capabilities", capabilities);
        
        return CompletableFuture.completedFuture(response);
    }
    
    private CompletableFuture<Map<String, Object>> handleToolsList() {
        List<Map<String, Object>> tools = new ArrayList<>();
        
        // Code Analysis Tools
        tools.add(createToolDefinition(
            "java-class-analyzer",
            "Analyze Java classes for patterns, dependencies, and potential issues",
            Map.of(
                "filePath", Map.of("type", "string", "description", "Path to Java file to analyze"),
                "includeMetrics", Map.of("type", "boolean", "description", "Include complexity metrics", "default", true)
            )
        ));
        
        tools.add(createToolDefinition(
            "spring-controller-generator",
            "Generate Spring Boot REST controllers with proper annotations",
            Map.of(
                "entityName", Map.of("type", "string", "description", "Name of the entity"),
                "packageName", Map.of("type", "string", "description", "Package name for the controller"),
                "includeCrud", Map.of("type", "boolean", "description", "Include CRUD operations", "default", true),
                "useResponseEntity", Map.of("type", "boolean", "description", "Use ResponseEntity wrapper", "default", true)
            )
        ));
        
        tools.add(createToolDefinition(
            "jpa-entity-generator",
            "Create JPA entities from specifications",
            Map.of(
                "entityName", Map.of("type", "string", "description", "Name of the entity"),
                "packageName", Map.of("type", "string", "description", "Package name for the entity"),
                "fields", Map.of("type", "array", "description", "List of entity fields with types"),
                "tableName", Map.of("type", "string", "description", "Database table name")
            )
        ));
        
        tools.add(createToolDefinition(
            "unit-test-generator",
            "Generate JUnit 5 test templates for existing classes",
            Map.of(
                "classPath", Map.of("type", "string", "description", "Path to the class to test"),
                "testType", Map.of("type", "string", "description", "Type of test (unit, integration)", "default", "unit"),
                "includeMockito", Map.of("type", "boolean", "description", "Include Mockito mocks", "default", true)
            )
        ));
        
        return CompletableFuture.completedFuture(Map.of("tools", tools));
    }
    
    private CompletableFuture<Map<String, Object>> handleToolsCall(JsonNode params) {
        String name = params.get("name").asText();
        JsonNode arguments = params.get("arguments");
        
        return switch (name) {
            case "java-class-analyzer" -> codeAnalysisTools.analyzeJavaClass(arguments);
            case "spring-controller-generator" -> springBootTools.generateController(arguments);
            case "jpa-entity-generator" -> springBootTools.generateJpaEntity(arguments);
            case "unit-test-generator" -> codeAnalysisTools.generateUnitTest(arguments);
            default -> CompletableFuture.completedFuture(createErrorResponse("Unknown tool: " + name));
        };
    }
    
    private CompletableFuture<Map<String, Object>> handleResourcesList() {
        List<Map<String, Object>> resources = new ArrayList<>();
        
        resources.add(createResourceDefinition(
            "java-project://current/structure",
            "Current Java project structure and organization",
            "application/json"
        ));
        
        resources.add(createResourceDefinition(
            "java-project://current/dependencies",
            "Project dependencies and their versions",
            "application/json"
        ));
        
        resources.add(createResourceDefinition(
            "java-project://current/configuration",
            "Application configuration files",
            "application/json"
        ));
        
        return CompletableFuture.completedFuture(Map.of("resources", resources));
    }
    
    private CompletableFuture<Map<String, Object>> handleResourcesRead(JsonNode params) {
        String uri = params.get("uri").asText();
        
        return switch (uri) {
            case "java-project://current/structure" -> projectStructureResource.getProjectStructure();
            case "java-project://current/dependencies" -> projectStructureResource.getDependencies();
            case "java-project://current/configuration" -> projectStructureResource.getConfiguration();
            default -> CompletableFuture.completedFuture(createErrorResponse("Unknown resource: " + uri));
        };
    }
    
    private CompletableFuture<Map<String, Object>> handlePromptsList() {
        List<Map<String, Object>> prompts = new ArrayList<>();
        
        prompts.add(createPromptDefinition(
            "create-spring-boot-service",
            "Step-by-step service creation with best practices",
            Map.of(
                "serviceName", Map.of("type", "string", "description", "Name of the service to create"),
                "includeDatabase", Map.of("type", "boolean", "description", "Include database integration", "default", false)
            )
        ));
        
        prompts.add(createPromptDefinition(
            "implement-crud-operations",
            "Generate complete CRUD operations for entities",
            Map.of(
                "entityName", Map.of("type", "string", "description", "Name of the entity"),
                "includeValidation", Map.of("type", "boolean", "description", "Include validation annotations", "default", true)
            )
        ));
        
        return CompletableFuture.completedFuture(Map.of("prompts", prompts));
    }
    
    private CompletableFuture<Map<String, Object>> handlePromptsGet(JsonNode params) {
        String name = params.get("name").asText();
        JsonNode arguments = params.get("arguments");
        
        return switch (name) {
            case "create-spring-boot-service" -> developmentPrompts.createSpringBootService(arguments);
            case "implement-crud-operations" -> developmentPrompts.implementCrudOperations(arguments);
            default -> CompletableFuture.completedFuture(createErrorResponse("Unknown prompt: " + name));
        };
    }
    
    private Map<String, Object> createToolDefinition(String name, String description, Map<String, Object> properties) {
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        
        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", properties);
        inputSchema.put("required", properties.keySet().stream()
            .filter(key -> !properties.get(key).toString().contains("default"))
            .toList());
        
        tool.put("inputSchema", inputSchema);
        return tool;
    }
    
    private Map<String, Object> createResourceDefinition(String uri, String description, String mimeType) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("uri", uri);
        resource.put("name", uri.substring(uri.lastIndexOf('/') + 1));
        resource.put("description", description);
        resource.put("mimeType", mimeType);
        return resource;
    }
    
    private Map<String, Object> createPromptDefinition(String name, String description, Map<String, Object> arguments) {
        Map<String, Object> prompt = new HashMap<>();
        prompt.put("name", name);
        prompt.put("description", description);
        prompt.put("arguments", arguments);
        return prompt;
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        return Map.of("error", Map.of("code", -1, "message", message));
    }
}