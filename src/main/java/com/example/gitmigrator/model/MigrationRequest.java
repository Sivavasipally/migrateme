package com.example.gitmigrator.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request object for repository migration operations.
 */
public class MigrationRequest {
    
    @NotNull
    @NotEmpty
    private List<String> repositoryUrls;
    private String repositoryName;
    private FrameworkType identifiedFramework;
    private boolean success;
    private String message;
    private String errorDetails;
    private String targetFramework;
    private boolean includeHelm;
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

	public String getMessage() {
		return message;
	}

	public String getRepositoryName() {
		return repositoryName;
	}

	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	public FrameworkType getIdentifiedFramework() {
		return identifiedFramework;
	}

	public void setIdentifiedFramework(FrameworkType identifiedFramework) {
		this.identifiedFramework = identifiedFramework;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

    public String getErrorMessage() {
        return (errorDetails != null && !errorDetails.isEmpty()) ? errorDetails : message;
    }

	public void setMessage(String message) {
		this.message = message;
	}

	public String getErrorDetails() {
		return errorDetails;
	}

	public void setErrorDetails(String errorDetails) {
		this.errorDetails = errorDetails;
	}
	private String token;
    
    public MigrationRequest() {}
    
    public MigrationRequest(List<String> repositoryUrls, String token) {
        this.repositoryUrls = repositoryUrls;
        this.token = token;
    }
    
    public List<String> getRepositoryUrls() { return repositoryUrls; }
    public void setRepositoryUrls(List<String> repositoryUrls) { this.repositoryUrls = repositoryUrls; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}