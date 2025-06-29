# Java Dev MCP Server - Distribution Guide

## Distribution and Packaging Instructions

This guide explains how to package and distribute the Java Dev MCP Server for other developers.

## 1. Building for Distribution

### Prerequisites
- Java 17+
- Gradle 8.4+

### Build Commands

#### Standard JAR
```bash
# Build executable JAR
./gradlew bootJar

# Output: build/libs/java-dev-mcp-server.jar
```

#### Fat JAR with All Dependencies
```bash
# Build fat JAR (includes all dependencies)
./gradlew build

# Verify JAR contents
jar -tf build/libs/java-dev-mcp-server.jar | head -20
```

#### Build with Version Info
```bash
# Build with specific version
./gradlew bootJar -Pversion=1.0.0

# Or set in build.gradle.kts
version = "1.0.0"
```

## 2. Testing the Distribution

### Local Testing
```bash
# Test the built JAR
java -jar build/libs/java-dev-mcp-server.jar

# Test with our test client
node test-client.js --test
```

### Automated Testing
```bash
# Run all tests before distribution
./gradlew test
./gradlew integrationTest
```

## 3. GitHub Release Distribution

### Create Release Script

```bash
#!/bin/bash
# release.sh

VERSION=$1
if [ -z "$VERSION" ]; then
    echo "Usage: ./release.sh <version>"
    exit 1
fi

echo "Building version $VERSION..."

# Update version
sed -i "s/version = \".*\"/version = \"$VERSION\"/" build.gradle.kts

# Build
./gradlew clean build bootJar

# Create release directory
mkdir -p releases/$VERSION
cp build/libs/java-dev-mcp-server.jar releases/$VERSION/
cp README.md TESTING.md INTEGRATION.md releases/$VERSION/

# Create zip archive
cd releases
zip -r java-dev-mcp-server-$VERSION.zip $VERSION/
cd ..

echo "Release $VERSION ready in releases/$VERSION/"
echo "Archive: releases/java-dev-mcp-server-$VERSION.zip"
```

### GitHub Actions Release Workflow

