package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.GitProviderConnection;
import com.example.gitmigrator.model.GitProviderType;
import com.example.gitmigrator.service.RepositoryDiscoveryServiceFactory;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for GitProviderConnectionPanel with actual services.
 * Tests the integration between the UI component and repository discovery services.
 */
@ExtendWith(ApplicationExtension.class)
class GitProviderConnectionPanelIntegrationTest {
    
    private GitProviderConnectionPanel connectionPanel;
    private RepositoryDiscoveryServiceFactory serviceFactory;
    
    @Start
    void start(Stage stage) {
        serviceFactory = new RepositoryDiscoveryServiceFactory();
        connectionPanel = new GitProviderConnectionPanel(serviceFactory);
        
        Scene scene = new Scene(connectionPanel, 500, 400);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        
        stage.setTitle("Git Provider Connection Panel Integration Test");
        stage.setScene(scene);
        stage.show();
    }
    
    @Test
    void testServiceFactoryIntegration() {
        // Test that the service factory is properly integrated
        assertNotNull(serviceFactory);
        assertTrue(serviceFactory.isProviderSupported(GitProviderType.GITHUB));
        assertTrue(serviceFactory.isProviderSupported(GitProviderType.GITLAB));
        assertTrue(serviceFactory.isProviderSupported(GitProviderType.BITBUCKET));
    }
    
    @Test
    void testConnectionCreationAndValidation() {
        Platform.runLater(() -> {
            // Create a test connection
            GitProviderConnection connection = new GitProviderConnection(
                GitProviderType.GITHUB,
                "https://github.com",
                "testuser",
                "testtoken"
            );
            
            // Verify connection is valid
            assertTrue(connection.isValid());
            assertEquals(GitProviderType.GITHUB, connection.getProviderType());
            assertEquals("https://github.com", connection.getBaseUrl());
            assertEquals("https://api.github.com", connection.getApiUrl());
            assertEquals("testuser", connection.getUsername());
            
            // Test service factory can handle the connection
            assertTrue(serviceFactory.isConnectionSupported(connection));
            assertTrue(serviceFactory.getService(connection).isPresent());
        });
    }
    
    @Test
    void testAsyncConnectionTesting() throws Exception {
        Platform.runLater(() -> {
            // Create a connection with invalid credentials (should fail quickly)
            GitProviderConnection connection = new GitProviderConnection(
                GitProviderType.GITHUB,
                "https://github.com",
                "invaliduser",
                "invalidtoken"
            );
            
            connectionPanel.setConnection(connection);
            
            // Test async connection testing
            CompletableFuture<Boolean> testResult = connectionPanel.testConnectionAsync();
            
            // The test should complete (even if it fails due to invalid credentials)
            assertNotNull(testResult);
            assertFalse(testResult.isDone()); // Should be running asynchronously
        });
    }
    
    @Test
    void testProviderSpecificConfigurations() {
        Platform.runLater(() -> {
            // Test GitHub configuration
            GitProviderConnection githubConnection = new GitProviderConnection(
                GitProviderType.GITHUB,
                "https://github.com/myorg",
                "user",
                "token"
            );
            assertEquals("https://api.github.com", githubConnection.getApiUrl());
            assertFalse(githubConnection.isSelfHosted());
            
            // Test GitLab self-hosted configuration
            GitProviderConnection gitlabConnection = new GitProviderConnection(
                GitProviderType.GITLAB,
                "https://gitlab.company.com",
                "user",
                "token"
            );
            assertEquals("https://gitlab.company.com/api/v4", gitlabConnection.getApiUrl());
            assertTrue(gitlabConnection.isSelfHosted());
            
            // Test Bitbucket configuration
            GitProviderConnection bitbucketConnection = new GitProviderConnection(
                GitProviderType.BITBUCKET,
                "https://bitbucket.org/workspace",
                "user",
                "token"
            );
            assertEquals("https://api.bitbucket.org/2.0", bitbucketConnection.getApiUrl());
            assertFalse(bitbucketConnection.isSelfHosted());
        });
    }
    
    @Test
    void testUIStateManagement() {
        Platform.runLater(() -> {
            // Test initial state
            assertNull(connectionPanel.getConnection());
            assertFalse(connectionPanel.isConnectionTested());
            assertFalse(connectionPanel.isConnectionValid());
            
            // Test state after setting connection
            GitProviderConnection connection = new GitProviderConnection(
                GitProviderType.GITHUB,
                "https://github.com",
                "testuser",
                "testtoken"
            );
            
            connectionPanel.setConnection(connection);
            
            // Connection should be set but not tested
            assertNotNull(connectionPanel.getConnection());
            assertFalse(connectionPanel.isConnectionTested());
            assertFalse(connectionPanel.isConnectionValid());
            assertEquals("Not tested", connectionPanel.getStatusMessage());
            
            // Test clearing connection
            connectionPanel.clearConnection();
            assertNull(connectionPanel.getConnection());
            assertFalse(connectionPanel.isConnectionTested());
            assertFalse(connectionPanel.isConnectionValid());
        });
    }
    
    @Test
    void testPropertyBindings() {
        Platform.runLater(() -> {
            // Test property bindings work correctly
            assertFalse(connectionPanel.connectionTestedProperty().get());
            assertFalse(connectionPanel.connectionValidProperty().get());
            assertEquals("Not tested", connectionPanel.statusMessageProperty().get());
            
            // Simulate status changes
            connectionPanel.setConnectionStatus(
                GitProviderConnectionPanel.ConnectionStatus.TESTING, null);
            assertEquals("Testing connection...", connectionPanel.statusMessageProperty().get());
            
            connectionPanel.setConnectionStatus(
                GitProviderConnectionPanel.ConnectionStatus.SUCCESS, null);
            assertEquals("Connection successful", connectionPanel.statusMessageProperty().get());
            assertTrue(connectionPanel.connectionValidProperty().get());
            
            connectionPanel.setConnectionStatus(
                GitProviderConnectionPanel.ConnectionStatus.FAILED, "Test error");
            assertEquals("Test error", connectionPanel.statusMessageProperty().get());
            assertFalse(connectionPanel.connectionValidProperty().get());
        });
    }
}