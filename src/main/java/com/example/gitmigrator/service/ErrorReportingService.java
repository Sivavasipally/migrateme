package com.example.gitmigrator.service;

import com.example.gitmigrator.model.ErrorCategory;
import com.example.gitmigrator.model.ErrorReport;

import java.util.List;

/**
 * Service interface for comprehensive error reporting and recovery suggestions
 */
public interface ErrorReportingService {
    
    /**
     * Create a comprehensive error report from an exception
     */
    ErrorReport createErrorReport(Throwable error, String context);
    
    /**
     * Create a custom error report
     */
    ErrorReport createErrorReport(String title, String description, ErrorCategory category, String context);
    
    /**
     * Get suggested actions for a specific error category
     */
    List<String> getSuggestedActions(ErrorCategory category, Throwable error);
    
    /**
     * Get recovery suggestions based on error type and context
     */
    List<String> getRecoverySuggestions(ErrorCategory category, String context);
    
    /**
     * Get related documentation links for an error category
     */
    List<String> getRelatedDocumentation(ErrorCategory category);
    
    /**
     * Format error message for user display
     */
    String formatUserFriendlyMessage(Throwable error, String context);
    
    /**
     * Check if an error is recoverable
     */
    boolean isRecoverable(ErrorCategory category, Throwable error);
    
    /**
     * Get help text for a specific error category
     */
    String getHelpText(ErrorCategory category);
    
    /**
     * Log error report for analysis
     */
    void logErrorReport(ErrorReport report);
    
    /**
     * Get common solutions for frequent errors
     */
    List<String> getCommonSolutions(String errorPattern);
}