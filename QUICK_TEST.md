# Quick Testing Guide

## Test Your MCP Server

### 1. Build and Test
```bash
./gradlew build
```

### 2. Test Tools List
```bash
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | java -jar build/libs/java-dev-mcp-server-1.0.0.jar
```

Expected output: JSON response with 4 available tools.

### 3. Test Controller Generation
```bash
echo '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"spring-controller-generator","arguments":{"entityName":"Product","packageName":"com.example.product","includeCrud":true}}}' | java -jar build/libs/java-dev-mcp-server-1.0.0.jar
```

### 4. Test Resource Access
```bash
echo '{"jsonrpc":"2.0","id":3,"method":"resources/list","params":{}}' | java -jar build/libs/java-dev-mcp-server-1.0.0.jar
```

### 5. Quick Validation
If all commands return JSON responses without errors, your MCP server is working correctly!

## What Developers Get

- **4 Tools**: Java analysis, Spring controller generation, JPA entity creation, unit test generation
- **3 Resources**: Project structure, dependencies, configuration insights  
- **2 Prompts**: Spring Boot service creation, CRUD implementation workflows
- **JSON-RPC Protocol**: Standard MCP communication for AI integration

## Next Steps

1. Read `INTEGRATION.md` for AI assistant setup
2. Check `DISTRIBUTION.md` for packaging options
3. Use `test-client.js` for interactive testing (requires Node.js fix)