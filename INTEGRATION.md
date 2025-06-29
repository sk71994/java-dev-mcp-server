# Java Dev MCP Server - Integration Guide

## How Other Developers Can Use This MCP Server

This guide explains how developers can integrate and use the Java Dev MCP Server in their development workflows.

## 1. For AI Assistant Integration (Claude, GPT, etc.)

### Claude Desktop Integration

1. **Download the MCP Server**
   ```bash
   git clone <repository-url>
   cd java-dev-mcp-server
   ./gradlew build
   ```

2. **Configure Claude Desktop**
   
   Add to your Claude Desktop configuration file (`~/.config/claude-desktop/claude_desktop_config.json`):
   
   ```json
   {
     "mcpServers": {
       "java-dev": {
         "command": "java",
         "args": ["-jar", "/path/to/java-dev-mcp-server/build/libs/java-dev-mcp-server.jar"],
         "env": {
           "JAVA_HOME": "/usr/lib/jvm/java-17"
         }
       }
     }
   }
   ```

3. **Alternative: Using Gradle**
   ```json
   {
     "mcpServers": {
       "java-dev": {
         "command": "./gradlew",
         "args": ["bootRun"],
         "cwd": "/path/to/java-dev-mcp-server"
       }
     }
   }
   ```

### VS Code with MCP Extension

1. **Install MCP Extension**
   - Search for "Model Context Protocol" in VS Code marketplace

2. **Configure MCP Settings**
   ```json
   {
     "mcp.servers": [
       {
         "name": "java-dev",
         "command": "java",
         "args": ["-jar", "/path/to/java-dev-mcp-server.jar"],
         "capabilities": ["tools", "resources", "prompts"]
       }
     ]
   }
   ```

## 2. Direct API Integration

### Using curl/http clients

```bash
# Start the server
./gradlew bootRun

# Send JSON-RPC messages via stdin
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | java -jar build/libs/java-dev-mcp-server.jar
```

### Using Node.js Client

```javascript
const { MCPClient } = require('@modelcontextprotocol/client');

const client = new MCPClient({
  command: 'java',
  args: ['-jar', '/path/to/java-dev-mcp-server.jar']
});

// Initialize connection
await client.initialize();

// Generate Spring controller
const result = await client.callTool('spring-controller-generator', {
  entityName: 'User',
  packageName: 'com.example.user',
  includeCrud: true
});

console.log(result.controllerCode);
```

### Using Python Client

```python
import json
import subprocess
from typing import Dict, Any

class JavaDevMCPClient:
    def __init__(self, jar_path: str):
        self.process = subprocess.Popen(
            ['java', '-jar', jar_path],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        self.message_id = 1
    
    def send_message(self, method: str, params: Dict[str, Any] = None) -> Dict[str, Any]:
        message = {
            "jsonrpc": "2.0",
            "id": self.message_id,
            "method": method,
            "params": params or {}
        }
        self.message_id += 1
        
        self.process.stdin.write(json.dumps(message) + '\n')
        self.process.stdin.flush()
        
        response = self.process.stdout.readline()
        return json.loads(response)
    
    def generate_controller(self, entity_name: str, package_name: str) -> str:
        result = self.send_message('tools/call', {
            'name': 'spring-controller-generator',
            'arguments': {
                'entityName': entity_name,
                'packageName': package_name,
                'includeCrud': True
            }
        })
        return result['result']['controllerCode']

# Usage
client = JavaDevMCPClient('/path/to/java-dev-mcp-server.jar')
controller_code = client.generate_controller('Product', 'com.example.product')
print(controller_code)
```

## 3. IDE Plugin Integration

### IntelliJ IDEA Plugin Development

Create a custom plugin that integrates with the MCP server:

```java
// IntelliJ Plugin Action
public class GenerateControllerAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        
        // Get user input
        String entityName = Messages.showInputDialog("Entity name:", "Generate Controller", null);
        
        // Call MCP server
        MCPClient client = new MCPClient();
        String controllerCode = client.generateController(entityName, getPackageName(project));
        
        // Create file in project
        createJavaFile(project, entityName + "Controller.java", controllerCode);
    }
}
```

### VS Code Extension Development

```typescript
// VS Code Extension
import * as vscode from 'vscode';
import { MCPClient } from './mcp-client';

export function activate(context: vscode.ExtensionContext) {
    const client = new MCPClient();
    
    const generateController = vscode.commands.registerCommand(
        'javadev.generateController',
        async () => {
            const entityName = await vscode.window.showInputBox({
                prompt: 'Enter entity name'
            });
            
            if (entityName) {
                const result = await client.callTool('spring-controller-generator', {
                    entityName,
                    packageName: getCurrentPackage(),
                    includeCrud: true
                });
                
                await createNewFile(`${entityName}Controller.java`, result.controllerCode);
            }
        }
    );
    
    context.subscriptions.push(generateController);
}
```

## 4. CI/CD Integration

### GitHub Actions

