# Testing the Java Dev MCP Server

## Overview
This document explains how to test the MCP server manually and how other developers can integrate it into their workflows.

## 1. Manual Testing with JSON-RPC

### Prerequisites
- Java 17+
- Built MCP server (`./gradlew build`)

### Start the Server
```bash
./gradlew bootRun
```

The server will start and listen for JSON-RPC messages on stdin/stdout.

### Test Messages

#### 1. Initialize the Server
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "clientInfo": {
      "name": "test-client",
      "version": "1.0.0"
    },
    "capabilities": {}
  }
}
```

Expected Response:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",
    "serverInfo": {
      "name": "Java Dev MCP Server",
      "version": "1.0.0"
    },
    "capabilities": {
      "tools": {"listChanged": true},
      "resources": {"subscribe": true, "listChanged": true},
      "prompts": {"listChanged": true}
    }
  }
}
```

#### 2. List Available Tools
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list",
  "params": {}
}
```

#### 3. Test Java Class Analyzer Tool
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "java-class-analyzer",
    "arguments": {
      "filePath": "/path/to/your/JavaClass.java",
      "includeMetrics": true
    }
  }
}
```

#### 4. Generate Spring Controller
```json
{
  "jsonrpc": "2.0",
  "id": 4,
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

#### 5. List Resources
```json
{
  "jsonrpc": "2.0",
  "id": 5,
  "method": "resources/list",
  "params": {}
}
```

#### 6. Read Project Structure
```json
{
  "jsonrpc": "2.0",
  "id": 6,
  "method": "resources/read",
  "params": {
    "uri": "java-project://current/structure"
  }
}
```

#### 7. List Prompts
```json
{
  "jsonrpc": "2.0",
  "id": 7,
  "method": "prompts/list",
  "params": {}
}
```

#### 8. Get Development Prompt
```json
{
  "jsonrpc": "2.0",
  "id": 8,
  "method": "prompts/get",
  "params": {
    "name": "create-spring-boot-service",
    "arguments": {
      "serviceName": "UserService",
      "includeDatabase": true
    }
  }
}
```

## 2. Testing with Node.js Script

Create a test script to interact with the MCP server:

```javascript
// test-mcp-client.js
const { spawn } = require('child_process');

class MCPTestClient {
    constructor() {
        this.server = null;
        this.messageId = 1;
    }

    start() {
        this.server = spawn('./gradlew', ['bootRun'], {
            stdio: ['pipe', 'pipe', 'inherit']
        });

        this.server.stdout.on('data', (data) => {
            console.log('Response:', data.toString());
        });

        // Wait for server to start
        setTimeout(() => this.runTests(), 2000);
    }

    sendMessage(method, params = {}) {
        const message = {
            jsonrpc: "2.0",
            id: this.messageId++,
            method,
            params
        };

        console.log('Sending:', JSON.stringify(message, null, 2));
        this.server.stdin.write(JSON.stringify(message) + '\n');
    }

    async runTests() {
        // Initialize
        this.sendMessage('initialize', {
            protocolVersion: "2024-11-05",
            clientInfo: { name: "test-client", version: "1.0.0" },
            capabilities: {}
        });

        await this.delay(1000);

        // List tools
        this.sendMessage('tools/list');

        await this.delay(1000);

        // Test controller generation
        this.sendMessage('tools/call', {
            name: 'spring-controller-generator',
            arguments: {
                entityName: 'Product',
                packageName: 'com.example.product',
                includeCrud: true
            }
        });
    }

    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    stop() {
        if (this.server) {
            this.server.kill();
        }
    }
}

const client = new MCPTestClient();
client.start();

// Graceful shutdown
process.on('SIGINT', () => {
    client.stop();
    process.exit();
});
```

## 3. Integration Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
Create integration tests that verify the complete MCP protocol flow:

```java
@SpringBootTest
class MCPServerIntegrationTest {
    
    @Test
    void shouldInitializeSuccessfully() {
        // Test initialization
    }
    
    @Test
    void shouldListToolsCorrectly() {
        // Test tools listing
    }
    
    @Test
    void shouldGenerateControllerCode() {
        // Test controller generation
    }
}
```

## 4. Performance Testing

### Load Testing
Test with multiple concurrent requests:

```bash
# Create multiple test files and run analyzer
for i in {1..10}; do
    echo '{"jsonrpc":"2.0","id":'$i',"method":"tools/call","params":{"name":"java-class-analyzer","arguments":{"filePath":"TestClass'$i'.java"}}}' | ./gradlew bootRun &
done
```

### Memory Testing
Monitor memory usage during extended operations:

```bash
# Run with JVM monitoring
./gradlew bootRun -Dspring.jmx.enabled=true
```

## 5. Error Testing

Test error conditions:

### Invalid JSON
```json
{
  "invalid": "json"
  "missing": "comma"
}
```

### Unknown Method
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "unknown/method",
  "params": {}
}
```

### Invalid Tool Parameters
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "java-class-analyzer",
    "arguments": {
      "filePath": "/nonexistent/file.java"
    }
  }
}
```

## 6. Debugging

### Enable Debug Logging
Add to `application.yml`:
```yaml
logging:
  level:
    com.javadev.mcp: DEBUG
    org.springframework: DEBUG
```

### Log Analysis
Monitor logs for:
- Request/response timing
- Error conditions
- Resource usage
- Protocol compliance

## 7. Validation Checklist

Before deployment, verify:

- [ ] All tools respond correctly
- [ ] All resources are accessible
- [ ] All prompts generate valid output
- [ ] Error handling works properly
- [ ] Performance meets requirements
- [ ] Memory usage is stable
- [ ] Protocol compliance is maintained