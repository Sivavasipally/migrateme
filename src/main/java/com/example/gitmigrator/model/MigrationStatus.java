package com.example.gitmigrator.model;

/**
 * Enumeration of migration status values for repositories.
 */
public enum MigrationStatus {
    NOT_STARTED("Not Started"),
    QUEUED("Queued"),
    CLONING("Cloning"),
    ANALYZING("Analyzing"),
    GENERATING("Generating"),
    VALIDATING("Validating"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    MigrationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isInProgress() {
        return this == QUEUED || this == CLONING || this == ANALYZING || 
               this == GENERATING || this == VALIDATING;
    }
    
    public boolean isCompleted() {
        return this == COMPLETED;
    }
    
    public boolean isFailed() {
        return this == FAILED || this == CANCELLED;
    }
}