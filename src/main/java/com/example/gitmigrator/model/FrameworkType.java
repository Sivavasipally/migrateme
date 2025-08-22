package com.example.gitmigrator.model;

/**
 * Enumeration of supported framework types for migration.
 */
public enum FrameworkType {
    SPRING_BOOT("Spring Boot"),
    REACT("React"),
    ANGULAR("Angular"),
    NODE_JS("Node.js"),
    MAVEN_JAVA("Maven Java"),
    GRADLE_JAVA("Gradle Java"),
    UNKNOWN("Unknown");
    
    private final String displayName;
    
    FrameworkType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}