package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.GitProviderConnection;
import com.example.gitmigrator.model.GitProviderType;
import com.example.gitmigrator.service.RepositoryDiscoveryServiceFactory;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for GitProviderConnectionPanel component.
 * Tests UI interactions, validation, and connection functionality.
 */
@ExtendWith(ApplicationExtension.class)
class GitProviderConnectionPanelTest {
    
    private GitProviderConnectionPanel connectionPanel;
    private RepositoryDiscoveryServiceFactory serviceFactory;
    
    @Start
    void start(Stage stage) {
        serviceFactory = new RepositoryDiscoveryServiceFactory();
        connectionPanel = new GitProviderConnectionPanel(serviceFactory);
        
        Scene scene = new Scene(connectionPanel, 400, 300);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        
        stage.setScene(scene);
        stage.show();
    }
    
    @BeforeEach
    void setUp() {
        Platform.runLater(() -> connectionPanel.clearConnection());
    }
    
    @Test
    void testInitialState(FxRobot robot) {
        // Verify initial state
        assertNull(connectionPanel.getConnection());
        assertFalse(connectionPanel.isConnectionTested());
        assertFalse(connectionPanel.isConnectionValid());
        assertEquals("Not tested", connectionPanel.getStatusMessage());
    }
    
    @Test
    void testProviderSelection(FxRobot robot) {
        Platform.runLater(() -> {
            // Test provider selection
            ComboBox<GitProviderType> providerSelector = robot.lookup(".combo-box").query();
            providerSelector.setValue(GitProviderType.GITHUB);
            
            // Verify URL field is updated
            TextField urlField = robot.lookup(".text-field").query();
            assertEquals(GitProviderType.GITHUB.getDefaultWebUrl(), urlField.getText());
        });
    }
    
    @Test
    void testUrlAutoDetection(FxRobot robot) {
        Platform.runLater(() -> {
            TextField urlField = robot.lookup(".text-field").query();
            ComboBox<GitProviderType> providerSelector = robot.lookup(".combo-box").query();
            
            // Test GitHub URL detection
            urlField.setText("https://github.com/myorg");
            assertEquals(GitProviderType.GITHUB, providerSelector.getValue());
            
            // Test GitLab URL detection
            urlField.setText("https://gitlab.com/mygroup");
            assertEquals(GitProviderType.GITLAB, providerSelector.getValue());
            
            // Test Bitbucket URL detection
            urlField.setText("https://bitbucket.org/myworkspace");
            assertEquals(GitProviderType.BITBUCKET, providerSelector.getValue());
        });
    }
    
    @Test
    void testConnectionValidation(FxRobot robot) {
        Platform.runLater(() -> {
            // Test invalid connection (missing fields)
            assertNull(connectionPanel.getConnection());
            
            // Set provider only
            ComboBox<GitProviderType> providerSelector = robot.lookup(".combo-box").query();
            providerSelector.setValue(GitProviderType.GITHUB);
            assertNull(connectionPanel.getConnection());
            
            // Add URL
            TextField urlField = robot.lookup(".text-field").query();
            urlField.setText("https://github.com/test");
            assertNull(connectionPanel.getConnection());
            
            // Add username
            TextField usernameField = robot.lookup(".text-field").queryAll().get(1);
            usernameField.setText("testuser");
            assertNull(connectionPanel.getConnection());
            
            // Add password - now should be valid
            PasswordField passwordField = robot.lookup(".password-field").query();
            passwordField.setText("testpass");
            
            GitProviderConnection connection = connectionPanel.getConnection();
            assertNotNull(connection);
            assertEquals(GitProviderType.GITHUB, connection.getProviderType());
            assertEquals("https://github.com/test", connection.getBaseUrl());
            assertEquals("testuser", connection.getUsername());
            assertEquals("testpass", connection.getPasswordAsString());
            assertTrue(connection.isValid());
        });
    }
    
