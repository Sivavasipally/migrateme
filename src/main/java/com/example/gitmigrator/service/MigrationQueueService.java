package com.example.gitmigrator.service;

import com.example.gitmigrator.model.MigrationConfiguration;
import com.example.gitmigrator.model.MigrationQueueItem;
import com.example.gitmigrator.model.MigrationResult;
import com.example.gitmigrator.model.RepositoryInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing migration queue operations.
 * Provides functionality for queue management, batch processing, and status tracking.
 */
public interface MigrationQueueService {
    
    /**
     * Adds a repository to the migration queue.
     * 
     * @param repository The repository to migrate
     * @param config The migration configuration
     * @return The queue item ID
     */
    String addToQueue(RepositoryInfo repository, MigrationConfiguration config);
    
    /**
     * Adds a repository to the queue with priority.
     * 
     * @param repository The repository to migrate
     * @param config The migration configuration
     * @param priority The priority (higher numbers = higher priority)
     * @return The queue item ID
     */
    String addToQueue(RepositoryInfo repository, MigrationConfiguration config, int priority);
    
    /**
     * Removes a repository from the queue.
     * 
     * @param queueItemId The queue item ID to remove
     * @return true if removed, false if not found or already processing
     */
    boolean removeFromQueue(String queueItemId);
    
    /**
     * Reorders the queue based on the provided list of queue item IDs.
     * 
     * @param orderedIds List of queue item IDs in desired order
     */
    void reorderQueue(List<String> orderedIds);
    
    /**
     * Processes all items in the queue.
     * 
     * @return CompletableFuture with list of migration results
     */
    CompletableFuture<List<MigrationResult>> processQueue();
    
    /**
     * Processes a specific number of items from the queue.
     * 
     * @param maxItems Maximum number of items to process
     * @return CompletableFuture with list of migration results
     */
    CompletableFuture<List<MigrationResult>> processQueue(int maxItems);
    
    /**
     * Pauses queue processing.
     */
    void pauseProcessing();
    
    /**
     * Resumes queue processing.
     */
    void resumeProcessing();
    
    /**
     * Cancels all pending queue items.
     */
    void cancelAllPending();
    
    /**
     * Cancels a specific queue item.
     * 
     * @param queueItemId The queue item ID to cancel
     * @return true if cancelled, false if not found or already completed
     */
    boolean cancelQueueItem(String queueItemId);
    
    /**
     * Gets the current queue status.
     * 
     * @return The queue status information
     */
    QueueStatus getQueueStatus();
    
    /**
     * Gets all queue items.
     * 
     * @return List of all queue items
     */
    List<MigrationQueueItem> getAllQueueItems();
    
    /**
     * Gets pending queue items (not yet processed).
     * 
     * @return List of pending queue items
     */
    List<MigrationQueueItem> getPendingQueueItems();
    
    /**
     * Gets completed queue items.
     * 
     * @return List of completed queue items
     */
    List<MigrationQueueItem> getCompletedQueueItems();
    
    /**
     * Gets a specific queue item by ID.
     * 
     * @param queueItemId The queue item ID
     * @return The queue item, or null if not found
     */
    MigrationQueueItem getQueueItem(String queueItemId);
    
    /**
     * Clears all completed queue items.
     */
    void clearCompletedItems();
    
    /**
     * Saves the current queue state to persistent storage.
     */
    void saveQueueState();
    
    /**
     * Loads the queue state from persistent storage.
     */
    void loadQueueState();
    
    /**
     * Sets the maximum number of concurrent migrations.
     * 
     * @param maxConcurrent Maximum concurrent migrations (1-10)
     */
    void setMaxConcurrentMigrations(int maxConcurrent);
    
    /**
     * Gets the maximum number of concurrent migrations.
     * 
     * @return Maximum concurrent migrations
     */
    int getMaxConcurrentMigrations();
    
    /**
     * Checks if the queue is currently processing.
     * 
     * @return true if processing, false otherwise
     */
    boolean isProcessing();
    
    /**
     * Checks if the queue is paused.
     * 
     * @return true if paused, false otherwise
     */
    boolean isPaused();
    
    /**
     * Gets the total number of items in the queue.
     * 
     * @return Total queue size
     */
    int getQueueSize();
    
    /**
     * Adds a queue event listener.
     * 
     * @param listener The listener to add
     */
    void addQueueEventListener(QueueEventListener listener);
    
    /**
     * Removes a queue event listener.
     * 
     * @param listener The listener to remove
     */
    void removeQueueEventListener(QueueEventListener listener);
}