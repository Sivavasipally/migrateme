package com.example.gitmigrator.service;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for Git operations using JGit library.
 * Handles repository cloning, authentication, and local file management.
 */
public class GitOperationService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitOperationService.class);
    
    private final String tempDirectory;
    
    public GitOperationService() {
        // Create temporary directory for cloned repositories
        this.tempDirectory = System.getProperty("java.io.tmpdir") + "/git-migrator";
        createTempDirectory();
    }
    
    /**
     * Clones a repository to a local temporary directory.
     * 
     * @param repoUrl The repository URL to clone
     * @param token Authentication token
     * @return File object pointing to the cloned repository directory
     * @throws RuntimeException if cloning fails
     */
    public File cloneRepository(String repoUrl, String token) {
        logger.info("Cloning repository: {}", repoUrl);
        
        // Extract repository name from URL
        String repoName = extractRepositoryName(repoUrl);
        String localPath = tempDirectory + "/" + repoName + "_" + System.currentTimeMillis();
        
        try {
            // Prepare credentials
            UsernamePasswordCredentialsProvider credentialsProvider = 
                new UsernamePasswordCredentialsProvider("git", token);
            
            // Clone the repository
            Git git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))
                    .setCredentialsProvider(credentialsProvider)
                    .setCloneAllBranches(false) // Only clone default branch for performance
                    .call();
            
            logger.info("Successfully cloned repository to: {}", localPath);
            
            // Close Git object to free resources
            git.close();
            
            return new File(localPath);
            
        } catch (GitAPIException e) {
            logger.error("Failed to clone repository {}: {}", repoUrl, e.getMessage());
            
            // Clean up partial clone if it exists
            cleanupDirectory(new File(localPath));
            
            throw new RuntimeException("Failed to clone repository: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts repository name from the URL.
     */
    private String extractRepositoryName(String repoUrl) {
        // Remove .git suffix if present
        String url = repoUrl.endsWith(".git") ? repoUrl.substring(0, repoUrl.length() - 4) : repoUrl;
        
        // Extract last part of the URL path
        String[] parts = url.split("/");
        String repoName = parts[parts.length - 1];
        
        // Sanitize repository name for use as directory name
        return repoName.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
    
    /**
     * Creates the temporary directory for cloned repositories.
     */
    private void createTempDirectory() {
        try {
            Path tempPath = Paths.get(tempDirectory);
            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath);
                logger.info("Created temporary directory: {}", tempDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to create temporary directory: {}", tempDirectory, e);
            throw new RuntimeException("Failed to create temporary directory", e);
        }
    }
    
    /**
     * Cleans up a cloned repository directory.
     * 
     * @param repoDir The repository directory to clean up
     */
    public void cleanupRepository(File repoDir) {
        if (repoDir != null && repoDir.exists()) {
            cleanupDirectory(repoDir);
        }
    }
    
    /**
     * Recursively deletes a directory and all its contents.
     */
    private void cleanupDirectory(File directory) {
        try {
            if (directory.exists()) {
                FileUtils.deleteDirectory(directory);
                logger.debug("Cleaned up directory: {}", directory.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.warn("Failed to cleanup directory {}: {}", directory.getAbsolutePath(), e.getMessage());
        }
    }
    
    /**
     * Cleans up all temporary files created during the current session.
     */
    public void cleanupAllTemporaryFiles() {
        try {
            File tempDir = new File(tempDirectory);
            if (tempDir.exists()) {
                FileUtils.cleanDirectory(tempDir);
                logger.info("Cleaned up all temporary files in: {}", tempDirectory);
            }
        } catch (IOException e) {
            logger.warn("Failed to cleanup temporary directory: {}", e.getMessage());
        }
    }
}