package com.example.gitmigrator.service;

import com.example.gitmigrator.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class GitLabServiceIntegrationTest {
    
    private GitLabServiceIntegration gitlabService;
    private GitServiceConfig validConfig;
    
    @BeforeEach
    void setUp() {
        gitlabService = new GitLabServiceIntegration();
        validConfig = new GitServiceConfig("gitlab", "https://gitlab.com/api/v4", "test-token");
    }
    
    @Test
    void testGetSupportedServices() {
        List<String> supportedServices = gitlabService.getSupportedServices();
        assertNotNull(supportedServices);
        assertEquals(1, supportedServices.size());
        assertTrue(supportedServices.contains("gitlab"));
    }
    
    @Test
    void testIsServiceSupported() {
        assertTrue(gitlabService.isServiceSupported("gitlab"));
        assertTrue(gitlabService.isServiceSupported("GITLAB"));
        assertTrue(gitlabService.isServiceSupported("GitLab"));
        
        assertFalse(gitlabService.isServiceSupported("github"));
        assertFalse(gitlabService.isServiceSupported("bitbucket"));
        assertFalse(gitlabService.isServiceSupported("invalid"));
        assertFalse(gitlabService.isServiceSupported(null));
    }
    
    @Test
    void testGetDefaultConfig() {
        GitServiceConfig defaultConfig = gitlabService.getDefaultConfig("gitlab");
        assertNotNull(defaultConfig);
        assertEquals("gitlab", defaultConfig.getServiceName());
        assertEquals("https://gitlab.com/api/v4", defaultConfig.getApiUrl());
        assertEquals(100, defaultConfig.getMaxRepositories());
        
        // Test with unsupported service
        assertNull(gitlabService.getDefaultConfig("github"));
        assertNull(gitlabService.getDefaultConfig(null));
    }
    
    @Test
    void testValidateConfig() {
        // Test valid config
        assertTrue(gitlabService.validateConfig(validConfig));
        
        // Test with different case
        GitServiceConfig caseConfig = new GitServiceConfig("GITLAB", "https://gitlab.com/api/v4", "test-token");
        assertTrue(gitlabService.validateConfig(caseConfig));
        
        // Test invalid configs
        assertFalse(gitlabService.validateConfig(null));
        
        GitServiceConfig noServiceName = new GitServiceConfig(null, "https://gitlab.com/api/v4", "test-token");
        assertFalse(gitlabService.validateConfig(noServiceName));
        
        GitServiceConfig wrongService = new GitServiceConfig("github", "https://gitlab.com/api/v4", "test-token");
        assertFalse(gitlabService.validateConfig(wrongService));
        
        GitServiceConfig noToken = new GitServiceConfig("gitlab", "https://gitlab.com/api/v4", null);
        assertFalse(gitlabService.validateConfig(noToken));
        
        GitServiceConfig emptyToken = new GitServiceConfig("gitlab", "https://gitlab.com/api/v4", "");
        assertFalse(gitlabService.validateConfig(emptyToken));
        
        GitServiceConfig noApiUrl = new GitServiceConfig("gitlab", null, "test-token");
        assertFalse(gitlabService.validateConfig(noApiUrl));
        
        GitServiceConfig wrongApiUrl = new GitServiceConfig("gitlab", "https://api.github.com", "test-token");
        assertFalse(gitlabService.validateConfig(wrongApiUrl));
    }
    
    @Test
    void testFetchRepositoriesWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("gitlab", "https://invalid-url.com", "invalid-token");
        
        CompletableFuture<List<RepositoryInfo>> future = gitlabService.fetchRepositories(invalidConfig);
        
        assertThrows(RuntimeException.class, () -> {
            future.join();
        });
    }
    
    @Test
    void testSearchRepositoriesWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("gitlab", "https://invalid-url.com", "invalid-token");
        
        CompletableFuture<List<RepositoryInfo>> future = gitlabService.searchRepositories("test", invalidConfig);
        
        assertThrows(RuntimeException.class, () -> {
            future.join();
        });
    }
    
    @Test
    void testAuthenticateServiceWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("gitlab", "https://invalid-url.com", "invalid-token");
        
        boolean result = gitlabService.authenticateService(invalidConfig);
        assertFalse(result);
    }
    
    @Test
    void testGetUserInfoWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("gitlab", "https://invalid-url.com", "invalid-token");
        
        CompletableFuture<GitUserInfo> future = gitlabService.getUserInfo(invalidConfig);
        
        assertThrows(RuntimeException.class, () -> {
            future.join();
        });
    }
    
    @Test
    void testGetUserOrganizationsWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("gitlab", "https://invalid-url.com", "invalid-token");
        
        CompletableFuture<List<GitOrganization>> future = gitlabService.getUserOrganizations(invalidConfig);
        
        assertThrows(RuntimeException.class, () -> {
            future.join();
        });
    }
    
    @Test
    void testTestConnectionWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("gitlab", "https://invalid-url.com", "invalid-token");
        
        CompletableFuture<GitConnectionTestResult> future = gitlabService.testConnection(invalidConfig);
        GitConnectionTestResult result = future.join();
        
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertTrue(result.getMessage().contains("Connection failed"));
    }
    
    @Test
    void testGetRateLimitInfo() {
        // GitLab rate limit info should return default values since GitLab doesn't have a dedicated endpoint
        CompletableFuture<GitRateLimitInfo> future = gitlabService.getRateLimitInfo(validConfig);
        GitRateLimitInfo rateLimitInfo = future.join();
        
        assertNotNull(rateLimitInfo);
        assertEquals(2000, rateLimitInfo.getLimit()); // Default GitLab limit
        assertEquals("api", rateLimitInfo.getResource());
        assertNotNull(rateLimitInfo.getResetTime());
    }
    
    @Test
    void testFetchRepositoriesPageWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("gitlab", "https://invalid-url.com", "invalid-token");
        
        CompletableFuture<GitRepositoryPage> future = gitlabService.fetchRepositoriesPage(invalidConfig, 1, 10);
        
        assertThrows(RuntimeException.class, () -> {
            future.join();
        });
    }
    
    @Test
    void testGetRepositoryDetailsWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("gitlab", "https://invalid-url.com", "invalid-token");
        
        CompletableFuture<RepositoryInfo> future = gitlabService.getRepositoryDetails(invalidConfig, "test/repo");
        
        assertThrows(RuntimeException.class, () -> {
            future.join();
        });
    }
    
    @Test
    void testConfigurationEndpoints() {
        // Test default endpoint
        GitServiceConfig config = new GitServiceConfig("gitlab", "https://gitlab.com/api/v4", "test-token");
        String endpoint = config.getRepositoriesEndpoint();
        assertEquals("https://gitlab.com/api/v4/projects?membership=true", endpoint);
        
        // Test with organization
        config.setOrganization("test-org");
        endpoint = config.getRepositoriesEndpoint();
        assertEquals("https://gitlab.com/api/v4/groups/test-org/projects", endpoint);
    }
    
    @Test
    void testServiceNameCaseInsensitivity() {
        GitServiceConfig upperCaseConfig = new GitServiceConfig("GITLAB", "https://gitlab.com/api/v4", "test-token");
        assertTrue(gitlabService.validateConfig(upperCaseConfig));
        
        GitServiceConfig mixedCaseConfig = new GitServiceConfig("GitLab", "https://gitlab.com/api/v4", "test-token");
        assertTrue(gitlabService.validateConfig(mixedCaseConfig));
    }
}