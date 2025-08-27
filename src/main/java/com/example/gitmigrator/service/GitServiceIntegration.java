package com.example.gitmigrator.service;

import com.example.gitmigrator.model.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for integrating with Git hosting services (GitHub, GitLab, Bitbucket).
 * Provides functionality to authenticate, browse, and fetch repositories from various Git services.
 */
public interface GitServiceIntegration {
    
    /**
     * Fetches repositories from the configured Git service.
     * 
     * @param config The Git service configuration
     * @return CompletableFuture with list of repositories
     */
    CompletableFuture<List<RepositoryInfo>> fetchRepositories(GitServiceConfig config);
    
    /**
     * Searches repositories in the configured Git service.
     * 
     * @param query The search query
     * @param config The Git service configuration
     * @return CompletableFuture with list of matching repositories
     */
    CompletableFuture<List<RepositoryInfo>> searchRepositories(String query, GitServiceConfig config);
    
    /**
     * Authenticates with the Git service using the provided configuration.
     * 
     * @param config The Git service configuration
     * @return true if authentication is successful, false otherwise
     */
    boolean authenticateService(GitServiceConfig config);
    
    /**
     * Gets a list of supported Git services.
     * 
     * @return List of supported service names
     */
    List<String> getSupportedServices();
    
    /**
     * Checks if a specific Git service is supported.
     * 
     * @param serviceName The name of the service to check
     * @return true if supported, false otherwise
     */
    boolean isServiceSupported(String serviceName);
    
    /**
     * Gets the default configuration for a specific Git service.
     * 
     * @param serviceName The name of the service
     * @return Default configuration, or null if service is not supported
     */
    GitServiceConfig getDefaultConfig(String serviceName);
    
    /**
     * Validates a Git service configuration.
     * 
     * @param config The configuration to validate
     * @return true if valid, false otherwise
     */
    boolean validateConfig(GitServiceConfig config);
    
    /**
     * Gets user information from the Git service.
     * 
     * @param config The Git service configuration
     * @return CompletableFuture with user information
     */
    CompletableFuture<GitUserInfo> getUserInfo(GitServiceConfig config);
    
    /**
     * Gets organizations/groups that the user has access to.
     * 
     * @param config The Git service configuration
     * @return CompletableFuture with list of organizations
     */
    CompletableFuture<List<GitOrganization>> getUserOrganizations(GitServiceConfig config);
    
    /**
     * Tests the connection to the Git service.
     * 
     * @param config The Git service configuration
     * @return CompletableFuture with connection test result
     */
    CompletableFuture<GitConnectionTestResult> testConnection(GitServiceConfig config);
    
    /**
     * Gets rate limit information for the Git service.
     * 
     * @param config The Git service configuration
     * @return CompletableFuture with rate limit information
     */
    CompletableFuture<GitRateLimitInfo> getRateLimitInfo(GitServiceConfig config);
    
    /**
     * Fetches repositories with pagination support.
     * 
     * @param config The Git service configuration
     * @param page The page number (1-based)
     * @param perPage The number of items per page
     * @return CompletableFuture with paginated repository results
     */
    CompletableFuture<GitRepositoryPage> fetchRepositoriesPage(GitServiceConfig config, int page, int perPage);
    
    /**
     * Gets detailed information about a specific repository.
     * 
     * @param config The Git service configuration
     * @param repositoryId The repository ID or full name
     * @return CompletableFuture with detailed repository information
     */
    CompletableFuture<RepositoryInfo> getRepositoryDetails(GitServiceConfig config, String repositoryId);
}