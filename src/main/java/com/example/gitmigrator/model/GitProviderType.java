package com.example.gitmigrator.model;

/**
 * Enumeration of supported Git providers for bulk repository discovery.
 */
public enum GitProviderType {
    GITHUB("GitHub", "https://api.github.com", "https://github.com"),
    GITLAB("GitLab", "https://gitlab.com/api/v4", "https://gitlab.com"),
    BITBUCKET("Bitbucket", "https://api.bitbucket.org/2.0", "https://bitbucket.org");
    
    private final String displayName;
    private final String defaultApiUrl;
    private final String defaultWebUrl;
    
    GitProviderType(String displayName, String defaultApiUrl, String defaultWebUrl) {
        this.displayName = displayName;
        this.defaultApiUrl = defaultApiUrl;
        this.defaultWebUrl = defaultWebUrl;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDefaultApiUrl() {
        return defaultApiUrl;
    }
    
    public String getDefaultWebUrl() {
        return defaultWebUrl;
    }
    
    /**
     * Determines if this provider supports self-hosted instances.
     */
    public boolean supportsSelfHosted() {
        return this == GITLAB; // GitLab commonly supports self-hosted instances
    }
    
    /**
     * Gets the provider type from service name string.
     */
    public static GitProviderType fromServiceName(String serviceName) {
        if (serviceName == null) return null;
        
        switch (serviceName.toLowerCase()) {
            case "github":
                return GITHUB;
            case "gitlab":
                return GITLAB;
            case "bitbucket":
                return BITBUCKET;
            default:
                return null;
        }
    }
    
    /**
     * Gets the service name for compatibility with existing GitServiceConfig.
     */
    public String getServiceName() {
        return name().toLowerCase();
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}