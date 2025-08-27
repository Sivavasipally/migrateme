package com.example.gitmigrator.service;

import com.example.gitmigrator.model.GitProviderConnection;
import com.example.gitmigrator.model.GitProviderType;
import com.example.gitmigrator.model.RepositoryMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GitHubRepositoryDiscoveryService.
 * Note: These tests use mock data and don't make actual API calls.
 */
class GitHubRepositoryDiscoveryServiceTest {
    
    private GitHubRepositoryDiscoveryService service;
    private GitProviderConnection testConnection;
    
    @BeforeEach
    void setUp() {
        service = new GitHubRepositoryDiscoveryService();
        testConnection = new GitProviderConnection(
                GitProviderType.GITHUB,
                "testuser",
                "testpassword"
        );
    }
    
    @Test
    void testGetSupportedProvider() {
        assertEquals(GitProviderType.GITHUB, service.getSupportedProvider());
    }
    
    @Test
    void testSupportsConnection() {
        assertTrue(service.supportsConnection(testConnection));
        
        // Test with null connection
        assertFalse(service.supportsConnection(null));
        
        // Test with different provider type
        GitProviderConnection gitlabConnection = new GitProviderConnection(
                GitProviderType.GITLAB,
                "testuser",
                "testpassword"
        );
        assertFalse(service.supportsConnection(gitlabConnection));
    }
    
    @Test
    void testGetMaxRepositoriesPerCall() {
        assertEquals(100, service.getMaxRepositoriesPerCall());
    }
    
    @Test
    void testSupportsAdvancedFiltering() {
        assertTrue(service.supportsAdvancedFiltering());
    }
    
    @Test
    void testConnectionValidation() {
        // Test valid connection
        assertTrue(testConnection.isValid());
        
        // Test invalid connection (no username)
        GitProviderConnection invalidConnection = new GitProviderConnection(
                GitProviderType.GITHUB,
                null,
                "testpassword"
        );
        assertFalse(invalidConnection.isValid());
        assertFalse(service.supportsConnection(invalidConnection));
    }
    
    @Test
    void testServiceInstantiation() {
        // Test default constructor
        GitHubRepositoryDiscoveryService defaultService = new GitHubRepositoryDiscoveryService();
        assertNotNull(defaultService);
        assertEquals(GitProviderType.GITHUB, defaultService.getSupportedProvider());
        
        // Test constructor with auth helper
        SecureAuthenticationHelper authHelper = new SecureAuthenticationHelper();
        GitHubRepositoryDiscoveryService serviceWithHelper = new GitHubRepositoryDiscoveryService(authHelper);
        assertNotNull(serviceWithHelper);
        assertEquals(GitProviderType.GITHUB, serviceWithHelper.getSupportedProvider());
    }
    
    // Note: Integration tests with actual GitHub API would require valid credentials
    // and should be run separately with proper test configuration
}