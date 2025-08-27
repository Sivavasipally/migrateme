package com.example.gitmigrator.model;

/**
 * Enumeration of supported framework types for migration.
 */
public enum FrameworkType {
    // Java Frameworks
    SPRING_BOOT("Spring Boot"),
    SPRING_CLASSIC("Spring Framework"),
    MAVEN_JAVA("Maven Java"),
    GRADLE_JAVA("Gradle Java"),
    
    // JavaScript/TypeScript Frameworks
    REACT("React"),
    ANGULAR("Angular"),
    ANGULAR_JS("AngularJS"),
    NODE_JS("Node.js"),
    
    // Python Frameworks
    FLASK("Flask"),
    FASTAPI("FastAPI"),
    PYTHON("Python"),
    
    // Multi-stack/Monorepo
    MULTI_STACK("Multi-Stack"),
    MONOREPO("Monorepo"),
    
    UNKNOWN("Unknown");
    
    private final String displayName;
    
    FrameworkType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}