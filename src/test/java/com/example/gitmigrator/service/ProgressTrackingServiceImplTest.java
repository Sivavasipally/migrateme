package com.example.gitmigrator.service;

import com.example.gitmigrator.model.MigrationProgress;
import com.example.gitmigrator.model.MigrationStatus;
import com.example.gitmigrator.model.MigrationStep;
import com.example.gitmigrator.model.RepositoryProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ProgressTrackingServiceImplTest {

    private ProgressTrackingServiceImpl progressService;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Set custom progress directory for testing
        System.setProperty("user.home", tempDir.toString());
        progressService = new ProgressTrackingServiceImpl();
    }

    @Test
    void testStartOperation() {
        // Given
        String operationId = "test-operation-1";
        List<String> repositoryIds = Arrays.asList("repo1", "repo2", "repo3");

        // When
        MigrationProgress progress = progressService.startOperation(operationId, repositoryIds);

        // Then
        assertNotNull(progress);
        assertEquals(operationId, progress.getOperationId());
        assertEquals(3, progress.getTotalRepositories());
        assertEquals(MigrationStatus.QUEUED, progress.getOverallStatus());
        assertEquals(3, progress.getRepositoryProgress().size());
        
        // Verify repository progress initialized
        for (String repoId : repositoryIds) {
            RepositoryProgress repoProgress = progress.getRepositoryProgress(repoId);
            assertNotNull(repoProgress);
            assertEquals(repoId, repoProgress.getRepositoryId());
            assertEquals(MigrationStatus.QUEUED, repoProgress.getStatus());
        }
    }

    @Test
    void testUpdateRepositoryProgress() {
        // Given
        String operationId = "test-operation-2";
        List<String> repositoryIds = Arrays.asList("repo1");
        progressService.startOperation(operationId, repositoryIds);

        // When
        progressService.updateRepositoryProgress(operationId, "repo1", MigrationStep.CLONING);

        // Then
        MigrationProgress progress = progressService.getProgress(operationId);
        RepositoryProgress repoProgress = progress.getRepositoryProgress("repo1");
        assertEquals(MigrationStep.CLONING, repoProgress.getCurrentStep());
    }

    @Test
    void testCompleteRepositoryStep() {
        // Given
        String operationId = "test-operation-3";
        List<String> repositoryIds = Arrays.asList("repo1");
        progressService.startOperation(operationId, repositoryIds);

        // When
        progressService.completeRepositoryStep(operationId, "repo1", MigrationStep.CLONING);

        // Then
        MigrationProgress progress = progressService.getProgress(operationId);
        RepositoryProgress repoProgress = progress.getRepositoryProgress("repo1");
        assertTrue(repoProgress.getCompletedSteps().contains(MigrationStep.CLONING));
        assertTrue(repoProgress.getProgressPercentage() > 0);
    }

    @Test
    void testCompleteRepository() {
        // Given
        String operationId = "test-operation-4";
        List<String> repositoryIds = Arrays.asList("repo1");
        progressService.startOperation(operationId, repositoryIds);

        // When
        progressService.completeRepository(operationId, "repo1");

        // Then
        MigrationProgress progress = progressService.getProgress(operationId);
        RepositoryProgress repoProgress = progress.getRepositoryProgress("repo1");
        assertEquals(MigrationStatus.COMPLETED, repoProgress.getStatus());
        assertEquals(1, progress.getCompletedRepositories());
        assertNotNull(repoProgress.getEndTime());
    }

    @Test
    void testFailRepository() {
        // Given
        String operationId = "test-operation-5";
        List<String> repositoryIds = Arrays.asList("repo1");
        progressService.startOperation(operationId, repositoryIds);
        String errorMessage = "Test error message";

        // When
        progressService.failRepository(operationId, "repo1", errorMessage);

        // Then
        MigrationProgress progress = progressService.getProgress(operationId);
        RepositoryProgress repoProgress = progress.getRepositoryProgress("repo1");
        assertEquals(MigrationStatus.FAILED, repoProgress.getStatus());
        assertEquals(errorMessage, repoProgress.getErrorMessage());
        assertEquals(1, progress.getFailedRepositories());
        assertNotNull(repoProgress.getEndTime());
    }

    @Test
    void testAddRepositoryLog() {
        // Given
        String operationId = "test-operation-6";
        List<String> repositoryIds = Arrays.asList("repo1");
        progressService.startOperation(operationId, repositoryIds);
        String logMessage = "Test log message";

        // When
        progressService.addRepositoryLog(operationId, "repo1", logMessage);

        // Then
        MigrationProgress progress = progressService.getProgress(operationId);
        RepositoryProgress repoProgress = progress.getRepositoryProgress("repo1");
        assertTrue(repoProgress.getLogs().stream().anyMatch(log -> log.contains(logMessage)));
    }

    @Test
    void testAddGlobalLog() {
        // Given
        String operationId = "test-operation-7";
        List<String> repositoryIds = Arrays.asList("repo1");
        progressService.startOperation(operationId, repositoryIds);
        String logMessage = "Global test log message";

        // When
        progressService.addGlobalLog(operationId, logMessage);

        // Then
        MigrationProgress progress = progressService.getProgress(operationId);
        assertTrue(progress.getGlobalLogs().stream().anyMatch(log -> log.contains(logMessage)));
    }

    @Test
    void testCompleteOperation() {
        // Given
        String operationId = "test-operation-8";
        List<String> repositoryIds = Arrays.asList("repo1");
        progressService.startOperation(operationId, repositoryIds);

        // When
        progressService.completeOperation(operationId);

        // Then
        MigrationProgress progress = progressService.getProgress(operationId);
        assertEquals(MigrationStatus.COMPLETED, progress.getOverallStatus());
        assertNotNull(progress.getEndTime());
    }

    @Test
    void testProgressListener() throws InterruptedException {
        // Given
        String operationId = "test-operation-9";
        List<String> repositoryIds = Arrays.asList("repo1");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<MigrationProgress> receivedProgress = new AtomicReference<>();

        // When
        progressService.addProgressListener(operationId, progress -> {
            receivedProgress.set(progress);
            latch.countDown();
        });
        
        progressService.startOperation(operationId, repositoryIds);

        // Then
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(receivedProgress.get());
        assertEquals(operationId, receivedProgress.get().getOperationId());
    }

    @Test
    void testRemoveProgressListener() throws InterruptedException {
        // Given
        String operationId = "test-operation-10";
        List<String> repositoryIds = Arrays.asList("repo1");
        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<Integer> callCount = new AtomicReference<>(0);

        java.util.function.Consumer<MigrationProgress> listener = progress -> {
            callCount.updateAndGet(v -> v + 1);
            latch.countDown();
        };

        progressService.addProgressListener(operationId, listener);
        progressService.startOperation(operationId, repositoryIds);
        
        // Remove listener after first call
        progressService.removeProgressListener(operationId, listener);
        
        // When - trigger another update
        progressService.addGlobalLog(operationId, "Test message");

        // Then - should not receive second update
        assertFalse(latch.await(500, TimeUnit.MILLISECONDS));
        assertEquals(1, callCount.get().intValue());
    }

    @Test
    void testGetActiveOperations() {
        // Given
        String operationId1 = "test-operation-11";
        String operationId2 = "test-operation-12";
        List<String> repositoryIds = Arrays.asList("repo1");

        // When
        progressService.startOperation(operationId1, repositoryIds);
        progressService.startOperation(operationId2, repositoryIds);

        // Then
        List<String> activeOperations = progressService.getActiveOperations();
        assertEquals(2, activeOperations.size());
        assertTrue(activeOperations.contains(operationId1));
        assertTrue(activeOperations.contains(operationId2));
    }

    @Test
    void testPersistAndLoadProgress() {
        // Given
        String operationId = "test-operation-13";
        List<String> repositoryIds = Arrays.asList("repo1", "repo2");
        MigrationProgress originalProgress = progressService.startOperation(operationId, repositoryIds);
        
        // Add some progress
        progressService.updateRepositoryProgress(operationId, "repo1", MigrationStep.ANALYZING);
        progressService.addGlobalLog(operationId, "Test persistence");

        // When - persist and create new service instance
        progressService.persistProgress(operationId);
        ProgressTrackingServiceImpl newService = new ProgressTrackingServiceImpl();
        MigrationProgress loadedProgress = newService.loadPersistedProgress(operationId);

        // Then
        assertNotNull(loadedProgress);
        assertEquals(operationId, loadedProgress.getOperationId());
        assertEquals(2, loadedProgress.getTotalRepositories());
        assertTrue(loadedProgress.getGlobalLogs().stream().anyMatch(log -> log.contains("Test persistence")));
    }

    @Test
    void testCleanupOldOperations() {
        // Given
        String operationId = "test-operation-14";
        List<String> repositoryIds = Arrays.asList("repo1");
        progressService.startOperation(operationId, repositoryIds);
        progressService.completeOperation(operationId);

        // Verify operation exists
        assertNotNull(progressService.getProgress(operationId));

        // When - cleanup operations older than 0 hours (should remove all completed)
        progressService.cleanupOldOperations(0);

        // Then - operation should be removed
        assertNull(progressService.getProgress(operationId));
    }

    @Test
    void testOverallProgressPercentage() {
        // Given
        String operationId = "test-operation-15";
        List<String> repositoryIds = Arrays.asList("repo1", "repo2", "repo3");
        progressService.startOperation(operationId, repositoryIds);

        // When - complete one repository
        progressService.completeRepository(operationId, "repo1");

        // Then
        MigrationProgress progress = progressService.getProgress(operationId);
        assertEquals(33.33, progress.getOverallProgressPercentage(), 0.01);

        // When - fail one repository
        progressService.failRepository(operationId, "repo2", "Test error");

        // Then
        progress = progressService.getProgress(operationId);
        assertEquals(66.67, progress.getOverallProgressPercentage(), 0.01);
    }
}