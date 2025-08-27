package com.example.gitmigrator.service;

import com.example.gitmigrator.model.GitProviderConnection;
import com.example.gitmigrator.model.GitProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GitLabRepositoryDiscoveryService.
 * Note: These tests use mock data and don't make actual API calls.
 */
class GitLabRepositoryDiscoveryServiceTest {
    
    private GitLabRepositoryDiscoveryService service;
    private GitProviderConnection testConnection;
    
    @BeforeEach
    void setUp() {
        service = new GitLabRepositoryDiscoveryService();
        testConnection = new GitProviderConnection(
                GitProviderType.GITLAB,
                "testuser",
                "testtoken"
        );
    }
    
    @Test
    void testGetSupportedProvider() {
        assertEquals(GitProviderType.GITLAB, service.getSupportedProvider());
    }
    
    @Test
    void testSupportsConnection() {
        assertTrue(service.supportsConnection(testConnection));
        
        // Test with null connection
        assertFalse(service.supportsConnection(null));
        
        // Test with different provider type
        GitProviderConnection githubConnection = new GitProviderConnection(
                GitProviderType.GITHUB,
                "testuser",
                "testtoken"
        );
        assertFalse(service.supportsConnection(githubConnection));
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
    void testSelfHostedConnection() {
        // Test self-hosted GitLab instance
        GitProviderConnection selfHostedConnection = new GitProviderConnection(
                GitProviderType.GITLAB,
                "https://gitlab.company.com",
                "testuser",
                "testtoken"
        );
        
        assertTrue(selfHostedConnection.isSelfHosted());
        assertTrue(service.supportsConnection(selfHostedConnection));
        assertEquals("https://gitlab.company.com/api/v4", selfHostedConnection.getApiUrl());
    }
    
    @Test
    void testServiceInstantiation() {
        // Test default constructor
        GitLabRepositoryDiscoveryService defaultService = new GitLabRepositoryDiscoveryService();
        assertNotNull(defaultService);
        assertEquals(GitProviderType.GITLAB, defaultService.getSupportedProvider());
        
        // Test constructor with auth helper
        SecureAuthenticationHelper authHelper = new SecureAuthenticationHelper();
        GitLabRepositoryDiscoveryService serviceWithHelper = new GitLabRepositoryDiscoveryService(authHelper);
        assertNotNull(serviceWithHelper);
        assertEquals(GitProviderType.GITLAB, serviceWithHelper.getSupportedProvider());
    }
}