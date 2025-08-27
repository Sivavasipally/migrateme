package com.example.gitmigrator.model;

import java.time.LocalDateTime;

/**
 * Rate limit information for a Git service.
 */
public class GitRateLimitInfo {
    
    private int limit;
    private int remaining;
    private LocalDateTime resetTime;
    private String resource;
    
    // Default constructor
    public GitRateLimitInfo() {}
    
    // Constructor with basic info
    public GitRateLimitInfo(int limit, int remaining, LocalDateTime resetTime) {
        this.limit = limit;
        this.remaining = remaining;
        this.resetTime = resetTime;
    }
    
    // Getters and Setters
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
    
    public int getRemaining() { return remaining; }
    public void setRemaining(int remaining) { this.remaining = remaining; }
    
    public LocalDateTime getResetTime() { return resetTime; }
    public void setResetTime(LocalDateTime resetTime) { this.resetTime = resetTime; }
    
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    
    // Convenience methods
    public int getUsed() {
        return limit - remaining;
    }
    
    public double getUsagePercentage() {
        if (limit == 0) return 0.0;
        return (double) getUsed() / limit * 100.0;
    }
    
    public boolean isNearLimit() {
        return getUsagePercentage() > 80.0;
    }
    
    public boolean isExhausted() {
        return remaining <= 0;
    }
    
    @Override
    public String toString() {
        return "GitRateLimitInfo{" +
                "limit=" + limit +
                ", remaining=" + remaining +
                ", resetTime=" + resetTime +
                ", resource='" + resource + '\'' +
                '}';
    }
}