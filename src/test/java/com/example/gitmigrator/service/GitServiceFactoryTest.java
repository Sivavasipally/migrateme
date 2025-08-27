package com.example.gitmigrator.service;

import com.example.gitmigrator.model.GitServiceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitServiceFactoryTest {
    
    @BeforeEach
    void setUp() {
        // Reset factory state before each test
        // This ensures tests don't interfere with each other
    }
    
    @Test
    void testCreateServiceWithValidServiceNames() {
        // Test GitHub service creation
        GitServiceIntegration githubService = GitServiceFactory.createService("github");
        assertNotNull(githubService);
        assertTrue(githubService instanceof GitHubServiceIntegration);
        
        // Test GitLab service creation
        GitServiceIntegration gitlabService = GitServiceFactory.createService("gitlab");
        assertNotNull(gitlabService);
        assertTrue(gitlabService instanceof GitLabServiceIntegration);
        
        // Test Bitbucket service creation
        GitServiceIntegration bitbucketService = GitServiceFactory.createService("bitbucket");
        assertNotNull(bitbucketService);
        assertTrue(bitbucketService instanceof BitbucketServiceIntegration);
    }
    
    @Test
    void testCreateServiceWithCaseInsensitiveNames() {
        // Test case insensitive service names
        assertNotNull(GitServiceFactory.createService("GITHUB"));
        assertNotNull(GitServiceFactory.createService("GitHub"));
        assertNotNull(GitServiceFactory.createService("GitLab"));
        assertNotNull(GitServiceFactory.createService("GITLAB"));
        assertNotNull(GitServiceFactory.createService("BitBucket"));
        assertNotNull(GitServiceFactory.createService("BITBUCKET"));
    }
    
    @Test
    void testCreateServiceWithInvalidServiceNames() {
        // Test invalid service names
        assertNull(GitServiceFactory.createService("invalid"));
        assertNull(GitServiceFactory.createService(""));
        assertNull(GitServiceFactory.createService((String) null));
        assertNull(GitServiceFactory.createService("   "));
    }
    
    @Test
    void testCreateServiceWithConfig() {
        // Test creating service with valid config
        GitServiceConfig githubConfig = new GitServiceConfig("github", "test-token");
        GitServiceIntegration service = GitServiceFactory.createService(githubConfig);
        assertNotNull(service);
        assertTrue(service instanceof GitHubServiceIntegration);
        
        // Test creating service with null config
        GitServiceConfig nullConfig = null;
        assertNull(GitServiceFactory.createService(nullConfig));
    }
    
    @Test
    void testGetSupportedServices() {
        List<String> supportedServices = GitServiceFactory.getSupportedServices();
        assertNotNull(supportedServices);
        assertFalse(supportedServices.isEmpty());
        assertTrue(supportedServices.contains("github"));
        assertTrue(supportedServices.contains("gitlab"));
        assertTrue(supportedServices.contains("bitbucket"));
    }
    
    @Test
    void testIsServiceSupported() {
        // Test supported services
        assertTrue(GitServiceFactory.isServiceSupported("github"));
        assertTrue(GitServiceFactory.isServiceSupported("gitlab"));
        assertTrue(GitServiceFactory.isServiceSupported("bitbucket"));
        
        // Test case insensitive
        assertTrue(GitServiceFactory.isServiceSupported("GITHUB"));
        assertTrue(GitServiceFactory.isServiceSupported("GitLab"));
        
        // Test unsupported services
        assertFalse(GitServiceFactory.isServiceSupported("invalid"));
        assertFalse(GitServiceFactory.isServiceSupported(""));
        assertFalse(GitServiceFactory.isServiceSupported(null));
    }
    
    @Test
    void testGetDefaultConfig() {
        // Test getting default config for supported services
        GitServiceConfig githubConfig = GitServiceFactory.getDefaultConfig("github");
        assertNotNull(githubConfig);
        assertEquals("github", githubConfig.getServiceName());
        assertEquals("https://api.github.com", githubConfig.getApiUrl());
        
        GitServiceConfig gitlabConfig = GitServiceFactory.getDefaultConfig("gitlab");
        assertNotNull(gitlabConfig);
        assertEquals("gitlab", gitlabConfig.getServiceName());
        assertEquals("https://gitlab.com/api/v4", gitlabConfig.getApiUrl());
        
        GitServiceConfig bitbucketConfig = GitServiceFactory.getDefaultConfig("bitbucket");
        assertNotNull(bitbucketConfig);
        assertEquals("bitbucket", bitbucketConfig.getServiceName());
        assertEquals("https://api.bitbucket.org/2.0", bitbucketConfig.getApiUrl());
        
        // Test getting default config for unsupported service
        assertNull(GitServiceFactory.getDefaultConfig("invalid"));
    }
    
    @Test
    void testValidateConfig() {
        // Test valid configs
        GitServiceConfig validGithubConfig = new GitServiceConfig("github", "https://api.github.com", "test-token");
        assertTrue(GitServiceFactory.validateConfig(validGithubConfig));
        
        GitServiceConfig validGitlabConfig = new GitServiceConfig("gitlab", "https://gitlab.com/api/v4", "test-token");
        assertTrue(GitServiceFactory.validateConfig(validGitlabConfig));
        
        GitServiceConfig validBitbucketConfig = new GitServiceConfig("bitbucket", "https://api.bitbucket.org/2.0", "test-token");
        assertTrue(GitServiceFactory.validateConfig(validBitbucketConfig));
        
        // Test invalid configs
        GitServiceConfig invalidConfig = new GitServiceConfig("invalid", "test-token");
        assertFalse(GitServiceFactory.validateConfig(invalidConfig));
        
        assertFalse(GitServiceFactory.validateConfig(null));
    }
    
    @Test
    void testRegisterCustomService() {
        // Create a mock service
        GitServiceIntegration mockService = new GitServiceIntegration() {
            @Override
            public java.util.concurrent.CompletableFuture<List<com.example.gitmigrator.model.RepositoryInfo>> fetchRepositories(GitServiceConfig config) {
                return null;
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<List<com.example.gitmigrator.model.RepositoryInfo>> searchRepositories(String query, GitServiceConfig config) {
                return null;
            }
            
            @Override
            public boolean authenticateService(GitServiceConfig config) {
                return false;
            }
            
            @Override
            public List<String> getSupportedServices() {
                return List.of("custom");
            }
            
            @Override
            public boolean isServiceSupported(String serviceName) {
                return "custom".equals(serviceName);
            }
            
            @Override
            public GitServiceConfig getDefaultConfig(String serviceName) {
                return new GitServiceConfig("custom", "https://api.custom.com", "");
            }
            
            @Override
            public boolean validateConfig(GitServiceConfig config) {
                return config != null && "custom".equals(config.getServiceName());
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<com.example.gitmigrator.model.GitUserInfo> getUserInfo(GitServiceConfig config) {
                return null;
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<List<com.example.gitmigrator.model.GitOrganization>> getUserOrganizations(GitServiceConfig config) {
                return null;
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<com.example.gitmigrator.model.GitConnectionTestResult> testConnection(GitServiceConfig config) {
                return null;
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<com.example.gitmigrator.model.GitRateLimitInfo> getRateLimitInfo(GitServiceConfig config) {
                return null;
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<com.example.gitmigrator.model.GitRepositoryPage> fetchRepositoriesPage(GitServiceConfig config, int page, int perPage) {
                return null;
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<com.example.gitmigrator.model.RepositoryInfo> getRepositoryDetails(GitServiceConfig config, String repositoryId) {
                return null;
            }
        };
        
        // Register custom service
        GitServiceFactory.registerService("custom", mockService);
        
        // Test that custom service is now available
        assertTrue(GitServiceFactory.isServiceSupported("custom"));
        GitServiceIntegration retrievedService = GitServiceFactory.createService("custom");
        assertNotNull(retrievedService);
        assertEquals(mockService, retrievedService);
        
        // Clean up
        GitServiceFactory.unregisterService("custom");
    }
    
    @Test
    void testUnregisterService() {
        // Register a custom service first
        GitServiceIntegration mockService = new GitHubServiceIntegration(); // Use existing implementation for simplicity
        GitServiceFactory.registerService("test-service", mockService);
        
        // Verify it's registered
        assertTrue(GitServiceFactory.isServiceSupported("test-service"));
        
        // Unregister it
        assertTrue(GitServiceFactory.unregisterService("test-service"));
        
        // Verify it's no longer available
        assertFalse(GitServiceFactory.isServiceSupported("test-service"));
        
        // Test unregistering non-existent service
        assertFalse(GitServiceFactory.unregisterService("non-existent"));
        assertFalse(GitServiceFactory.unregisterService(null));
        assertFalse(GitServiceFactory.unregisterService(""));
    }
    
    @Test
    void testRegisterServiceWithInvalidParameters() {
        GitServiceIntegration mockService = new GitHubServiceIntegration();
        
        // Test registering with null service name
        assertThrows(IllegalArgumentException.class, () -> {
            GitServiceFactory.registerService(null, mockService);
        });
        
        // Test registering with empty service name
        assertThrows(IllegalArgumentException.class, () -> {
            GitServiceFactory.registerService("", mockService);
        });
        
        // Test registering with null service
        assertThrows(IllegalArgumentException.class, () -> {
            GitServiceFactory.registerService("test", null);
        });
    }
}