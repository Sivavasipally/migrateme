package com.example.gitmigrator.service;

import com.example.gitmigrator.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Implementation of MigrationQueueService for managing migration queue operations.
 * Provides thread-safe queue management with persistence and event notifications.
 */
public class MigrationQueueServiceImpl implements MigrationQueueService {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationQueueServiceImpl.class);
    private static final String QUEUE_STATE_FILE = ".git-migrator/queue-state.json";
    
    private final List<MigrationQueueItem> queueItems = Collections.synchronizedList(new ArrayList<>());
    private final List<QueueEventListener> eventListeners = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;
    private final Path queueStateFile;
    
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicInteger maxConcurrent = new AtomicInteger(3);
    private final AtomicInteger currentlyProcessing = new AtomicInteger(0);
    
    private MigrationOrchestratorService migrationOrchestratorService;
    private CompletableFuture<List<MigrationResult>> currentProcessingFuture;
    
    public MigrationQueueServiceImpl() {
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "MigrationQueue-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Initialize queue state file
        String userHome = System.getProperty("user.home");
        this.queueStateFile = Paths.get(userHome, QUEUE_STATE_FILE);
        
        // Create directory if it doesn't exist
        try {
            Files.createDirectories(queueStateFile.getParent());
        } catch (IOException e) {
            logger.warn("Failed to create queue state directory", e);
        }
        
        // Load existing queue state
        loadQueueState();
    }
    
    public void setMigrationOrchestratorService(MigrationOrchestratorService migrationOrchestratorService) {
        this.migrationOrchestratorService = migrationOrchestratorService;
    }
    
    @Override
    public String addToQueue(RepositoryInfo repository, MigrationConfiguration config) {
        return addToQueue(repository, config, 0);
    }
    
    @Override
    public String addToQueue(RepositoryInfo repository, MigrationConfiguration config, int priority) {
        MigrationQueueItem queueItem = new MigrationQueueItem(repository, config, priority);
        
        synchronized (queueItems) {
            queueItems.add(queueItem);
            // Sort by priority (higher priority first)
            queueItems.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        }
        
        logger.info("Added repository to queue: {} (priority: {})", repository.getName(), priority);
        
        // Notify listeners
        notifyListeners(listener -> listener.onItemAdded(queueItem));
        notifyStatusChanged();
        
        // Auto-save queue state
        saveQueueState();
        
        return queueItem.getId();
    }
    
    @Override
    public boolean removeFromQueue(String queueItemId) {
        synchronized (queueItems) {
            MigrationQueueItem item = findQueueItem(queueItemId);
            if (item == null) {
                return false;
            }
            
            // Don't remove if currently processing
            if (item.getStatus() == QueueItemStatus.PROCESSING) {
                logger.warn("Cannot remove queue item that is currently processing: {}", queueItemId);
                return false;
            }
            
            boolean removed = queueItems.remove(item);
            if (removed) {
                logger.info("Removed repository from queue: {}", item.getRepositoryName());
                notifyListeners(listener -> listener.onItemRemoved(item));
                notifyStatusChanged();
                saveQueueState();
            }
            
            return removed;
        }
    }
    
    @Override
    public void reorderQueue(List<String> orderedIds) {
        synchronized (queueItems) {
            Map<String, MigrationQueueItem> itemMap = queueItems.stream()
                    .collect(Collectors.toMap(MigrationQueueItem::getId, item -> item));
            
            List<MigrationQueueItem> reorderedItems = new ArrayList<>();
            
            // Add items in the specified order
            for (String id : orderedIds) {
                MigrationQueueItem item = itemMap.get(id);
                if (item != null && item.getStatus() == QueueItemStatus.PENDING) {
                    reorderedItems.add(item);
                    itemMap.remove(id);
                }
            }
            
            // Add any remaining pending items
            itemMap.values().stream()
                    .filter(item -> item.getStatus() == QueueItemStatus.PENDING)
                    .forEach(reorderedItems::add);
            
            // Add non-pending items at the end
            itemMap.values().stream()
                    .filter(item -> item.getStatus() != QueueItemStatus.PENDING)
                    .forEach(reorderedItems::add);
            
            queueItems.clear();
            queueItems.addAll(reorderedItems);
            
            logger.info("Reordered queue with {} items", orderedIds.size());
            notifyStatusChanged();
            saveQueueState();
        }
    }
    
    @Override
    public CompletableFuture<List<MigrationResult>> processQueue() {
        return processQueue(Integer.MAX_VALUE);
    }
    
    @Override
    public CompletableFuture<List<MigrationResult>> processQueue(int maxItems) {
        if (isProcessing.get()) {
            logger.warn("Queue is already processing");
            return currentProcessingFuture != null ? currentProcessingFuture : CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        if (migrationOrchestratorService == null) {
            logger.error("MigrationOrchestratorService not set");
            return CompletableFuture.failedFuture(new IllegalStateException("MigrationOrchestratorService not set"));
        }
        
        List<MigrationQueueItem> itemsToProcess = getPendingQueueItems().stream()
                .limit(maxItems)
                .collect(Collectors.toList());
        
        if (itemsToProcess.isEmpty()) {
            logger.info("No pending items to process");
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        isProcessing.set(true);
        notifyListeners(QueueEventListener::onQueueProcessingStarted);
        notifyStatusChanged();
        
        logger.info("Starting queue processing with {} items (max concurrent: {})", 
                   itemsToProcess.size(), maxConcurrent.get());
        
        currentProcessingFuture = CompletableFuture.supplyAsync(() -> {
            List<MigrationResult> allResults = Collections.synchronizedList(new ArrayList<>());
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            Semaphore semaphore = new Semaphore(maxConcurrent.get());
            
            for (MigrationQueueItem queueItem : itemsToProcess) {
                if (isPaused.get()) {
                    logger.info("Queue processing paused, stopping new items");
                    break;
                }
                
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        semaphore.acquire();
                        if (isPaused.get()) {
                            return;
                        }
                        
                        processQueueItem(queueItem, allResults);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Queue processing interrupted");
                    } finally {
                        semaphore.release();
                    }
                }, executorService);
                
                futures.add(future);
            }
            
            // Wait for all items to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            return allResults;
        }, executorService).whenComplete((results, throwable) -> {
            isProcessing.set(false);
            currentlyProcessing.set(0);
            
            if (throwable != null) {
                logger.error("Queue processing failed", throwable);
            } else {
                int successful = (int) results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
                int failed = results.size() - successful;
                
                logger.info("Queue processing completed: {} successful, {} failed", successful, failed);
                notifyListeners(listener -> listener.onQueueProcessingCompleted(results.size(), successful, failed));
            }
            
            notifyStatusChanged();
            saveQueueState();
        });
        
        return currentProcessingFuture;
    }
    
    private void processQueueItem(MigrationQueueItem queueItem, List<MigrationResult> allResults) {
        currentlyProcessing.incrementAndGet();
        queueItem.markAsProcessing();
        
        logger.info("Processing queue item: {}", queueItem.getRepositoryName());
        notifyListeners(listener -> listener.onItemStarted(queueItem));
        notifyStatusChanged();
        
        try {
            // Create migration request
            MigrationRequest request = new MigrationRequest();
            request.setRepositoryUrls(List.of(queueItem.getRepository().getCloneUrl()));
            request.setTargetFramework(queueItem.getConfiguration().getTargetPlatform());
            request.setIncludeHelm(queueItem.getConfiguration().isIncludeHelm());
            request.setIncludeDockerfile(queueItem.getConfiguration().isIncludeDockerfile());
            
            // Process migration
            List<MigrationResult> results = migrationOrchestratorService.migrateRepositories(request);
            MigrationResult result = results.isEmpty() ? createFailureResult(queueItem, "No results returned") : results.get(0);
            
            // Update queue item
            queueItem.markAsCompleted(result);
            allResults.add(result);
            
            logger.info("Completed queue item: {} - {}", queueItem.getRepositoryName(), 
                       result.isSuccess() ? "SUCCESS" : "FAILED");
            
            notifyListeners(listener -> listener.onItemCompleted(queueItem, result));
            
        } catch (Exception e) {
            logger.error("Failed to process queue item: {}", queueItem.getRepositoryName(), e);
            
            MigrationResult failureResult = createFailureResult(queueItem, e.getMessage());
            queueItem.markAsCompleted(failureResult);
            allResults.add(failureResult);
            
            notifyListeners(listener -> listener.onItemFailed(queueItem, e));
        } finally {
            currentlyProcessing.decrementAndGet();
            notifyStatusChanged();
        }
    }
    
    private MigrationResult createFailureResult(MigrationQueueItem queueItem, String errorMessage) {
        MigrationResult result = new MigrationResult();
        result.setRepositoryName(queueItem.getRepositoryName());
        result.setSuccess(false);
        result.setMessage("Queue processing failed: " + errorMessage);
        return result;
    }
    
    @Override
    public void pauseProcessing() {
        if (isPaused.compareAndSet(false, true)) {
            logger.info("Queue processing paused");
            notifyListeners(QueueEventListener::onQueueProcessingPaused);
            notifyStatusChanged();
        }
    }
    
    @Override
    public void resumeProcessing() {
        if (isPaused.compareAndSet(true, false)) {
            logger.info("Queue processing resumed");
            notifyListeners(QueueEventListener::onQueueProcessingResumed);
            notifyStatusChanged();
        }
    }
    
    @Override
    public void cancelAllPending() {
        synchronized (queueItems) {
            List<MigrationQueueItem> pendingItems = queueItems.stream()
                    .filter(item -> item.getStatus() == QueueItemStatus.PENDING)
                    .collect(Collectors.toList());
            
            for (MigrationQueueItem item : pendingItems) {
                item.setStatus(QueueItemStatus.CANCELLED);
                notifyListeners(listener -> listener.onItemCancelled(item));
            }
            
            if (!pendingItems.isEmpty()) {
                logger.info("Cancelled {} pending queue items", pendingItems.size());
                notifyStatusChanged();
                saveQueueState();
            }
        }
    }
    
    @Override
    public boolean cancelQueueItem(String queueItemId) {
        synchronized (queueItems) {
            MigrationQueueItem item = findQueueItem(queueItemId);
            if (item == null || item.isCompleted()) {
                return false;
            }
            
            if (item.getStatus() == QueueItemStatus.PROCESSING) {
                logger.warn("Cannot cancel queue item that is currently processing: {}", queueItemId);
                return false;
            }
            
            item.setStatus(QueueItemStatus.CANCELLED);
            logger.info("Cancelled queue item: {}", item.getRepositoryName());
            
            notifyListeners(listener -> listener.onItemCancelled(item));
            notifyStatusChanged();
            saveQueueState();
            
            return true;
        }
    }
    
    @Override
    public QueueStatus getQueueStatus() {
        synchronized (queueItems) {
            QueueStatus status = new QueueStatus();
            status.setTotalItems(queueItems.size());
            status.setProcessing(isProcessing.get());
            status.setPaused(isPaused.get());
            status.setMaxConcurrent(maxConcurrent.get());
            
            // Count items by status
            Map<QueueItemStatus, Long> statusCounts = queueItems.stream()
                    .collect(Collectors.groupingBy(MigrationQueueItem::getStatus, Collectors.counting()));
            
            status.setPendingItems(statusCounts.getOrDefault(QueueItemStatus.PENDING, 0L).intValue());
            status.setProcessingItems(statusCounts.getOrDefault(QueueItemStatus.PROCESSING, 0L).intValue());
            status.setCompletedItems(statusCounts.getOrDefault(QueueItemStatus.COMPLETED, 0L).intValue());
            status.setFailedItems(statusCounts.getOrDefault(QueueItemStatus.FAILED, 0L).intValue());
            
            // Set last processed time
            queueItems.stream()
                    .filter(item -> item.getProcessedDate() != null)
                    .max(Comparator.comparing(MigrationQueueItem::getProcessedDate))
                    .ifPresent(item -> status.setLastProcessedTime(item.getProcessedDate()));
            
            return status;
        }
    }
    
    @Override
    public List<MigrationQueueItem> getAllQueueItems() {
        synchronized (queueItems) {
            return new ArrayList<>(queueItems);
        }
    }
    
    @Override
    public List<MigrationQueueItem> getPendingQueueItems() {
        synchronized (queueItems) {
            return queueItems.stream()
                    .filter(item -> item.getStatus() == QueueItemStatus.PENDING)
                    .collect(Collectors.toList());
        }
    }
    
    @Override
    public List<MigrationQueueItem> getCompletedQueueItems() {
        synchronized (queueItems) {
            return queueItems.stream()
                    .filter(MigrationQueueItem::isCompleted)
                    .collect(Collectors.toList());
        }
    }
    
    @Override
    public MigrationQueueItem getQueueItem(String queueItemId) {
        return findQueueItem(queueItemId);
    }
    
    @Override
    public void clearCompletedItems() {
        synchronized (queueItems) {
            List<MigrationQueueItem> completedItems = getCompletedQueueItems();
            queueItems.removeAll(completedItems);
            
            if (!completedItems.isEmpty()) {
                logger.info("Cleared {} completed queue items", completedItems.size());
                notifyStatusChanged();
                saveQueueState();
            }
        }
    }
    
    @Override
    public void saveQueueState() {
        try {
            synchronized (queueItems) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(queueStateFile.toFile(), queueItems);
                logger.debug("Saved queue state with {} items", queueItems.size());
            }
        } catch (IOException e) {
            logger.error("Failed to save queue state", e);
        }
    }
    
    @Override
    public void loadQueueState() {
        if (!Files.exists(queueStateFile)) {
            logger.debug("No existing queue state file found");
            return;
        }
        
        try {
            List<MigrationQueueItem> loadedItems = objectMapper.readValue(
                    queueStateFile.toFile(), 
                    new TypeReference<List<MigrationQueueItem>>() {}
            );
            
            synchronized (queueItems) {
                queueItems.clear();
                queueItems.addAll(loadedItems);
                
                // Reset processing items to pending (in case of unexpected shutdown)
                queueItems.stream()
                        .filter(item -> item.getStatus() == QueueItemStatus.PROCESSING)
                        .forEach(item -> item.setStatus(QueueItemStatus.PENDING));
            }
            
            logger.info("Loaded queue state with {} items", loadedItems.size());
            notifyStatusChanged();
            
        } catch (IOException e) {
            logger.error("Failed to load queue state", e);
        }
    }
    
    @Override
    public void setMaxConcurrentMigrations(int maxConcurrent) {
        if (maxConcurrent < 1 || maxConcurrent > 10) {
            throw new IllegalArgumentException("Max concurrent migrations must be between 1 and 10");
        }
        
        this.maxConcurrent.set(maxConcurrent);
        logger.info("Set max concurrent migrations to: {}", maxConcurrent);
        notifyStatusChanged();
    }
    
    @Override
    public int getMaxConcurrentMigrations() {
        return maxConcurrent.get();
    }
    
    @Override
    public boolean isProcessing() {
        return isProcessing.get();
    }
    
    @Override
    public boolean isPaused() {
        return isPaused.get();
    }
    
    @Override
    public int getQueueSize() {
        return queueItems.size();
    }
    
    @Override
    public void addQueueEventListener(QueueEventListener listener) {
        eventListeners.add(listener);
    }
    
    @Override
    public void removeQueueEventListener(QueueEventListener listener) {
        eventListeners.remove(listener);
    }
    
    private MigrationQueueItem findQueueItem(String queueItemId) {
        synchronized (queueItems) {
            return queueItems.stream()
                    .filter(item -> item.getId().equals(queueItemId))
                    .findFirst()
                    .orElse(null);
        }
    }
    
    private void notifyListeners(java.util.function.Consumer<QueueEventListener> action) {
        eventListeners.forEach(listener -> {
            try {
                action.accept(listener);
            } catch (Exception e) {
                logger.warn("Queue event listener failed", e);
            }
        });
    }
    
    private void notifyStatusChanged() {
        QueueStatus status = getQueueStatus();
        notifyListeners(listener -> listener.onQueueStatusChanged(status));
    }
    
    public void shutdown() {
        logger.info("Shutting down migration queue service");
        
        // Cancel current processing
        if (currentProcessingFuture != null && !currentProcessingFuture.isDone()) {
            currentProcessingFuture.cancel(true);
        }
        
        // Save current state
        saveQueueState();
        
        // Shutdown executor
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}