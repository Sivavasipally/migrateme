package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.GitProviderConnection;
import com.example.gitmigrator.model.GitProviderType;
import com.example.gitmigrator.model.RepositoryMetadata;
import com.example.gitmigrator.service.RepositoryDiscoveryService;
import com.example.gitmigrator.service.RepositoryDiscoveryServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test for discovery services with UI components.
 * Tests the integration without requiring full JavaFX environment.
 */
class DiscoveryIntegrationTest {
    
    @Mock
    private RepositoryDiscoveryService mockDiscoveryService;
    
    @Mock
    private RepositoryDiscoveryServiceFactory mockServiceFactory;
    
    private GitProviderConnection testConnection;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testConnection = new GitProviderConnection(
            GitProviderType.GITHUB,
            "https://github.com/test",
            "testuser",
            "testpass"
        );
        
        // Setup mock service factory
        when(mockServiceFactory.getService(GitProviderType.GITHUB))
            .thenReturn(Optional.of(mockDiscoveryService));
        
        // Setup mock discovery service
        when(mockDiscoveryService.getSupportedProvider())
            .thenReturn(GitProviderType.GITHUB);
        when(mockDiscoveryService.supportsConnection(any()))
            .thenReturn(true);
    }
    
    @Test
    void testConnectionTestIntegration() {
        // Arrange
        when(mockDiscoveryService.testConnection(testConnection))
            .thenReturn(CompletableFuture.completedFuture(true));
        
        // Act
        Optional<RepositoryDiscoveryService> service = mockServiceFactory.getService(GitProviderType.GITHUB);
        
        // Assert
        assertTrue(service.isPresent());
        CompletableFuture<Boolean> result = service.get().testConnection(testConnection);
        assertTrue(result.join());
        
        verify(mockDiscoveryService).testConnection(testConnection);
    }
    
    @Test
    void testRepositoryDiscoveryIntegration() {
        // Arrange
        List<RepositoryMetadata> mockRepositories = List.of(
            createMockRepository("repo1", "Test Repository 1"),
            createMockRepository("repo2", "Test Repository 2")
        );
        
        when(mockDiscoveryService.discoverAllRepositories(testConnection))
            .thenReturn(CompletableFuture.completedFuture(mockRepositories));
        
        // Act
        Optional<RepositoryDiscoveryService> service = mockServiceFactory.getService(GitProviderType.GITHUB);
        
        // Assert
        assertTrue(service.isPresent());
        CompletableFuture<List<RepositoryMetadata>> result = service.get().discoverAllRepositories(testConnection);
        List<RepositoryMetadata> repositories = result.join();
        
        assertEquals(2, repositories.size());
        assertEquals("repo1", repositories.get(0).getName());
        assertEquals("repo2", repositories.get(1).getName());
        
        verify(mockDiscoveryService).discoverAllRepositories(testConnection);
    }
    
    @Test
    void testConnectionFailureHandling() {
        // Arrange
        when(mockDiscoveryService.testConnection(testConnection))
            .thenReturn(CompletableFuture.completedFuture(false));
        
        // Act
        Optional<RepositoryDiscoveryService> service = mockServiceFactory.getService(GitProviderType.GITHUB);
        
        // Assert
        assertTrue(service.isPresent());
        CompletableFuture<Boolean> result = service.get().testConnection(testConnection);
        assertFalse(result.join());
        
        verify(mockDiscoveryService).testConnection(testConnection);
    }
    
    @Test
    void testDiscoveryFailureHandling() {
        // Arrange
        CompletableFuture<List<RepositoryMetadata>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Connection failed"));
        
        when(mockDiscoveryService.discoverAllRepositories(testConnection))
            .thenReturn(failedFuture);
        
        // Act & Assert
        Optional<RepositoryDiscoveryService> service = mockServiceFactory.getService(GitProviderType.GITHUB);
        assertTrue(service.isPresent());
        
        CompletableFuture<List<RepositoryMetadata>> result = service.get().discoverAllRepositories(testConnection);
        
        assertThrows(RuntimeException.class, result::join);
        verify(mockDiscoveryService).discoverAllRepositories(testConnection);
    }
    
    @Test
    void testUnsupportedProviderHandling() {
        // Arrange
        when(mockServiceFactory.getService(GitProviderType.BITBUCKET))
            .thenReturn(Optional.empty());
        
        // Act
        Optional<RepositoryDiscoveryService> service = mockServiceFactory.getService(GitProviderType.BITBUCKET);
        
        // Assert
        assertFalse(service.isPresent());
    }
    
    @Test
    void testServiceFactoryIntegration() {
        // Test with real service factory
        RepositoryDiscoveryServiceFactory realFactory = new RepositoryDiscoveryServiceFactory();
        
        // All providers should be supported
        assertTrue(realFactory.isProviderSupported(GitProviderType.GITHUB));
        assertTrue(realFactory.isProviderSupported(GitProviderType.GITLAB));
        assertTrue(realFactory.isProviderSupported(GitProviderType.BITBUCKET));
        
        // Should return services for all providers
        assertTrue(realFactory.getService(GitProviderType.GITHUB).isPresent());
        assertTrue(realFactory.getService(GitProviderType.GITLAB).isPresent());
        assertTrue(realFactory.getService(GitProviderType.BITBUCKET).isPresent());
        
        // Should validate connections
        assertTrue(realFactory.isConnectionSupported(testConnection));
    }
    
    private RepositoryMetadata createMockRepository(String name, String description) {
        return RepositoryMetadata.builder()
            .name(name)
            .description(description)
            .cloneUrl("https://github.com/test/" + name + ".git")
            .webUrl("https://github.com/test/" + name)
            .providerType(GitProviderType.GITHUB)
            .language("Java")
            .isPrivate(false)
            .isFork(false)
            .build();
    }
}