```yaml
name: Release
on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      
      - name: Build with Gradle
        run: ./gradlew clean build bootJar
      
      - name: Get version from tag
        id: get_version
        run: echo "VERSION=${GITHUB_REF#refs/tags/v}" >> $GITHUB_OUTPUT
      
      - name: Create release package
        run: |
          mkdir -p release-package
          cp build/libs/java-dev-mcp-server.jar release-package/
          cp README.md TESTING.md INTEGRATION.md release-package/
          cp test-client.js release-package/
          chmod +x release-package/test-client.js
          
          # Create startup scripts
          cat > release-package/start-server.sh << 'EOF'
          #!/bin/bash
          java -jar java-dev-mcp-server.jar "$@"
          EOF
          chmod +x release-package/start-server.sh
          
          cat > release-package/start-server.bat << 'EOF'
          @echo off
          java -jar java-dev-mcp-server.jar %*
          EOF
      
      - name: Create archives
        run: |
          cd release-package
          zip -r ../java-dev-mcp-server-${{ steps.get_version.outputs.VERSION }}.zip .
          tar -czf ../java-dev-mcp-server-${{ steps.get_version.outputs.VERSION }}.tar.gz .
          cd ..
      
      - name: Generate checksums
        run: |
          sha256sum java-dev-mcp-server-${{ steps.get_version.outputs.VERSION }}.zip > checksums.txt
          sha256sum java-dev-mcp-server-${{ steps.get_version.outputs.VERSION }}.tar.gz >> checksums.txt
          sha256sum release-package/java-dev-mcp-server.jar >> checksums.txt
      
      - name: Create Release
        uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: ${{ github.ref }}
          release_name: Java Dev MCP Server ${{ steps.get_version.outputs.VERSION }}
          body: |
            ## Java Dev MCP Server ${{ steps.get_version.outputs.VERSION }}
            
            ### Features
            - Spring Boot controller generation
            - JPA entity creation
            - Java class analysis
            - Unit test generation
            - Project structure insights
            
            ### Downloads
            - **JAR**: `java-dev-mcp-server.jar`
            - **ZIP Archive**: `java-dev-mcp-server-${{ steps.get_version.outputs.VERSION }}.zip`
            - **TAR Archive**: `java-dev-mcp-server-${{ steps.get_version.outputs.VERSION }}.tar.gz`
            
            ### Quick Start
            ```bash
            # Download and run
            wget https://github.com/your-org/java-dev-mcp-server/releases/download/v${{ steps.get_version.outputs.VERSION }}/java-dev-mcp-server.jar
            java -jar java-dev-mcp-server.jar
            
            # Or test with included client
            node test-client.js
            ```
            
            ### Requirements
            - Java 17 or higher
            - For testing: Node.js 16+
            
            See [INTEGRATION.md](INTEGRATION.md) for detailed usage instructions.
          draft: false
          prerelease: false
      
      - name: Upload JAR
        uses: actions/upload-release-asset@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          upload_url: ${{ steps.create_release.outputs.upload_url }}
          asset_path: ./release-package/java-dev-mcp-server.jar
          asset_name: java-dev-mcp-server.jar
          asset_content_type: application/java-archive
      
      - name: Upload ZIP
        uses: actions/upload-release-asset@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          upload_url: ${{ steps.create_release.outputs.upload_url }}
          asset_path: ./java-dev-mcp-server-${{ steps.get_version.outputs.VERSION }}.zip
          asset_name: java-dev-mcp-server-${{ steps.get_version.outputs.VERSION }}.zip
          asset_content_type: application/zip
      
      - name: Upload TAR
        uses: actions/upload-release-asset@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          upload_url: ${{ steps.create_release.outputs.upload_url }}
          asset_path: ./java-dev-mcp-server-${{ steps.get_version.outputs.VERSION }}.tar.gz
          asset_name: java-dev-mcp-server-${{ steps.get_version.outputs.VERSION }}.tar.gz
          asset_content_type: application/gzip
      
      - name: Upload Checksums
        uses: actions/upload-release-asset@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          upload_url: ${{ steps.create_release.outputs.upload_url }}
          asset_path: ./checksums.txt
          asset_name: checksums.txt
          asset_content_type: text/plain
```

## 4. Docker Distribution

### Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim

LABEL maintainer="your-email@example.com"
LABEL description="Java Dev MCP Server"
LABEL version="1.0.0"

WORKDIR /app

# Copy the JAR file
COPY build/libs/java-dev-mcp-server.jar app.jar

# Create non-root user
RUN groupadd -r mcpuser && useradd -r -g mcpuser mcpuser
RUN chown mcpuser:mcpuser /app
USER mcpuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | java -jar app.jar || exit 1

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
```

### Docker Compose
```yaml
version: '3.8'

services:
  java-dev-mcp:
    build: .
    image: java-dev-mcp-server:latest
    container_name: java-dev-mcp
    restart: unless-stopped
    stdin_open: true
    tty: true
    environment:
      - JAVA_OPTS=-Xmx512m -Xms256m
    volumes:
      - ./workspace:/workspace:ro
    working_dir: /workspace
```

### Docker Hub Publishing
```bash
# Build and tag
docker build -t your-username/java-dev-mcp-server:latest .
docker tag your-username/java-dev-mcp-server:latest your-username/java-dev-mcp-server:1.0.0

# Push to Docker Hub
docker push your-username/java-dev-mcp-server:latest
docker push your-username/java-dev-mcp-server:1.0.0
```

## 5. Package Manager Distribution

### Homebrew Formula

Create `Formula/java-dev-mcp-server.rb`:
```ruby
class JavaDevMcpServer < Formula
  desc "MCP Server for Java Backend Development"
  homepage "https://github.com/your-org/java-dev-mcp-server"
  url "https://github.com/your-org/java-dev-mcp-server/archive/v1.0.0.tar.gz"
  sha256 "your-sha256-hash"
  license "MIT"

  depends_on "openjdk@17"

  def install
    ENV["JAVA_HOME"] = Language::Java.java_home("17")
    system "./gradlew", "bootJar"
    
    libexec.install "build/libs/java-dev-mcp-server.jar"
    
    # Create wrapper script
    (bin/"java-dev-mcp").write <<~EOS
      #!/bin/bash
      export JAVA_HOME="#{Formula["openjdk@17"].opt_prefix}"
      exec "$JAVA_HOME/bin/java" -jar "#{libexec}/java-dev-mcp-server.jar" "$@"
    EOS
  end

  test do
    # Test that the server starts and responds
    require "open3"
    Open3.popen3("#{bin}/java-dev-mcp") do |stdin, stdout, stderr, wait_thr|
      stdin.puts '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'
      stdin.close
      
      output = stdout.read
      assert_match "protocolVersion", output
    end
  end
