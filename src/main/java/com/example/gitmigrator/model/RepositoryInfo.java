package com.example.gitmigrator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Data Transfer Object representing repository information from Git APIs.
 * Enhanced with JavaFX properties for UI binding.
 */
public class RepositoryInfo {
    
    @JsonProperty("id")
    private Long id;
    
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
    
    // Constructors
    public RepositoryInfo() {}
    
    public RepositoryInfo(String name, String fullName, String cloneUrl) {
        this.name = name;
        this.fullName = fullName;
        this.cloneUrl = cloneUrl;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCloneUrl() { return cloneUrl; }
    public void setCloneUrl(String cloneUrl) { this.cloneUrl = cloneUrl; }
    
    public String getSshUrl() { return sshUrl; }
    public void setSshUrl(String sshUrl) { this.sshUrl = sshUrl; }
    
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    
    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
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