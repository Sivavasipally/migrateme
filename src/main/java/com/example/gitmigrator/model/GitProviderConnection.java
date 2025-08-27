package com.example.gitmigrator.model;

import java.util.Arrays;

/**
 * Represents a connection configuration to a Git provider for bulk repository discovery.
 * Handles URL parsing, authentication, and API endpoint derivation.
 */
public class GitProviderConnection {
    
    private final GitProviderType providerType;
    private final String baseUrl;
    private final String apiUrl;
    private final String username;
    private char[] password; // Secure password storage
    private final boolean verifySsl;
    
    public GitProviderConnection(GitProviderType providerType, String username, String password) {
        this(providerType, null, null, username, password, true);
    }
    
    public GitProviderConnection(GitProviderType providerType, String baseUrl, 
                               String username, String password) {
        this(providerType, baseUrl, null, username, password, true);
    }
    
    public GitProviderConnection(GitProviderType providerType, String baseUrl, 
                               String apiUrl, String username, String password, boolean verifySsl) {
        this.providerType = providerType;
        this.baseUrl = baseUrl != null ? baseUrl : providerType.getDefaultWebUrl();
        this.apiUrl = apiUrl != null ? apiUrl : deriveApiUrl(providerType, baseUrl);
        this.username = username;
        this.password = password != null ? password.toCharArray() : null;
        this.verifySsl = verifySsl;
    }
    
    /**
     * Derives API URL from base URL and provider type.
     */
    private String deriveApiUrl(GitProviderType providerType, String baseUrl) {
        if (baseUrl == null) {
            return providerType.getDefaultApiUrl();
        }
        
        // For self-hosted instances, derive API URL from base URL
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        
        switch (providerType) {
            case GITLAB:
                return cleanBaseUrl + "/api/v4";
            case GITHUB:
                // GitHub Enterprise
                return cleanBaseUrl + "/api/v3";
            case BITBUCKET:
                // Bitbucket Server
                return cleanBaseUrl + "/rest/api/1.0";
            default:
                return providerType.getDefaultApiUrl();
        }
    }
    
    public GitProviderType getProviderType() {
        return providerType;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public String getUsername() {
        return username;
    }
    
    /**
     * Gets the password as a character array.
     * Note: The returned array should be cleared after use.
     */
    public char[] getPassword() {
        return password != null ? Arrays.copyOf(password, password.length) : null;
    }
    
    /**
     * Gets the password as a string.
     * Note: Use with caution as strings are immutable and remain in memory.
     */
    public String getPasswordAsString() {
        return password != null ? new String(password) : null;
    }
    
    public boolean isVerifySsl() {
        return verifySsl;
    }
    
    /**
     * Checks if this connection is for a self-hosted instance.
     */
    public boolean isSelfHosted() {
        return !baseUrl.equals(providerType.getDefaultWebUrl());
    }
    
    /**
     * Validates the connection configuration.
     */
    public boolean isValid() {
        return providerType != null && 
               baseUrl != null && !baseUrl.trim().isEmpty() &&
               apiUrl != null && !apiUrl.trim().isEmpty() &&
               username != null && !username.trim().isEmpty() &&
               password != null && password.length > 0;
    }
    
    /**
     * Converts to GitServiceConfig for compatibility with existing services.
     */
    public GitServiceConfig toGitServiceConfig() {
        GitServiceConfig config = new GitServiceConfig();
        config.setServiceName(providerType.getServiceName());
        config.setApiUrl(apiUrl);
        config.setUsername(username);
        config.setToken(getPasswordAsString()); // Using password as token for basic auth
        return config;
    }
    
    /**
     * Clears sensitive data from memory.
     */
    public void clearCredentials() {
        if (password != null) {
            Arrays.fill(password, '\0');
            password = null;
        }
    }
    
    @Override
    public String toString() {
        return "GitProviderConnection{" +
                "providerType=" + providerType +
                ", baseUrl='" + baseUrl + '\'' +
                ", apiUrl='" + apiUrl + '\'' +
                ", username='" + username + '\'' +
                ", verifySsl=" + verifySsl +
                ", selfHosted=" + isSelfHosted() +
                '}';
    }
}