package com.example.gitmigrator.service;

import com.example.gitmigrator.model.ErrorCategory;
import com.example.gitmigrator.model.ErrorReport;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Helper service for interactive error recovery and automated fixes
 */
public interface ErrorRecoveryHelper {
    
    /**
     * Attempt automatic recovery for recoverable errors
     */
    CompletableFuture<Boolean> attemptAutoRecovery(ErrorReport errorReport);
    
    /**
     * Get interactive recovery options for user selection
     */
    List<RecoveryOption> getInteractiveRecoveryOptions(ErrorReport errorReport);
    
    /**
     * Execute a specific recovery option
     */
    CompletableFuture<RecoveryResult> executeRecoveryOption(RecoveryOption option, ErrorReport errorReport);
    
    /**
     * Check if automatic recovery is available for an error
     */
    boolean canAutoRecover(ErrorCategory category, Throwable error);
    
    /**
     * Get estimated recovery time for an error
     */
    long getEstimatedRecoveryTime(ErrorCategory category);
    
    /**
     * Recovery option for user selection
     */
    class RecoveryOption {
        private final String id;
        private final String title;
        private final String description;
        private final boolean requiresUserInput;
        private final long estimatedTime;
        
        public RecoveryOption(String id, String title, String description, boolean requiresUserInput, long estimatedTime) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.requiresUserInput = requiresUserInput;
            this.estimatedTime = estimatedTime;
        }
        
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public boolean requiresUserInput() { return requiresUserInput; }
        public long getEstimatedTime() { return estimatedTime; }
    }
    
    /**
     * Result of a recovery attempt
     */
    class RecoveryResult {
        private final boolean successful;
        private final String message;
        private final Throwable error;
        
        public RecoveryResult(boolean successful, String message, Throwable error) {
            this.successful = successful;
            this.message = message;
            this.error = error;
        }
        
        public static RecoveryResult success(String message) {
            return new RecoveryResult(true, message, null);
        }
        
        public static RecoveryResult failure(String message, Throwable error) {
            return new RecoveryResult(false, message, error);
        }
        
        public boolean isSuccessful() { return successful; }
        public String getMessage() { return message; }
        public Throwable getError() { return error; }
    }
}