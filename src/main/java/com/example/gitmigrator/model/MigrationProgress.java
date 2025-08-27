package com.example.gitmigrator.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Represents the overall progress of a migration operation
 */
public class MigrationProgress {
    private final String operationId;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private MigrationStatus overallStatus;
    private final ConcurrentMap<String, RepositoryProgress> repositoryProgress;
    private final List<String> globalLogs;
    private int totalRepositories;
    private int completedRepositories;
    private int failedRepositories;

    public MigrationProgress(String operationId) {
        this.operationId = operationId;
        this.startTime = LocalDateTime.now();
        this.overallStatus = MigrationStatus.QUEUED;
        this.repositoryProgress = new ConcurrentHashMap<>();
        this.globalLogs = new ArrayList<>();
        this.totalRepositories = 0;
        this.completedRepositories = 0;
        this.failedRepositories = 0;
    }

    public String getOperationId() {
        return operationId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public MigrationStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(MigrationStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public ConcurrentMap<String, RepositoryProgress> getRepositoryProgress() {
        return repositoryProgress;
    }

    public List<String> getGlobalLogs() {
        return globalLogs;
    }

    public synchronized void addGlobalLog(String message) {
        globalLogs.add(String.format("[%s] %s", LocalDateTime.now(), message));
    }

    public int getTotalRepositories() {
        return totalRepositories;
    }

    public void setTotalRepositories(int totalRepositories) {
        this.totalRepositories = totalRepositories;
    }

    public int getCompletedRepositories() {
        return completedRepositories;
    }

    public synchronized void incrementCompletedRepositories() {
        this.completedRepositories++;
    }

    public int getFailedRepositories() {
        return failedRepositories;
    }

    public synchronized void incrementFailedRepositories() {
        this.failedRepositories++;
    }

    public double getOverallProgressPercentage() {
        if (totalRepositories == 0) return 0.0;
        return ((double) (completedRepositories + failedRepositories) / totalRepositories) * 100.0;
    }

    public void addRepositoryProgress(String repositoryId, RepositoryProgress progress) {
        repositoryProgress.put(repositoryId, progress);
    }

    public RepositoryProgress getRepositoryProgress(String repositoryId) {
        return repositoryProgress.get(repositoryId);
    }
}