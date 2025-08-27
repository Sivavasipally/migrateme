package com.example.gitmigrator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Information about a Git service user.
 */
public class GitUserInfo {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("login")
    private String username;
    
    @JsonProperty("name")
    private String displayName;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    @JsonProperty("html_url")
    private String profileUrl;
    
    @JsonProperty("company")
    private String company;
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("public_repos")
    private Integer publicRepos;
    
    @JsonProperty("private_repos")
    private Integer privateRepos;
    
    // Default constructor
    public GitUserInfo() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public String getProfileUrl() { return profileUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
    
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public Integer getPublicRepos() { return publicRepos; }
    public void setPublicRepos(Integer publicRepos) { this.publicRepos = publicRepos; }
    
    public Integer getPrivateRepos() { return privateRepos; }
    public void setPrivateRepos(Integer privateRepos) { this.privateRepos = privateRepos; }
    
    @Override
    public String toString() {
        return "GitUserInfo{" +
                "username='" + username + '\'' +
                ", displayName='" + displayName + '\'' +
                ", email='" + email + '\'' +
                ", publicRepos=" + publicRepos +
                '}';
    }
}