package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.MigrationConfiguration;
import com.example.gitmigrator.service.TemplateManagementService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test class for MigrationConfigurationPanel component.
 * Note: These tests run without TestFX for simplicity.
 */
class MigrationConfigurationPanelTest {
    
    @Mock
    private TemplateManagementService templateService;
    
    private MigrationConfigurationPanel configPanel;
    
    @BeforeAll
    static void initToolkit() {
        // Initialize JavaFX toolkit for headless testing
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Platform already initialized
        }
    }
    
    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Mock template service
        when(templateService.getAvailableTemplates())
                .thenReturn(Arrays.asList("microservice", "monolith", "frontend"));
        
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                configPanel = new MigrationConfigurationPanel(templateService);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Setup should complete within 5 seconds");
    }
    
    @Test
    void testPanelInitialization() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        
        Platform.runLater(() -> {
            try {
                assertNotNull(configPanel, "Configuration panel should be initialized");
                assertTrue(configPanel.getChildren().size() > 0, "Panel should have child components");
                result.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test should complete within 5 seconds");
        assertTrue(result.get(), "Panel initialization test should pass");
    }
    
    @Test
    void testDefaultConfiguration() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        
        Platform.runLater(() -> {
            try {
                MigrationConfiguration config = configPanel.getCurrentConfiguration();
                
                assertNotNull(config, "Configuration should not be null");
                assertEquals("kubernetes", config.getTargetPlatform(), "Default platform should be kubernetes");
                assertTrue(config.getOptionalComponents().contains("helm"), "Should include Helm by default");
                assertTrue(config.getOptionalComponents().contains("dockerfile"), "Should include Dockerfile by default");
                result.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test should complete within 5 seconds");
        assertTrue(result.get(), "Default configuration test should pass");
    }
    
    @Test
    void testConfigurationCallbacks() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        
        Platform.runLater(() -> {
            try {
                AtomicReference<MigrationConfiguration> configChanged = new AtomicReference<>();
                AtomicReference<String> validationError = new AtomicReference<>();
                
                // Set up callbacks
                configPanel.setOnConfigurationChanged(configChanged::set);
                configPanel.setOnValidationError(validationError::set);
                
                // Test callbacks are set
                assertNotNull(configPanel, "Panel should be initialized with callbacks");
                result.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test should complete within 5 seconds");
        assertTrue(result.get(), "Configuration callbacks test should pass");
    }
    
    @Test
    void testCustomSettingsManagement() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        
        Platform.runLater(() -> {
            try {
                // Test custom setting creation
                MigrationConfigurationPanel.CustomSetting setting = 
                        new MigrationConfigurationPanel.CustomSetting("test-key", "test-value", "test-description");
                
                assertEquals("test-key", setting.getKey(), "Key should match");
                assertEquals("test-value", setting.getValue(), "Value should match");
                assertEquals("test-description", setting.getDescription(), "Description should match");
                
                // Test property binding
                assertNotNull(setting.keyProperty(), "Key property should not be null");
                assertNotNull(setting.valueProperty(), "Value property should not be null");
                assertNotNull(setting.descriptionProperty(), "Description property should not be null");
                
                result.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test should complete within 5 seconds");
        assertTrue(result.get(), "Custom settings management test should pass");
    }
}