package com.javadev.mcp.prompts;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class DevelopmentPrompts {
    
    private static final Logger logger = LoggerFactory.getLogger(DevelopmentPrompts.class);
    
    public CompletableFuture<Map<String, Object>> createSpringBootService(JsonNode arguments) {
        try {
            String serviceName = arguments.get("serviceName").asText();
            boolean includeDatabase = arguments.has("includeDatabase") ? 
                arguments.get("includeDatabase").asBoolean() : false;
            
            logger.info("Creating Spring Boot service prompt for: {}", serviceName);
            
            List<Map<String, Object>> messages = new ArrayList<>();
            
            messages.add(createMessage("user", createServiceCreationPrompt(serviceName, includeDatabase)));
            
            return CompletableFuture.completedFuture(Map.of(
                "description", "Step-by-step guide to create a Spring Boot service",
                "messages", messages
            ));
            
        } catch (Exception e) {
            logger.error("Error creating Spring Boot service prompt", e);
            return CompletableFuture.completedFuture(createErrorResponse("Error creating prompt: " + e.getMessage()));
        }
    }
    
    public CompletableFuture<Map<String, Object>> implementCrudOperations(JsonNode arguments) {
        try {
            String entityName = arguments.get("entityName").asText();
            boolean includeValidation = arguments.has("includeValidation") ? 
                arguments.get("includeValidation").asBoolean() : true;
            
            logger.info("Creating CRUD operations prompt for entity: {}", entityName);
            
            List<Map<String, Object>> messages = new ArrayList<>();
            
            messages.add(createMessage("user", createCrudImplementationPrompt(entityName, includeValidation)));
            
            return CompletableFuture.completedFuture(Map.of(
                "description", "Complete guide to implement CRUD operations",
                "messages", messages
            ));
            
        } catch (Exception e) {
            logger.error("Error creating CRUD operations prompt", e);
            return CompletableFuture.completedFuture(createErrorResponse("Error creating prompt: " + e.getMessage()));
        }
    }
    
    private String createServiceCreationPrompt(String serviceName, boolean includeDatabase) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("I need to create a new Spring Boot service called '").append(serviceName).append("'. ");
        prompt.append("Please help me implement this service following Spring Boot best practices.\n\n");
        
        prompt.append("Requirements:\n");
        prompt.append("1. Create a clean, well-structured service class\n");
        prompt.append("2. Follow Spring Boot conventions and annotations\n");
        prompt.append("3. Include proper logging\n");
        prompt.append("4. Add comprehensive error handling\n");
        prompt.append("5. Write unit tests for the service\n");
        
        if (includeDatabase) {
            prompt.append("6. Include database integration with JPA/Hibernate\n");
            prompt.append("7. Create repository layer\n");
            prompt.append("8. Add transaction management\n");
        }
        
        prompt.append("\nPlease provide:\n");
        prompt.append("- Service interface and implementation\n");
        prompt.append("- Proper dependency injection setup\n");
        prompt.append("- Exception handling classes\n");
        prompt.append("- Unit test templates\n");
        
        if (includeDatabase) {
            prompt.append("- Repository interface\n");
            prompt.append("- Entity class if needed\n");
            prompt.append("- Database configuration\n");
        }
        
        prompt.append("\nCode should be:\n");
        prompt.append("- Production-ready\n");
        prompt.append("- Well-documented\n");
        prompt.append("- Follow SOLID principles\n");
        prompt.append("- Include validation where appropriate\n");
        
        return prompt.toString();
    }
    
    private String createCrudImplementationPrompt(String entityName, boolean includeValidation) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("I need to implement complete CRUD operations for the '").append(entityName).append("' entity. ");
        prompt.append("Please help me create a full-stack implementation following Spring Boot best practices.\n\n");
        
        prompt.append("Required Components:\n");
        prompt.append("1. JPA Entity class with proper annotations\n");
        prompt.append("2. Repository interface extending JpaRepository\n");
        prompt.append("3. Service layer with business logic\n");
        prompt.append("4. REST Controller with all CRUD endpoints\n");
        prompt.append("5. Exception handling for all scenarios\n");
        
        if (includeValidation) {
            prompt.append("6. Bean validation annotations\n");
            prompt.append("7. Custom validation logic where needed\n");
        }
        
        prompt.append("\nCRUD Operations to implement:\n");
        prompt.append("- CREATE: POST /api/").append(entityName.toLowerCase()).append("s\n");
        prompt.append("- READ: GET /api/").append(entityName.toLowerCase()).append("s (all) and GET /api/").append(entityName.toLowerCase()).append("s/{id} (single)\n");
        prompt.append("- UPDATE: PUT /api/").append(entityName.toLowerCase()).append("s/{id}\n");
        prompt.append("- DELETE: DELETE /api/").append(entityName.toLowerCase()).append("s/{id}\n");
        
        prompt.append("\nAdditional Requirements:\n");
        prompt.append("- Use ResponseEntity for proper HTTP responses\n");
        prompt.append("- Include pagination for list endpoints\n");
        prompt.append("- Add proper error responses (404, 400, 500)\n");
        prompt.append("- Include audit fields (createdAt, updatedAt)\n");
        prompt.append("- Write comprehensive unit and integration tests\n");
        
        if (includeValidation) {
            prompt.append("- Validate input data with appropriate constraints\n");
            prompt.append("- Return meaningful validation error messages\n");
        }
        
        prompt.append("\nPlease provide:\n");
        prompt.append("1. Complete entity class with JPA annotations\n");
        prompt.append("2. Repository interface\n");
        prompt.append("3. Service interface and implementation\n");
        prompt.append("4. Controller class with all endpoints\n");
        prompt.append("5. Custom exception classes\n");
        prompt.append("6. Global exception handler\n");
        prompt.append("7. Test classes for all layers\n");
        
        prompt.append("\nEnsure the code is:\n");
        prompt.append("- Production-ready and secure\n");
        prompt.append("- Well-documented with JavaDoc\n");
        prompt.append("- Following RESTful conventions\n");
        prompt.append("- Optimized for performance\n");
        prompt.append("- Easy to maintain and extend\n");
        
        return prompt.toString();
    }
    
    private Map<String, Object> createMessage(String role, String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", role);
        message.put("content", Map.of(
            "type", "text",
            "text", content
        ));
        return message;
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        return Map.of("error", Map.of("code", -1, "message", message));
    }
}