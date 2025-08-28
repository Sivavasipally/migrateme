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
    private String errorMessage;
    
    // Additional wizard-specific fields
    private RepositoryInfo repository;
    private MigrationStatus status;
    private String localPath;
    private java.util.List<GeneratedFile> generatedFiles;
    private String commitMessage;
    private java.time.LocalDateTime executionTime;

    public String getErrorMessage() {
        // Prefer explicit errorDetails, fall back to message
        return (errorDetails != null && !errorDetails.isEmpty()) ? errorDetails : message;
    }

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	/** e.g., "kubernetes" or "openshift" */
    private String targetFramework;

    /** include Helm chart generation? */
    private boolean includeHelm;

    /** include Dockerfile generation? */
    private boolean includeDockerfile;
    
    public String getTargetFramework() {
		return targetFramework;
	}

	public void setTargetFramework(String targetFramework) {
		this.targetFramework = targetFramework;
	}

	public boolean isIncludeHelm() {
		return includeHelm;
	}

	public void setIncludeHelm(boolean includeHelm) {
		this.includeHelm = includeHelm;
	}

	public boolean isIncludeDockerfile() {
		return includeDockerfile;
	}

	public void setIncludeDockerfile(boolean includeDockerfile) {
		this.includeDockerfile = includeDockerfile;
	}

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
    
    // Wizard-specific getters and setters
    public RepositoryInfo getRepository() {
        return repository;
    }
    
    public void setRepository(RepositoryInfo repository) {
        this.repository = repository;
    }
    
    public MigrationStatus getStatus() {
        return status;
    }
    
    public void setStatus(MigrationStatus status) {
        this.status = status;
    }
    
    public String getLocalPath() {
        return localPath;
    }
    
    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }
    
    public java.util.List<GeneratedFile> getGeneratedFiles() {
        return generatedFiles;
    }
    
    public void setGeneratedFiles(java.util.List<GeneratedFile> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }
    
    public String getCommitMessage() {
        return commitMessage;
    }
    
    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
    
    public java.time.LocalDateTime getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(java.time.LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }
}