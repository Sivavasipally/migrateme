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