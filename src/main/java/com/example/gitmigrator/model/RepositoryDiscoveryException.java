package com.example.gitmigrator.model;

/**
 * Exception thrown during repository discovery operations.
 * Provides specific error types and recovery suggestions.
 */
public class RepositoryDiscoveryException extends Exception {
    
    private final ErrorType errorType;
    private final String providerName;
    private final String recoverySuggestion;
    
    public enum ErrorType {
        CONNECTION_FAILED("Connection to Git provider failed"),
        AUTHENTICATION_FAILED("Authentication with Git provider failed"),
        RATE_LIMIT_EXCEEDED("API rate limit exceeded"),
        INVALID_CREDENTIALS("Invalid username or password"),
        INSUFFICIENT_PERMISSIONS("Insufficient permissions to access repositories"),
        INVALID_URL("Invalid Git provider URL"),
        NETWORK_ERROR("Network connectivity error"),
        API_ERROR("Git provider API error"),
        TIMEOUT("Operation timed out"),
        UNKNOWN_ERROR("Unknown error occurred");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public RepositoryDiscoveryException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.providerName = null;
        this.recoverySuggestion = null;
    }
    
    public RepositoryDiscoveryException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.providerName = null;
        this.recoverySuggestion = null;
    }
    
    public RepositoryDiscoveryException(ErrorType errorType, String message, 
                                      String providerName, String recoverySuggestion) {
        super(message);
        this.errorType = errorType;
        this.providerName = providerName;
        this.recoverySuggestion = recoverySuggestion;
    }
    
    public RepositoryDiscoveryException(ErrorType errorType, String message, Throwable cause,
                                      String providerName, String recoverySuggestion) {
        super(message, cause);
        this.errorType = errorType;
        this.providerName = providerName;
        this.recoverySuggestion = recoverySuggestion;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
    
    public String getProviderName() {
        return providerName;
    }
    
    public String getRecoverySuggestion() {
        return recoverySuggestion;
    }
    
    /**
     * Gets a user-friendly error message with recovery suggestion.
     */
    public String getUserFriendlyMessage() {
        StringBuilder message = new StringBuilder();
        
        if (providerName != null) {
            message.append(providerName).append(": ");
        }
        
        message.append(errorType.getDescription());
        
        if (getMessage() != null && !getMessage().isEmpty()) {
            message.append(" - ").append(getMessage());
        }
        
        if (recoverySuggestion != null && !recoverySuggestion.isEmpty()) {
            message.append("\n\nSuggestion: ").append(recoverySuggestion);
        }
        
        return message.toString();
    }
    
    /**
     * Checks if this error is recoverable (user can retry).
     */
    public boolean isRecoverable() {
        switch (errorType) {
            case NETWORK_ERROR:
            case TIMEOUT:
            case RATE_LIMIT_EXCEEDED:
            case API_ERROR:
                return true;
            case AUTHENTICATION_FAILED:
            case INVALID_CREDENTIALS:
            case INSUFFICIENT_PERMISSIONS:
            case INVALID_URL:
                return false;
            default:
                return false;
        }
    }
    
    /**
     * Gets the recommended retry delay in milliseconds for recoverable errors.
     */
    public long getRetryDelayMs() {
        switch (errorType) {
            case RATE_LIMIT_EXCEEDED:
                return 60000; // 1 minute
            case NETWORK_ERROR:
            case TIMEOUT:
                return 5000; // 5 seconds
            case API_ERROR:
                return 10000; // 10 seconds
            default:
                return 0;
        }
    }
    
    // Static factory methods for common error scenarios
    public static RepositoryDiscoveryException connectionFailed(String providerName, Throwable cause) {
        return new RepositoryDiscoveryException(
                ErrorType.CONNECTION_FAILED,
                "Failed to connect to " + providerName,
                cause,
                providerName,
                "Check your network connection and server URL"
        );
    }
    
    public static RepositoryDiscoveryException authenticationFailed(String providerName) {
        return new RepositoryDiscoveryException(
                ErrorType.AUTHENTICATION_FAILED,
                "Authentication failed",
                providerName,
                "Verify your username and password are correct"
        );
    }
    
    public static RepositoryDiscoveryException rateLimitExceeded(String providerName, long retryAfterMs) {
        return new RepositoryDiscoveryException(
                ErrorType.RATE_LIMIT_EXCEEDED,
                "API rate limit exceeded",
                providerName,
                "Please wait " + (retryAfterMs / 1000) + " seconds before retrying"
        );
    }
    
    public static RepositoryDiscoveryException invalidCredentials(String providerName) {
        return new RepositoryDiscoveryException(
                ErrorType.INVALID_CREDENTIALS,
                "Invalid username or password",
                providerName,
                "Check your credentials and try again"
        );
    }
    
    public static RepositoryDiscoveryException insufficientPermissions(String providerName) {
        return new RepositoryDiscoveryException(
                ErrorType.INSUFFICIENT_PERMISSIONS,
                "Insufficient permissions to access repositories",
                providerName,
                "Ensure your account has permission to view repositories"
        );
    }
}