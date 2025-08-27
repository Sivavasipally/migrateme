package com.example.gitmigrator.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the progress of migrating a single repository
 */
public class RepositoryProgress {
    private final String repositoryId;
    private final String repositoryName;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private MigrationStatus status;
    private MigrationStep currentStep;
    private final List<MigrationStep> completedSteps;
    private final List<String> logs;
    private double progressPercentage;
    private String errorMessage;

    public RepositoryProgress(String repositoryId, String repositoryName) {
        this.repositoryId = repositoryId;
        this.repositoryName = repositoryName;
        this.startTime = LocalDateTime.now();
        this.status = MigrationStatus.QUEUED;
        this.completedSteps = new ArrayList<>();
        this.logs = new ArrayList<>();
        this.progressPercentage = 0.0;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public String getRepositoryName() {
        return repositoryName;
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

    public MigrationStatus getStatus() {
        return status;
    }

    public void setStatus(MigrationStatus status) {
        this.status = status;
    }

    public MigrationStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(MigrationStep currentStep) {
        this.currentStep = currentStep;
        addLog("Starting step: " + currentStep.getDisplayName());
    }

    public List<MigrationStep> getCompletedSteps() {
        return completedSteps;
    }

    public void completeStep(MigrationStep step) {
        if (!completedSteps.contains(step)) {
            completedSteps.add(step);
            addLog("Completed step: " + step.getDisplayName());
            updateProgressPercentage();
        }
    }

    public List<String> getLogs() {
        return logs;
    }

    public synchronized void addLog(String message) {
        logs.add(String.format("[%s] %s", LocalDateTime.now(), message));
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    private void updateProgressPercentage() {
        MigrationStep[] allSteps = MigrationStep.values();
        this.progressPercentage = ((double) completedSteps.size() / allSteps.length) * 100.0;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = MigrationStatus.FAILED;
        addLog("Error: " + errorMessage);
    }

    public boolean isCompleted() {
        return status == MigrationStatus.COMPLETED || status == MigrationStatus.FAILED;
    }

    public String getStatusDisplayText() {
        if (status.isInProgress() && currentStep != null) {
            return currentStep.getDisplayName();
        }
        return status.getDisplayName();
    }
}