    @Test
    void testSetConnection(FxRobot robot) {
        Platform.runLater(() -> {
            GitProviderConnection testConnection = new GitProviderConnection(
                GitProviderType.GITLAB,
                "https://gitlab.example.com",
                "testuser",
                "testpass"
            );
            
            connectionPanel.setConnection(testConnection);
            
            // Verify UI is updated
            ComboBox<GitProviderType> providerSelector = robot.lookup(".combo-box").query();
            TextField urlField = robot.lookup(".text-field").query();
            TextField usernameField = robot.lookup(".text-field").queryAll().get(1);
            PasswordField passwordField = robot.lookup(".password-field").query();
            
            assertEquals(GitProviderType.GITLAB, providerSelector.getValue());
            assertEquals("https://gitlab.example.com", urlField.getText());
            assertEquals("testuser", usernameField.getText());
            // Password field should be cleared for security
            assertTrue(passwordField.getText().isEmpty());
        });
    }
    
    @Test
    void testClearConnection(FxRobot robot) {
        Platform.runLater(() -> {
            // Set some values first
            ComboBox<GitProviderType> providerSelector = robot.lookup(".combo-box").query();
            TextField urlField = robot.lookup(".text-field").query();
            TextField usernameField = robot.lookup(".text-field").queryAll().get(1);
            PasswordField passwordField = robot.lookup(".password-field").query();
            
            providerSelector.setValue(GitProviderType.GITHUB);
            urlField.setText("https://github.com/test");
            usernameField.setText("testuser");
            passwordField.setText("testpass");
            
            // Clear connection
            connectionPanel.clearConnection();
            
            // Verify all fields are cleared
            assertNull(providerSelector.getValue());
            assertTrue(urlField.getText().isEmpty());
            assertTrue(usernameField.getText().isEmpty());
            assertTrue(passwordField.getText().isEmpty());
            assertFalse(connectionPanel.isConnectionTested());
            assertFalse(connectionPanel.isConnectionValid());
        });
    }
    
    @Test
    void testConnectionStatusUpdates(FxRobot robot) {
        Platform.runLater(() -> {
            // Test status changes
            assertEquals("Not tested", connectionPanel.getStatusMessage());
            
            // Simulate status changes (would normally happen during connection testing)
            connectionPanel.setConnectionStatus(
                GitProviderConnectionPanel.ConnectionStatus.TESTING, null);
            assertEquals("Testing connection...", connectionPanel.getStatusMessage());
            
            connectionPanel.setConnectionStatus(
                GitProviderConnectionPanel.ConnectionStatus.SUCCESS, null);
            assertEquals("Connection successful", connectionPanel.getStatusMessage());
            assertTrue(connectionPanel.isConnectionValid());
            
            connectionPanel.setConnectionStatus(
                GitProviderConnectionPanel.ConnectionStatus.FAILED, "Invalid credentials");
            assertEquals("Invalid credentials", connectionPanel.getStatusMessage());
            assertFalse(connectionPanel.isConnectionValid());
        });
    }
    
    @Test
    void testSelfHostedDetection(FxRobot robot) {
        Platform.runLater(() -> {
            ComboBox<GitProviderType> providerSelector = robot.lookup(".combo-box").query();
            TextField urlField = robot.lookup(".text-field").query();
            TextField usernameField = robot.lookup(".text-field").queryAll().get(1);
            PasswordField passwordField = robot.lookup(".password-field").query();
            
            // Test self-hosted GitLab
            providerSelector.setValue(GitProviderType.GITLAB);
            urlField.setText("https://gitlab.company.com");
            usernameField.setText("testuser");
            passwordField.setText("testpass");
            
            GitProviderConnection connection = connectionPanel.getConnection();
            assertNotNull(connection);
            assertTrue(connection.isSelfHosted());
            assertEquals("https://gitlab.company.com/api/v4", connection.getApiUrl());
        });
    }
}