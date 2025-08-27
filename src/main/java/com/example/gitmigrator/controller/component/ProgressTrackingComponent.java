package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.MigrationProgress;
import com.example.gitmigrator.model.MigrationStatus;
import com.example.gitmigrator.model.RepositoryProgress;
import com.example.gitmigrator.service.ProgressTrackingService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * JavaFX component for displaying detailed migration progress
 */
public class ProgressTrackingComponent extends VBox {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private final ProgressTrackingService progressService;
    private final Map<String, RepositoryProgressRow> repositoryRows;
    private final Consumer<MigrationProgress> progressListener;
    
    private Label operationLabel;
    private ProgressBar overallProgressBar;
    private Label overallStatusLabel;
    private VBox repositoryContainer;
    private TextArea logArea;
    private CheckBox autoScrollLogs;
    
    private String currentOperationId;
    
    public ProgressTrackingComponent(ProgressTrackingService progressService) {
        this.progressService = progressService;
        this.repositoryRows = new HashMap<>();
        this.progressListener = this::updateProgress;
        
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        operationLabel = new Label("No active operation");
        operationLabel.getStyleClass().add("operation-label");
        
        overallProgressBar = new ProgressBar(0);
        overallProgressBar.setPrefWidth(400);
        overallProgressBar.getStyleClass().add("overall-progress");
        
        overallStatusLabel = new Label("Ready");
        overallStatusLabel.getStyleClass().add("status-label");
        
        repositoryContainer = new VBox(5);
        repositoryContainer.getStyleClass().add("repository-container");
        
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(8);
        logArea.getStyleClass().add("log-area");
        
        autoScrollLogs = new CheckBox("Auto-scroll logs");
        autoScrollLogs.setSelected(true);
    }
    
    private void setupLayout() {
        setSpacing(10);
        setPadding(new Insets(10));
        getStyleClass().add("progress-tracking-component");
        
        // Overall progress section
        VBox overallSection = new VBox(5);
        overallSection.getStyleClass().add("overall-section");
        
        HBox progressRow = new HBox(10);
        progressRow.getChildren().addAll(overallProgressBar, overallStatusLabel);
        
        overallSection.getChildren().addAll(operationLabel, progressRow);
        
        // Repository progress section
        Label repoLabel = new Label("Repository Progress:");
        repoLabel.getStyleClass().add("section-label");
        
        ScrollPane repoScrollPane = new ScrollPane(repositoryContainer);
        repoScrollPane.setFitToWidth(true);
        repoScrollPane.setPrefHeight(200);
        repoScrollPane.getStyleClass().add("repository-scroll");
        
        // Log section
        Label logLabel = new Label("Operation Logs:");
        logLabel.getStyleClass().add("section-label");
        
        VBox logSection = new VBox(5);
        logSection.getChildren().addAll(logLabel, autoScrollLogs, logArea);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        
        getChildren().addAll(overallSection, repoLabel, repoScrollPane, logSection);
        VBox.setVgrow(logSection, Priority.ALWAYS);
    }
    
    public void startTracking(String operationId) {
        if (currentOperationId != null) {
            progressService.removeProgressListener(currentOperationId, progressListener);
        }
        
        currentOperationId = operationId;
        repositoryRows.clear();
        repositoryContainer.getChildren().clear();
        logArea.clear();
        
        progressService.addProgressListener(operationId, progressListener);
        
        // Load initial progress
        MigrationProgress progress = progressService.getProgress(operationId);
        if (progress != null) {
            updateProgress(progress);
        }
    }
    
    public void stopTracking() {
        if (currentOperationId != null) {
            progressService.removeProgressListener(currentOperationId, progressListener);
            currentOperationId = null;
        }
    }
    
    private void updateProgress(MigrationProgress progress) {
        Platform.runLater(() -> {
            updateOverallProgress(progress);
            updateRepositoryProgress(progress);
            updateLogs(progress);
        });
    }
    
