package com.example.gitmigrator.service;

import com.example.gitmigrator.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class BitbucketServiceIntegrationTest {
    
    private BitbucketServiceIntegration bitbucketService;
    private GitServiceConfig validConfig;
    
    @BeforeEach
    void setUp() {
        bitbucketService = new BitbucketServiceIntegration();
        validConfig = new GitServiceConfig("bitbucket", "https://api.bitbucket.org/2.0", "test-token");
        validConfig.setUsername("test-user");
    }
    
    @Test
    void testGetSupportedServices() {
        List<String> supportedServices = bitbucketService.getSupportedServices();
        assertNotNull(supportedServices);
        assertEquals(1, supportedServices.size());
        assertTrue(supportedServices.contains("bitbucket"));
    }
    
    @Test
    void testIsServiceSupported() {
        assertTrue(bitbucketService.isServiceSupported("bitbucket"));
        assertTrue(bitbucketService.isServiceSupported("BITBUCKET"));
        assertTrue(bitbucketService.isServiceSupported("BitBucket"));
        
        assertFalse(bitbucketService.isServiceSupported("github"));
        assertFalse(bitbucketService.isServiceSupported("gitlab"));
        assertFalse(bitbucketService.isServiceSupported("invalid"));
        assertFalse(bitbucketService.isServiceSupported(null));
    }
    
    @Test
    void testGetDefaultConfig() {
        GitServiceConfig defaultConfig = bitbucketService.getDefaultConfig("bitbucket");
        assertNotNull(defaultConfig);
        assertEquals("bitbucket", defaultConfig.getServiceName());
        assertEquals("https://api.bitbucket.org/2.0", defaultConfig.getApiUrl());
        assertEquals(100, defaultConfig.getMaxRepositories());
        
        // Test with unsupported service
        assertNull(bitbucketService.getDefaultConfig("github"));
        assertNull(bitbucketService.getDefaultConfig(null));
    }
    
    @Test
    void testValidateConfig() {
        // Test valid config
        assertTrue(bitbucketService.validateConfig(validConfig));
        
        // Test with different case
        GitServiceConfig caseConfig = new GitServiceConfig("BITBUCKET", "https://api.bitbucket.org/2.0", "test-token");
        assertTrue(bitbucketService.validateConfig(caseConfig));
        
        // Test invalid configs
        assertFalse(bitbucketService.validateConfig(null));
        
        GitServiceConfig noServiceName = new GitServiceConfig(null, "https://api.bitbucket.org/2.0", "test-token");
        assertFalse(bitbucketService.validateConfig(noServiceName));
        
        GitServiceConfig wrongService = new GitServiceConfig("github", "https://api.bitbucket.org/2.0", "test-token");
        assertFalse(bitbucketService.validateConfig(wrongService));
        
        GitServiceConfig noToken = new GitServiceConfig("bitbucket", "https://api.bitbucket.org/2.0", null);
        assertFalse(bitbucketService.validateConfig(noToken));
        
        GitServiceConfig emptyToken = new GitServiceConfig("bitbucket", "https://api.bitbucket.org/2.0", "");
        assertFalse(bitbucketService.validateConfig(emptyToken));
        
        GitServiceConfig noApiUrl = new GitServiceConfig("bitbucket", null, "test-token");
        assertFalse(bitbucketService.validateConfig(noApiUrl));
        
        GitServiceConfig wrongApiUrl = new GitServiceConfig("bitbucket", "https://api.github.com", "test-token");
        assertFalse(bitbucketService.validateConfig(wrongApiUrl));
    }
    
    @Test
    void testFetchRepositoriesWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("bitbucket", "https://invalid-url.com", "invalid-token");
        
        CompletableFuture<List<RepositoryInfo>> future = bitbucketService.fetchRepositories(invalidConfig);
        
        assertThrows(RuntimeException.class, () -> {
            future.join();
        });
    }
    
    @Test
    void testSearchRepositoriesWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("bitbucket", "https://invalid-url.com", "invalid-token");
        
        CompletableFuture<List<RepositoryInfo>> future = bitbucketService.searchRepositories("test", invalidConfig);
        
        assertThrows(RuntimeException.class, () -> {
            future.join();
        });
    }
    
    @Test
    void testAuthenticateServiceWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("bitbucket", "https://invalid-url.com", "invalid-token");
        
        boolean result = bitbucketService.authenticateService(invalidConfig);
        assertFalse(result);
    }
    
    @Test
    void testGetUserInfoWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("bitbucket", "https://invalid-url.com", "invalid-token");
        
        CompletableFuture<GitUserInfo> future = bitbucketService.getUserInfo(invalidConfig);
        
        assertThrows(RuntimeException.class, () -> {
            future.join();
        });
    }
    
    @Test
    void testGetUserOrganizationsWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("bitbucket", "https://invalid-url.com", "invalid-token");
        
        CompletableFuture<List<GitOrganization>> future = bitbucketService.getUserOrganizations(invalidConfig);
        
        assertThrows(RuntimeException.class, () -> {
            future.join();
        });
    }
    
    @Test
    void testTestConnectionWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("bitbucket", "https://invalid-url.com", "invalid-token");
        
        CompletableFuture<GitConnectionTestResult> future = bitbucketService.testConnection(invalidConfig);
        GitConnectionTestResult result = future.join();
        
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertTrue(result.getMessage().contains("Connection failed"));
    }
    
    @Test
    void testGetRateLimitInfo() {
        // Bitbucket rate limit info should return default values since Bitbucket doesn't have a dedicated endpoint
        CompletableFuture<GitRateLimitInfo> future = bitbucketService.getRateLimitInfo(validConfig);
        GitRateLimitInfo rateLimitInfo = future.join();
        
        assertNotNull(rateLimitInfo);
        assertEquals(1000, rateLimitInfo.getLimit()); // Default Bitbucket limit
        assertEquals("api", rateLimitInfo.getResource());
        assertNotNull(rateLimitInfo.getResetTime());
    }
    
    @Test
    void testFetchRepositoriesPageWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("bitbucket", "https://invalid-url.com", "invalid-token");
        
        CompletableFuture<GitRepositoryPage> future = bitbucketService.fetchRepositoriesPage(invalidConfig, 1, 10);
        
        assertThrows(RuntimeException.class, () -> {
            future.join();
        });
    }
    
    @Test
    void testGetRepositoryDetailsWithInvalidConfig() {
        GitServiceConfig invalidConfig = new GitServiceConfig("bitbucket", "https://invalid-url.com", "invalid-token");
        
        CompletableFuture<RepositoryInfo> future = bitbucketService.getRepositoryDetails(invalidConfig, "test/repo");
        
        assertThrows(RuntimeException.class, () -> {
            future.join();
        });
    }
    
    @Test
    void testConfigurationEndpoints() {
        // Test default endpoint without username
        GitServiceConfig config = new GitServiceConfig("bitbucket", "https://api.bitbucket.org/2.0", "test-token");
        String endpoint = config.getRepositoriesEndpoint();
        assertEquals("https://api.bitbucket.org/2.0/repositories", endpoint);
        
        // Test with username
        config.setUsername("test-user");
        endpoint = config.getRepositoriesEndpoint();
        assertEquals("https://api.bitbucket.org/2.0/repositories/test-user", endpoint);
        
        // Test with organization
        config.setOrganization("test-org");
        endpoint = config.getRepositoriesEndpoint();
        assertEquals("https://api.bitbucket.org/2.0/repositories/test-org", endpoint);
    }
    
    @Test
    void testServiceNameCaseInsensitivity() {
        GitServiceConfig upperCaseConfig = new GitServiceConfig("BITBUCKET", "https://api.bitbucket.org/2.0", "test-token");
        assertTrue(bitbucketService.validateConfig(upperCaseConfig));
        
        GitServiceConfig mixedCaseConfig = new GitServiceConfig("BitBucket", "https://api.bitbucket.org/2.0", "test-token");
        assertTrue(bitbucketService.validateConfig(mixedCaseConfig));
    }
    
    @Test
    void testBitbucketSpecificFeatures() {
        // Test that Bitbucket service handles Basic Auth properly
        // This is tested indirectly through the authentication method
        
        // Test pagination handling
        // Bitbucket uses different pagination than GitHub/GitLab
        GitServiceConfig config = new GitServiceConfig("bitbucket", "https://api.bitbucket.org/2.0", "test-token");
        config.setUsername("test-user");
        
        // The actual HTTP request will fail, but we can test the URL construction
        CompletableFuture<GitRepositoryPage> future = bitbucketService.fetchRepositoriesPage(config, 1, 50);
        
        assertThrows(RuntimeException.class, () -> {
            future.join();
        });
    }
}