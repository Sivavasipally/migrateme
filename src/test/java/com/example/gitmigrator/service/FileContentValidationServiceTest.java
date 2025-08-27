package com.example.gitmigrator.service;

import com.example.gitmigrator.model.GeneratedFile;
import com.example.gitmigrator.model.ValidationIssue;
import com.example.gitmigrator.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for FileContentValidationService
 */
class FileContentValidationServiceTest {
    
    private FileContentValidationService validationService;
    
    @BeforeEach
    void setUp() {
        validationService = new FileContentValidationService();
    }
    
    @Test
    void testValidDockerfile() {
        GeneratedFile dockerfile = new GeneratedFile(
            "Dockerfile",
            "Dockerfile",
            "FROM openjdk:17-jre-slim\n" +
            "COPY target/app.jar /app.jar\n" +
            "EXPOSE 8080\n" +
            "ENTRYPOINT [\"java\", \"-jar\", \"/app.jar\"]",
            GeneratedFile.FileType.DOCKERFILE
        );
        
        ValidationResult result = validationService.validateContent(dockerfile);
        assertTrue(result.isValid());
        assertTrue(result.getIssues().isEmpty());
    }
    
    @Test
    void testInvalidDockerfile() {
        GeneratedFile dockerfile = new GeneratedFile(
            "Dockerfile",
            "Dockerfile",
            "# Missing FROM instruction\n" +
            "COPY target/app.jar /app.jar\n" +
            "EXPOSE 8080",
            GeneratedFile.FileType.DOCKERFILE
        );
        
        ValidationResult result = validationService.validateContent(dockerfile);
        assertFalse(result.isValid());
        assertFalse(result.getIssues().isEmpty());
        
        // Should have error about missing FROM instruction
        assertTrue(result.getIssues().stream()
            .anyMatch(issue -> issue.getMessage().contains("FROM instruction")));
    }
    
    @Test
    void testDockerfileBestPractices() {
        GeneratedFile dockerfile = new GeneratedFile(
            "Dockerfile",
            "Dockerfile",
            "FROM ubuntu:20.04\n" +
            "RUN apt-get update\n" +  // Should warn about not combining with install
            "ADD local-file.txt /app/\n" +  // Should warn about using ADD instead of COPY
            "EXPOSE abc",  // Should warn about invalid port
            GeneratedFile.FileType.DOCKERFILE
        );
        
        ValidationResult result = validationService.validateContent(dockerfile);
        assertFalse(result.getIssues().isEmpty());
        
        // Check for specific warnings
        assertTrue(result.getIssues().stream()
            .anyMatch(issue -> issue.getMessage().contains("apt-get")));
        assertTrue(result.getIssues().stream()
            .anyMatch(issue -> issue.getMessage().contains("COPY instead of ADD")));
        assertTrue(result.getIssues().stream()
            .anyMatch(issue -> issue.getMessage().contains("EXPOSE")));
    }
    
    @Test
    void testValidYaml() {
        GeneratedFile yamlFile = new GeneratedFile(
            "config.yaml",
            "config.yaml",
            "apiVersion: v1\n" +
            "kind: ConfigMap\n" +
            "metadata:\n" +
            "  name: my-config\n" +
            "data:\n" +
            "  key1: value1\n" +
            "  key2: value2",
            GeneratedFile.FileType.YAML
        );
        
        ValidationResult result = validationService.validateContent(yamlFile);
        assertTrue(result.isValid());
    }
    
    @Test
    void testInvalidYaml() {
        GeneratedFile yamlFile = new GeneratedFile(
            "invalid.yaml",
            "invalid.yaml",
            "apiVersion: v1\n" +
            "kind: ConfigMap\n" +
            "metadata:\n" +
            "  name: my-config\n" +
            "data:\n" +
            "  key1: value1\n" +
            "  key2: [unclosed array",  // Invalid YAML syntax
            GeneratedFile.FileType.YAML
        );
        
        ValidationResult result = validationService.validateContent(yamlFile);
        assertFalse(result.isValid());
        assertFalse(result.getIssues().isEmpty());
    }
    
    @Test
    void testKubernetesYamlValidation() {
        GeneratedFile k8sFile = new GeneratedFile(
            "deployment.yaml",
            "deployment.yaml",
            "# Missing required fields\n" +
            "apiVersion: apps/v1\n" +
            "kind: Deployment\n" +
            "metadata:\n" +
            "  name: my-app",
            GeneratedFile.FileType.YAML
        );
        
        ValidationResult result = validationService.validateContent(k8sFile);
        assertFalse(result.isValid());
        
        // Should have errors about missing spec (apiVersion is now present)
        assertTrue(result.getIssues().stream()
            .anyMatch(issue -> issue.getMessage().contains("spec")));
    }
    
    @Test
    void testValidJson() {
        GeneratedFile jsonFile = new GeneratedFile(
            "package.json",
            "package.json",
            "{\n" +
            "  \"name\": \"my-app\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"dependencies\": {\n" +
            "    \"express\": \"^4.18.0\"\n" +
            "  }\n" +
            "}",
            GeneratedFile.FileType.JSON
        );
        
        ValidationResult result = validationService.validateContent(jsonFile);
        assertTrue(result.isValid());
    }
    