end
```

### NPM Package (for Node.js Integration)

Create `package.json`:
```json
{
  "name": "java-dev-mcp-server",
  "version": "1.0.0",
  "description": "MCP Server for Java Backend Development",
  "main": "index.js",
  "bin": {
    "java-dev-mcp": "./bin/java-dev-mcp.js"
  },
  "scripts": {
    "postinstall": "node scripts/download-jar.js",
    "test": "node test/test.js"
  },
  "keywords": ["mcp", "java", "development", "code-generation"],
  "author": "Your Name",
  "license": "MIT",
  "engines": {
    "node": ">=16.0.0"
  },
  "dependencies": {
    "child_process": "^1.0.0"
  },
  "files": [
    "bin/",
    "lib/",
    "scripts/",
    "README.md"
  ]
}
```

Create `bin/java-dev-mcp.js`:
```javascript
#!/usr/bin/env node

const { spawn } = require('child_process');
const path = require('path');

const jarPath = path.join(__dirname, '..', 'lib', 'java-dev-mcp-server.jar');

const child = spawn('java', ['-jar', jarPath, ...process.argv.slice(2)], {
  stdio: 'inherit'
});

child.on('exit', (code) => {
  process.exit(code);
});
```

## 6. Snap Package (Linux)

Create `snap/snapcraft.yaml`:
```yaml
name: java-dev-mcp-server
version: '1.0.0'
summary: MCP Server for Java Backend Development
description: |
  A Model Context Protocol server that provides tools for Java backend development,
  including Spring Boot code generation, JPA entities, and code analysis.

grade: stable
confinement: strict
base: core22

parts:
  java-dev-mcp:
    plugin: gradle
    source: .
    build-packages:
      - openjdk-17-jdk
    stage-packages:
      - openjdk-17-jre-headless
    override-build: |
      craftctl default
      ./gradlew bootJar
      cp build/libs/java-dev-mcp-server.jar $CRAFT_PART_INSTALL/

apps:
  java-dev-mcp-server:
    command: java -jar $SNAP/java-dev-mcp-server.jar
    plugs:
      - home
      - network
```

Build and publish:
```bash
snapcraft
snapcraft upload java-dev-mcp-server_1.0.0_amd64.snap --release=stable
```

## 7. Installation Scripts

### Universal Install Script

Create `install.sh`:
```bash
#!/bin/bash

set -e

VERSION="1.0.0"
INSTALL_DIR="/usr/local/bin"
JAR_NAME="java-dev-mcp-server.jar"
SCRIPT_NAME="java-dev-mcp"

echo "Installing Java Dev MCP Server v$VERSION..."

# Check Java
if ! command -v java &> /dev/null; then
    echo "Error: Java 17+ is required but not installed."
    echo "Please install Java and try again."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "Error: Java 17+ is required. Found version $JAVA_VERSION"
    exit 1
fi

# Download JAR
echo "Downloading $JAR_NAME..."
curl -L "https://github.com/your-org/java-dev-mcp-server/releases/download/v$VERSION/$JAR_NAME" -o "/tmp/$JAR_NAME"

# Verify download
if [ ! -f "/tmp/$JAR_NAME" ]; then
    echo "Error: Failed to download $JAR_NAME"
    exit 1
fi

# Install JAR
sudo mkdir -p "$INSTALL_DIR"
sudo cp "/tmp/$JAR_NAME" "$INSTALL_DIR/"

