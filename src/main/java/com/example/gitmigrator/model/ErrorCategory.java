package com.example.gitmigrator.model;

/**
 * Categories of errors that can occur during migration
 */
public enum ErrorCategory {
    NETWORK("Network Error", "Issues related to network connectivity or remote repositories"),
    AUTHENTICATION("Authentication Error", "Problems with Git service authentication or credentials"),
    REPOSITORY("Repository Error", "Issues with repository access, structure, or content"),
    FRAMEWORK_DETECTION("Framework Detection Error", "Problems detecting or analyzing project framework"),
    FILE_GENERATION("File Generation Error", "Issues generating migration artifacts"),
    VALIDATION("Validation Error", "Problems validating generated files or configurations"),
    DOCKER("Docker Error", "Issues related to Docker operations or Dockerfile generation"),
    KUBERNETES("Kubernetes Error", "Problems with Kubernetes manifest generation or validation"),
    HELM("Helm Error", "Issues with Helm chart generation or validation"),
    CONFIGURATION("Configuration Error", "Problems with migration configuration or settings"),
    SYSTEM("System Error", "General system or runtime errors"),
    UNKNOWN("Unknown Error", "Unclassified or unexpected errors");

    private final String displayName;
    private final String description;

    ErrorCategory(String displayName, String description) {
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
     * Categorize an error based on its type and message
     */
    public static ErrorCategory categorizeError(Throwable error) {
        if (error == null) {
            return UNKNOWN;
        }

        String errorMessage = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
        String errorClass = error.getClass().getSimpleName().toLowerCase();

        // Network-related errors
        if (errorClass.contains("connect") || errorClass.contains("timeout") || 
            errorMessage.contains("connection") || errorMessage.contains("network") ||
            errorMessage.contains("unreachable") || errorMessage.contains("timeout")) {
            return NETWORK;
        }

        // Authentication errors
        if (errorClass.contains("auth") || errorMessage.contains("unauthorized") ||
            errorMessage.contains("authentication") || errorMessage.contains("credential") ||
            errorMessage.contains("token") || errorMessage.contains("permission denied")) {
            return AUTHENTICATION;
        }

        // Repository errors
        if (errorMessage.contains("repository") || errorMessage.contains("git") ||
            errorMessage.contains("clone") || errorMessage.contains("branch") ||
            errorMessage.contains("commit")) {
            return REPOSITORY;
        }

        // Docker errors
        if (errorMessage.contains("docker") || errorMessage.contains("dockerfile") ||
            errorMessage.contains("container") || errorMessage.contains("image")) {
            return DOCKER;
        }

        // Kubernetes errors
        if (errorMessage.contains("kubernetes") || errorMessage.contains("k8s") ||
            errorMessage.contains("kubectl") || errorMessage.contains("manifest")) {
            return KUBERNETES;
        }

        // Helm errors
        if (errorMessage.contains("helm") || errorMessage.contains("chart") ||
            errorMessage.contains("tiller")) {
            return HELM;
        }

        // File I/O errors
        if (errorClass.contains("io") || errorClass.contains("file") ||
            errorMessage.contains("file") || errorMessage.contains("directory") ||
            errorMessage.contains("path")) {
            return FILE_GENERATION;
        }

        // Validation errors
        if (errorMessage.contains("validation") || errorMessage.contains("invalid") ||
            errorMessage.contains("syntax") || errorMessage.contains("format")) {
            return VALIDATION;
        }

        // Configuration errors
        if (errorMessage.contains("configuration") || errorMessage.contains("config") ||
            errorMessage.contains("setting") || errorMessage.contains("property")) {
            return CONFIGURATION;
        }

        return UNKNOWN;
    }
}