package com.example.gitmigrator.model;

/**
 * Configuration for Git service integration (GitHub, GitLab, Bitbucket).
 */
public class GitServiceConfig {
    
    private String serviceName; // github, gitlab, bitbucket
    private String apiUrl;
    private String token;
    private String username;
    private String organization;
    private boolean useOAuth;
    private int maxRepositories = 100;
    
    // Default constructor
    public GitServiceConfig() {}
    
    // Constructor with service name and token
    public GitServiceConfig(String serviceName, String token) {
        this.serviceName = serviceName;
        this.token = token;
        this.apiUrl = getDefaultApiUrl(serviceName);
    }
    
    // Constructor with all basic fields
    public GitServiceConfig(String serviceName, String apiUrl, String token) {
        this.serviceName = serviceName;
        this.apiUrl = apiUrl;
        this.token = token;
    }
    
    // Getters and Setters
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { 
        this.serviceName = serviceName;
        if (this.apiUrl == null || this.apiUrl.isEmpty()) {
            this.apiUrl = getDefaultApiUrl(serviceName);
        }
    }
    
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    
    public boolean isUseOAuth() { return useOAuth; }
    public void setUseOAuth(boolean useOAuth) { this.useOAuth = useOAuth; }
    
    public int getMaxRepositories() { return maxRepositories; }
    public void setMaxRepositories(int maxRepositories) { this.maxRepositories = maxRepositories; }
    
    // Utility methods
    public static String getDefaultApiUrl(String serviceName) {
        if (serviceName == null) return null;
        
        switch (serviceName.toLowerCase()) {
            case "github":
                return "https://api.github.com";
            case "gitlab":
                return "https://gitlab.com/api/v4";
            case "bitbucket":
                return "https://api.bitbucket.org/2.0";
            default:
                return null;
        }
    }
    
    public String getRepositoriesEndpoint() {
        if (apiUrl == null || serviceName == null) return null;
        
        switch (serviceName.toLowerCase()) {
            case "github":
                if (organization != null && !organization.isEmpty()) {
                    return apiUrl + "/orgs/" + organization + "/repos";
                } else {
                    return apiUrl + "/user/repos";
                }
            case "gitlab":
                if (organization != null && !organization.isEmpty()) {
                    return apiUrl + "/groups/" + organization + "/projects";
                } else {
                    return apiUrl + "/projects?membership=true";
                }
            case "bitbucket":
                if (organization != null && !organization.isEmpty()) {
                    return apiUrl + "/repositories/" + organization;
                } else if (username != null && !username.isEmpty()) {
                    return apiUrl + "/repositories/" + username;
                } else {
                    return apiUrl + "/repositories";
                }
            default:
                return apiUrl;
        }
    }
    
    public boolean isValid() {
        return serviceName != null && !serviceName.isEmpty() &&
               apiUrl != null && !apiUrl.isEmpty() &&
               token != null && !token.isEmpty();
    }
    
    @Override
    public String toString() {
        return "GitServiceConfig{" +
                "serviceName='" + serviceName + '\'' +
                ", apiUrl='" + apiUrl + '\'' +
                ", username='" + username + '\'' +
                ", organization='" + organization + '\'' +
                ", maxRepositories=" + maxRepositories +
                '}';
    }
}