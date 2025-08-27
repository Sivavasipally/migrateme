package com.example.gitmigrator.service;

import com.example.gitmigrator.model.GitServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory for creating Git service integration instances.
 * Provides a centralized way to create and manage different Git service implementations.
 */
public class GitServiceFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(GitServiceFactory.class);
    
    private static final Map<String, GitServiceIntegration> serviceInstances = new HashMap<>();
    
    static {
        // Initialize service instances
        serviceInstances.put("github", new GitHubServiceIntegration());
        serviceInstances.put("gitlab", new GitLabServiceIntegration());
        serviceInstances.put("bitbucket", new BitbucketServiceIntegration());
    }
    
    /**
     * Creates a Git service integration instance for the specified service name.
     * 
     * @param serviceName The name of the Git service (github, gitlab, bitbucket)
     * @return GitServiceIntegration instance, or null if service is not supported
     */
    public static GitServiceIntegration createService(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            logger.warn("Service name cannot be null or empty");
            return null;
        }
        
        String normalizedServiceName = serviceName.toLowerCase().trim();
        GitServiceIntegration service = serviceInstances.get(normalizedServiceName);
        
        if (service == null) {
            logger.warn("Unsupported Git service: {}", serviceName);
        } else {
            logger.debug("Created Git service integration for: {}", serviceName);
        }
        
        return service;
    }
    
    /**
     * Creates a Git service integration instance based on the provided configuration.
     * 
     * @param config The Git service configuration
     * @return GitServiceIntegration instance, or null if service is not supported
     */
    public static GitServiceIntegration createService(GitServiceConfig config) {
        if (config == null) {
            logger.warn("Git service config cannot be null");
            return null;
        }
        
        return createService(config.getServiceName());
    }
    
    /**
     * Gets a list of all supported Git service names.
     * 
     * @return List of supported service names
     */
    public static List<String> getSupportedServices() {
        return serviceInstances.values().stream()
                .flatMap(service -> service.getSupportedServices().stream())
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * Checks if a specific Git service is supported.
     * 
     * @param serviceName The name of the service to check
     * @return true if supported, false otherwise
     */
    public static boolean isServiceSupported(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return false;
        }
        
        String normalizedServiceName = serviceName.toLowerCase().trim();
        return serviceInstances.containsKey(normalizedServiceName);
    }
    
    /**
     * Gets the default configuration for a specific Git service.
     * 
     * @param serviceName The name of the service
     * @return Default configuration, or null if service is not supported
     */
    public static GitServiceConfig getDefaultConfig(String serviceName) {
        GitServiceIntegration service = createService(serviceName);
        if (service != null) {
            return service.getDefaultConfig(serviceName);
        }
        return null;
    }
    
    /**
     * Validates a Git service configuration.
     * 
     * @param config The configuration to validate
     * @return true if valid, false otherwise
     */
    public static boolean validateConfig(GitServiceConfig config) {
        GitServiceIntegration service = createService(config);
        if (service != null) {
            return service.validateConfig(config);
        }
        return false;
    }
    
    /**
     * Gets all available service instances.
     * This method is primarily for testing purposes.
     * 
     * @return Map of service name to service instance
     */
    protected static Map<String, GitServiceIntegration> getServiceInstances() {
        return new HashMap<>(serviceInstances);
    }
    
    /**
     * Registers a custom Git service integration.
     * This allows for extending the factory with additional services.
     * 
     * @param serviceName The name of the service
     * @param serviceIntegration The service integration instance
     */
    public static void registerService(String serviceName, GitServiceIntegration serviceIntegration) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }
        
        if (serviceIntegration == null) {
            throw new IllegalArgumentException("Service integration cannot be null");
        }
        
        String normalizedServiceName = serviceName.toLowerCase().trim();
        serviceInstances.put(normalizedServiceName, serviceIntegration);
        
        logger.info("Registered custom Git service integration: {}", serviceName);
    }
    
    /**
     * Unregisters a Git service integration.
     * This allows for removing services from the factory.
     * 
     * @param serviceName The name of the service to unregister
     * @return true if the service was removed, false if it didn't exist
     */
    public static boolean unregisterService(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return false;
        }
        
        String normalizedServiceName = serviceName.toLowerCase().trim();
        GitServiceIntegration removed = serviceInstances.remove(normalizedServiceName);
        
        if (removed != null) {
            logger.info("Unregistered Git service integration: {}", serviceName);
            return true;
        }
        
        return false;
    }
}