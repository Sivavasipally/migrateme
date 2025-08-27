package com.example.gitmigrator.model;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Filter criteria for repository search and filtering in bulk discovery.
 * Supports text search, language filtering, visibility, and date-based filtering.
 */
public class RepositoryFilter {
    
    private String searchQuery;
    private Set<String> languages;
    private Boolean includePrivate;
    private Boolean includeForks;
    private Boolean includeArchived;
    private LocalDateTime updatedAfter;
    private LocalDateTime updatedBefore;
    private Long minSize; // in KB
    private Long maxSize; // in KB
    private Integer minStars;
    private String ownerType; // user, organization, group
    
    public RepositoryFilter() {}
    
    public RepositoryFilter(String searchQuery) {
        this.searchQuery = searchQuery;
    }
    
    // Getters and Setters
    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
    
    public Set<String> getLanguages() { return languages; }
    public void setLanguages(Set<String> languages) { this.languages = languages; }
    
    public Boolean getIncludePrivate() { return includePrivate; }
    public void setIncludePrivate(Boolean includePrivate) { this.includePrivate = includePrivate; }
    
    public Boolean getIncludeForks() { return includeForks; }
    public void setIncludeForks(Boolean includeForks) { this.includeForks = includeForks; }
    
    public Boolean getIncludeArchived() { return includeArchived; }
    public void setIncludeArchived(Boolean includeArchived) { this.includeArchived = includeArchived; }
    
    public LocalDateTime getUpdatedAfter() { return updatedAfter; }
    public void setUpdatedAfter(LocalDateTime updatedAfter) { this.updatedAfter = updatedAfter; }
    
    public LocalDateTime getUpdatedBefore() { return updatedBefore; }
    public void setUpdatedBefore(LocalDateTime updatedBefore) { this.updatedBefore = updatedBefore; }
    
    public Long getMinSize() { return minSize; }
    public void setMinSize(Long minSize) { this.minSize = minSize; }
    
    public Long getMaxSize() { return maxSize; }
    public void setMaxSize(Long maxSize) { this.maxSize = maxSize; }
    
    public Integer getMinStars() { return minStars; }
    public void setMinStars(Integer minStars) { this.minStars = minStars; }
    
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    
    /**
     * Checks if a repository matches this filter.
     */
    public boolean matches(RepositoryMetadata repository) {
        if (repository == null) return false;
        
        // Text search in name and description
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String query = searchQuery.toLowerCase();
            boolean nameMatch = repository.getName() != null && 
                              repository.getName().toLowerCase().contains(query);
            boolean descMatch = repository.getDescription() != null && 
                              repository.getDescription().toLowerCase().contains(query);
            boolean fullNameMatch = repository.getFullName() != null && 
                                  repository.getFullName().toLowerCase().contains(query);
            
            if (!nameMatch && !descMatch && !fullNameMatch) {
                return false;
            }
        }
        
        // Language filter
        if (languages != null && !languages.isEmpty()) {
            if (repository.getLanguage() == null || 
                !languages.contains(repository.getLanguage())) {
                return false;
            }
        }
        
        // Privacy filter
        if (includePrivate != null && !includePrivate && repository.isPrivate()) {
            return false;
        }
        
        // Fork filter
        if (includeForks != null && !includeForks && repository.isFork()) {
            return false;
        }
        
        // Archived filter
        if (includeArchived != null && !includeArchived && repository.isArchived()) {
            return false;
        }
        
        // Date filters
        LocalDateTime repoUpdated = repository.getMostRecentActivity();
        if (updatedAfter != null && repoUpdated != null && repoUpdated.isBefore(updatedAfter)) {
            return false;
        }
        if (updatedBefore != null && repoUpdated != null && repoUpdated.isAfter(updatedBefore)) {
            return false;
        }
        
        // Size filters
        if (minSize != null && repository.getSize() < minSize) {
            return false;
        }
        if (maxSize != null && repository.getSize() > maxSize) {
            return false;
        }
        
        // Star filter
        if (minStars != null && repository.getStarCount() < minStars) {
            return false;
        }
        
        // Owner type filter
        if (ownerType != null && !ownerType.isEmpty()) {
            if (repository.getOwnerType() == null || 
                !repository.getOwnerType().equalsIgnoreCase(ownerType)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if this filter has any active criteria.
     */
    public boolean hasActiveCriteria() {
        return (searchQuery != null && !searchQuery.trim().isEmpty()) ||
               (languages != null && !languages.isEmpty()) ||
               includePrivate != null ||
               includeForks != null ||
               includeArchived != null ||
               updatedAfter != null ||
               updatedBefore != null ||
               minSize != null ||
               maxSize != null ||
               minStars != null ||
               (ownerType != null && !ownerType.trim().isEmpty());
    }
    
    /**
     * Clears all filter criteria.
     */
    public void clear() {
        searchQuery = null;
        languages = null;
        includePrivate = null;
        includeForks = null;
        includeArchived = null;
        updatedAfter = null;
        updatedBefore = null;
        minSize = null;
        maxSize = null;
        minStars = null;
        ownerType = null;
    }
    
    @Override
    public String toString() {
        return "RepositoryFilter{" +
                "searchQuery='" + searchQuery + '\'' +
                ", languages=" + languages +
                ", includePrivate=" + includePrivate +
                ", includeForks=" + includeForks +
                ", includeArchived=" + includeArchived +
                ", updatedAfter=" + updatedAfter +
                ", minStars=" + minStars +
                '}';
    }
}