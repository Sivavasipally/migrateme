package com.example.gitmigrator.service;

import com.example.gitmigrator.model.MigrationConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateVersionManagerTest {

    private TemplateVersionManager versionManager;
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        versionManager = new TemplateVersionManager();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testReadTemplateWithVersionField() throws IOException {
        // Create a template file with _templateVersion field
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("_templateVersion", "1.0");
        templateData.put("targetPlatform", "kubernetes");
        templateData.put("templateName", "test-template");
        templateData.put("enableValidation", true);

        File templateFile = tempDir.resolve("test-template.json").toFile();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(templateFile, templateData);

        // Read and migrate the template
        MigrationConfiguration config = versionManager.readAndMigrateTemplate(templateFile);

        // Verify the configuration was loaded correctly
        assertNotNull(config);
        assertEquals("kubernetes", config.getTargetPlatform());
        assertEquals("test-template", config.getTemplateName());
        assertTrue(config.isEnableValidation());
    }

    @Test
    void testReadLegacyTemplate() throws IOException {
        // Create a legacy template file without _templateVersion field
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("targetPlatform", "openshift");
        templateData.put("templateName", "legacy-template");
        templateData.put("enableValidation", false);

        File templateFile = tempDir.resolve("legacy-template.json").toFile();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(templateFile, templateData);

        // Read and migrate the template
        MigrationConfiguration config = versionManager.readAndMigrateTemplate(templateFile);

        // Verify the configuration was loaded correctly
        assertNotNull(config);
        assertEquals("openshift", config.getTargetPlatform());
        assertEquals("legacy-template", config.getTemplateName());
        assertFalse(config.isEnableValidation());
    }
}