package com.example.gitmigrator.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Metadata about a repository discovered from a Git provider.
 * Contains comprehensive information for repository selection and filtering.
 */
public class RepositoryMetadata {
    
    private final String id;
    private final String name;
    private final String fullName;
    private final String description;
    private final String cloneUrl;
    private final String webUrl;
    private final String defaultBranch;
    private final boolean isPrivate;
    private final boolean isFork;
    private final boolean isArchived;
    private final String language;
    private final long size; // in KB
    private final int starCount;
    private final int forkCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime lastActivityAt;
    private final String ownerId;
    private final String ownerName;
    private final String ownerType; // user, organization, group
    private final List<String> topics;
    private final GitProviderType providerType;
    
    private RepositoryMetadata(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.fullName = builder.fullName;
        this.description = builder.description;
        this.cloneUrl = builder.cloneUrl;
        this.webUrl = builder.webUrl;
        this.defaultBranch = builder.defaultBranch;
        this.isPrivate = builder.isPrivate;
        this.isFork = builder.isFork;
        this.isArchived = builder.isArchived;
        this.language = builder.language;
        this.size = builder.size;
        this.starCount = builder.starCount;
        this.forkCount = builder.forkCount;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.lastActivityAt = builder.lastActivityAt;
        this.ownerId = builder.ownerId;
        this.ownerName = builder.ownerName;
        this.ownerType = builder.ownerType;
        this.topics = builder.topics;
        this.providerType = builder.providerType;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getFullName() { return fullName; }
    public String getDescription() { return description; }
    public String getCloneUrl() { return cloneUrl; }
    public String getWebUrl() { return webUrl; }
    public String getDefaultBranch() { return defaultBranch; }
    public boolean isPrivate() { return isPrivate; }
    public boolean isFork() { return isFork; }
    public boolean isArchived() { return isArchived; }
    public String getLanguage() { return language; }
    public long getSize() { return size; }
    public int getStarCount() { return starCount; }
    public int getForkCount() { return forkCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public String getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public String getOwnerType() { return ownerType; }
    public List<String> getTopics() { return topics; }
    public GitProviderType getProviderType() { return providerType; }
    
    /**
     * Converts this metadata to a RepositoryInfo for migration.
     */
    public RepositoryInfo toRepositoryInfo() {
        RepositoryInfo repoInfo = new RepositoryInfo();
        repoInfo.setName(name);
        repoInfo.setUrl(cloneUrl);
        repoInfo.setDescription(description);
        repoInfo.setDefaultBranch(defaultBranch != null ? defaultBranch : "main");
        return repoInfo;
    }
    
    /**
     * Gets a display-friendly size string.
     */
    public String getFormattedSize() {
        if (size < 1024) {
            return size + " KB";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f MB", size / 1024.0);
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0));
        }
    }
    
    /**
     * Gets the most recent activity date.
     */
    public LocalDateTime getMostRecentActivity() {
        LocalDateTime mostRecent = updatedAt;
        if (lastActivityAt != null && (mostRecent == null || lastActivityAt.isAfter(mostRecent))) {
            mostRecent = lastActivityAt;
        }
        return mostRecent;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private String name;
        private String fullName;
        private String description;
        private String cloneUrl;
        private String webUrl;
        private String defaultBranch = "main";
        private boolean isPrivate;
        private boolean isFork;
        private boolean isArchived;
        private String language;
        private long size;
        private int starCount;
        private int forkCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime lastActivityAt;
        private String ownerId;
        private String ownerName;
        private String ownerType;
        private List<String> topics;
        private GitProviderType providerType;
        
        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder cloneUrl(String cloneUrl) { this.cloneUrl = cloneUrl; return this; }
        public Builder webUrl(String webUrl) { this.webUrl = webUrl; return this; }
        public Builder defaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; return this; }
        public Builder isPrivate(boolean isPrivate) { this.isPrivate = isPrivate; return this; }
        public Builder isFork(boolean isFork) { this.isFork = isFork; return this; }
        public Builder isArchived(boolean isArchived) { this.isArchived = isArchived; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Builder size(long size) { this.size = size; return this; }
        public Builder starCount(int starCount) { this.starCount = starCount; return this; }
        public Builder forkCount(int forkCount) { this.forkCount = forkCount; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder lastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; return this; }
        public Builder ownerId(String ownerId) { this.ownerId = ownerId; return this; }
        public Builder ownerName(String ownerName) { this.ownerName = ownerName; return this; }
        public Builder ownerType(String ownerType) { this.ownerType = ownerType; return this; }
        public Builder topics(List<String> topics) { this.topics = topics; return this; }
        public Builder providerType(GitProviderType providerType) { this.providerType = providerType; return this; }
        
        public RepositoryMetadata build() {
            return new RepositoryMetadata(this);
        }
    }
    
    @Override
    public String toString() {
        return "RepositoryMetadata{" +
                "name='" + name + '\'' +
                ", fullName='" + fullName + '\'' +
                ", language='" + language + '\'' +
                ", size=" + getFormattedSize() +
                ", isPrivate=" + isPrivate +
                ", providerType=" + providerType +
                '}';
    }
}