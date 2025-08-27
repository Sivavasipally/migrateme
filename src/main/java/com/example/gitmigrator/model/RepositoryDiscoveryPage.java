package com.example.gitmigrator.model;

import java.util.List;

/**
 * Represents a paginated result of repository discovery operations.
 * Contains repositories for the current page along with pagination metadata.
 */
public class RepositoryDiscoveryPage {
    
    private final List<RepositoryMetadata> repositories;
    private final int currentPage;
    private final int totalPages;
    private final int perPage;
    private final long totalRepositories;
    private final boolean hasNextPage;
    private final boolean hasPreviousPage;
    private final String nextPageUrl;
    private final String previousPageUrl;
    
    private RepositoryDiscoveryPage(Builder builder) {
        this.repositories = builder.repositories;
        this.currentPage = builder.currentPage;
        this.totalPages = builder.totalPages;
        this.perPage = builder.perPage;
        this.totalRepositories = builder.totalRepositories;
        this.hasNextPage = builder.hasNextPage;
        this.hasPreviousPage = builder.hasPreviousPage;
        this.nextPageUrl = builder.nextPageUrl;
        this.previousPageUrl = builder.previousPageUrl;
    }
    
    // Getters
    public List<RepositoryMetadata> getRepositories() { return repositories; }
    public int getCurrentPage() { return currentPage; }
    public int getTotalPages() { return totalPages; }
    public int getPerPage() { return perPage; }
    public long getTotalRepositories() { return totalRepositories; }
    public boolean hasNextPage() { return hasNextPage; }
    public boolean hasPreviousPage() { return hasPreviousPage; }
    public String getNextPageUrl() { return nextPageUrl; }
    public String getPreviousPageUrl() { return previousPageUrl; }
    
    /**
     * Gets the number of repositories in this page.
     */
    public int getRepositoryCount() {
        return repositories != null ? repositories.size() : 0;
    }
    
    /**
     * Checks if this is the first page.
     */
    public boolean isFirstPage() {
        return currentPage == 1;
    }
    
    /**
     * Checks if this is the last page.
     */
    public boolean isLastPage() {
        return currentPage == totalPages;
    }
    
    /**
     * Gets the starting repository number for this page (1-based).
     */
    public long getStartRepositoryNumber() {
        return (long) (currentPage - 1) * perPage + 1;
    }
    
    /**
     * Gets the ending repository number for this page (1-based).
     */
    public long getEndRepositoryNumber() {
        return Math.min(getStartRepositoryNumber() + getRepositoryCount() - 1, totalRepositories);
    }
    
    /**
     * Gets a summary string for this page (e.g., "1-25 of 150 repositories").
     */
    public String getPageSummary() {
        if (totalRepositories == 0) {
            return "No repositories found";
        }
        return String.format("%d-%d of %d repositories", 
                getStartRepositoryNumber(), getEndRepositoryNumber(), totalRepositories);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private List<RepositoryMetadata> repositories;
        private int currentPage = 1;
        private int totalPages = 1;
        private int perPage = 25;
        private long totalRepositories = 0;
        private boolean hasNextPage = false;
        private boolean hasPreviousPage = false;
        private String nextPageUrl;
        private String previousPageUrl;
        
        public Builder repositories(List<RepositoryMetadata> repositories) {
            this.repositories = repositories;
            return this;
        }
        
        public Builder currentPage(int currentPage) {
            this.currentPage = currentPage;
            return this;
        }
        
        public Builder totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }
        
        public Builder perPage(int perPage) {
            this.perPage = perPage;
            return this;
        }
        
        public Builder totalRepositories(long totalRepositories) {
            this.totalRepositories = totalRepositories;
            return this;
        }
        
        public Builder hasNextPage(boolean hasNextPage) {
            this.hasNextPage = hasNextPage;
            return this;
        }
        
        public Builder hasPreviousPage(boolean hasPreviousPage) {
            this.hasPreviousPage = hasPreviousPage;
            return this;
        }
        
        public Builder nextPageUrl(String nextPageUrl) {
            this.nextPageUrl = nextPageUrl;
            return this;
        }
        
        public Builder previousPageUrl(String previousPageUrl) {
            this.previousPageUrl = previousPageUrl;
            return this;
        }
        
        /**
         * Auto-calculates pagination flags based on current page and total pages.
         */
        public Builder autoCalculatePagination() {
            this.hasPreviousPage = currentPage > 1;
            this.hasNextPage = currentPage < totalPages;
            return this;
        }
        
        /**
         * Auto-calculates total pages based on total repositories and per page.
         */
        public Builder autoCalculateTotalPages() {
            if (totalRepositories > 0 && perPage > 0) {
                this.totalPages = (int) Math.ceil((double) totalRepositories / perPage);
            }
            return this;
        }
        
        public RepositoryDiscoveryPage build() {
            return new RepositoryDiscoveryPage(this);
        }
    }
    
    @Override
    public String toString() {
        return "RepositoryDiscoveryPage{" +
                "repositoryCount=" + getRepositoryCount() +
                ", currentPage=" + currentPage +
                ", totalPages=" + totalPages +
                ", totalRepositories=" + totalRepositories +
                ", hasNextPage=" + hasNextPage +
                ", hasPreviousPage=" + hasPreviousPage +
                '}';
    }
}