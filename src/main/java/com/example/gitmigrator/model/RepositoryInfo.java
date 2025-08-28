package com.example.gitmigrator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Enhanced Data Transfer Object representing repository information from Git APIs.
 * Enhanced with JavaFX properties for UI binding and additional metadata.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepositoryInfo {
    
    @JsonProperty("id")
    private String id;
    
    // JavaFX property for table selection
    private BooleanProperty selected = new SimpleBooleanProperty(false);
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("full_name")
    private String fullName;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("clone_url")
    private String cloneUrl;
    
    @JsonProperty("ssh_url")
    private String sshUrl;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    @JsonProperty("default_branch")
    private String defaultBranch;
    
    @JsonProperty("language")
    private String language;
    
    // Enhanced metadata fields
    private String localPath;
    private FrameworkType detectedFramework;
    
    @JsonProperty("updated_at")
    private LocalDateTime lastCommitDate;
    
    private String lastCommitMessage;
    private int estimatedComplexity; // 1-5 scale
    
    @JsonProperty("size")
    private long repositorySize;
    
    private List<String> languages;
    private MigrationConfiguration migrationConfig;
    private MigrationStatus status = MigrationStatus.NOT_STARTED;
    
    // Additional metadata for enhanced analysis
    private java.util.Map<String, Object> additionalMetadata;
    
    // Constructors
    public RepositoryInfo() {}
    
    public RepositoryInfo(String name, String fullName, String cloneUrl) {
        this.name = name;
        this.fullName = fullName;
        this.cloneUrl = cloneUrl;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    // Convenience method for setting numeric IDs
    public void setId(Long id) { 
        this.id = id != null ? id.toString() : null; 
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCloneUrl() { return cloneUrl; }
    public void setCloneUrl(String cloneUrl) { this.cloneUrl = cloneUrl; }
    
    // Convenience method for getting URL (fallback to cloneUrl if htmlUrl is not set)
    public String getUrl() { 
        return htmlUrl != null ? htmlUrl : cloneUrl; 
    }
    
    // Convenience method for setting URL (sets both htmlUrl and cloneUrl if cloneUrl is not set)
    public void setUrl(String url) { 
        this.htmlUrl = url;
        if (this.cloneUrl == null) {
            this.cloneUrl = url;
        }
    }
    
    public String getSshUrl() { return sshUrl; }
    public void setSshUrl(String sshUrl) { this.sshUrl = sshUrl; }
    
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    
    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    // Enhanced metadata getters and setters
    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }
    
    public FrameworkType getDetectedFramework() { return detectedFramework; }
    public void setDetectedFramework(FrameworkType detectedFramework) { this.detectedFramework = detectedFramework; }
    
    public LocalDateTime getLastCommitDate() { return lastCommitDate; }
    public void setLastCommitDate(LocalDateTime lastCommitDate) { this.lastCommitDate = lastCommitDate; }
    
    public String getLastCommitMessage() { return lastCommitMessage; }
    public void setLastCommitMessage(String lastCommitMessage) { this.lastCommitMessage = lastCommitMessage; }
    
    public int getEstimatedComplexity() { return estimatedComplexity; }
    public void setEstimatedComplexity(int estimatedComplexity) { this.estimatedComplexity = estimatedComplexity; }
    
    public long getRepositorySize() { return repositorySize; }
    public void setRepositorySize(long repositorySize) { this.repositorySize = repositorySize; }
    
    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }
    
    public MigrationConfiguration getMigrationConfig() { return migrationConfig; }
    public void setMigrationConfig(MigrationConfiguration migrationConfig) { this.migrationConfig = migrationConfig; }
    
    public MigrationStatus getStatus() { return status; }
    public void setStatus(MigrationStatus status) { this.status = status; }
    
    public java.util.Map<String, Object> getAdditionalMetadata() { return additionalMetadata; }
    public void setAdditionalMetadata(java.util.Map<String, Object> additionalMetadata) { this.additionalMetadata = additionalMetadata; }
    
    // JavaFX selection property methods
    public BooleanProperty selectedProperty() { return selected; }
    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean selected) { this.selected.set(selected); }
    
    @Override
    public String toString() {
        return "RepositoryInfo{" +
                "name='" + name + '\'' +
                ", fullName='" + fullName + '\'' +
                ", cloneUrl='" + cloneUrl + '\'' +
                '}';
    }
}