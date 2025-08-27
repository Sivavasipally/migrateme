package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.GitProviderType;
import com.example.gitmigrator.model.RepositoryFilter;
import com.example.gitmigrator.model.RepositoryMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RepositoryDiscoveryPanel that don't require JavaFX UI.
 * Tests the core logic and data handling functionality.
 */
class RepositoryDiscoveryPanelUnitTest {
    
    private List<RepositoryMetadata> testRepositories;
    
    @BeforeEach
    void setUp() {
        testRepositories = Arrays.asList(
            RepositoryMetadata.builder()
                .id("1")
                .name("spring-boot-app")
                .fullName("myorg/spring-boot-app")
                .description("A Spring Boot application")
                .cloneUrl("https://github.com/myorg/spring-boot-app.git")
                .language("Java")
                .size(1024)
                .starCount(15)
                .isPrivate(false)
                .isFork(false)
                .isArchived(false)
                .updatedAt(LocalDateTime.now().minusDays(5))
                .providerType(GitProviderType.GITHUB)
                .build(),
            
            RepositoryMetadata.builder()
                .id("2")
                .name("react-frontend")
                .fullName("myorg/react-frontend")
                .description("React frontend application")
                .cloneUrl("https://github.com/myorg/react-frontend.git")
                .language("JavaScript")
                .size(512)
                .starCount(8)
                .isPrivate(true)
                .isFork(false)
                .isArchived(false)
                .updatedAt(LocalDateTime.now().minusDays(2))
                .providerType(GitProviderType.GITHUB)
                .build(),
            
            RepositoryMetadata.builder()
                .id("3")
                .name("python-utils")
                .fullName("myorg/python-utils")
                .description("Python utility functions")
                .cloneUrl("https://github.com/myorg/python-utils.git")
                .language("Python")
                .size(256)
                .starCount(3)
                .isPrivate(false)
                .isFork(true)
                .isArchived(false)
                .updatedAt(LocalDateTime.now().minusDays(10))
                .providerType(GitProviderType.GITHUB)
                .build()
        );
    }
    
    @Test
    void testRepositoryFilterMatching() {
        RepositoryFilter filter = new RepositoryFilter();
        
        // Test search query matching
        filter.setSearchQuery("spring");
        assertTrue(filter.matches(testRepositories.get(0))); // spring-boot-app
        assertFalse(filter.matches(testRepositories.get(1))); // react-frontend
        assertFalse(filter.matches(testRepositories.get(2))); // python-utils
        
        // Test language filtering
        filter.clear();
        filter.setLanguages(Set.of("Java"));
        assertTrue(filter.matches(testRepositories.get(0))); // Java
        assertFalse(filter.matches(testRepositories.get(1))); // JavaScript
        assertFalse(filter.matches(testRepositories.get(2))); // Python
        
        // Test privacy filtering
        filter.clear();
        filter.setIncludePrivate(false);
        assertTrue(filter.matches(testRepositories.get(0))); // public
        assertFalse(filter.matches(testRepositories.get(1))); // private
        assertTrue(filter.matches(testRepositories.get(2))); // public
        
        // Test fork filtering
        filter.clear();
        filter.setIncludeForks(false);
        assertTrue(filter.matches(testRepositories.get(0))); // not a fork
        assertTrue(filter.matches(testRepositories.get(1))); // not a fork
        assertFalse(filter.matches(testRepositories.get(2))); // is a fork
    }
    
    @Test
    void testRepositoryFilterCombination() {
        RepositoryFilter filter = new RepositoryFilter();
        
        // Combine search and language filters
        filter.setSearchQuery("app");
        filter.setLanguages(Set.of("Java"));
        
        assertTrue(filter.matches(testRepositories.get(0))); // spring-boot-app, Java
        assertFalse(filter.matches(testRepositories.get(1))); // react-frontend, JavaScript
        assertFalse(filter.matches(testRepositories.get(2))); // python-utils, Python
        
        // Test with no matches
        filter.setSearchQuery("nonexistent");
        assertFalse(filter.matches(testRepositories.get(0)));
        assertFalse(filter.matches(testRepositories.get(1)));
        assertFalse(filter.matches(testRepositories.get(2)));
    }
    
