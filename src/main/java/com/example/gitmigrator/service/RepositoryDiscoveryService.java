package com.example.gitmigrator.service;

import com.example.gitmigrator.model.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for discovering repositories from Git providers for bulk operations.
 * Focuses on bulk repository discovery with filtering and search capabilities.
 * Complements the existing GitServiceIntegration for enhanced repository discovery workflows.
 */
public interface RepositoryDiscoveryService {
    
    /**
     * Tests connection to the Git provider with given connection details.
     * 
     * @param connection The Git provider connection configuration
     * @return CompletableFuture that resolves to true if connection is successful
     */
    CompletableFuture<Boolean> testConnection(GitProviderConnection connection);
    
    /**
     * Discovers all repositories the user has access to from the Git provider.
     * 
     * @param connection The Git provider connection configuration
     * @return CompletableFuture that resolves to list of repository metadata
     */
    CompletableFuture<List<RepositoryMetadata>> discoverAllRepositories(GitProviderConnection connection);
    
    /**
     * Discovers repositories within a specific organization/workspace/group.
     * 
     * @param connection The Git provider connection configuration
     * @param organizationId The organization/workspace/group identifier
     * @return CompletableFuture that resolves to list of repository metadata
     */
    CompletableFuture<List<RepositoryMetadata>> discoverOrganizationRepositories(
            GitProviderConnection connection, String organizationId);
    
    /**
     * Searches repositories by query string (name, description, etc.).
     * 
     * @param connection The Git provider connection configuration
     * @param query The search query
     * @return CompletableFuture that resolves to list of matching repository metadata
     */
    CompletableFuture<List<RepositoryMetadata>> searchRepositories(
            GitProviderConnection connection, String query);
    
    /**
     * Discovers repositories with advanced filtering capabilities.
     * 
     * @param connection The Git provider connection configuration
     * @param filter The repository filter criteria
     * @return CompletableFuture that resolves to list of filtered repository metadata
     */
    CompletableFuture<List<RepositoryMetadata>> discoverRepositoriesWithFilter(
            GitProviderConnection connection, RepositoryFilter filter);
    
    /**
     * Discovers repositories with pagination support for large datasets.
     * 
     * @param connection The Git provider connection configuration
     * @param page The page number (1-based)
     * @param perPage The number of repositories per page
     * @return CompletableFuture that resolves to paginated repository results
     */
    CompletableFuture<RepositoryDiscoveryPage> discoverRepositoriesPage(
            GitProviderConnection connection, int page, int perPage);
    
    /**
     * Gets the Git provider type this service supports.
     * 
     * @return The supported Git provider type
     */
    GitProviderType getSupportedProvider();
    
    /**
     * Validates if the connection configuration is supported by this service.
     * 
     * @param connection The connection to validate
     * @return true if the connection is supported, false otherwise
     */
    boolean supportsConnection(GitProviderConnection connection);
    
    /**
     * Gets the maximum number of repositories this service can discover in one call.
     * Used for pagination and performance optimization.
     * 
     * @return Maximum repositories per discovery call
     */
    int getMaxRepositoriesPerCall();
    
    /**
     * Checks if the service supports advanced filtering capabilities.
     * 
     * @return true if advanced filtering is supported
     */
    boolean supportsAdvancedFiltering();
}