```yaml
name: Java Code Generation
on:
  workflow_dispatch:
    inputs:
      entity_name:
        description: 'Entity name to generate'
        required: true

jobs:
  generate-code:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Download MCP Server
        run: |
          wget https://github.com/your-org/java-dev-mcp-server/releases/latest/download/java-dev-mcp-server.jar
      
      - name: Generate Controller
        run: |
          echo '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"spring-controller-generator","arguments":{"entityName":"${{ github.event.inputs.entity_name }}","packageName":"com.example","includeCrud":true}}}' | java -jar java-dev-mcp-server.jar > result.json
          
      - name: Extract Generated Code
        run: |
          jq -r '.result.controllerCode' result.json > src/main/java/com/example/${{ github.event.inputs.entity_name }}Controller.java
          
      - name: Commit Generated Code
        run: |
          git config --local user.email "action@github.com"
          git config --local user.name "GitHub Action"
          git add .
          git commit -m "Generate ${{ github.event.inputs.entity_name }}Controller"
          git push
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any
    
    parameters {
        string(name: 'ENTITY_NAME', defaultValue: 'Sample', description: 'Entity name')
        string(name: 'PACKAGE_NAME', defaultValue: 'com.example', description: 'Package name')
    }
    
    stages {
        stage('Generate Code') {
            steps {
                script {
                    sh '''
                        echo '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"spring-controller-generator","arguments":{"entityName":"'${ENTITY_NAME}'","packageName":"'${PACKAGE_NAME}'","includeCrud":true}}}' | java -jar java-dev-mcp-server.jar > result.json
                        
                        jq -r '.result.controllerCode' result.json > src/main/java/com/example/${ENTITY_NAME}Controller.java
                        jq -r '.result.instructions[]' result.json
                    '''
                }
            }
        }
        
        stage('Validate Generated Code') {
            steps {
                sh './gradlew compileJava'
            }
        }
    }
}
```

## 5. Docker Integration

### Dockerfile for MCP Server

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app
COPY build/libs/java-dev-mcp-server.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
```

### Docker Compose with Client

```yaml
version: '3.8'
services:
  mcp-server:
    build: .
    ports:
      - "8080:8080"
    
  mcp-client:
    image: node:18
    volumes:
      - ./client:/app
    working_dir: /app
    command: node client.js
    depends_on:
      - mcp-server
```

## 6. Distribution Methods

### Release Packaging

1. **Standalone JAR**
   ```bash
   ./gradlew bootJar
   # Distribute build/libs/java-dev-mcp-server.jar
   ```

2. **Native Binary (GraalVM)**
   ```bash
   ./gradlew nativeCompile
   # Distribute build/native/nativeCompile/java-dev-mcp-server
   ```

3. **Homebrew Formula**
   ```ruby
   class JavaDevMcpServer < Formula
     desc "MCP Server for Java Development"
     homepage "https://github.com/your-org/java-dev-mcp-server"
     url "https://github.com/your-org/java-dev-mcp-server/archive/v1.0.0.tar.gz"
     sha256 "..."
     
     depends_on "openjdk@17"
     
     def install
       system "./gradlew", "bootJar"
       libexec.install "build/libs/java-dev-mcp-server.jar"
       bin.write_jar_script libexec/"java-dev-mcp-server.jar", "java-dev-mcp"
     end
   end
   ```

4. **NPM Package**
   ```json
   {
     "name": "java-dev-mcp-server",
     "version": "1.0.0",
     "bin": {
       "java-dev-mcp": "./bin/java-dev-mcp.sh"
     },
     "files": ["bin/", "lib/"]
   }
   ```

## 7. Usage Examples

### Example 1: Rapid Prototyping
```bash
# Generate complete CRUD setup
echo '{"jsonrpc":"2.0","id":1,"method":"prompts/get","params":{"name":"implement-crud-operations","arguments":{"entityName":"User","includeValidation":true}}}' | java -jar java-dev-mcp-server.jar
```

### Example 2: Code Review Integration
```bash
# Analyze code quality
find src -name "*.java" | while read file; do
  echo '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"java-class-analyzer","arguments":{"filePath":"'$file'","includeMetrics":true}}}' | java -jar java-dev-mcp-server.jar
done
```

### Example 3: Project Scaffolding
```bash
# Generate project structure analysis
echo '{"jsonrpc":"2.0","id":1,"method":"resources/read","params":{"uri":"java-project://current/structure"}}' | java -jar java-dev-mcp-server.jar
```

## 8. Best Practices

### For Developers Using the MCP Server

1. **Initialize Once**: Always call `initialize` before using tools
2. **Handle Errors**: Check response for error conditions
3. **Resource Management**: Properly shutdown the server process
4. **Caching**: Cache server responses when appropriate
5. **Validation**: Validate generated code before committing

### For Integrators

1. **Version Pinning**: Pin to specific MCP server versions
2. **Health Checks**: Implement server health monitoring
3. **Graceful Degradation**: Handle server unavailability
4. **Security**: Run in isolated environments
5. **Logging**: Implement comprehensive logging for debugging

## 9. Troubleshooting

### Common Issues

1. **Server Won't Start**
   - Check Java version (requires 17+)
   - Verify JAR file integrity
   - Check port availability

2. **No Response from Server**
   - Verify JSON-RPC message format
   - Check server logs for errors
   - Ensure proper initialization

3. **Generated Code Issues**
   - Validate input parameters
   - Check file paths for analysis tools
   - Verify package name formats

### Debug Mode

Enable debug logging:
```bash
java -jar java-dev-mcp-server.jar --logging.level.com.javadev.mcp=DEBUG
```

This comprehensive integration guide should help developers understand how to use and integrate your Java Dev MCP Server effectively!