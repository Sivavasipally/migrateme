package com.example.gitmigrator.service;

import com.example.gitmigrator.model.MigrationProgress;
import com.example.gitmigrator.model.MigrationStep;
import com.example.gitmigrator.model.RepositoryProgress;

import java.util.List;
import java.util.function.Consumer;

/**
 * Service interface for tracking migration progress
 */
public interface ProgressTrackingService {
    
    /**
     * Start tracking a new migration operation
     */
    MigrationProgress startOperation(String operationId, List<String> repositoryIds);
    
    /**
     * Get progress for a specific operation
     */
    MigrationProgress getProgress(String operationId);
    
    /**
     * Update progress for a specific repository
     */
    void updateRepositoryProgress(String operationId, String repositoryId, MigrationStep step);
    
    /**
     * Complete a step for a repository
     */
    void completeRepositoryStep(String operationId, String repositoryId, MigrationStep step);
    
    /**
     * Mark repository as completed
     */
    void completeRepository(String operationId, String repositoryId);
    
    /**
     * Mark repository as failed
     */
    void failRepository(String operationId, String repositoryId, String errorMessage);
    
    /**
     * Add log entry for a repository
     */
    void addRepositoryLog(String operationId, String repositoryId, String message);
    
    /**
     * Add global log entry for the operation
     */
    void addGlobalLog(String operationId, String message);
    
    /**
     * Complete the entire operation
     */
    void completeOperation(String operationId);
    
    /**
     * Register a progress listener
     */
    void addProgressListener(String operationId, Consumer<MigrationProgress> listener);
    
    /**
     * Remove a progress listener
     */
    void removeProgressListener(String operationId, Consumer<MigrationProgress> listener);
    
    /**
     * Get all active operations
     */
    List<String> getActiveOperations();
    
    /**
     * Clean up completed operations older than specified hours
     */
    void cleanupOldOperations(int hoursOld);
    
    /**
     * Persist progress to disk for recovery
     */
    void persistProgress(String operationId);
    
    /**
     * Load persisted progress from disk
     */
    MigrationProgress loadPersistedProgress(String operationId);
}