# Create wrapper script
sudo tee "$INSTALL_DIR/$SCRIPT_NAME" > /dev/null << EOF
#!/bin/bash
exec java -jar "$INSTALL_DIR/$JAR_NAME" "\$@"
EOF

sudo chmod +x "$INSTALL_DIR/$SCRIPT_NAME"

# Cleanup
rm "/tmp/$JAR_NAME"

echo "✅ Java Dev MCP Server installed successfully!"
echo "Usage: $SCRIPT_NAME"
echo "Test: echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}' | $SCRIPT_NAME"
```

### Windows Install Script

Create `install.ps1`:
```powershell
param(
    [string]$Version = "1.0.0",
    [string]$InstallDir = "$env:ProgramFiles\JavaDevMCP"
)

Write-Host "Installing Java Dev MCP Server v$Version..." -ForegroundColor Green

# Check Java
try {
    $javaVersion = & java -version 2>&1 | Select-String "version" | ForEach-Object { $_.ToString().Split('"')[1] }
    $majorVersion = $javaVersion.Split('.')[0]
    
    if ([int]$majorVersion -lt 17) {
        throw "Java 17+ required, found $javaVersion"
    }
} catch {
    Write-Error "Java 17+ is required but not found. Please install Java and try again."
    exit 1
}

# Create install directory
New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null

# Download JAR
$jarUrl = "https://github.com/your-org/java-dev-mcp-server/releases/download/v$Version/java-dev-mcp-server.jar"
$jarPath = Join-Path $InstallDir "java-dev-mcp-server.jar"

Write-Host "Downloading JAR..." -ForegroundColor Yellow
Invoke-WebRequest -Uri $jarUrl -OutFile $jarPath

# Create batch file
$batPath = Join-Path $InstallDir "java-dev-mcp.bat"
@"
@echo off
java -jar "$jarPath" %*
"@ | Out-File -FilePath $batPath -Encoding ASCII

# Add to PATH
$envPath = [Environment]::GetEnvironmentVariable("PATH", "Machine")
if ($envPath -notlike "*$InstallDir*") {
    [Environment]::SetEnvironmentVariable("PATH", "$envPath;$InstallDir", "Machine")
    Write-Host "Added $InstallDir to system PATH" -ForegroundColor Green
}

Write-Host "✅ Java Dev MCP Server installed successfully!" -ForegroundColor Green
Write-Host "Usage: java-dev-mcp" -ForegroundColor Yellow
Write-Host "Restart your terminal to use the command." -ForegroundColor Yellow
```

## 8. Release Checklist

Before creating a release:

- [ ] Update version in `build.gradle.kts`
- [ ] Update `README.md` with new features
- [ ] Run full test suite: `./gradlew test`
- [ ] Build and test JAR: `./gradlew bootJar && java -jar build/libs/java-dev-mcp-server.jar`
- [ ] Test with sample client: `node test-client.js --test`
- [ ] Update `CHANGELOG.md`
- [ ] Create Git tag: `git tag v1.0.0`
- [ ] Push tag: `git push origin v1.0.0`
- [ ] GitHub Actions will create the release automatically
- [ ] Test download and installation from release
- [ ] Update documentation if needed
- [ ] Announce release (if applicable)

## 9. Update Distribution

### Automated Updates

Create `updater.sh`:
```bash
#!/bin/bash

CURRENT_VERSION=$(java -jar java-dev-mcp-server.jar --version 2>/dev/null | grep "version" || echo "unknown")
LATEST_VERSION=$(curl -s https://api.github.com/repos/your-org/java-dev-mcp-server/releases/latest | grep "tag_name" | cut -d '"' -f 4)

if [ "$CURRENT_VERSION" != "$LATEST_VERSION" ]; then
    echo "New version available: $LATEST_VERSION"
    echo "Run: curl -sSL https://raw.githubusercontent.com/your-org/java-dev-mcp-server/main/install.sh | bash"
else
    echo "You have the latest version: $CURRENT_VERSION"
fi
```

This comprehensive distribution guide covers all major packaging and distribution methods for your Java Dev MCP Server!