package com.example.gitmigrator.service;

import com.example.gitmigrator.model.MigrationConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateVersionManagerFixTest {

    @TempDir
    Path tempDir;

    @Test
    void testObjectMapperIgnoresUnknownProperties() throws IOException {
        // Create a template file with _templateVersion field
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("_templateVersion", "1.0");
        templateData.put("targetPlatform", "kubernetes");
        templateData.put("templateName", "test-template");
        templateData.put("enableValidation", true);

        File templateFile = tempDir.resolve("test-template.json").toFile();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(templateFile, templateData);

        // Test that TemplateVersionManager can handle the file
        TemplateVersionManager versionManager = new TemplateVersionManager();
        
        // This should not throw an exception
        assertDoesNotThrow(() -> {
            MigrationConfiguration config = versionManager.readAndMigrateTemplate(templateFile);
            assertNotNull(config);
            assertEquals("kubernetes", config.getTargetPlatform());
            assertEquals("test-template", config.getTemplateName());
            assertTrue(config.isEnableValidation());
        });
    }
}