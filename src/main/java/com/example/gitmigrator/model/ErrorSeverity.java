package com.example.gitmigrator.model;

/**
 * Severity levels for errors
 */
public enum ErrorSeverity {
    LOW("Low", "Minor issues that don't prevent migration completion"),
    MEDIUM("Medium", "Issues that may affect migration quality or require attention"),
    HIGH("High", "Serious issues that prevent successful migration"),
    CRITICAL("Critical", "Severe system errors that require immediate attention");

    private final String displayName;
    private final String description;

    ErrorSeverity(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determine severity based on error category and context
     */
    public static ErrorSeverity determineSeverity(ErrorCategory category, Throwable error) {
        switch (category) {
            case SYSTEM:
                return CRITICAL;
            case AUTHENTICATION:
            case NETWORK:
                return HIGH;
            case DOCKER:
            case KUBERNETES:
            case HELM:
            case REPOSITORY:
                return MEDIUM;
            case VALIDATION:
            case CONFIGURATION:
                return LOW;
            default:
                return MEDIUM;
        }
    }
}