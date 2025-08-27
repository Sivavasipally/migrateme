package com.example.gitmigrator.model;

import java.util.List;

/**
 * Paginated result for repository listings.
 */
public class GitRepositoryPage {
    
    private List<RepositoryInfo> repositories;
    private int currentPage;
    private int totalPages;
    private int totalCount;
    private int pageSize;
    private boolean hasNextPage;
    private boolean hasPreviousPage;
    
    // Default constructor
    public GitRepositoryPage() {}
    
    // Constructor with basic info
    public GitRepositoryPage(List<RepositoryInfo> repositories, int currentPage, int totalPages, int totalCount) {
        this.repositories = repositories;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalCount = totalCount;
        this.pageSize = repositories != null ? repositories.size() : 0;
        this.hasNextPage = currentPage < totalPages;
        this.hasPreviousPage = currentPage > 1;
    }
    
    // Getters and Setters
    public List<RepositoryInfo> getRepositories() { return repositories; }
    public void setRepositories(List<RepositoryInfo> repositories) { 
        this.repositories = repositories;
        this.pageSize = repositories != null ? repositories.size() : 0;
    }
    
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { 
        this.currentPage = currentPage;
        this.hasPreviousPage = currentPage > 1;
        this.hasNextPage = currentPage < totalPages;
    }
    
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { 
        this.totalPages = totalPages;
        this.hasNextPage = currentPage < totalPages;
    }
    
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    
    public boolean isHasNextPage() { return hasNextPage; }
    public void setHasNextPage(boolean hasNextPage) { this.hasNextPage = hasNextPage; }
    
    public boolean isHasPreviousPage() { return hasPreviousPage; }
    public void setHasPreviousPage(boolean hasPreviousPage) { this.hasPreviousPage = hasPreviousPage; }
    
    // Convenience methods
    public int getStartIndex() {
        return (currentPage - 1) * pageSize + 1;
    }
    
    public int getEndIndex() {
        return Math.min(getStartIndex() + pageSize - 1, totalCount);
    }
    
    public boolean isEmpty() {
        return repositories == null || repositories.isEmpty();
    }
    
    @Override
    public String toString() {
        return "GitRepositoryPage{" +
                "repositories=" + (repositories != null ? repositories.size() : 0) +
                ", currentPage=" + currentPage +
                ", totalPages=" + totalPages +
                ", totalCount=" + totalCount +
                '}';
    }
}