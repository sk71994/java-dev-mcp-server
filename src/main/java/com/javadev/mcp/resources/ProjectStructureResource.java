package com.javadev.mcp.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Component
public class ProjectStructureResource {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectStructureResource.class);
    
    public CompletableFuture<Map<String, Object>> getProjectStructure() {
        try {
            String currentDir = System.getProperty("user.dir");
            logger.info("Analyzing project structure for: {}", currentDir);
            
            Map<String, Object> structure = analyzeProjectStructure(currentDir);
            
            return CompletableFuture.completedFuture(Map.of(
                "contents", List.of(Map.of(
                    "type", "text",
                    "text", "Project structure analysis completed"
                )),
                "structure", structure
            ));
            
        } catch (Exception e) {
            logger.error("Error analyzing project structure", e);
            return CompletableFuture.completedFuture(createErrorResponse("Error analyzing project structure: " + e.getMessage()));
        }
    }
    
    public CompletableFuture<Map<String, Object>> getDependencies() {
        try {
            String currentDir = System.getProperty("user.dir");
            logger.info("Analyzing project dependencies for: {}", currentDir);
            
            Map<String, Object> dependencies = analyzeDependencies(currentDir);
            
            return CompletableFuture.completedFuture(Map.of(
                "contents", List.of(Map.of(
                    "type", "text",
                    "text", "Project dependencies analysis completed"
                )),
                "dependencies", dependencies
            ));
            
        } catch (Exception e) {
            logger.error("Error analyzing dependencies", e);
            return CompletableFuture.completedFuture(createErrorResponse("Error analyzing dependencies: " + e.getMessage()));
        }
    }
    
    public CompletableFuture<Map<String, Object>> getConfiguration() {
        try {
            String currentDir = System.getProperty("user.dir");
            logger.info("Analyzing project configuration for: {}", currentDir);
            
            Map<String, Object> configuration = analyzeConfiguration(currentDir);
            
            return CompletableFuture.completedFuture(Map.of(
                "contents", List.of(Map.of(
                    "type", "text",
                    "text", "Project configuration analysis completed"
                )),
                "configuration", configuration
            ));
            
        } catch (Exception e) {
            logger.error("Error analyzing configuration", e);
            return CompletableFuture.completedFuture(createErrorResponse("Error analyzing configuration: " + e.getMessage()));
        }
    }
    
    private Map<String, Object> analyzeProjectStructure(String projectPath) throws IOException {
        Map<String, Object> structure = new HashMap<>();
        Path rootPath = Paths.get(projectPath);
        
        structure.put("rootPath", projectPath);
        structure.put("projectName", rootPath.getFileName().toString());
        
        // Detect project type
        String projectType = detectProjectType(rootPath);
        structure.put("projectType", projectType);
        
        // Analyze directory structure
        Map<String, Object> directories = new HashMap<>();
        
        if (Files.exists(rootPath.resolve("src"))) {
            directories.put("src", analyzeSourceStructure(rootPath.resolve("src")));
        }
        
        if (Files.exists(rootPath.resolve("test")) || Files.exists(rootPath.resolve("src/test"))) {
            directories.put("test", analyzeTestStructure(rootPath));
        }
        
        if (Files.exists(rootPath.resolve("target"))) {
            directories.put("target", Map.of("type", "build", "description", "Maven build output"));
        }
        
        if (Files.exists(rootPath.resolve("build"))) {
            directories.put("build", Map.of("type", "build", "description", "Gradle build output"));
        }
        
        structure.put("directories", directories);
        
        // Count files by type
        Map<String, Integer> fileCounts = countFilesByType(rootPath);
        structure.put("fileCounts", fileCounts);
        
        return structure;
    }
    
    private Map<String, Object> analyzeSourceStructure(Path srcPath) throws IOException {
        Map<String, Object> srcStructure = new HashMap<>();
        
        if (Files.exists(srcPath.resolve("main/java"))) {
            srcStructure.put("mainJava", analyzeJavaPackages(srcPath.resolve("main/java")));
        }
        
        if (Files.exists(srcPath.resolve("main/resources"))) {
            srcStructure.put("mainResources", analyzeResources(srcPath.resolve("main/resources")));
        }
        
        if (Files.exists(srcPath.resolve("test/java"))) {
            srcStructure.put("testJava", analyzeJavaPackages(srcPath.resolve("test/java")));
        }
        
        return srcStructure;
    }
    
    private Map<String, Object> analyzeJavaPackages(Path javaPath) throws IOException {
        Map<String, Object> packages = new HashMap<>();
        
        try (Stream<Path> paths = Files.walk(javaPath)) {
            paths.filter(Files::isDirectory)
                 .filter(path -> !path.equals(javaPath))
                 .forEach(packagePath -> {
                     String packageName = javaPath.relativize(packagePath).toString().replace(File.separator, ".");
                     try {
                         long javaFileCount = Files.list(packagePath)
                             .filter(path -> path.toString().endsWith(".java"))
                             .count();
                         
                         if (javaFileCount > 0) {
                             packages.put(packageName, Map.of(
                                 "javaFiles", javaFileCount,
                                 "path", packagePath.toString()
                             ));
                         }
                     } catch (IOException e) {
                         logger.warn("Error analyzing package: {}", packagePath, e);
                     }
                 });
        }
        
        return packages;
    }
    
    private Map<String, Object> analyzeResources(Path resourcesPath) throws IOException {
        Map<String, Object> resources = new HashMap<>();
        
        try (Stream<Path> paths = Files.list(resourcesPath)) {
            paths.forEach(path -> {
                String fileName = path.getFileName().toString();
                if (Files.isRegularFile(path)) {
                    String type = determineResourceType(fileName);
                    resources.put(fileName, Map.of(
                        "type", type,
                        "size", path.toFile().length()
                    ));
                }
            });
        }
        
        return resources;
    }
    
    private Map<String, Object> analyzeTestStructure(Path rootPath) throws IOException {
        Map<String, Object> testStructure = new HashMap<>();
        
        Path testPath = rootPath.resolve("src/test/java");
        if (!Files.exists(testPath)) {
            testPath = rootPath.resolve("test");
        }
        
        if (Files.exists(testPath)) {
            try (Stream<Path> paths = Files.walk(testPath)) {
                long testFileCount = paths.filter(path -> path.toString().endsWith("Test.java") || 
                                                           path.toString().endsWith("Tests.java"))
                                         .count();
                
                testStructure.put("testFileCount", testFileCount);
                testStructure.put("testPath", testPath.toString());
            }
        }
        
        return testStructure;
    }
    
    private String detectProjectType(Path rootPath) {
        if (Files.exists(rootPath.resolve("pom.xml"))) {
            return "Maven";
        } else if (Files.exists(rootPath.resolve("build.gradle")) || Files.exists(rootPath.resolve("build.gradle.kts"))) {
            return "Gradle";
        } else if (Files.exists(rootPath.resolve("package.json"))) {
            return "Node.js";
        } else {
            return "Unknown";
        }
    }
    
    private Map<String, Integer> countFilesByType(Path rootPath) throws IOException {
        Map<String, Integer> counts = new HashMap<>();
        
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                 .forEach(path -> {
                     String fileName = path.getFileName().toString();
                     String extension = getFileExtension(fileName);
                     counts.merge(extension, 1, Integer::sum);
                 });
        }
        
        return counts;
    }
    
    private Map<String, Object> analyzeDependencies(String projectPath) throws IOException {
        Map<String, Object> dependencies = new HashMap<>();
        Path rootPath = Paths.get(projectPath);
        
        // Analyze Maven dependencies
        if (Files.exists(rootPath.resolve("pom.xml"))) {
            dependencies.put("maven", analyzeMavenDependencies(rootPath.resolve("pom.xml")));
        }
        
        // Analyze Gradle dependencies
        if (Files.exists(rootPath.resolve("build.gradle"))) {
            dependencies.put("gradle", analyzeGradleDependencies(rootPath.resolve("build.gradle")));
        } else if (Files.exists(rootPath.resolve("build.gradle.kts"))) {
            dependencies.put("gradle", analyzeGradleDependencies(rootPath.resolve("build.gradle.kts")));
        }
        
        return dependencies;
    }
    
    private Map<String, Object> analyzeMavenDependencies(Path pomPath) throws IOException {
        Map<String, Object> mavenInfo = new HashMap<>();
        
        try {
            String content = Files.readString(pomPath);
            
            // Simple parsing - in production, use proper XML parser
            mavenInfo.put("hasDependencies", content.contains("<dependencies>"));
            mavenInfo.put("hasSpringBoot", content.contains("spring-boot"));
            mavenInfo.put("hasJUnit", content.contains("junit"));
            mavenInfo.put("hasMockito", content.contains("mockito"));
            
            // Count dependencies (rough estimate)
            int dependencyCount = content.split("<dependency>").length - 1;
            mavenInfo.put("dependencyCount", dependencyCount);
            
        } catch (IOException e) {
            logger.warn("Error reading pom.xml", e);
            mavenInfo.put("error", "Unable to read pom.xml");
        }
        
        return mavenInfo;
    }
    
    private Map<String, Object> analyzeGradleDependencies(Path buildPath) throws IOException {
        Map<String, Object> gradleInfo = new HashMap<>();
        
        try {
            String content = Files.readString(buildPath);
            
            gradleInfo.put("hasDependencies", content.contains("dependencies"));
            gradleInfo.put("hasSpringBoot", content.contains("spring-boot"));
            gradleInfo.put("hasJUnit", content.contains("junit"));
            gradleInfo.put("hasMockito", content.contains("mockito"));
            
            // Count dependencies (rough estimate)
            int dependencyCount = content.split("implementation|testImplementation|api").length - 1;
            gradleInfo.put("dependencyCount", dependencyCount);
            
        } catch (IOException e) {
            logger.warn("Error reading build.gradle", e);
            gradleInfo.put("error", "Unable to read build.gradle");
        }
        
        return gradleInfo;
    }
    
    private Map<String, Object> analyzeConfiguration(String projectPath) throws IOException {
        Map<String, Object> configuration = new HashMap<>();
        Path rootPath = Paths.get(projectPath);
        
        // Look for common configuration files
        String[] configFiles = {
            "application.properties",
            "application.yml",
            "application.yaml",
            "config/application.properties",
            "src/main/resources/application.properties",
            "src/main/resources/application.yml"
        };
        
        List<Map<String, Object>> foundConfigs = new ArrayList<>();
        
        for (String configFile : configFiles) {
            Path configPath = rootPath.resolve(configFile);
            if (Files.exists(configPath)) {
                Map<String, Object> configInfo = new HashMap<>();
                configInfo.put("file", configFile);
                configInfo.put("type", getConfigType(configFile));
                configInfo.put("size", Files.size(configPath));
                foundConfigs.add(configInfo);
            }
        }
        
        configuration.put("configurationFiles", foundConfigs);
        
        return configuration;
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return "no-extension";
        }
        return fileName.substring(lastDot + 1);
    }
    
    private String determineResourceType(String fileName) {
        if (fileName.endsWith(".properties")) {
            return "properties";
        } else if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return "yaml";
        } else if (fileName.endsWith(".xml")) {
            return "xml";
        } else if (fileName.endsWith(".json")) {
            return "json";
        } else {
            return "other";
        }
    }
    
    private String getConfigType(String fileName) {
        if (fileName.contains(".properties")) {
            return "Properties";
        } else if (fileName.contains(".yml") || fileName.contains(".yaml")) {
            return "YAML";
        } else {
            return "Unknown";
        }
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        return Map.of("error", Map.of("code", -1, "message", message));
    }
}