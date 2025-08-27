package com.example.gitmigrator.service;

import com.example.gitmigrator.model.CredentialValidationResult;
import com.example.gitmigrator.model.GitProviderConnection;
import com.example.gitmigrator.model.GitProviderType;

import java.util.regex.Pattern;

/**
 * Service for validating Git provider credentials and connection configurations.
 * Provides validation rules specific to each Git provider.
 */
public class CredentialValidationService {
    
    // URL validation patterns
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "^https?://(api\\.)?github\\.com(/.*)?$|^https?://[\\w.-]+/api/v3(/.*)?$"
    );
    private static final Pattern GITLAB_URL_PATTERN = Pattern.compile(
            "^https?://(www\\.)?gitlab\\.com(/.*)?$|^https?://[\\w.-]+(/.*)?$"
    );
    private static final Pattern BITBUCKET_URL_PATTERN = Pattern.compile(
            "^https?://(api\\.)?bitbucket\\.org(/.*)?$|^https?://[\\w.-]+(/.*)?$"
    );
    
    // Username validation patterns
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    
    // Minimum password requirements
    private static final int MIN_PASSWORD_LENGTH = 1;
    private static final int MAX_PASSWORD_LENGTH = 1000;
    
    /**
     * Validates a Git provider connection configuration.
     * 
     * @param connection The connection to validate
     * @return ValidationResult with details about validation status
     */
    public CredentialValidationResult validateConnection(GitProviderConnection connection) {
        if (connection == null) {
            return CredentialValidationResult.invalid("Connection cannot be null");
        }
        
        CredentialValidationResult.Builder resultBuilder = CredentialValidationResult.builder();
        
        // Validate provider type
        if (connection.getProviderType() == null) {
            resultBuilder.addError("Provider type is required");
        }
        
        // Validate base URL
        String baseUrlValidation = validateBaseUrl(connection.getProviderType(), connection.getBaseUrl());
        if (baseUrlValidation != null) {
            resultBuilder.addError(baseUrlValidation);
        }
        
        // Validate API URL
        String apiUrlValidation = validateApiUrl(connection.getProviderType(), connection.getApiUrl());
        if (apiUrlValidation != null) {
            resultBuilder.addError(apiUrlValidation);
        }
        
        // Validate username
        String usernameValidation = validateUsername(connection.getUsername());
        if (usernameValidation != null) {
            resultBuilder.addError(usernameValidation);
        }
        
        // Validate password
        String passwordValidation = validatePassword(connection.getPassword());
        if (passwordValidation != null) {
            resultBuilder.addError(passwordValidation);
        }
        
        // Provider-specific validations
        addProviderSpecificValidations(connection, resultBuilder);
        
        return resultBuilder.build();
    }
    
    /**
     * Validates just the credentials (username/password) without URL validation.
     * 
     * @param username The username to validate
     * @param password The password to validate
     * @return ValidationResult for credentials only
     */
    public CredentialValidationResult validateCredentials(String username, char[] password) {
        CredentialValidationResult.Builder resultBuilder = CredentialValidationResult.builder();
        
        String usernameValidation = validateUsername(username);
        if (usernameValidation != null) {
            resultBuilder.addError(usernameValidation);
        }
        
        String passwordValidation = validatePassword(password);
        if (passwordValidation != null) {
            resultBuilder.addError(passwordValidation);
        }
        
        return resultBuilder.build();
    }
    
    /**
     * Checks if a URL is valid for the given provider type.
     * 
     * @param providerType The Git provider type
     * @param url The URL to validate
     * @return true if URL is valid for the provider
     */
    public boolean isValidUrlForProvider(GitProviderType providerType, String url) {
        if (providerType == null || url == null || url.trim().isEmpty()) {
            return false;
        }
        
        switch (providerType) {
            case GITHUB:
                return GITHUB_URL_PATTERN.matcher(url).matches();
            case GITLAB:
                return GITLAB_URL_PATTERN.matcher(url).matches();
            case BITBUCKET:
                return BITBUCKET_URL_PATTERN.matcher(url).matches();
            default:
                return false;
        }
    }
    
    /**
     * Gets validation suggestions for a specific provider type.
     * 
     * @param providerType The Git provider type
     * @return Validation suggestions and requirements
     */
    public String getValidationSuggestions(GitProviderType providerType) {
        if (providerType == null) {
            return "Please select a Git provider type";
        }
        
        switch (providerType) {
            case GITHUB:
                return "For GitHub:\n" +
                       "• Use github.com for GitHub.com or your GitHub Enterprise URL\n" +
                       "• Username should be your GitHub username\n" +
                       "• Password should be a Personal Access Token (recommended) or your account password";
            case GITLAB:
                return "For GitLab:\n" +
                       "• Use gitlab.com for GitLab.com or your self-hosted GitLab URL\n" +
                       "• Username should be your GitLab username\n" +
                       "• Password should be a Personal Access Token or your account password";
            case BITBUCKET:
                return "For Bitbucket:\n" +
                       "• Use bitbucket.org for Bitbucket Cloud or your Bitbucket Server URL\n" +
                       "• Username should be your Bitbucket username\n" +
                       "• Password should be an App Password (recommended) or your account password";
            default:
                return "Please select a supported Git provider";
        }
    }
    
    private String validateBaseUrl(GitProviderType providerType, String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "Base URL is required";
        }
        
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            return "URL must start with http:// or https://";
        }
        
        if (providerType != null && !isValidUrlForProvider(providerType, baseUrl)) {
            return "URL format is not valid for " + providerType.getDisplayName();
        }
        
        return null;
    }
    
    private String validateApiUrl(GitProviderType providerType, String apiUrl) {
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            return "API URL is required";
        }
        
        if (!apiUrl.startsWith("http://") && !apiUrl.startsWith("https://")) {
            return "API URL must start with http:// or https://";
        }
        
        return null;
    }
    
    private String validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "Username is required";
        }
        
        if (username.length() > 100) {
            return "Username is too long (maximum 100 characters)";
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return "Username contains invalid characters (only letters, numbers, dots, hyphens, and underscores allowed)";
        }
        
        return null;
    }
    
    private String validatePassword(char[] password) {
        if (password == null || password.length == 0) {
            return "Password is required";
        }
        
        if (password.length < MIN_PASSWORD_LENGTH) {
            return "Password is too short";
        }
        
        if (password.length > MAX_PASSWORD_LENGTH) {
            return "Password is too long";
        }
        
        // Check for null characters (security concern)
        for (char c : password) {
            if (c == '\0') {
                return "Password contains invalid characters";
            }
        }
        
        return null;
    }
    
    private void addProviderSpecificValidations(GitProviderConnection connection, 
                                              CredentialValidationResult.Builder resultBuilder) {
        if (connection.getProviderType() == null) {
            return;
        }
        
        switch (connection.getProviderType()) {
            case GITHUB:
                validateGitHubSpecific(connection, resultBuilder);
                break;
            case GITLAB:
                validateGitLabSpecific(connection, resultBuilder);
                break;
            case BITBUCKET:
                validateBitbucketSpecific(connection, resultBuilder);
                break;
        }
    }
    
    private void validateGitHubSpecific(GitProviderConnection connection, 
                                      CredentialValidationResult.Builder resultBuilder) {
        // GitHub-specific validation rules
        if (connection.getBaseUrl() != null && 
            connection.getBaseUrl().contains("github.com") && 
            !connection.isVerifySsl()) {
            resultBuilder.addWarning("SSL verification is disabled for GitHub.com - this is not recommended");
        }
    }
    
    private void validateGitLabSpecific(GitProviderConnection connection, 
                                      CredentialValidationResult.Builder resultBuilder) {
        // GitLab-specific validation rules
        if (connection.isSelfHosted()) {
            resultBuilder.addInfo("Self-hosted GitLab instance detected");
        }
    }
    
    private void validateBitbucketSpecific(GitProviderConnection connection, 
                                         CredentialValidationResult.Builder resultBuilder) {
        // Bitbucket-specific validation rules
        if (connection.getBaseUrl() != null && 
            connection.getBaseUrl().contains("bitbucket.org")) {
            resultBuilder.addInfo("Using Bitbucket Cloud - consider using App Passwords instead of account passwords");
        }
    }
}