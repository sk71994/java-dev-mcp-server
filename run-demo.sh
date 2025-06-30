#!/bin/bash

echo "🚀 Java Dev MCP Server Interactive Demo"
echo "======================================="
echo ""

# Set Java 17
export JAVA_HOME=$(jenv javahome)
echo "📋 Using Java: $(java -version 2>&1 | head -1)"
echo ""

echo "🔨 Building project..."
./gradlew clean build -q
echo "✅ Build complete!"
echo ""

echo "🚀 Starting MCP Server in background..."
java -jar build/libs/java-dev-mcp-server-1.0.0.jar &
MCP_PID=$!
echo "📋 MCP Server started with PID: $MCP_PID"

# Wait for server to start
sleep 2

echo ""
echo "🎯 Ready for demo! The MCP server is now running and can:"
echo "   ✅ Generate Spring Boot controllers"
echo "   ✅ Create JPA entities with validation"
echo "   ✅ Analyze Java classes for complexity"
echo "   ✅ Generate unit test templates"
echo "   ✅ Provide project structure insights"
echo ""
echo "💡 In IntelliJ, you can now integrate this MCP server with AI assistants"
echo "   to get intelligent Java development assistance!"
echo ""

read -p "Press Enter to stop the demo..."

echo "🛑 Stopping MCP Server..."
kill $MCP_PID
echo "✅ Demo complete!"