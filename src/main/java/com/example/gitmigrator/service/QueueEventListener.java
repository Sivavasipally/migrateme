package com.example.gitmigrator.service;

import com.example.gitmigrator.model.MigrationQueueItem;
import com.example.gitmigrator.model.MigrationResult;

/**
 * Event listener interface for migration queue events.
 */
public interface QueueEventListener {
    
    /**
     * Called when an item is added to the queue.
     * 
     * @param queueItem The added queue item
     */
    default void onItemAdded(MigrationQueueItem queueItem) {}
    
    /**
     * Called when an item is removed from the queue.
     * 
     * @param queueItem The removed queue item
     */
    default void onItemRemoved(MigrationQueueItem queueItem) {}
    
    /**
     * Called when an item starts processing.
     * 
     * @param queueItem The queue item that started processing
     */
    default void onItemStarted(MigrationQueueItem queueItem) {}
    
    /**
     * Called when an item completes processing.
     * 
     * @param queueItem The completed queue item
     * @param result The migration result
     */
    default void onItemCompleted(MigrationQueueItem queueItem, MigrationResult result) {}
    
    /**
     * Called when an item fails processing.
     * 
     * @param queueItem The failed queue item
     * @param error The error that occurred
     */
    default void onItemFailed(MigrationQueueItem queueItem, Exception error) {}
    
    /**
     * Called when an item is cancelled.
     * 
     * @param queueItem The cancelled queue item
     */
    default void onItemCancelled(MigrationQueueItem queueItem) {}
    
    /**
     * Called when queue processing starts.
     */
    default void onQueueProcessingStarted() {}
    
    /**
     * Called when queue processing completes.
     * 
     * @param totalProcessed Total number of items processed
     * @param successful Number of successful migrations
     * @param failed Number of failed migrations
     */
    default void onQueueProcessingCompleted(int totalProcessed, int successful, int failed) {}
    
    /**
     * Called when queue processing is paused.
     */
    default void onQueueProcessingPaused() {}
    
    /**
     * Called when queue processing is resumed.
     */
    default void onQueueProcessingResumed() {}
    
    /**
     * Called when the queue status changes.
     * 
     * @param status The new queue status
     */
    default void onQueueStatusChanged(QueueStatus status) {}
}