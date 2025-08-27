package com.example.gitmigrator.service;

import com.example.gitmigrator.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MigrationQueueServiceImpl.
 */
class MigrationQueueServiceImplTest {
    
    private MigrationQueueService queueService;
    private MigrationOrchestratorService mockOrchestratorService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // Override system property to use temp directory for testing
        System.setProperty("user.home", tempDir.toString());
        
        queueService = new MigrationQueueServiceImpl();
        mockOrchestratorService = mock(MigrationOrchestratorService.class);
        ((MigrationQueueServiceImpl) queueService).setMigrationOrchestratorService(mockOrchestratorService);
    }
    
    @Test
    void testAddToQueue() {
        RepositoryInfo repo = createTestRepository("test-repo");
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        
        String queueItemId = queueService.addToQueue(repo, config);
        
        assertNotNull(queueItemId);
        assertEquals(1, queueService.getQueueSize());
        
        MigrationQueueItem queueItem = queueService.getQueueItem(queueItemId);
        assertNotNull(queueItem);
        assertEquals("test-repo", queueItem.getRepositoryName());
        assertEquals(QueueItemStatus.PENDING, queueItem.getStatus());
    }
    
    @Test
    void testAddToQueueWithPriority() {
        RepositoryInfo repo1 = createTestRepository("repo1");
        RepositoryInfo repo2 = createTestRepository("repo2");
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        
        // Add with different priorities
        String lowPriorityId = queueService.addToQueue(repo1, config, 1);
        String highPriorityId = queueService.addToQueue(repo2, config, 5);
        
        List<MigrationQueueItem> pendingItems = queueService.getPendingQueueItems();
        assertEquals(2, pendingItems.size());
        
        // Higher priority should be first
        assertEquals("repo2", pendingItems.get(0).getRepositoryName());
        assertEquals("repo1", pendingItems.get(1).getRepositoryName());
    }
    
    @Test
    void testRemoveFromQueue() {
        RepositoryInfo repo = createTestRepository("test-repo");
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        
        String queueItemId = queueService.addToQueue(repo, config);
        assertEquals(1, queueService.getQueueSize());
        
        boolean removed = queueService.removeFromQueue(queueItemId);
        assertTrue(removed);
        assertEquals(0, queueService.getQueueSize());
        
        // Try to remove non-existent item
        boolean removedAgain = queueService.removeFromQueue(queueItemId);
        assertFalse(removedAgain);
    }
    
    @Test
    void testReorderQueue() {
        RepositoryInfo repo1 = createTestRepository("repo1");
        RepositoryInfo repo2 = createTestRepository("repo2");
        RepositoryInfo repo3 = createTestRepository("repo3");
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        
        String id1 = queueService.addToQueue(repo1, config);
        String id2 = queueService.addToQueue(repo2, config);
        String id3 = queueService.addToQueue(repo3, config);
        
        // Reorder: 3, 1, 2
        queueService.reorderQueue(List.of(id3, id1, id2));
        
        List<MigrationQueueItem> pendingItems = queueService.getPendingQueueItems();
        assertEquals("repo3", pendingItems.get(0).getRepositoryName());
        assertEquals("repo1", pendingItems.get(1).getRepositoryName());
        assertEquals("repo2", pendingItems.get(2).getRepositoryName());
    }
    
    @Test
    void testQueueStatus() {
        RepositoryInfo repo1 = createTestRepository("repo1");
        RepositoryInfo repo2 = createTestRepository("repo2");
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        
        queueService.addToQueue(repo1, config);
        queueService.addToQueue(repo2, config);
        
        QueueStatus status = queueService.getQueueStatus();
        assertEquals(2, status.getTotalItems());
        assertEquals(2, status.getPendingItems());
        assertEquals(0, status.getProcessingItems());
        assertEquals(0, status.getCompletedItems());
        assertEquals(0, status.getFailedItems());
        assertFalse(status.isProcessing());
        assertFalse(status.isPaused());
    }
    
    @Test
    void testPauseAndResumeProcessing() {
        assertFalse(queueService.isPaused());
        
        queueService.pauseProcessing();
        assertTrue(queueService.isPaused());
        
        queueService.resumeProcessing();
        assertFalse(queueService.isPaused());
    }
    
    @Test
    void testCancelQueueItem() {
        RepositoryInfo repo = createTestRepository("test-repo");
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        
        String queueItemId = queueService.addToQueue(repo, config);
        
        boolean cancelled = queueService.cancelQueueItem(queueItemId);
        assertTrue(cancelled);
        
        MigrationQueueItem queueItem = queueService.getQueueItem(queueItemId);
        assertEquals(QueueItemStatus.CANCELLED, queueItem.getStatus());
        
        // Try to cancel already cancelled item
        boolean cancelledAgain = queueService.cancelQueueItem(queueItemId);
        assertFalse(cancelledAgain);
    }
    
    @Test
    void testCancelAllPending() {
        RepositoryInfo repo1 = createTestRepository("repo1");
        RepositoryInfo repo2 = createTestRepository("repo2");
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        
        queueService.addToQueue(repo1, config);
        queueService.addToQueue(repo2, config);
        
        assertEquals(2, queueService.getPendingQueueItems().size());
        
        queueService.cancelAllPending();
        
        assertEquals(0, queueService.getPendingQueueItems().size());
        assertEquals(2, queueService.getAllQueueItems().size()); // Items still exist but cancelled
        
        // Verify all items are cancelled
        queueService.getAllQueueItems().forEach(item -> 
            assertEquals(QueueItemStatus.CANCELLED, item.getStatus()));
    }
    
    @Test
    void testClearCompletedItems() {
        RepositoryInfo repo = createTestRepository("test-repo");
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        
        String queueItemId = queueService.addToQueue(repo, config);
        
        // Manually mark as completed for testing
        MigrationQueueItem queueItem = queueService.getQueueItem(queueItemId);
        MigrationResult result = new MigrationResult();
        result.setSuccess(true);
        queueItem.markAsCompleted(result);
        
        assertEquals(1, queueService.getCompletedQueueItems().size());
        assertEquals(1, queueService.getQueueSize());
        
        queueService.clearCompletedItems();
        
        assertEquals(0, queueService.getCompletedQueueItems().size());
        assertEquals(0, queueService.getQueueSize());
    }
    
    @Test
    void testMaxConcurrentMigrations() {
        assertEquals(3, queueService.getMaxConcurrentMigrations()); // Default value
        
        queueService.setMaxConcurrentMigrations(5);
        assertEquals(5, queueService.getMaxConcurrentMigrations());
        
        // Test invalid values
        assertThrows(IllegalArgumentException.class, () -> 
            queueService.setMaxConcurrentMigrations(0));
        assertThrows(IllegalArgumentException.class, () -> 
            queueService.setMaxConcurrentMigrations(11));
    }
    
    @Test
    void testQueueEventListener() {
        TestQueueEventListener listener = new TestQueueEventListener();
        queueService.addQueueEventListener(listener);
        
        RepositoryInfo repo = createTestRepository("test-repo");
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        
        String queueItemId = queueService.addToQueue(repo, config);
        
        // Verify event was fired
        assertTrue(listener.itemAddedCalled);
        assertNotNull(listener.lastAddedItem);
        assertEquals("test-repo", listener.lastAddedItem.getRepositoryName());
        
        // Test remove event
        queueService.removeFromQueue(queueItemId);
        assertTrue(listener.itemRemovedCalled);
        
        // Remove listener
        queueService.removeQueueEventListener(listener);
        
        // Add another item - listener should not be called
        listener.reset();
        queueService.addToQueue(createTestRepository("another-repo"), config);
        assertFalse(listener.itemAddedCalled);
    }
    
    @Test
    void testProcessQueueWithMockOrchestrator() throws Exception {
        // Setup mock to return successful result
        MigrationResult successResult = new MigrationResult();
        successResult.setSuccess(true);
        successResult.setRepositoryName("test-repo");
        when(mockOrchestratorService.migrateRepositories(any(MigrationRequest.class)))
            .thenReturn(List.of(successResult));
        
        RepositoryInfo repo = createTestRepository("test-repo");
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        
        queueService.addToQueue(repo, config);
        
        CompletableFuture<List<MigrationResult>> future = queueService.processQueue();
        List<MigrationResult> results = future.get(5, TimeUnit.SECONDS);
        
        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
        assertEquals("test-repo", results.get(0).getRepositoryName());
        
        // Verify orchestrator was called
        verify(mockOrchestratorService, times(1)).migrateRepositories(any(MigrationRequest.class));
    }
    
    private RepositoryInfo createTestRepository(String name) {
        RepositoryInfo repo = new RepositoryInfo();
        repo.setName(name);
        repo.setCloneUrl("https://github.com/test/" + name + ".git");
        repo.setDescription("Test repository: " + name);
        return repo;
    }
    
    private static class TestQueueEventListener implements QueueEventListener {
        boolean itemAddedCalled = false;
        boolean itemRemovedCalled = false;
        MigrationQueueItem lastAddedItem = null;
        
        @Override
        public void onItemAdded(MigrationQueueItem queueItem) {
            itemAddedCalled = true;
            lastAddedItem = queueItem;
        }
        
        @Override
        public void onItemRemoved(MigrationQueueItem queueItem) {
            itemRemovedCalled = true;
        }
        
        void reset() {
            itemAddedCalled = false;
            itemRemovedCalled = false;
            lastAddedItem = null;
        }
    }
}