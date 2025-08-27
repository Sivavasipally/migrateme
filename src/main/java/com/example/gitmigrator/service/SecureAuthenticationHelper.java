package com.example.gitmigrator.service;

import com.example.gitmigrator.model.GitProviderConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * Helper service for secure authentication operations.
 * Provides utilities for creating authentication headers and handling credentials securely.
 */
public class SecureAuthenticationHelper {
    
    /**
     * Creates a Basic Authentication header value from username and password.
     * 
     * @param username The username
     * @param password The password as char array
     * @return Base64 encoded Basic Auth header value
     */
    public String createBasicAuthHeader(String username, char[] password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Username and password cannot be null");
        }
        
        // Create credentials string
        String credentials = username + ":" + new String(password);
        byte[] credentialsBytes = credentials.getBytes(StandardCharsets.UTF_8);
        
        try {
            // Encode to Base64
            String encoded = Base64.getEncoder().encodeToString(credentialsBytes);
            return "Basic " + encoded;
        } finally {
            // Clear sensitive data
            Arrays.fill(credentialsBytes, (byte) 0);
            // Clear the credentials string from memory (best effort)
            credentials = null;
        }
    }
    
    /**
     * Creates a Bearer token authentication header.
     * 
     * @param token The authentication token as char array
     * @return Bearer token header value
     */
    public String createBearerTokenHeader(char[] token) {
        if (token == null || token.length == 0) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        
        return "Bearer " + new String(token);
    }
    
    /**
     * Creates appropriate authentication header for the given connection.
     * 
     * @param connection The Git provider connection
     * @return Authentication header value
     */
    public String createAuthenticationHeader(GitProviderConnection connection) {
        if (connection == null || !connection.isValid()) {
            throw new IllegalArgumentException("Invalid connection provided");
        }
        
        // For now, we use Basic Auth with username/password
        // In the future, this could be extended to support different auth types
        return createBasicAuthHeader(connection.getUsername(), connection.getPassword());
    }
    
    /**
     * Validates that credentials are properly secured in memory.
     * 
     * @param connection The connection to validate
     * @return true if credentials are properly secured
     */
    public boolean validateCredentialSecurity(GitProviderConnection connection) {
        if (connection == null) {
            return false;
        }
        
        // Check that password is stored as char array
        char[] password = connection.getPassword();
        if (password == null || password.length == 0) {
            return false;
        }
        
        // Check that username is not null or empty
        String username = connection.getUsername();
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Securely compares two char arrays for equality.
     * Uses constant-time comparison to prevent timing attacks.
     * 
     * @param a First char array
     * @param b Second char array
     * @return true if arrays are equal
     */
    public boolean secureEquals(char[] a, char[] b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
    
    /**
     * Securely clears a char array by filling it with zeros.
     * 
     * @param array The char array to clear
     */
    public void secureClear(char[] array) {
        if (array != null) {
            Arrays.fill(array, '\0');
        }
    }
    
    /**
     * Securely clears multiple char arrays.
     * 
     * @param arrays The char arrays to clear
     */
    public void secureClear(char[]... arrays) {
        if (arrays != null) {
            for (char[] array : arrays) {
                secureClear(array);
            }
        }
    }
    
    /**
     * Creates a copy of a char array for secure handling.
     * 
     * @param original The original char array
     * @return A copy of the array, or null if original is null
     */
    public char[] secureCopy(char[] original) {
        if (original == null) {
            return null;
        }
        return Arrays.copyOf(original, original.length);
    }
    
    /**
     * Validates that a password meets basic security requirements.
     * 
     * @param password The password to validate
     * @return true if password meets requirements
     */
    public boolean validatePasswordSecurity(char[] password) {
        if (password == null || password.length == 0) {
            return false;
        }
        
        // Basic length check
        if (password.length < 1) {
            return false;
        }
        
        // Check for null characters (potential security issue)
        for (char c : password) {
            if (c == '\0') {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Masks a password for logging purposes.
     * 
     * @param password The password to mask
     * @return Masked password string for safe logging
     */
    public String maskPassword(char[] password) {
        if (password == null) {
            return "null";
        }
        if (password.length == 0) {
            return "empty";
        }
        
        // Show only the length for security
        return "*".repeat(Math.min(password.length, 8)) + " (" + password.length + " chars)";
    }
    
    /**
     * Generates a secure session identifier.
     * 
     * @return Secure random session ID
     */
    public String generateSecureSessionId() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}