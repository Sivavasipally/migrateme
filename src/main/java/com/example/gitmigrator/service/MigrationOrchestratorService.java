package com.example.gitmigrator.service;

import com.example.gitmigrator.model.FrameworkType;
import com.example.gitmigrator.model.MigrationRequest;
import com.example.gitmigrator.model.MigrationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrator service that coordinates the complete migration process.
 * Manages the clone-analyze-transform workflow for multiple repositories.
 */
public class MigrationOrchestratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationOrchestratorService.class);
    
    private GitApiService gitApiService;
    private GitOperationService gitOperationService;
    private TransformationService transformationService;
    
    // Constructor for dependency injection
    public MigrationOrchestratorService() {
        // Services will be injected via setter methods
    }
    
    public void setServices(GitApiService gitApiService, GitOperationService gitOperationService, TransformationService transformationService) {
        this.gitApiService = gitApiService;
        this.gitOperationService = gitOperationService;
        this.transformationService = transformationService;
    }
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    /**
     * Orchestrates the migration of multiple repositories.
     * 
     * @param request Migration request containing repository URLs and authentication
     * @return List of migration results
     */
    public List<MigrationResult> migrateRepositories(MigrationRequest request) {
        logger.info("Starting migration for {} repositories", request.getRepositoryUrls().size());
        
        List<CompletableFuture<MigrationResult>> futures = new ArrayList<>();
        
        // Process each repository asynchronously
        for (String repoUrl : request.getRepositoryUrls()) {
            CompletableFuture<MigrationResult> future = CompletableFuture.supplyAsync(
                () -> migrateRepository(repoUrl, request.getToken()), 
                executorService
            );
            futures.add(future);
        }
        
        // Wait for all migrations to complete
        List<MigrationResult> results = new ArrayList<>();
        for (CompletableFuture<MigrationResult> future : futures) {
            try {
                MigrationResult result = future.get(10, TimeUnit.MINUTES); // 10-minute timeout per repo
                results.add(result);
            } catch (Exception e) {
                logger.error("Migration future failed", e);
                MigrationResult failureResult = new MigrationResult();
                failureResult.setRepositoryName("Unknown");
                failureResult.setSuccess(false);
                failureResult.setMessage("Migration timeout or error: " + e.getMessage());
                results.add(failureResult);
            }
        }
        
        logger.info("Completed migration process. Results: {} total", results.size());
        return results;
    }
    
    /**
     * Migrates a single repository through the complete clone-analyze-transform process.
     */
    private MigrationResult migrateRepository(String repoUrl, String token) {
        String repositoryName = extractRepositoryName(repoUrl);
        logger.info("Processing repository: {}", repositoryName);
        
        MigrationResult result = new MigrationResult();
        result.setRepositoryName(repositoryName);
        
        File repoDir = null;
        
        try {
            // Step 1: Clone repository
            logger.debug("Step 1: Cloning repository {}", repositoryName);
            repoDir = gitOperationService.cloneRepository(repoUrl, token);
            
            if (repoDir == null || !repoDir.exists()) {
                throw new RuntimeException("Failed to clone repository - directory does not exist");
            }
            
            // Step 2: Analyze and transform repository
            logger.debug("Step 2: Analyzing and transforming repository {}", repositoryName);
            FrameworkType framework = transformationService.processRepository(repoDir);
            
            // Step 3: Success
            result.setIdentifiedFramework(framework);
            result.setSuccess(true);
            result.setMessage(String.format("Successfully migrated %s (%s)", 
                                           repositoryName, framework.getDisplayName()));
            
            logger.info("Successfully processed repository: {} as {}", repositoryName, framework);
            
        } catch (Exception e) {
            logger.error("Failed to process repository {}: {}", repositoryName, e.getMessage(), e);
            
            result.setSuccess(false);
            result.setMessage("Migration failed: " + e.getMessage());
            result.setErrorDetails(getStackTrace(e));
            
        } finally {
            // Step 4: Cleanup
            if (repoDir != null) {
                try {
                    gitOperationService.cleanupRepository(repoDir);
                    logger.debug("Cleaned up repository directory: {}", repoDir.getAbsolutePath());
                } catch (Exception e) {
                    logger.warn("Failed to cleanup repository directory {}: {}", 
                               repoDir.getAbsolutePath(), e.getMessage());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Extracts repository name from URL.
     */
    private String extractRepositoryName(String repoUrl) {
        if (repoUrl == null || repoUrl.isEmpty()) {
            return "unknown";
        }
        
        // Remove .git suffix if present
        String url = repoUrl.endsWith(".git") ? repoUrl.substring(0, repoUrl.length() - 4) : repoUrl;
        
        // Extract last part of the URL path
        String[] parts = url.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "unknown";
    }
    
    /**
     * Converts exception stack trace to string for error details.
     */
    private String getStackTrace(Exception e) {
        if (e == null) return null;
        
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Cleanup method called on application shutdown.
     */
    public void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            gitOperationService.cleanupAllTemporaryFiles();
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}