    private void updateOverallProgress(MigrationProgress progress) {
        operationLabel.setText("Operation: " + progress.getOperationId() + 
                               " (Started: " + progress.getStartTime().format(TIME_FORMATTER) + ")");
        
        double progressValue = progress.getOverallProgressPercentage() / 100.0;
        overallProgressBar.setProgress(progressValue);
        
        String statusText = String.format("%s - %d/%d repositories (%d failed)", 
                                        progress.getOverallStatus().getDisplayName(),
                                        progress.getCompletedRepositories() + progress.getFailedRepositories(),
                                        progress.getTotalRepositories(),
                                        progress.getFailedRepositories());
        overallStatusLabel.setText(statusText);
        
        // Update style based on status
        overallStatusLabel.getStyleClass().removeAll("status-completed", "status-failed", "status-in-progress");
        switch (progress.getOverallStatus()) {
            case COMPLETED:
                overallStatusLabel.getStyleClass().add("status-completed");
                break;
            case FAILED:
                overallStatusLabel.getStyleClass().add("status-failed");
                break;
            case QUEUED:
            case CLONING:
            case ANALYZING:
            case GENERATING:
            case VALIDATING:
                overallStatusLabel.getStyleClass().add("status-in-progress");
                break;
        }
    }
    
    private void updateRepositoryProgress(MigrationProgress progress) {
        for (Map.Entry<String, RepositoryProgress> entry : progress.getRepositoryProgress().entrySet()) {
            String repoId = entry.getKey();
            RepositoryProgress repoProgress = entry.getValue();
            
            RepositoryProgressRow row = repositoryRows.get(repoId);
            if (row == null) {
                row = new RepositoryProgressRow(repoProgress);
                repositoryRows.put(repoId, row);
                repositoryContainer.getChildren().add(row);
            } else {
                row.updateProgress(repoProgress);
            }
        }
    }
    
    private void updateLogs(MigrationProgress progress) {
        StringBuilder logText = new StringBuilder();
        
        // Add global logs
        for (String log : progress.getGlobalLogs()) {
            logText.append(log).append("\n");
        }
        
        // Add repository logs
        for (RepositoryProgress repoProgress : progress.getRepositoryProgress().values()) {
            for (String log : repoProgress.getLogs()) {
                logText.append("[").append(repoProgress.getRepositoryName()).append("] ").append(log).append("\n");
            }
        }
        
        logArea.setText(logText.toString());
        
        // Auto-scroll to bottom if enabled
        if (autoScrollLogs.isSelected()) {
            logArea.setScrollTop(Double.MAX_VALUE);
        }
    }
    
    /**
     * Inner class representing a single repository progress row
     */
    private static class RepositoryProgressRow extends HBox {
        private final Label nameLabel;
        private final ProgressBar progressBar;
        private final Label statusLabel;
        private final Label timeLabel;
        
        public RepositoryProgressRow(RepositoryProgress repoProgress) {
            setSpacing(10);
            setPadding(new Insets(5));
            getStyleClass().add("repository-progress-row");
            
            nameLabel = new Label(repoProgress.getRepositoryName());
            nameLabel.setPrefWidth(150);
            nameLabel.getStyleClass().add("repository-name");
            
            progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(200);
            
            statusLabel = new Label();
            statusLabel.setPrefWidth(200);
            statusLabel.getStyleClass().add("repository-status");
            
            timeLabel = new Label();
            timeLabel.getStyleClass().add("repository-time");
            
            getChildren().addAll(nameLabel, progressBar, statusLabel, timeLabel);
            HBox.setHgrow(statusLabel, Priority.ALWAYS);
            
            updateProgress(repoProgress);
        }
        
        public void updateProgress(RepositoryProgress repoProgress) {
            double progressValue = repoProgress.getProgressPercentage() / 100.0;
            progressBar.setProgress(progressValue);
            
            statusLabel.setText(repoProgress.getStatusDisplayText());
            
            if (repoProgress.getEndTime() != null) {
                long duration = java.time.Duration.between(repoProgress.getStartTime(), repoProgress.getEndTime()).getSeconds();
                timeLabel.setText(duration + "s");
            } else {
                long duration = java.time.Duration.between(repoProgress.getStartTime(), java.time.LocalDateTime.now()).getSeconds();
                timeLabel.setText(duration + "s (running)");
            }
            
            // Update style based on status
            getStyleClass().removeAll("row-completed", "row-failed", "row-in-progress");
            switch (repoProgress.getStatus()) {
                case COMPLETED:
                    getStyleClass().add("row-completed");
                    break;
                case FAILED:
                    getStyleClass().add("row-failed");
                    break;
                case QUEUED:
                case CLONING:
                case ANALYZING:
                case GENERATING:
                case VALIDATING:
                    getStyleClass().add("row-in-progress");
                    break;
            }
        }
    }
}