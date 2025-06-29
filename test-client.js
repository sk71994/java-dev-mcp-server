#!/usr/bin/env node

const { spawn } = require('child_process');
const readline = require('readline');

class MCPTestClient {
    constructor() {
        this.server = null;
        this.messageId = 1;
        this.responses = new Map();
    }

    start() {
        console.log('🚀 Starting Java Dev MCP Server...');
        
        this.server = spawn('./gradlew', ['bootRun'], {
            stdio: ['pipe', 'pipe', 'inherit'],
            cwd: __dirname
        });

        this.server.stdout.on('data', (data) => {
            const lines = data.toString().split('\n').filter(line => line.trim());
            lines.forEach(line => {
                try {
                    const response = JSON.parse(line);
                    console.log('📨 Response:', JSON.stringify(response, null, 2));
                    this.responses.set(response.id, response);
                } catch (e) {
                    // Ignore non-JSON output (Spring Boot logs)
                }
            });
        });

        this.server.on('close', (code) => {
            console.log(`❌ Server exited with code ${code}`);
        });

        // Wait for server to start
        setTimeout(() => this.runInteractiveMode(), 3000);
    }

    sendMessage(method, params = {}) {
        const message = {
            jsonrpc: "2.0",
            id: this.messageId++,
            method,
            params
        };

        console.log('📤 Sending:', JSON.stringify(message, null, 2));
        this.server.stdin.write(JSON.stringify(message) + '\n');
        return message.id;
    }

    async runTests() {
        console.log('\n🧪 Running Automated Tests...\n');

        // Test 1: Initialize
        console.log('Test 1: Initialize Server');
        this.sendMessage('initialize', {
            protocolVersion: "2024-11-05",
            clientInfo: { name: "test-client", version: "1.0.0" },
            capabilities: {}
        });

        await this.delay(2000);

        // Test 2: List tools
        console.log('\nTest 2: List Available Tools');
        this.sendMessage('tools/list');

        await this.delay(2000);

        // Test 3: Generate Spring Controller
        console.log('\nTest 3: Generate Spring Controller');
        this.sendMessage('tools/call', {
            name: 'spring-controller-generator',
            arguments: {
                entityName: 'Product',
                packageName: 'com.example.product',
                includeCrud: true,
                useResponseEntity: true
            }
        });

        await this.delay(2000);

        // Test 4: Generate JPA Entity
        console.log('\nTest 4: Generate JPA Entity');
        this.sendMessage('tools/call', {
            name: 'jpa-entity-generator',
            arguments: {
                entityName: 'Product',
                packageName: 'com.example.product',
                tableName: 'products',
                fields: [
                    { name: 'name', type: 'String', nullable: 'false' },
                    { name: 'price', type: 'Double', nullable: 'false' },
                    { name: 'description', type: 'String', nullable: 'true' }
                ]
            }
        });

        await this.delay(2000);

        // Test 5: List Resources
        console.log('\nTest 5: List Resources');
        this.sendMessage('resources/list');

        await this.delay(2000);

        // Test 6: Read Project Structure
        console.log('\nTest 6: Read Project Structure');
        this.sendMessage('resources/read', {
            uri: 'java-project://current/structure'
        });

        await this.delay(2000);

        // Test 7: List Prompts
        console.log('\nTest 7: List Available Prompts');
        this.sendMessage('prompts/list');

        await this.delay(2000);

        // Test 8: Get Development Prompt
        console.log('\nTest 8: Get Spring Boot Service Creation Prompt');
        this.sendMessage('prompts/get', {
            name: 'create-spring-boot-service',
            arguments: {
                serviceName: 'ProductService',
                includeDatabase: true
            }
        });

        await this.delay(2000);

        console.log('\n✅ All tests completed! Check responses above.');
        this.runInteractiveMode();
    }

    runInteractiveMode() {
        console.log('\n🎮 Interactive Mode Started');
        console.log('Available commands:');
        console.log('  test          - Run all automated tests');
        console.log('  init          - Initialize server');
        console.log('  tools         - List tools');
        console.log('  resources     - List resources'); 
        console.log('  prompts       - List prompts');
        console.log('  controller    - Generate sample controller');
        console.log('  entity        - Generate sample entity');
        console.log('  analyze       - Analyze Java file (provide path)');
        console.log('  quit/exit     - Stop the server and exit');
        console.log('');

        const rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout,
            prompt: 'MCP> '
        });

        rl.prompt();

        rl.on('line', (input) => {
            const [command, ...args] = input.trim().split(' ');

            switch (command.toLowerCase()) {
                case 'test':
                    this.runTests();
                    break;

                case 'init':
                    this.sendMessage('initialize', {
                        protocolVersion: "2024-11-05",
                        clientInfo: { name: "interactive-client", version: "1.0.0" },
                        capabilities: {}
                    });
                    break;

                case 'tools':
                    this.sendMessage('tools/list');
                    break;

                case 'resources':
                    this.sendMessage('resources/list');
                    break;

                case 'prompts':
                    this.sendMessage('prompts/list');
                    break;

                case 'controller':
                    const entityName = args[0] || 'Sample';
                    this.sendMessage('tools/call', {
                        name: 'spring-controller-generator',
                        arguments: {
                            entityName,
                            packageName: `com.example.${entityName.toLowerCase()}`,
                            includeCrud: true,
                            useResponseEntity: true
                        }
                    });
                    break;

                case 'entity':
                    const entityName2 = args[0] || 'Sample';
                    this.sendMessage('tools/call', {
                        name: 'jpa-entity-generator',
                        arguments: {
                            entityName: entityName2,
                            packageName: `com.example.${entityName2.toLowerCase()}`,
                            tableName: `${entityName2.toLowerCase()}s`,
                            fields: [
                                { name: 'name', type: 'String', nullable: 'false' },
                                { name: 'value', type: 'String', nullable: 'true' }
                            ]
                        }
                    });
                    break;

                case 'analyze':
                    const filePath = args.join(' ');
                    if (!filePath) {
                        console.log('Please provide a file path: analyze /path/to/file.java');
                    } else {
                        this.sendMessage('tools/call', {
                            name: 'java-class-analyzer',
                            arguments: {
                                filePath,
                                includeMetrics: true
                            }
                        });
                    }
                    break;

                case 'quit':
                case 'exit':
                    console.log('👋 Goodbye!');
                    this.stop();
                    rl.close();
                    return;

                default:
                    if (command) {
                        console.log(`Unknown command: ${command}`);
                    }
                    break;
            }

            rl.prompt();
        });

        rl.on('close', () => {
            this.stop();
        });
    }

    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    stop() {
        if (this.server) {
            this.server.kill();
            this.server = null;
        }
    }
}

// Handle command line arguments
const args = process.argv.slice(2);
const client = new MCPTestClient();

if (args.includes('--test') || args.includes('-t')) {
    // Run automated tests only
    client.start();
    setTimeout(() => client.runTests(), 3000);
} else {
    // Start interactive mode
    client.start();
}

// Graceful shutdown
process.on('SIGINT', () => {
    console.log('\n👋 Shutting down...');
    client.stop();
    process.exit(0);
});

process.on('SIGTERM', () => {
    client.stop();
    process.exit(0);
});