    @Test
    void testRepositoryFilterHasActiveCriteria() {
        RepositoryFilter filter = new RepositoryFilter();
        
        // Initially no active criteria
        assertFalse(filter.hasActiveCriteria());
        
        // Add search query
        filter.setSearchQuery("test");
        assertTrue(filter.hasActiveCriteria());
        
        // Clear and add language filter
        filter.clear();
        assertFalse(filter.hasActiveCriteria());
        
        filter.setLanguages(Set.of("Java"));
        assertTrue(filter.hasActiveCriteria());
        
        // Clear and add privacy filter
        filter.clear();
        filter.setIncludePrivate(false);
        assertTrue(filter.hasActiveCriteria());
    }
    
    @Test
    void testRepositoryMetadataConversion() {
        RepositoryMetadata metadata = testRepositories.get(0);
        
        // Test conversion to RepositoryInfo
        var repoInfo = metadata.toRepositoryInfo();
        assertEquals(metadata.getName(), repoInfo.getName());
        assertEquals(metadata.getCloneUrl(), repoInfo.getUrl());
        assertEquals(metadata.getDescription(), repoInfo.getDescription());
        assertEquals(metadata.getDefaultBranch(), repoInfo.getDefaultBranch());
    }
    
    @Test
    void testRepositoryMetadataFormatting() {
        RepositoryMetadata metadata = testRepositories.get(0);
        
        // Test size formatting
        assertEquals("1.0 MB", metadata.getFormattedSize());
        
        // Test most recent activity
        assertNotNull(metadata.getMostRecentActivity());
        assertEquals(metadata.getUpdatedAt(), metadata.getMostRecentActivity());
    }
    
    @Test
    void testRepositoryMetadataBuilder() {
        RepositoryMetadata metadata = RepositoryMetadata.builder()
            .id("test-id")
            .name("test-repo")
            .fullName("org/test-repo")
            .description("Test repository")
            .cloneUrl("https://github.com/org/test-repo.git")
            .language("Java")
            .size(1024)
            .starCount(5)
            .isPrivate(false)
            .isFork(false)
            .isArchived(false)
            .updatedAt(LocalDateTime.now())
            .providerType(GitProviderType.GITHUB)
            .build();
        
        assertEquals("test-id", metadata.getId());
        assertEquals("test-repo", metadata.getName());
        assertEquals("org/test-repo", metadata.getFullName());
        assertEquals("Test repository", metadata.getDescription());
        assertEquals("https://github.com/org/test-repo.git", metadata.getCloneUrl());
        assertEquals("Java", metadata.getLanguage());
        assertEquals(1024, metadata.getSize());
        assertEquals(5, metadata.getStarCount());
        assertFalse(metadata.isPrivate());
        assertFalse(metadata.isFork());
        assertFalse(metadata.isArchived());
        assertEquals(GitProviderType.GITHUB, metadata.getProviderType());
    }
    
    @Test
    void testRepositoryFilterDateFiltering() {
        RepositoryFilter filter = new RepositoryFilter();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        
        filter.setUpdatedAfter(cutoffDate);
        
        // spring-boot-app updated 5 days ago (after cutoff)
        assertTrue(filter.matches(testRepositories.get(0)));
        
        // react-frontend updated 2 days ago (after cutoff)
        assertTrue(filter.matches(testRepositories.get(1)));
        
        // python-utils updated 10 days ago (before cutoff)
        assertFalse(filter.matches(testRepositories.get(2)));
    }
    
    @Test
    void testRepositoryFilterEdgeCases() {
        RepositoryFilter filter = new RepositoryFilter();
        
        // Test null repository
        assertFalse(filter.matches(null));
        
        // Test empty search query
        filter.setSearchQuery("");
        assertTrue(filter.matches(testRepositories.get(0)));
        
        filter.setSearchQuery("   ");
        assertTrue(filter.matches(testRepositories.get(0)));
        
        // Test null language in repository
        RepositoryMetadata repoWithoutLanguage = RepositoryMetadata.builder()
            .id("no-lang")
            .name("no-language-repo")
            .fullName("org/no-language-repo")
            .cloneUrl("https://github.com/org/no-language-repo.git")
            .language(null)
            .size(100)
            .isPrivate(false)
            .isFork(false)
            .isArchived(false)
            .providerType(GitProviderType.GITHUB)
            .build();
        
        filter.clear();
        filter.setLanguages(Set.of("Java"));
        assertFalse(filter.matches(repoWithoutLanguage));
    }
}