package com.example.gitmigrator.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an item in the migration queue with priority and status tracking.
 */
public class MigrationQueueItem {
    
    private String id;
    private RepositoryInfo repository;
    private MigrationConfiguration configuration;
    private QueueItemStatus status;
    private int priority;
    private LocalDateTime addedDate;
    private LocalDateTime processedDate;
    private MigrationResult result;
    
    // Default constructor
    public MigrationQueueItem() {
        this.id = UUID.randomUUID().toString();
        this.status = QueueItemStatus.PENDING;
        this.priority = 0;
        this.addedDate = LocalDateTime.now();
    }
    
    // Constructor with repository and configuration
    public MigrationQueueItem(RepositoryInfo repository, MigrationConfiguration configuration) {
        this();
        this.repository = repository;
        this.configuration = configuration;
    }
    
    // Constructor with priority
    public MigrationQueueItem(RepositoryInfo repository, MigrationConfiguration configuration, int priority) {
        this(repository, configuration);
        this.priority = priority;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public RepositoryInfo getRepository() { return repository; }
    public void setRepository(RepositoryInfo repository) { this.repository = repository; }
    
    public MigrationConfiguration getConfiguration() { return configuration; }
    public void setConfiguration(MigrationConfiguration configuration) { this.configuration = configuration; }
    
    public QueueItemStatus getStatus() { return status; }
    public void setStatus(QueueItemStatus status) { this.status = status; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    
    public LocalDateTime getAddedDate() { return addedDate; }
    public void setAddedDate(LocalDateTime addedDate) { this.addedDate = addedDate; }
    
    public LocalDateTime getProcessedDate() { return processedDate; }
    public void setProcessedDate(LocalDateTime processedDate) { this.processedDate = processedDate; }
    
    public MigrationResult getResult() { return result; }
    public void setResult(MigrationResult result) { this.result = result; }
    
    // Convenience methods
    public String getRepositoryName() {
        return repository != null ? repository.getName() : "Unknown";
    }
    
    public boolean isCompleted() {
        return status == QueueItemStatus.COMPLETED || status == QueueItemStatus.FAILED;
    }
    
    public boolean isInProgress() {
        return status == QueueItemStatus.PROCESSING;
    }
    
    public void markAsProcessing() {
        this.status = QueueItemStatus.PROCESSING;
        this.processedDate = LocalDateTime.now();
    }
    
    public void markAsCompleted(MigrationResult result) {
        this.result = result;
        this.status = result.isSuccess() ? QueueItemStatus.COMPLETED : QueueItemStatus.FAILED;
    }
    
    @Override
    public String toString() {
        return "MigrationQueueItem{" +
                "id='" + id + '\'' +
                ", repository=" + (repository != null ? repository.getName() : "null") +
                ", status=" + status +
                ", priority=" + priority +
                '}';
    }
}