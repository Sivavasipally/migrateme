package com.example.gitmigrator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Information about a Git service organization/group.
 */
public class GitOrganization {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("login")
    private String name;
    
    @JsonProperty("display_name")
    private String displayName;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    @JsonProperty("html_url")
    private String url;
    
    @JsonProperty("public_repos")
    private Integer publicRepos;
    
    @JsonProperty("private_repos")
    private Integer privateRepos;
    
    // Default constructor
    public GitOrganization() {}
    
    // Constructor with name
    public GitOrganization(String name) {
        this.name = name;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public Integer getPublicRepos() { return publicRepos; }
    public void setPublicRepos(Integer publicRepos) { this.publicRepos = publicRepos; }
    
    public Integer getPrivateRepos() { return privateRepos; }
    public void setPrivateRepos(Integer privateRepos) { this.privateRepos = privateRepos; }
    
    @Override
    public String toString() {
        return "GitOrganization{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", publicRepos=" + publicRepos +
                '}';
    }
}