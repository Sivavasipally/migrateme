package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.GeneratedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for FilePreviewComponent
 */
class FilePreviewComponentTest {
    
    private FilePreviewComponent filePreviewComponent;
    
    @BeforeEach
    void setUp() {
        // Note: This test would require JavaFX toolkit initialization in a real test environment
        // For now, we'll test the logic that doesn't require UI components
    }
    
    @Test
    void testGeneratedFileModel() {
        // Test GeneratedFile model
        GeneratedFile file = new GeneratedFile();
        file.setFileName("Dockerfile");
        file.setRelativePath("docker/Dockerfile");
        file.setContent("FROM openjdk:17-jre-slim\nCOPY app.jar /app.jar\nEXPOSE 8080");
        file.setFileType(GeneratedFile.FileType.DOCKERFILE);
        
        assertEquals("Dockerfile", file.getFileName());
        assertEquals("docker/Dockerfile", file.getRelativePath());
        assertEquals(GeneratedFile.FileType.DOCKERFILE, file.getFileType());
        assertTrue(file.isNew());
        assertTrue(file.hasChanges()); // New files are considered as having changes
        
        // Test with original content
        file.setOriginalContent("FROM openjdk:11-jre-slim\nCOPY app.jar /app.jar");
        assertTrue(file.hasChanges());
        
        // Test revert
        file.revertChanges();
        assertEquals("FROM openjdk:11-jre-slim\nCOPY app.jar /app.jar", file.getContent());
        assertFalse(file.isModified());
    }
    
    @Test
    void testFileTypeDetection() {
        assertEquals(GeneratedFile.FileType.DOCKERFILE, 
            GeneratedFile.FileType.fromFileName("Dockerfile"));
        assertEquals(GeneratedFile.FileType.DOCKERFILE, 
            GeneratedFile.FileType.fromFileName("app.dockerfile"));
        assertEquals(GeneratedFile.FileType.YAML, 
            GeneratedFile.FileType.fromFileName("deployment.yaml"));
        assertEquals(GeneratedFile.FileType.YAML, 
            GeneratedFile.FileType.fromFileName("config.yml"));
        assertEquals(GeneratedFile.FileType.JSON, 
            GeneratedFile.FileType.fromFileName("package.json"));
        assertEquals(GeneratedFile.FileType.XML, 
            GeneratedFile.FileType.fromFileName("pom.xml"));
        assertEquals(GeneratedFile.FileType.SHELL, 
            GeneratedFile.FileType.fromFileName("build.sh"));
        assertEquals(GeneratedFile.FileType.PROPERTIES, 
            GeneratedFile.FileType.fromFileName("application.properties"));
        assertEquals(GeneratedFile.FileType.TEXT, 
            GeneratedFile.FileType.fromFileName("README.txt"));
        assertEquals(GeneratedFile.FileType.TEXT, 
            GeneratedFile.FileType.fromFileName("unknown.xyz"));
    }
    
    @Test
    void testFileTreeItemModel() {
        // Test directory item
        com.example.gitmigrator.model.FileTreeItem dirItem = new com.example.gitmigrator.model.FileTreeItem("docker", "docker");
        assertTrue(dirItem.isDirectory());
        assertNull(dirItem.getGeneratedFile());
        assertEquals("docker", dirItem.getValue());
        assertEquals("docker", dirItem.getFullPath());
        
        // Test file item
        GeneratedFile file = new GeneratedFile("Dockerfile", "docker/Dockerfile", 
            "FROM openjdk:17", GeneratedFile.FileType.DOCKERFILE);
        com.example.gitmigrator.model.FileTreeItem fileItem = new com.example.gitmigrator.model.FileTreeItem(file);
        
        assertFalse(fileItem.isDirectory());
        assertEquals(file, fileItem.getGeneratedFile());
        assertEquals("Dockerfile", fileItem.getValue());
        assertEquals("docker/Dockerfile", fileItem.getFullPath());
        assertTrue(fileItem.isNew());
        assertFalse(fileItem.isModified());
        
        // Test modified file
        file.setContent("FROM openjdk:11");
        assertTrue(fileItem.isModified());
    }
    
    @Test
    void testGeneratedFileList() {
        List<GeneratedFile> files = Arrays.asList(
            new GeneratedFile("Dockerfile", "Dockerfile", "FROM openjdk:17", GeneratedFile.FileType.DOCKERFILE),
            new GeneratedFile("deployment.yaml", "k8s/deployment.yaml", "apiVersion: apps/v1", GeneratedFile.FileType.YAML),
            new GeneratedFile("service.yaml", "k8s/service.yaml", "apiVersion: v1", GeneratedFile.FileType.YAML),
            new GeneratedFile("build.sh", "scripts/build.sh", "#!/bin/bash", GeneratedFile.FileType.SHELL)
        );
        
        assertEquals(4, files.size());
        
        // Test file types
        assertEquals(GeneratedFile.FileType.DOCKERFILE, files.get(0).getFileType());
        assertEquals(GeneratedFile.FileType.YAML, files.get(1).getFileType());
        assertEquals(GeneratedFile.FileType.YAML, files.get(2).getFileType());
        assertEquals(GeneratedFile.FileType.SHELL, files.get(3).getFileType());
        
        // Test paths
        assertEquals("Dockerfile", files.get(0).getRelativePath());
        assertEquals("k8s/deployment.yaml", files.get(1).getRelativePath());
        assertEquals("k8s/service.yaml", files.get(2).getRelativePath());
        assertEquals("scripts/build.sh", files.get(3).getRelativePath());
    }
}