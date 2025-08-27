package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.service.GitServiceFactory;
import com.example.gitmigrator.service.RepositoryDiscoveryServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the enhanced AddRepositoryDialog with tabbed interface.
 * This test focuses on verifying the class structure and method signatures without requiring JavaFX runtime.
 */
public class AddRepositoryDialogTabbedTest {
    
    private GitServiceFactory gitServiceFactory;
    private RepositoryDiscoveryServiceFactory discoveryServiceFactory;
    
    @BeforeEach
    void setUp() {
        gitServiceFactory = new GitServiceFactory();
        discoveryServiceFactory = new RepositoryDiscoveryServiceFactory();
    }
    
    @Test
    void testDialogClassHasRequiredConstructors() {
        // Test that the dialog class has the expected constructors
        Class<AddRepositoryDialog> dialogClass = AddRepositoryDialog.class;
        
        // Check for constructor with GitServiceFactory only
        assertDoesNotThrow(() -> {
            dialogClass.getConstructor(GitServiceFactory.class);
        }, "Should have constructor with GitServiceFactory parameter");
        
        // Check for constructor with both factories
        assertDoesNotThrow(() -> {
            dialogClass.getConstructor(GitServiceFactory.class, RepositoryDiscoveryServiceFactory.class);
        }, "Should have constructor with both factory parameters");
    }
    
    @Test
    void testDialogClassHasRequiredMethods() {
        // Test that the dialog class has the expected public methods
        Class<AddRepositoryDialog> dialogClass = AddRepositoryDialog.class;
        
        // Check for switchToSingleMode method
        assertDoesNotThrow(() -> {
            Method method = dialogClass.getMethod("switchToSingleMode");
            assertEquals(void.class, method.getReturnType(), "switchToSingleMode should return void");
        }, "Should have switchToSingleMode method");
        
        // Check for switchToBulkMode method
        assertDoesNotThrow(() -> {
            Method method = dialogClass.getMethod("switchToBulkMode");
            assertEquals(void.class, method.getReturnType(), "switchToBulkMode should return void");
        }, "Should have switchToBulkMode method");
        
        // Check for testConnection method
        assertDoesNotThrow(() -> {
            dialogClass.getMethod("testConnection", com.example.gitmigrator.model.GitProviderConnection.class);
        }, "Should have testConnection method");
        
        // Check for discoverRepositoriesAsync method
        assertDoesNotThrow(() -> {
            dialogClass.getMethod("discoverRepositoriesAsync");
        }, "Should have discoverRepositoriesAsync method");
    }
    
    @Test
    void testDialogClassHasRequiredFields() {
        // Test that the dialog class has the expected private fields for tabbed interface
        Class<AddRepositoryDialog> dialogClass = AddRepositoryDialog.class;
        
        // Check for tabPane field
        assertDoesNotThrow(() -> {
            dialogClass.getDeclaredField("tabPane");
        }, "Should have tabPane field");
        
        // Check for singleRepositoryTab field
        assertDoesNotThrow(() -> {
            dialogClass.getDeclaredField("singleRepositoryTab");
        }, "Should have singleRepositoryTab field");
        
        // Check for bulkDiscoveryTab field
        assertDoesNotThrow(() -> {
            dialogClass.getDeclaredField("bulkDiscoveryTab");
        }, "Should have bulkDiscoveryTab field");
        
        // Check for connectionPanel field
        assertDoesNotThrow(() -> {
            dialogClass.getDeclaredField("connectionPanel");
        }, "Should have connectionPanel field");
        
        // Check for discoveryPanel field
        assertDoesNotThrow(() -> {
            dialogClass.getDeclaredField("discoveryPanel");
        }, "Should have discoveryPanel field");
    }
    
    @Test
    void testFactoryDependencies() {
        // Test that the required factory classes exist and can be instantiated
        assertNotNull(gitServiceFactory, "GitServiceFactory should be available");
        assertNotNull(discoveryServiceFactory, "RepositoryDiscoveryServiceFactory should be available");
        
        // Test that the factories have the expected types
        assertEquals(GitServiceFactory.class, gitServiceFactory.getClass());
        assertEquals(RepositoryDiscoveryServiceFactory.class, discoveryServiceFactory.getClass());
    }
    
    @Test
    void testDialogClassStructure() {
        // Test that the dialog class extends the expected base class or implements expected interfaces
        Class<AddRepositoryDialog> dialogClass = AddRepositoryDialog.class;
        
        // Verify it's a public class
        assertTrue(java.lang.reflect.Modifier.isPublic(dialogClass.getModifiers()), 
            "AddRepositoryDialog should be a public class");
        
        // Verify it's not abstract
        assertFalse(java.lang.reflect.Modifier.isAbstract(dialogClass.getModifiers()), 
            "AddRepositoryDialog should not be abstract");
        
        // Verify it's not an interface
        assertFalse(dialogClass.isInterface(), "AddRepositoryDialog should not be an interface");
    }
}