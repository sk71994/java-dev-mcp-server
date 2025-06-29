# Java Backend Developer MCP Server

A comprehensive Model Context Protocol (MCP) server designed specifically for Java backend developers to enhance their daily development workflow.

## Features

### Tools (AI-Controlled Actions)
- **java-class-analyzer**: Analyze Java classes for patterns, dependencies, and potential issues
- **spring-controller-generator**: Generate Spring Boot REST controllers with proper annotations
- **jpa-entity-generator**: Create JPA entities from specifications
- **unit-test-generator**: Generate JUnit 5 test templates for existing classes

### Resources (Data Sources)
- **java-project://current/structure**: Current Java project structure and organization
- **java-project://current/dependencies**: Project dependencies and their versions
- **java-project://current/configuration**: Application configuration files

### Prompts (User-Invoked Templates)
- **create-spring-boot-service**: Step-by-step service creation with best practices
- **implement-crud-operations**: Generate complete CRUD operations for entities

## Requirements

- Java 17 or higher
- Gradle 8.x

## Building

```bash
./gradlew build
```

## Running

```bash
./gradlew bootRun
```

Or run the JAR directly:

```bash
java -jar build/libs/java-dev-mcp-server.jar
```

## Usage

The server communicates via JSON-RPC over stdio. Example tool usage:

```json
{
  "method": "tools/call",
  "params": {
    "name": "spring-controller-generator",
    "arguments": {
      "entityName": "User",
      "packageName": "com.example.user",
      "includeCrud": true,
      "useResponseEntity": true
    }
  }
}
```

## Integration

This MCP server can be integrated with AI assistants like Claude to provide Java development assistance.

## License

MIT License