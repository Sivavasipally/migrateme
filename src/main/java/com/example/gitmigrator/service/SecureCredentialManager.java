package com.example.gitmigrator.service;

import com.example.gitmigrator.model.GitProviderConnection;


import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Secure credential manager for handling Git provider credentials.
 * Provides session-only storage with automatic cleanup and timeout functionality.
 */
public class SecureCredentialManager {
    
    private static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 30;
    private static final int CLEANUP_INTERVAL_MINUTES = 5;
    
    private final Map<String, CredentialSession> activeSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private final SecureRandom secureRandom = new SecureRandom();
    
    public SecureCredentialManager() {
        // Start cleanup task
        cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredSessions,
                CLEANUP_INTERVAL_MINUTES,
                CLEANUP_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }
    
    /**
     * Stores credentials securely for a session.
     * 
     * @param connection The Git provider connection with credentials
     * @return Session ID for retrieving credentials later
     */
    public String storeCredentials(GitProviderConnection connection) {
        if (connection == null || !connection.isValid()) {
            throw new IllegalArgumentException("Invalid connection provided");
        }
        
        String sessionId = generateSessionId();
        CredentialSession session = new CredentialSession(connection);
        activeSessions.put(sessionId, session);
        
        return sessionId;
    }
    
    /**
     * Retrieves credentials for a session.
     * 
     * @param sessionId The session ID
     * @return GitProviderConnection with credentials, or null if not found/expired
     */
    public GitProviderConnection getCredentials(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        
        CredentialSession session = activeSessions.get(sessionId);
        if (session == null || session.isExpired()) {
            if (session != null) {
                removeSession(sessionId);
            }
            return null;
        }
        
        // Update last accessed time
        session.updateLastAccessed();
        return session.getConnection();
    }
    
    /**
     * Removes credentials for a session.
     * 
     * @param sessionId The session ID to remove
     */
    public void removeSession(String sessionId) {
        CredentialSession session = activeSessions.remove(sessionId);
        if (session != null) {
            session.clearCredentials();
        }
    }
    
    /**
     * Extends the session timeout for active credentials.
     * 
     * @param sessionId The session ID
     * @param additionalMinutes Additional minutes to extend
     * @return true if session was extended, false if not found
     */
    public boolean extendSession(String sessionId, int additionalMinutes) {
        CredentialSession session = activeSessions.get(sessionId);
        if (session != null && !session.isExpired()) {
            session.extendTimeout(additionalMinutes);
            return true;
        }
        return false;
    }
    
    /**
     * Checks if a session is active and valid.
     * 
     * @param sessionId The session ID
     * @return true if session is active and not expired
     */
    public boolean isSessionActive(String sessionId) {
        CredentialSession session = activeSessions.get(sessionId);
        return session != null && !session.isExpired();
    }
    
    /**
     * Gets the number of active sessions.
     * 
     * @return Number of active credential sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * Clears all stored credentials and sessions.
     */
    public void clearAllSessions() {
        activeSessions.values().forEach(CredentialSession::clearCredentials);
        activeSessions.clear();
    }
    
    /**
     * Validates that credentials are properly secured.
     * 
     * @param connection The connection to validate
     * @return true if credentials are properly secured
     */
    public boolean validateCredentialSecurity(GitProviderConnection connection) {
        if (connection == null) {
            return false;
        }
        
        // Check that password is stored as char array (not string)
        char[] password = connection.getPassword();
        if (password == null || password.length == 0) {
            return false;
        }
        
        // Validate that connection has proper security settings
        return connection.isValid();
    }
    
    private String generateSessionId() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    private void cleanupExpiredSessions() {
        activeSessions.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                entry.getValue().clearCredentials();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Internal class for managing credential sessions.
     */
    private static class CredentialSession {
        private final GitProviderConnection connection;
        private LocalDateTime createdAt;
        private LocalDateTime lastAccessedAt;
        private LocalDateTime expiresAt;
        
        public CredentialSession(GitProviderConnection connection) {
            this.connection = connection;
            this.createdAt = LocalDateTime.now();
            this.lastAccessedAt = LocalDateTime.now();
            this.expiresAt = LocalDateTime.now().plusMinutes(DEFAULT_SESSION_TIMEOUT_MINUTES);
        }
        
        public GitProviderConnection getConnection() {
            return connection;
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
        
        public void updateLastAccessed() {
            this.lastAccessedAt = LocalDateTime.now();
        }
        
        public void extendTimeout(int additionalMinutes) {
            this.expiresAt = this.expiresAt.plusMinutes(additionalMinutes);
        }
        
        public void clearCredentials() {
            if (connection != null) {
                connection.clearCredentials();
            }
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public LocalDateTime getLastAccessedAt() {
            return lastAccessedAt;
        }
        
        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }
    }
    
    /**
     * Shutdown hook to clean up resources.
     */
    public void shutdown() {
        clearAllSessions();
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}