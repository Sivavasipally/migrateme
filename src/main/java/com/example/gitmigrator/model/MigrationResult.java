package com.example.gitmigrator.model;

/**
 * Result object for migration operations.
 */
public class MigrationResult {
    
    private String repositoryName;
    private FrameworkType identifiedFramework;
    private boolean success;
    private String message;
    private String errorDetails;
    
    public MigrationResult() {}
    
    public MigrationResult(String repositoryName, FrameworkType identifiedFramework, 
                          boolean success, String message) {
        this.repositoryName = repositoryName;
        this.identifiedFramework = identifiedFramework;
        this.success = success;
        this.message = message;
    }
    
    // Getters and Setters
    public String getRepositoryName() { return repositoryName; }
    public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }
    
    public FrameworkType getIdentifiedFramework() { return identifiedFramework; }
    public void setIdentifiedFramework(FrameworkType identifiedFramework) { this.identifiedFramework = identifiedFramework; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
}