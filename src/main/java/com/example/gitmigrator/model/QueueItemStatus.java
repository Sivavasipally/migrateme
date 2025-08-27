package com.example.gitmigrator.model;

/**
 * Enumeration of queue item status values.
 */
public enum QueueItemStatus {
    PENDING("Pending"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    QueueItemStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isActive() {
        return this == PENDING || this == PROCESSING;
    }
    
    public boolean isFinished() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}