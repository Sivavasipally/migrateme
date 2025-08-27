package com.example.gitmigrator.service;

import com.example.gitmigrator.model.GitProviderConnection;
import com.example.gitmigrator.model.GitProviderType;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory service for managing and providing repository discovery services
 * for different Git providers (GitHub, GitLab, Bitbucket).
 */
public class RepositoryDiscoveryServiceFactory {
    
    private final Map<GitProviderType, RepositoryDiscoveryService> serviceMap;
    
    public RepositoryDiscoveryServiceFactory() {
        this.serviceMap = createDefaultServices();
    }
    
    public RepositoryDiscoveryServiceFactory(List<RepositoryDiscoveryService> discoveryServices) {
        this.serviceMap = discoveryServices.stream()
                .collect(Collectors.toMap(
                        RepositoryDiscoveryService::getSupportedProvider,
                        Function.identity()
                ));
    }
    
    private Map<GitProviderType, RepositoryDiscoveryService> createDefaultServices() {
        Map<GitProviderType, RepositoryDiscoveryService> services = new HashMap<>();
        services.put(GitProviderType.GITHUB, new GitHubRepositoryDiscoveryService());
        services.put(GitProviderType.GITLAB, new GitLabRepositoryDiscoveryService());
        services.put(GitProviderType.BITBUCKET, new BitbucketRepositoryDiscoveryService());
        return services;
    }
    
    /**
     * Gets the appropriate discovery service for the given provider type.
     * 
     * @param providerType The Git provider type
     * @return Optional containing the discovery service, or empty if not supported
     */
    public Optional<RepositoryDiscoveryService> getService(GitProviderType providerType) {
        return Optional.ofNullable(serviceMap.get(providerType));
    }
    
    /**
     * Gets the appropriate discovery service for the given connection.
     * 
     * @param connection The Git provider connection
     * @return Optional containing the discovery service, or empty if not supported
     */
    public Optional<RepositoryDiscoveryService> getService(GitProviderConnection connection) {
        if (connection == null || connection.getProviderType() == null) {
            return Optional.empty();
        }
        
        RepositoryDiscoveryService service = serviceMap.get(connection.getProviderType());
        if (service != null && service.supportsConnection(connection)) {
            return Optional.of(service);
        }
        
        return Optional.empty();
    }
    
    /**
     * Checks if a provider type is supported.
     * 
     * @param providerType The Git provider type to check
     * @return true if supported, false otherwise
     */
    public boolean isProviderSupported(GitProviderType providerType) {
        return serviceMap.containsKey(providerType);
    }
    
    /**
     * Gets all supported provider types.
     * 
     * @return List of supported Git provider types
     */
    public List<GitProviderType> getSupportedProviders() {
        return serviceMap.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all available discovery services.
     * 
     * @return List of all repository discovery services
     */
    public List<RepositoryDiscoveryService> getAllServices() {
        return serviceMap.values().stream()
                .collect(Collectors.toList());
    }
    
    /**
     * Validates if a connection is supported by any available service.
     * 
     * @param connection The connection to validate
     * @return true if the connection is supported, false otherwise
     */
    public boolean isConnectionSupported(GitProviderConnection connection) {
        return getService(connection).isPresent();
    }
    
    /**
     * Gets the maximum repositories per call for a given provider.
     * 
     * @param providerType The Git provider type
     * @return Maximum repositories per call, or default value if provider not supported
     */
    public int getMaxRepositoriesPerCall(GitProviderType providerType) {
        return getService(providerType)
                .map(RepositoryDiscoveryService::getMaxRepositoriesPerCall)
                .orElse(100); // Default fallback
    }
    
    /**
     * Checks if advanced filtering is supported for a given provider.
     * 
     * @param providerType The Git provider type
     * @return true if advanced filtering is supported, false otherwise
     */
    public boolean supportsAdvancedFiltering(GitProviderType providerType) {
        return getService(providerType)
                .map(RepositoryDiscoveryService::supportsAdvancedFiltering)
                .orElse(false);
    }
}