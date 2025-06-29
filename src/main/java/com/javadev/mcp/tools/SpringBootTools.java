package com.javadev.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class SpringBootTools {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringBootTools.class);
    
    public CompletableFuture<Map<String, Object>> generateController(JsonNode arguments) {
        try {
            String entityName = arguments.get("entityName").asText();
            String packageName = arguments.get("packageName").asText();
            boolean includeCrud = arguments.has("includeCrud") ? arguments.get("includeCrud").asBoolean() : true;
            boolean useResponseEntity = arguments.has("useResponseEntity") ? arguments.get("useResponseEntity").asBoolean() : true;
            
            logger.info("Generating Spring Boot controller for entity: {}", entityName);
            
            String controllerCode = generateControllerCode(entityName, packageName, includeCrud, useResponseEntity);
            String fileName = entityName + "Controller.java";
            
            return CompletableFuture.completedFuture(Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "Spring Boot controller generated successfully"
                )),
                "controllerCode", controllerCode,
                "fileName", fileName,
                "instructions", List.of(
                    "1. Create controller file: " + fileName,
                    "2. Copy the generated controller code",
                    "3. Adjust package imports if needed",
                    "4. Implement the service layer methods",
                    "5. Add validation annotations as needed"
                )
            ));
            
        } catch (Exception e) {
            logger.error("Error generating Spring Boot controller", e);
            return CompletableFuture.completedFuture(createErrorResponse("Error generating controller: " + e.getMessage()));
        }
    }
    
    public CompletableFuture<Map<String, Object>> generateJpaEntity(JsonNode arguments) {
        try {
            String entityName = arguments.get("entityName").asText();
            String packageName = arguments.get("packageName").asText();
            String tableName = arguments.has("tableName") ? arguments.get("tableName").asText() : entityName.toLowerCase();
            
            List<Map<String, String>> fields = new ArrayList<>();
            if (arguments.has("fields")) {
                arguments.get("fields").forEach(field -> {
                    Map<String, String> fieldInfo = new HashMap<>();
                    fieldInfo.put("name", field.get("name").asText());
                    fieldInfo.put("type", field.get("type").asText());
                    fieldInfo.put("nullable", field.has("nullable") ? field.get("nullable").asText() : "true");
                    fields.add(fieldInfo);
                });
            }
            
            logger.info("Generating JPA entity: {}", entityName);
            
            String entityCode = generateEntityCode(entityName, packageName, tableName, fields);
            String fileName = entityName + ".java";
            
            return CompletableFuture.completedFuture(Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "JPA Entity generated successfully"
                )),
                "entityCode", entityCode,
                "fileName", fileName,
                "instructions", List.of(
                    "1. Create entity file: " + fileName,
                    "2. Copy the generated entity code",
                    "3. Add additional validation annotations if needed",
                    "4. Consider adding relationships with other entities",
                    "5. Review and adjust database column mappings"
                )
            ));
            
        } catch (Exception e) {
            logger.error("Error generating JPA entity", e);
            return CompletableFuture.completedFuture(createErrorResponse("Error generating JPA entity: " + e.getMessage()));
        }
    }
    
    private String generateControllerCode(String entityName, String packageName, boolean includeCrud, boolean useResponseEntity) {
        StringBuilder code = new StringBuilder();
        
        code.append("package ").append(packageName).append(";\n\n");
        
        code.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        code.append("import org.springframework.web.bind.annotation.*;\n");
        if (useResponseEntity) {
            code.append("import org.springframework.http.ResponseEntity;\n");
            code.append("import org.springframework.http.HttpStatus;\n");
        }
        code.append("import org.springframework.validation.annotation.Validated;\n");
        code.append("import jakarta.validation.Valid;\n");
        code.append("import java.util.List;\n");
        code.append("import java.util.Optional;\n\n");
        
        String lowerEntityName = toLowerCamelCase(entityName);
        String serviceName = entityName + "Service";
        String lowerServiceName = toLowerCamelCase(serviceName);
        
        code.append("@RestController\n");
        code.append("@RequestMapping(\"/api/").append(lowerEntityName).append("s\")\n");
        code.append("@Validated\n");
        code.append("public class ").append(entityName).append("Controller {\n\n");
        
        code.append("    @Autowired\n");
        code.append("    private ").append(serviceName).append(" ").append(lowerServiceName).append(";\n\n");
        
        if (includeCrud) {
            generateGetAllMethod(code, entityName, lowerServiceName, useResponseEntity);
            generateGetByIdMethod(code, entityName, lowerEntityName, lowerServiceName, useResponseEntity);
            generateCreateMethod(code, entityName, lowerEntityName, lowerServiceName, useResponseEntity);
            generateUpdateMethod(code, entityName, lowerEntityName, lowerServiceName, useResponseEntity);
            generateDeleteMethod(code, entityName, lowerEntityName, lowerServiceName, useResponseEntity);
        }
        
        code.append("}\n");
        
        return code.toString();
    }
    
    private void generateGetAllMethod(StringBuilder code, String entityName, String serviceName, boolean useResponseEntity) {
        code.append("    @GetMapping\n");
        if (useResponseEntity) {
            code.append("    public ResponseEntity<List<").append(entityName).append(">> getAll").append(entityName).append("s() {\n");
            code.append("        List<").append(entityName).append("> entities = ").append(serviceName).append(".findAll();\n");
            code.append("        return ResponseEntity.ok(entities);\n");
        } else {
            code.append("    public List<").append(entityName).append("> getAll").append(entityName).append("s() {\n");
            code.append("        return ").append(serviceName).append(".findAll();\n");
        }
        code.append("    }\n\n");
    }
    
    private void generateGetByIdMethod(StringBuilder code, String entityName, String lowerEntityName, String serviceName, boolean useResponseEntity) {
        code.append("    @GetMapping(\"/{id}\")\n");
        if (useResponseEntity) {
            code.append("    public ResponseEntity<").append(entityName).append("> get").append(entityName).append("ById(@PathVariable Long id) {\n");
            code.append("        Optional<").append(entityName).append("> ").append(lowerEntityName).append(" = ").append(serviceName).append(".findById(id);\n");
            code.append("        return ").append(lowerEntityName).append(".map(ResponseEntity::ok)\n");
            code.append("                .orElse(ResponseEntity.notFound().build());\n");
        } else {
            code.append("    public ").append(entityName).append(" get").append(entityName).append("ById(@PathVariable Long id) {\n");
            code.append("        return ").append(serviceName).append(".findById(id)\n");
            code.append("                .orElseThrow(() -> new RuntimeException(\"").append(entityName).append(" not found with id: \" + id));\n");
        }
        code.append("    }\n\n");
    }
    
    private void generateCreateMethod(StringBuilder code, String entityName, String lowerEntityName, String serviceName, boolean useResponseEntity) {
        code.append("    @PostMapping\n");
        if (useResponseEntity) {
            code.append("    public ResponseEntity<").append(entityName).append("> create").append(entityName).append("(@Valid @RequestBody ").append(entityName).append(" ").append(lowerEntityName).append(") {\n");
            code.append("        ").append(entityName).append(" saved").append(entityName).append(" = ").append(serviceName).append(".save(").append(lowerEntityName).append(");\n");
            code.append("        return ResponseEntity.status(HttpStatus.CREATED).body(saved").append(entityName).append(");\n");
        } else {
            code.append("    public ").append(entityName).append(" create").append(entityName).append("(@Valid @RequestBody ").append(entityName).append(" ").append(lowerEntityName).append(") {\n");
            code.append("        return ").append(serviceName).append(".save(").append(lowerEntityName).append(");\n");
        }
        code.append("    }\n\n");
    }
    
    private void generateUpdateMethod(StringBuilder code, String entityName, String lowerEntityName, String serviceName, boolean useResponseEntity) {
        code.append("    @PutMapping(\"/{id}\")\n");
        if (useResponseEntity) {
            code.append("    public ResponseEntity<").append(entityName).append("> update").append(entityName).append("(@PathVariable Long id, @Valid @RequestBody ").append(entityName).append(" ").append(lowerEntityName).append(") {\n");
            code.append("        if (!").append(serviceName).append(".existsById(id)) {\n");
            code.append("            return ResponseEntity.notFound().build();\n");
            code.append("        }\n");
            code.append("        ").append(lowerEntityName).append(".setId(id);\n");
            code.append("        ").append(entityName).append(" updated").append(entityName).append(" = ").append(serviceName).append(".save(").append(lowerEntityName).append(");\n");
            code.append("        return ResponseEntity.ok(updated").append(entityName).append(");\n");
        } else {
            code.append("    public ").append(entityName).append(" update").append(entityName).append("(@PathVariable Long id, @Valid @RequestBody ").append(entityName).append(" ").append(lowerEntityName).append(") {\n");
            code.append("        ").append(lowerEntityName).append(".setId(id);\n");
            code.append("        return ").append(serviceName).append(".save(").append(lowerEntityName).append(");\n");
        }
        code.append("    }\n\n");
    }
    
    private void generateDeleteMethod(StringBuilder code, String entityName, String lowerEntityName, String serviceName, boolean useResponseEntity) {
        code.append("    @DeleteMapping(\"/{id}\")\n");
        if (useResponseEntity) {
            code.append("    public ResponseEntity<Void> delete").append(entityName).append("(@PathVariable Long id) {\n");
            code.append("        if (!").append(serviceName).append(".existsById(id)) {\n");
            code.append("            return ResponseEntity.notFound().build();\n");
            code.append("        }\n");
            code.append("        ").append(serviceName).append(".deleteById(id);\n");
            code.append("        return ResponseEntity.noContent().build();\n");
        } else {
            code.append("    public void delete").append(entityName).append("(@PathVariable Long id) {\n");
            code.append("        ").append(serviceName).append(".deleteById(id);\n");
        }
        code.append("    }\n\n");
    }
    
    private String generateEntityCode(String entityName, String packageName, String tableName, List<Map<String, String>> fields) {
        StringBuilder code = new StringBuilder();
        
        code.append("package ").append(packageName).append(";\n\n");
        
        code.append("import jakarta.persistence.*;\n");
        code.append("import jakarta.validation.constraints.*;\n");
        code.append("import java.time.LocalDateTime;\n");
        code.append("import java.util.Objects;\n\n");
        
        code.append("@Entity\n");
        code.append("@Table(name = \"").append(tableName).append("\")\n");
        code.append("public class ").append(entityName).append(" {\n\n");
        
        code.append("    @Id\n");
        code.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
        code.append("    private Long id;\n\n");
        
        for (Map<String, String> field : fields) {
            generateField(code, field);
        }
        
        code.append("    @Column(name = \"created_at\")\n");
        code.append("    private LocalDateTime createdAt;\n\n");
        
        code.append("    @Column(name = \"updated_at\")\n");
        code.append("    private LocalDateTime updatedAt;\n\n");
        
        generateConstructors(code, entityName, fields);
        generateGettersAndSetters(code, fields);
        generateEqualsAndHashCode(code, entityName);
        generateToString(code, entityName, fields);
        
        code.append("    @PrePersist\n");
        code.append("    protected void onCreate() {\n");
        code.append("        createdAt = LocalDateTime.now();\n");
        code.append("        updatedAt = LocalDateTime.now();\n");
        code.append("    }\n\n");
        
        code.append("    @PreUpdate\n");
        code.append("    protected void onUpdate() {\n");
        code.append("        updatedAt = LocalDateTime.now();\n");
        code.append("    }\n");
        
        code.append("}\n");
        
        return code.toString();
    }
    
    private void generateField(StringBuilder code, Map<String, String> field) {
        String name = field.get("name");
        String type = field.get("type");
        boolean nullable = "true".equals(field.get("nullable"));
        
        if ("String".equals(type)) {
            if (!nullable) {
                code.append("    @NotBlank\n");
            }
            code.append("    @Column(name = \"").append(toSnakeCase(name)).append("\"");
            if (!nullable) {
                code.append(", nullable = false");
            }
            code.append(")\n");
        } else if ("Integer".equals(type) || "Long".equals(type)) {
            if (!nullable) {
                code.append("    @NotNull\n");
            }
            code.append("    @Column(name = \"").append(toSnakeCase(name)).append("\"");
            if (!nullable) {
                code.append(", nullable = false");
            }
            code.append(")\n");
        } else {
            code.append("    @Column(name = \"").append(toSnakeCase(name)).append("\")\n");
        }
        
        code.append("    private ").append(type).append(" ").append(name).append(";\n\n");
    }
    
    private void generateConstructors(StringBuilder code, String entityName, List<Map<String, String>> fields) {
        code.append("    public ").append(entityName).append("() {\n");
        code.append("    }\n\n");
        
        if (!fields.isEmpty()) {
            code.append("    public ").append(entityName).append("(");
            for (int i = 0; i < fields.size(); i++) {
                Map<String, String> field = fields.get(i);
                code.append(field.get("type")).append(" ").append(field.get("name"));
                if (i < fields.size() - 1) {
                    code.append(", ");
                }
            }
            code.append(") {\n");
            
            for (Map<String, String> field : fields) {
                String name = field.get("name");
                code.append("        this.").append(name).append(" = ").append(name).append(";\n");
            }
            code.append("    }\n\n");
        }
    }
    
    private void generateGettersAndSetters(StringBuilder code, List<Map<String, String>> fields) {
        code.append("    public Long getId() {\n");
        code.append("        return id;\n");
        code.append("    }\n\n");
        
        code.append("    public void setId(Long id) {\n");
        code.append("        this.id = id;\n");
        code.append("    }\n\n");
        
        for (Map<String, String> field : fields) {
            String name = field.get("name");
            String type = field.get("type");
            String capitalizedName = toUpperCamelCase(name);
            
            code.append("    public ").append(type).append(" get").append(capitalizedName).append("() {\n");
            code.append("        return ").append(name).append(";\n");
            code.append("    }\n\n");
            
            code.append("    public void set").append(capitalizedName).append("(").append(type).append(" ").append(name).append(") {\n");
            code.append("        this.").append(name).append(" = ").append(name).append(";\n");
            code.append("    }\n\n");
        }
        
        code.append("    public LocalDateTime getCreatedAt() {\n");
        code.append("        return createdAt;\n");
        code.append("    }\n\n");
        
        code.append("    public LocalDateTime getUpdatedAt() {\n");
        code.append("        return updatedAt;\n");
        code.append("    }\n\n");
    }
    
    private void generateEqualsAndHashCode(StringBuilder code, String entityName) {
        code.append("    @Override\n");
        code.append("    public boolean equals(Object o) {\n");
        code.append("        if (this == o) return true;\n");
        code.append("        if (o == null || getClass() != o.getClass()) return false;\n");
        code.append("        ").append(entityName).append(" that = (").append(entityName).append(") o;\n");
        code.append("        return Objects.equals(id, that.id);\n");
        code.append("    }\n\n");
        
        code.append("    @Override\n");
        code.append("    public int hashCode() {\n");
        code.append("        return Objects.hash(id);\n");
        code.append("    }\n\n");
    }
    
    private void generateToString(StringBuilder code, String entityName, List<Map<String, String>> fields) {
        code.append("    @Override\n");
        code.append("    public String toString() {\n");
        code.append("        return \"").append(entityName).append("{\" +\n");
        code.append("                \"id=\" + id +\n");
        
        for (Map<String, String> field : fields) {
            String name = field.get("name");
            code.append("                \", ").append(name).append("='\" + ").append(name).append(" + '\\'' +\n");
        }
        
        code.append("                \", createdAt=\" + createdAt +\n");
        code.append("                \", updatedAt=\" + updatedAt +\n");
        code.append("                '}';\n");
        code.append("    }\n\n");
    }
    
    private String toLowerCamelCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
    
    private String toUpperCamelCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private String toSnakeCase(String str) {
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        return Map.of("error", Map.of("code", -1, "message", message));
    }
}