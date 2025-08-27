package com.example.gitmigrator.service;

import com.example.gitmigrator.model.GitServiceConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Git service implementations.
 */
class GitServiceIntegrationTest {
    
    @Test
    void testAllServicesAreAvailable() {
        // Test that all expected services are available through the factory
        List<String> supportedServices = GitServiceFactory.getSupportedServices();
        
        assertNotNull(supportedServices);
        assertTrue(supportedServices.contains("github"));
        assertTrue(supportedServices.contains("gitlab"));
        assertTrue(supportedServices.contains("bitbucket"));
        assertEquals(3, supportedServices.size());
    }
    
    @Test
    void testServiceCreation() {
        // Test GitHub service
        GitServiceIntegration githubService = GitServiceFactory.createService("github");
        assertNotNull(githubService);
        assertTrue(githubService instanceof GitHubServiceIntegration);
        assertTrue(githubService.isServiceSupported("github"));
        
        // Test GitLab service
        GitServiceIntegration gitlabService = GitServiceFactory.createService("gitlab");
        assertNotNull(gitlabService);
        assertTrue(gitlabService instanceof GitLabServiceIntegration);
        assertTrue(gitlabService.isServiceSupported("gitlab"));
        
        // Test Bitbucket service
        GitServiceIntegration bitbucketService = GitServiceFactory.createService("bitbucket");
        assertNotNull(bitbucketService);
        assertTrue(bitbucketService instanceof BitbucketServiceIntegration);
        assertTrue(bitbucketService.isServiceSupported("bitbucket"));
    }
    
    @Test
    void testDefaultConfigurations() {
        // Test GitHub default config
        GitServiceConfig githubConfig = GitServiceFactory.getDefaultConfig("github");
        assertNotNull(githubConfig);
        assertEquals("github", githubConfig.getServiceName());
        assertEquals("https://api.github.com", githubConfig.getApiUrl());
        
        // Test GitLab default config
        GitServiceConfig gitlabConfig = GitServiceFactory.getDefaultConfig("gitlab");
        assertNotNull(gitlabConfig);
        assertEquals("gitlab", gitlabConfig.getServiceName());
        assertEquals("https://gitlab.com/api/v4", gitlabConfig.getApiUrl());
        
        // Test Bitbucket default config
        GitServiceConfig bitbucketConfig = GitServiceFactory.getDefaultConfig("bitbucket");
        assertNotNull(bitbucketConfig);
        assertEquals("bitbucket", bitbucketConfig.getServiceName());
        assertEquals("https://api.bitbucket.org/2.0", bitbucketConfig.getApiUrl());
    }
    
    @Test
    void testConfigValidation() {
        // Test valid configurations
        GitServiceConfig validGithubConfig = new GitServiceConfig("github", "https://api.github.com", "test-token");
        assertTrue(GitServiceFactory.validateConfig(validGithubConfig));
        
        GitServiceConfig validGitlabConfig = new GitServiceConfig("gitlab", "https://gitlab.com/api/v4", "test-token");
        assertTrue(GitServiceFactory.validateConfig(validGitlabConfig));
        
        GitServiceConfig validBitbucketConfig = new GitServiceConfig("bitbucket", "https://api.bitbucket.org/2.0", "test-token");
        assertTrue(GitServiceFactory.validateConfig(validBitbucketConfig));
        
        // Test invalid configuration
        GitServiceConfig invalidConfig = new GitServiceConfig("invalid", "test-token");
        assertFalse(GitServiceFactory.validateConfig(invalidConfig));
    }
    
    @Test
    void testServiceSpecificFeatures() {
        // Test that each service has its specific supported services list
        GitServiceIntegration githubService = GitServiceFactory.createService("github");
        assertEquals(List.of("github"), githubService.getSupportedServices());
        
        GitServiceIntegration gitlabService = GitServiceFactory.createService("gitlab");
        assertEquals(List.of("gitlab"), gitlabService.getSupportedServices());
        
        GitServiceIntegration bitbucketService = GitServiceFactory.createService("bitbucket");
        assertEquals(List.of("bitbucket"), bitbucketService.getSupportedServices());
    }
}