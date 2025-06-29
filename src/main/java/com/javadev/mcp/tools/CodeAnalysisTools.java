package com.javadev.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class CodeAnalysisTools {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeAnalysisTools.class);
    
    private final JavaParser javaParser;
    
    public CodeAnalysisTools() {
        this.javaParser = new JavaParser();
    }
    
    public CompletableFuture<Map<String, Object>> analyzeJavaClass(JsonNode arguments) {
        try {
            String filePath = arguments.get("filePath").asText();
            boolean includeMetrics = arguments.has("includeMetrics") ? 
                arguments.get("includeMetrics").asBoolean() : true;
            
            logger.info("Analyzing Java class: {}", filePath);
            
            if (!Files.exists(Paths.get(filePath))) {
                return CompletableFuture.completedFuture(createErrorResponse("File not found: " + filePath));
            }
            
            String content = Files.readString(Paths.get(filePath));
            CompilationUnit cu = javaParser.parse(content).getResult().orElse(null);
            
            if (cu == null) {
                return CompletableFuture.completedFuture(createErrorResponse("Unable to parse Java file"));
            }
            
            Map<String, Object> analysis = new HashMap<>();
            analysis.put("filePath", filePath);
            analysis.put("packageName", cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("default"));
            
            List<String> imports = new ArrayList<>();
            cu.getImports().forEach(imp -> imports.add(imp.getNameAsString()));
            analysis.put("imports", imports);
            
            List<Map<String, Object>> classes = new ArrayList<>();
            cu.accept(new ClassAnalyzer(classes, includeMetrics), null);
            analysis.put("classes", classes);
            
            if (includeMetrics) {
                analysis.put("metrics", calculateMetrics(cu));
            }
            
            List<String> issues = detectIssues(cu);
            analysis.put("issues", issues);
            
            return CompletableFuture.completedFuture(Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "Java Class Analysis completed successfully"
                )),
                "analysis", analysis
            ));
            
        } catch (Exception e) {
            logger.error("Error analyzing Java class", e);
            return CompletableFuture.completedFuture(createErrorResponse("Error analyzing Java class: " + e.getMessage()));
        }
    }
    
    public CompletableFuture<Map<String, Object>> generateUnitTest(JsonNode arguments) {
        try {
            String classPath = arguments.get("classPath").asText();
            String testType = arguments.has("testType") ? arguments.get("testType").asText() : "unit";
            boolean includeMockito = arguments.has("includeMockito") ? 
                arguments.get("includeMockito").asBoolean() : true;
            
            logger.info("Generating unit test for: {}", classPath);
            
            if (!Files.exists(Paths.get(classPath))) {
                return CompletableFuture.completedFuture(createErrorResponse("Class file not found: " + classPath));
            }
            
            String content = Files.readString(Paths.get(classPath));
            CompilationUnit cu = javaParser.parse(content).getResult().orElse(null);
            
            if (cu == null) {
                return CompletableFuture.completedFuture(createErrorResponse("Unable to parse Java file"));
            }
            
            String testCode = generateTestCode(cu, testType, includeMockito);
            String testFileName = getTestFileName(classPath);
            
            return CompletableFuture.completedFuture(Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "Unit test generated successfully"
                )),
                "testCode", testCode,
                "testFileName", testFileName,
                "instructions", List.of(
                    "1. Create test file: " + testFileName,
                    "2. Copy the generated test code",
                    "3. Adjust package declaration if needed",
                    "4. Add necessary test dependencies to build file"
                )
            ));
            
        } catch (Exception e) {
            logger.error("Error generating unit test", e);
            return CompletableFuture.completedFuture(createErrorResponse("Error generating unit test: " + e.getMessage()));
        }
    }
    
    private String generateTestCode(CompilationUnit cu, String testType, boolean includeMockito) {
        StringBuilder testCode = new StringBuilder();
        
        String packageName = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
        if (!packageName.isEmpty()) {
            testCode.append("package ").append(packageName).append(";\n\n");
        }
        
        testCode.append("import org.junit.jupiter.api.Test;\n");
        testCode.append("import org.junit.jupiter.api.BeforeEach;\n");
        testCode.append("import org.junit.jupiter.api.DisplayName;\n");
        testCode.append("import static org.junit.jupiter.api.Assertions.*;\n");
        
        if (includeMockito) {
            testCode.append("import org.mockito.Mock;\n");
            testCode.append("import org.mockito.MockitoAnnotations;\n");
            testCode.append("import static org.mockito.Mockito.*;\n");
        }
        
        testCode.append("\n");
        
        cu.getTypes().forEach(type -> {
            if (type instanceof ClassOrInterfaceDeclaration clazz) {
                String className = clazz.getNameAsString();
                String testClassName = className + "Test";
                
                testCode.append("@DisplayName(\"").append(className).append(" Tests\")\n");
                testCode.append("class ").append(testClassName).append(" {\n\n");
                
                testCode.append("    private ").append(className).append(" ").append(toLowerCamelCase(className)).append(";\n\n");
                
                testCode.append("    @BeforeEach\n");
                testCode.append("    void setUp() {\n");
                if (includeMockito) {
                    testCode.append("        MockitoAnnotations.openMocks(this);\n");
                }
                testCode.append("        ").append(toLowerCamelCase(className)).append(" = new ").append(className).append("();\n");
                testCode.append("    }\n\n");
                
                clazz.getMethods().forEach(method -> {
                    if (method.isPublic() && !method.isConstructorDeclaration()) {
                        String methodName = method.getNameAsString();
                        testCode.append("    @Test\n");
                        testCode.append("    @DisplayName(\"Should test ").append(methodName).append("\")\n");
                        testCode.append("    void test").append(toUpperCamelCase(methodName)).append("() {\n");
                        testCode.append("        // Given\n");
                        testCode.append("        // TODO: Setup test data\n\n");
                        testCode.append("        // When\n");
                        testCode.append("        // TODO: Call method under test\n\n");
                        testCode.append("        // Then\n");
                        testCode.append("        // TODO: Add assertions\n");
                        testCode.append("        fail(\"Not implemented yet\");\n");
                        testCode.append("    }\n\n");
                    }
                });
                
                testCode.append("}\n");
            }
        });
        
        return testCode.toString();
    }
    
    private String getTestFileName(String classPath) {
        String fileName = new File(classPath).getName();
        String className = fileName.substring(0, fileName.lastIndexOf('.'));
        return className + "Test.java";
    }
    
    private Map<String, Object> calculateMetrics(CompilationUnit cu) {
        Map<String, Object> metrics = new HashMap<>();
        
        MetricsCalculator calculator = new MetricsCalculator();
        cu.accept(calculator, null);
        
        metrics.put("totalClasses", calculator.classCount);
        metrics.put("totalMethods", calculator.methodCount);
        metrics.put("totalLines", calculator.lineCount);
        metrics.put("averageMethodsPerClass", calculator.classCount > 0 ? 
            (double) calculator.methodCount / calculator.classCount : 0);
        
        return metrics;
    }
    
    private List<String> detectIssues(CompilationUnit cu) {
        List<String> issues = new ArrayList<>();
        
        IssueDetector detector = new IssueDetector(issues);
        cu.accept(detector, null);
        
        return issues;
    }
    
    private String toLowerCamelCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
    
    private String toUpperCamelCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        return Map.of("error", Map.of("code", -1, "message", message));
    }
    
    private static class ClassAnalyzer extends VoidVisitorAdapter<Void> {
        private final List<Map<String, Object>> classes;
        private final boolean includeMetrics;
        
        public ClassAnalyzer(List<Map<String, Object>> classes, boolean includeMetrics) {
            this.classes = classes;
            this.includeMetrics = includeMetrics;
        }
        
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            Map<String, Object> classInfo = new HashMap<>();
            classInfo.put("name", n.getNameAsString());
            classInfo.put("isInterface", n.isInterface());
            classInfo.put("isAbstract", n.isAbstract());
            
            List<String> methods = new ArrayList<>();
            n.getMethods().forEach(method -> methods.add(method.getNameAsString()));
            classInfo.put("methods", methods);
            
            List<String> fields = new ArrayList<>();
            n.getFields().forEach(field -> field.getVariables().forEach(var -> fields.add(var.getNameAsString())));
            classInfo.put("fields", fields);
            
            if (includeMetrics) {
                classInfo.put("methodCount", methods.size());
                classInfo.put("fieldCount", fields.size());
            }
            
            classes.add(classInfo);
            super.visit(n, arg);
        }
    }
    
    private static class MetricsCalculator extends VoidVisitorAdapter<Void> {
        int classCount = 0;
        int methodCount = 0;
        int lineCount = 0;
        
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            classCount++;
            super.visit(n, arg);
        }
        
        @Override
        public void visit(MethodDeclaration n, Void arg) {
            methodCount++;
            super.visit(n, arg);
        }
    }
    
    private static class IssueDetector extends VoidVisitorAdapter<Void> {
        private final List<String> issues;
        
        public IssueDetector(List<String> issues) {
            this.issues = issues;
        }
        
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            if (n.getMethods().size() > 20) {
                issues.add("Class " + n.getNameAsString() + " has too many methods (" + n.getMethods().size() + ")");
            }
            
            if (n.getFields().size() > 15) {
                issues.add("Class " + n.getNameAsString() + " has too many fields (" + n.getFields().size() + ")");
            }
            
            super.visit(n, arg);
        }
        
        @Override
        public void visit(MethodDeclaration n, Void arg) {
            if (n.getParameters().size() > 5) {
                issues.add("Method " + n.getNameAsString() + " has too many parameters (" + n.getParameters().size() + ")");
            }
            
            super.visit(n, arg);
        }
    }
}