package com.example.gitmigrator.service;

import com.example.gitmigrator.model.MigrationProgress;
import com.example.gitmigrator.model.MigrationStatus;
import com.example.gitmigrator.model.MigrationStep;
import com.example.gitmigrator.model.RepositoryProgress;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of progress tracking service with persistence support
 */
public class ProgressTrackingServiceImpl implements ProgressTrackingService {
    
    private static final Logger logger = Logger.getLogger(ProgressTrackingServiceImpl.class.getName());
    private static final String PROGRESS_DIR = System.getProperty("user.home") + "/.gitmigrator/progress";
    
    private final ConcurrentMap<String, MigrationProgress> activeOperations;
    private final ConcurrentMap<String, List<Consumer<MigrationProgress>>> progressListeners;
    private final ObjectMapper objectMapper;
    
    public ProgressTrackingServiceImpl() {
        this.activeOperations = new ConcurrentHashMap<>();
        this.progressListeners = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Ensure progress directory exists
        new File(PROGRESS_DIR).mkdirs();
    }
    
    @Override
    public MigrationProgress startOperation(String operationId, List<String> repositoryIds) {
        MigrationProgress progress = new MigrationProgress(operationId);
        progress.setTotalRepositories(repositoryIds.size());
        progress.addGlobalLog("Starting migration operation with " + repositoryIds.size() + " repositories");
        
        // Initialize repository progress
        for (String repoId : repositoryIds) {
            RepositoryProgress repoProgress = new RepositoryProgress(repoId, repoId);
            progress.addRepositoryProgress(repoId, repoProgress);
        }
        
        activeOperations.put(operationId, progress);
        persistProgress(operationId);
        notifyListeners(operationId, progress);
        
        return progress;
    }
    
    @Override
    public MigrationProgress getProgress(String operationId) {
        return activeOperations.get(operationId);
    }
    
    @Override
    public void updateRepositoryProgress(String operationId, String repositoryId, MigrationStep step) {
        MigrationProgress progress = activeOperations.get(operationId);
        if (progress != null) {
            RepositoryProgress repoProgress = progress.getRepositoryProgress(repositoryId);
            if (repoProgress != null) {
                repoProgress.setCurrentStep(step);
                persistProgress(operationId);
                notifyListeners(operationId, progress);
            }
        }
    }
    
    @Override
    public void completeRepositoryStep(String operationId, String repositoryId, MigrationStep step) {
        MigrationProgress progress = activeOperations.get(operationId);
        if (progress != null) {
            RepositoryProgress repoProgress = progress.getRepositoryProgress(repositoryId);
            if (repoProgress != null) {
                repoProgress.completeStep(step);
                persistProgress(operationId);
                notifyListeners(operationId, progress);
            }
        }
    }
    
    @Override
    public void completeRepository(String operationId, String repositoryId) {
        MigrationProgress progress = activeOperations.get(operationId);
        if (progress != null) {
            RepositoryProgress repoProgress = progress.getRepositoryProgress(repositoryId);
            if (repoProgress != null) {
                repoProgress.setStatus(MigrationStatus.COMPLETED);
                repoProgress.setEndTime(LocalDateTime.now());
                progress.incrementCompletedRepositories();
                progress.addGlobalLog("Completed migration for repository: " + repositoryId);
                
                persistProgress(operationId);
                notifyListeners(operationId, progress);
            }
        }
    }
    
    @Override
    public void failRepository(String operationId, String repositoryId, String errorMessage) {
        MigrationProgress progress = activeOperations.get(operationId);
        if (progress != null) {
            RepositoryProgress repoProgress = progress.getRepositoryProgress(repositoryId);
            if (repoProgress != null) {
                repoProgress.setErrorMessage(errorMessage);
                repoProgress.setEndTime(LocalDateTime.now());
                progress.incrementFailedRepositories();
                progress.addGlobalLog("Failed migration for repository: " + repositoryId + " - " + errorMessage);
                
                persistProgress(operationId);
                notifyListeners(operationId, progress);
            }
        }
    }
    
    @Override
    public void addRepositoryLog(String operationId, String repositoryId, String message) {
        MigrationProgress progress = activeOperations.get(operationId);
        if (progress != null) {
            RepositoryProgress repoProgress = progress.getRepositoryProgress(repositoryId);
            if (repoProgress != null) {
                repoProgress.addLog(message);
                notifyListeners(operationId, progress);
            }
        }
    }
    
    @Override
    public void addGlobalLog(String operationId, String message) {
        MigrationProgress progress = activeOperations.get(operationId);
        if (progress != null) {
            progress.addGlobalLog(message);
            notifyListeners(operationId, progress);
        }
    }
    
    @Override
    public void completeOperation(String operationId) {
        MigrationProgress progress = activeOperations.get(operationId);
        if (progress != null) {
            progress.setOverallStatus(MigrationStatus.COMPLETED);
            progress.setEndTime(LocalDateTime.now());
            progress.addGlobalLog("Migration operation completed");
            
            persistProgress(operationId);
            notifyListeners(operationId, progress);
        }
    }
    
    @Override
    public void addProgressListener(String operationId, Consumer<MigrationProgress> listener) {
        progressListeners.computeIfAbsent(operationId, k -> new CopyOnWriteArrayList<>()).add(listener);
    }
    
    @Override
    public void removeProgressListener(String operationId, Consumer<MigrationProgress> listener) {
        List<Consumer<MigrationProgress>> listeners = progressListeners.get(operationId);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                progressListeners.remove(operationId);
            }
        }
    }
    
    @Override
    public List<String> getActiveOperations() {
        return new ArrayList<>(activeOperations.keySet());
    }
    
    @Override
    public void cleanupOldOperations(int hoursOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hoursOld);
        
        activeOperations.entrySet().removeIf(entry -> {
            MigrationProgress progress = entry.getValue();
            LocalDateTime endTime = progress.getEndTime();
            if (endTime != null && endTime.isBefore(cutoff)) {
                // Remove listeners
                progressListeners.remove(entry.getKey());
                // Remove persisted file
                File progressFile = new File(PROGRESS_DIR, entry.getKey() + ".json");
                if (progressFile.exists()) {
                    progressFile.delete();
                }
                return true;
            }
            return false;
        });
    }
    
    @Override
    public void persistProgress(String operationId) {
        MigrationProgress progress = activeOperations.get(operationId);
        if (progress != null) {
            try {
                File progressFile = new File(PROGRESS_DIR, operationId + ".json");
                objectMapper.writeValue(progressFile, progress);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to persist progress for operation: " + operationId, e);
            }
        }
    }
    
    @Override
    public MigrationProgress loadPersistedProgress(String operationId) {
        try {
            File progressFile = new File(PROGRESS_DIR, operationId + ".json");
            if (progressFile.exists()) {
                MigrationProgress progress = objectMapper.readValue(progressFile, MigrationProgress.class);
                activeOperations.put(operationId, progress);
                return progress;
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load persisted progress for operation: " + operationId, e);
        }
        return null;
    }
    
    private void notifyListeners(String operationId, MigrationProgress progress) {
        List<Consumer<MigrationProgress>> listeners = progressListeners.get(operationId);
        if (listeners != null) {
            for (Consumer<MigrationProgress> listener : listeners) {
                try {
                    listener.accept(progress);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error notifying progress listener", e);
                }
            }
        }
    }
}