package com.example.gitmigrator.model;

/**
 * Result of testing connection to a Git service.
 */
public class GitConnectionTestResult {
    
    private boolean successful;
    private String message;
    private long responseTimeMs;
    private String serviceName;
    private String apiVersion;
    private GitUserInfo userInfo;
    
    // Default constructor
    public GitConnectionTestResult() {}
    
    // Constructor with basic info
    public GitConnectionTestResult(boolean successful, String message) {
        this.successful = successful;
        this.message = message;
    }
    
    // Constructor with response time
    public GitConnectionTestResult(boolean successful, String message, long responseTimeMs) {
        this(successful, message);
        this.responseTimeMs = responseTimeMs;
    }
    
    // Getters and Setters
    public boolean isSuccessful() { return successful; }
    public void setSuccessful(boolean successful) { this.successful = successful; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
    
    public GitUserInfo getUserInfo() { return userInfo; }
    public void setUserInfo(GitUserInfo userInfo) { this.userInfo = userInfo; }
    
    @Override
    public String toString() {
        return "GitConnectionTestResult{" +
                "successful=" + successful +
                ", message='" + message + '\'' +
                ", responseTimeMs=" + responseTimeMs +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }
}