    @Test
    void testInvalidJson() {
        GeneratedFile jsonFile = new GeneratedFile(
            "invalid.json",
            "invalid.json",
            "{\n" +
            "  \"name\": \"my-app\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"dependencies\": {\n" +
            "    \"express\": \"^4.18.0\"\n" +
            "  }\n" +
            // Missing closing brace
            "",
            GeneratedFile.FileType.JSON
        );
        
        ValidationResult result = validationService.validateContent(jsonFile);
        assertFalse(result.isValid());
        assertFalse(result.getIssues().isEmpty());
    }
    
    @Test
    void testValidXml() {
        GeneratedFile xmlFile = new GeneratedFile(
            "pom.xml",
            "pom.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project>\n" +
            "  <modelVersion>4.0.0</modelVersion>\n" +
            "  <groupId>com.example</groupId>\n" +
            "  <artifactId>my-app</artifactId>\n" +
            "  <version>1.0.0</version>\n" +
            "</project>",
            GeneratedFile.FileType.XML
        );
        
        ValidationResult result = validationService.validateContent(xmlFile);
        assertTrue(result.isValid());
    }
    
    @Test
    void testInvalidXml() {
        GeneratedFile xmlFile = new GeneratedFile(
            "invalid.xml",
            "invalid.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project>\n" +
            "  <modelVersion>4.0.0</modelVersion>\n" +
            "  <groupId>com.example</groupId>\n" +
            "  <artifactId>my-app</artifactId>\n" +
            "  <version>1.0.0</version>\n" +
            // Missing closing tag
            "",
            GeneratedFile.FileType.XML
        );
        
        ValidationResult result = validationService.validateContent(xmlFile);
        assertFalse(result.isValid());
        assertFalse(result.getIssues().isEmpty());
    }
    
    @Test
    void testShellScriptValidation() {
        GeneratedFile shellFile = new GeneratedFile(
            "build.sh",
            "build.sh",
            "#!/bin/bash\n" +
            "set -e\n" +
            "echo \"Building application\"\n" +
            "mvn clean package",
            GeneratedFile.FileType.SHELL
        );
        
        ValidationResult result = validationService.validateContent(shellFile);
        assertTrue(result.isValid());
    }
    
    @Test
    void testShellScriptWarnings() {
        GeneratedFile shellFile = new GeneratedFile(
            "script.sh",
            "script.sh",
            "# Missing shebang\n" +
            "echo $USER\n" +  // Unquoted variable
            "rm -rf /tmp/dangerous",  // Potentially dangerous command
            GeneratedFile.FileType.SHELL
        );
        
        ValidationResult result = validationService.validateContent(shellFile);
        assertFalse(result.getIssues().isEmpty());
        
        // Should have warnings about missing shebang and unquoted variable
        assertTrue(result.getIssues().stream()
            .anyMatch(issue -> issue.getMessage().contains("shebang")));
        assertTrue(result.getIssues().stream()
            .anyMatch(issue -> issue.getMessage().contains("variable")));
    }
    
    @Test
    void testPropertiesValidation() {
        GeneratedFile propsFile = new GeneratedFile(
            "application.properties",
            "application.properties",
            "server.port=8080\n" +
            "spring.datasource.url=jdbc:h2:mem:testdb\n" +
            "spring.jpa.hibernate.ddl-auto=create-drop",
            GeneratedFile.FileType.PROPERTIES
        );
        
        ValidationResult result = validationService.validateContent(propsFile);
        assertTrue(result.isValid());
    }
    
    @Test
    void testPropertiesWithDuplicates() {
        GeneratedFile propsFile = new GeneratedFile(
            "application.properties",
            "application.properties",
            "server.port=8080\n" +
            "spring.datasource.url=jdbc:h2:mem:testdb\n" +
            "server.port=9090\n" +  // Duplicate key
            "invalid line without equals",  // Invalid format
            GeneratedFile.FileType.PROPERTIES
        );
        
        ValidationResult result = validationService.validateContent(propsFile);
        assertFalse(result.isValid()); // Should be invalid due to ERROR severity issue
        
        // Should have warnings about duplicate key and invalid format
        assertTrue(result.getIssues().stream()
            .anyMatch(issue -> issue.getMessage().contains("defined multiple times")));
        assertTrue(result.getIssues().stream()
            .anyMatch(issue -> issue.getMessage().contains("key=value or key:value format")));
    }
    
    @Test
    void testPlainTextFile() {
        GeneratedFile textFile = new GeneratedFile(
            "README.txt",
            "README.txt",
            "This is a plain text file.\n" +
            "It should not have any validation issues.",
            GeneratedFile.FileType.TEXT
        );
        
        ValidationResult result = validationService.validateContent(textFile);
        assertTrue(result.isValid());
        assertTrue(result.getIssues().isEmpty());
    }
}