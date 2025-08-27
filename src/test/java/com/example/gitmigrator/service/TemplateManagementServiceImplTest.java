package com.example.gitmigrator.service;

import com.example.gitmigrator.model.MigrationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TemplateManagementServiceImpl.
 */
class TemplateManagementServiceImplTest {
    
    private TemplateManagementService templateService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // Override system property to use temp directory for testing
        System.setProperty("user.home", tempDir.toString());
        templateService = new TemplateManagementServiceImpl();
    }
    
    @Test
    void testSaveAndLoadTemplate() {
        // Create a test configuration
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        config.setIncludeHelm(true);
        config.setIncludeDockerfile(true);
        config.setCustomSetting("base-image", "openjdk:17");
        
        // Save the template
        templateService.saveTemplate("test-template", config);
        
        // Load the template
        MigrationConfiguration loadedConfig = templateService.loadTemplate("test-template");
        
        // Verify the loaded configuration
        assertNotNull(loadedConfig);
        assertEquals("kubernetes", loadedConfig.getTargetPlatform());
        assertTrue(loadedConfig.isIncludeHelm());
        assertTrue(loadedConfig.isIncludeDockerfile());
        assertEquals("openjdk:17", loadedConfig.getCustomSetting("base-image"));
    }
    
    @Test
    void testGetAvailableTemplates() {
        // Initially should have default templates
        List<String> templates = templateService.getAvailableTemplates();
        assertTrue(templates.size() >= 4); // At least the 4 default templates
        
        // Add a custom template
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        templateService.saveTemplate("custom-template", config);
        
        // Check that it appears in the list
        templates = templateService.getAvailableTemplates();
        assertTrue(templates.contains("custom-template"));
    }
    
    @Test
    void testDeleteTemplate() {
        // Create and save a template
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        templateService.saveTemplate("delete-me", config);
        
        // Verify it exists
        assertTrue(templateService.templateExists("delete-me"));
        
        // Delete it
        boolean deleted = templateService.deleteTemplate("delete-me");
        assertTrue(deleted);
        
        // Verify it's gone
        assertFalse(templateService.templateExists("delete-me"));
        
        // Try to delete non-existent template
        boolean deletedAgain = templateService.deleteTemplate("delete-me");
        assertFalse(deletedAgain);
    }
    
    @Test
    void testTemplateExists() {
        // Test non-existent template
        assertFalse(templateService.templateExists("non-existent"));
        
        // Create a template
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        templateService.saveTemplate("exists-test", config);
        
        // Test existing template
        assertTrue(templateService.templateExists("exists-test"));
    }
    
    @Test
    void testValidateTemplate() {
        // Valid configuration
        MigrationConfiguration validConfig = new MigrationConfiguration("kubernetes");
        assertTrue(templateService.validateTemplate(validConfig));
        
        // Invalid configuration - null
        assertFalse(templateService.validateTemplate(null));
        
        // Invalid configuration - empty target platform
        MigrationConfiguration invalidConfig = new MigrationConfiguration("");
        assertFalse(templateService.validateTemplate(invalidConfig));
        
        // Invalid configuration - invalid target platform
        MigrationConfiguration invalidPlatform = new MigrationConfiguration("invalid-platform");
        assertFalse(templateService.validateTemplate(invalidPlatform));
    }
    
    @Test
    void testExportAndImportTemplate() throws Exception {
        // Create a template
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        config.setIncludeHelm(true);
        config.setCustomSetting("test-key", "test-value");
        templateService.saveTemplate("export-test", config);
        
        // Export to a file
        File exportFile = tempDir.resolve("exported-template.json").toFile();
        templateService.exportTemplate("export-test", exportFile);
        
        assertTrue(exportFile.exists());
        
        // Import from the file
        String importedName = templateService.importTemplate(exportFile);
        assertNotNull(importedName);
        
        // Verify the imported template
        MigrationConfiguration importedConfig = templateService.loadTemplate(importedName);
        assertNotNull(importedConfig);
        assertEquals("kubernetes", importedConfig.getTargetPlatform());
        assertTrue(importedConfig.isIncludeHelm());
        assertEquals("test-value", importedConfig.getCustomSetting("test-key"));
    }
    
    @Test
    void testSaveTemplateWithInvalidInput() {
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        
        // Test null name
        assertThrows(IllegalArgumentException.class, () -> 
            templateService.saveTemplate(null, config));
        
        // Test empty name
        assertThrows(IllegalArgumentException.class, () -> 
            templateService.saveTemplate("", config));
        
        // Test null configuration
        assertThrows(IllegalArgumentException.class, () -> 
            templateService.saveTemplate("test", null));
        
        // Test invalid configuration
        MigrationConfiguration invalidConfig = new MigrationConfiguration("invalid");
        assertThrows(IllegalArgumentException.class, () -> 
            templateService.saveTemplate("test", invalidConfig));
    }
    
    @Test
    void testLoadNonExistentTemplate() {
        MigrationConfiguration config = templateService.loadTemplate("non-existent");
        assertNull(config);
        
        // Test null name
        config = templateService.loadTemplate(null);
        assertNull(config);
        
        // Test empty name
        config = templateService.loadTemplate("");
        assertNull(config);
    }
    
    @Test
    void testDefaultTemplatesCreated() {
        List<String> templates = templateService.getAvailableTemplates();
        
        // Check that default templates are created
        assertTrue(templates.contains("microservices-k8s"));
        assertTrue(templates.contains("frontend-app"));
        assertTrue(templates.contains("monolith-migration"));
        assertTrue(templates.contains("openshift-basic"));
        
        // Verify one of the default templates
        MigrationConfiguration microservicesConfig = templateService.loadTemplate("microservices-k8s");
        assertNotNull(microservicesConfig);
        assertEquals("kubernetes", microservicesConfig.getTargetPlatform());
        assertTrue(microservicesConfig.isIncludeHelm());
        assertTrue(microservicesConfig.isIncludeDockerfile());
        assertTrue(microservicesConfig.isIncludeCiCd());
        assertTrue(microservicesConfig.isIncludeMonitoring());
    }
}