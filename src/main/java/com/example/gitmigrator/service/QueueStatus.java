package com.example.gitmigrator.service;

import java.time.LocalDateTime;

/**
 * Status information for the migration queue.
 */
public class QueueStatus {
    
    private int totalItems;
    private int pendingItems;
    private int processingItems;
    private int completedItems;
    private int failedItems;
    private boolean isProcessing;
    private boolean isPaused;
    private int maxConcurrent;
    private LocalDateTime lastProcessedTime;
    private String currentOperation;
    
    // Default constructor
    public QueueStatus() {}
    
    // Constructor with basic info
    public QueueStatus(int totalItems, int pendingItems, int processingItems, 
                      int completedItems, int failedItems) {
        this.totalItems = totalItems;
        this.pendingItems = pendingItems;
        this.processingItems = processingItems;
        this.completedItems = completedItems;
        this.failedItems = failedItems;
    }
    
    // Getters and Setters
    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    
    public int getPendingItems() { return pendingItems; }
    public void setPendingItems(int pendingItems) { this.pendingItems = pendingItems; }
    
    public int getProcessingItems() { return processingItems; }
    public void setProcessingItems(int processingItems) { this.processingItems = processingItems; }
    
    public int getCompletedItems() { return completedItems; }
    public void setCompletedItems(int completedItems) { this.completedItems = completedItems; }
    
    public int getFailedItems() { return failedItems; }
    public void setFailedItems(int failedItems) { this.failedItems = failedItems; }
    
    public boolean isProcessing() { return isProcessing; }
    public void setProcessing(boolean processing) { isProcessing = processing; }
    
    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { isPaused = paused; }
    
    public int getMaxConcurrent() { return maxConcurrent; }
    public void setMaxConcurrent(int maxConcurrent) { this.maxConcurrent = maxConcurrent; }
    
    public LocalDateTime getLastProcessedTime() { return lastProcessedTime; }
    public void setLastProcessedTime(LocalDateTime lastProcessedTime) { this.lastProcessedTime = lastProcessedTime; }
    
    public String getCurrentOperation() { return currentOperation; }
    public void setCurrentOperation(String currentOperation) { this.currentOperation = currentOperation; }
    
    // Convenience methods
    public int getActiveItems() {
        return pendingItems + processingItems;
    }
    
    public int getFinishedItems() {
        return completedItems + failedItems;
    }
    
    public double getCompletionPercentage() {
        if (totalItems == 0) return 0.0;
        return (double) getFinishedItems() / totalItems * 100.0;
    }
    
    public double getSuccessRate() {
        int finished = getFinishedItems();
        if (finished == 0) return 0.0;
        return (double) completedItems / finished * 100.0;
    }
    
    @Override
    public String toString() {
        return "QueueStatus{" +
                "total=" + totalItems +
                ", pending=" + pendingItems +
                ", processing=" + processingItems +
                ", completed=" + completedItems +
                ", failed=" + failedItems +
                ", isProcessing=" + isProcessing +
                ", isPaused=" + isPaused +
                '}';
    }
}