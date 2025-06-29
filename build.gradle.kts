plugins {
    java
    id("org.springframework.boot") version "3.1.5"
    id("io.spring.dependency-management") version "1.1.3"
    application
}

group = "com.javadev.mcp"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.github.javaparser:javaparser-core:3.25.1")
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("commons-io:commons-io:2.15.0")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

application {
    mainClass.set("com.javadev.mcp.JavaDevMcpServerApplication")
}

tasks.test {
    useJUnitPlatform()
}