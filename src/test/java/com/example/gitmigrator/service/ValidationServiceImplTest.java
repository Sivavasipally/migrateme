package com.example.gitmigrator.service;

import com.example.gitmigrator.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceImplTest {
    
    private ValidationServiceImpl validationService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        validationService = new ValidationServiceImpl();
    }
    
    @Test
    void testValidateDockerfileWithValidFile() throws IOException {
        // Create a valid Dockerfile
        Path dockerfilePath = tempDir.resolve("Dockerfile");
        String dockerfileContent = """
            FROM openjdk:11-jre-slim
            WORKDIR /app
            COPY target/app.jar app.jar
            EXPOSE 8080
            CMD ["java", "-jar", "app.jar"]
            """;
        Files.writeString(dockerfilePath, dockerfileContent);
        
        ValidationResult result = validationService.validateDockerfile(dockerfilePath.toFile());
        
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("Dockerfile validation passed", result.getSummary());
        assertTrue(result.getIssues().isEmpty());
    }
    
    @Test
    void testValidateDockerfileWithInvalidFile() throws IOException {
        // Create an invalid Dockerfile (missing FROM)
        Path dockerfilePath = tempDir.resolve("Dockerfile");
        String dockerfileContent = """
            WORKDIR /app
            COPY target/app.jar app.jar
            EXPOSE 8080
            CMD ["java", "-jar", "app.jar"]
            """;
        Files.writeString(dockerfilePath, dockerfileContent);
        
        ValidationResult result = validationService.validateDockerfile(dockerfilePath.toFile());
        
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getIssues().isEmpty());
        assertTrue(result.getIssues().stream().anyMatch(issue -> issue.getType().equals("MISSING_FROM")));
    }
    
    @Test
    void testValidateDockerfileWithNonExistentFile() {
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent-dockerfile");
        
        ValidationResult result = validationService.validateDockerfile(nonExistentFile);
        
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getIssues().isEmpty());
        assertTrue(result.getIssues().stream().anyMatch(issue -> issue.getType().equals("FILE_NOT_FOUND")));
    }
    
    @Test
    void testValidateHelmChartWithValidStructure() throws IOException {
        // Create a valid Helm chart structure
        Path chartDir = tempDir.resolve("test-chart");
        Files.createDirectories(chartDir);
        Files.createDirectories(chartDir.resolve("templates"));
        
        // Create Chart.yaml
        String chartYaml = """
            apiVersion: v2
            name: test-chart
            version: 1.0.0
            description: A test Helm chart
            """;
        Files.writeString(chartDir.resolve("Chart.yaml"), chartYaml);
        
        // Create values.yaml
        String valuesYaml = """
            replicaCount: 1
            image:
              repository: nginx
              tag: latest
            """;
        Files.writeString(chartDir.resolve("values.yaml"), valuesYaml);
        
        ValidationResult result = validationService.validateHelmChart(chartDir.toFile());
        
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("Helm chart validation passed", result.getSummary());
    }
    
    @Test
    void testValidateHelmChartWithMissingFiles() throws IOException {
        // Create a Helm chart directory without required files
        Path chartDir = tempDir.resolve("incomplete-chart");
        Files.createDirectories(chartDir);
        
        ValidationResult result = validationService.validateHelmChart(chartDir.toFile());
        
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getIssues().isEmpty());
        assertTrue(result.getIssues().stream().anyMatch(issue -> issue.getType().equals("MISSING_CHART_YAML")));
    }
    
    @Test
    void testValidateHelmChartWithNonExistentDirectory() {
        File nonExistentDir = new File(tempDir.toFile(), "nonexistent-chart");
        
        ValidationResult result = validationService.validateHelmChart(nonExistentDir);
        
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getIssues().stream().anyMatch(issue -> issue.getType().equals("CHART_NOT_FOUND")));
    }
    
    @Test
    void testValidateKubernetesManifestWithValidFile() throws IOException {
        // Create a valid Kubernetes manifest
        Path manifestPath = tempDir.resolve("deployment.yaml");
        String manifestContent = """
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: test-deployment
            spec:
              replicas: 1
              selector:
                matchLabels:
                  app: test
              template:
                metadata:
                  labels:
                    app: test
                spec:
                  containers:
                  - name: test
                    image: nginx:1.20
                    ports:
                    - containerPort: 80
            """;
        Files.writeString(manifestPath, manifestContent);
        
        ValidationResult result = validationService.validateKubernetesManifest(manifestPath.toFile());
        
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("Kubernetes manifest validation passed", result.getSummary());
    }
    
    @Test
    void testValidateKubernetesManifestWithInvalidFile() throws IOException {
        // Create an invalid Kubernetes manifest (missing required fields)
        Path manifestPath = tempDir.resolve("invalid-manifest.yaml");
        String manifestContent = """
            spec:
              replicas: 1
            """;
        Files.writeString(manifestPath, manifestContent);
        
        ValidationResult result = validationService.validateKubernetesManifest(manifestPath.toFile());
        
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getIssues().isEmpty());
        assertTrue(result.getIssues().stream().anyMatch(issue -> issue.getType().equals("MISSING_API_VERSION")));
        assertTrue(result.getIssues().stream().anyMatch(issue -> issue.getType().equals("MISSING_KIND")));
        assertTrue(result.getIssues().stream().anyMatch(issue -> issue.getType().equals("MISSING_METADATA")));
    }
    
    @Test
    void testValidateKubernetesManifestsMultipleFiles() throws IOException {
        // Create multiple manifest files
        Path manifest1 = tempDir.resolve("deployment.yaml");
        Path manifest2 = tempDir.resolve("service.yaml");
        
        String deploymentContent = """
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: test-deployment
            spec:
              replicas: 1
            """;
        
        String serviceContent = """
            apiVersion: v1
            kind: Service
            metadata:
              name: test-service
            spec:
              ports:
              - port: 80
            """;
        
        Files.writeString(manifest1, deploymentContent);
        Files.writeString(manifest2, serviceContent);
        
        List<File> manifests = List.of(manifest1.toFile(), manifest2.toFile());
        ValidationResult result = validationService.validateKubernetesManifests(manifests);
        
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getSummary().contains("All 2 Kubernetes manifests are valid"));
    }
    
    @Test
    void testValidateDockerComposeWithValidFile() throws IOException {
        // Create a valid Docker Compose file
        Path composePath = tempDir.resolve("docker-compose.yml");
        String composeContent = """
            version: '3.8'
            services:
              web:
                image: nginx:latest
                ports:
                  - "80:80"
              db:
                image: postgres:13
                environment:
                  POSTGRES_PASSWORD: password
            """;
        Files.writeString(composePath, composeContent);
        
        ValidationResult result = validationService.validateDockerCompose(composePath.toFile());
        
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("Docker Compose validation passed", result.getSummary());
    }
    
    @Test
    void testValidateDockerComposeWithInvalidFile() throws IOException {
        // Create an invalid Docker Compose file (missing version)
        Path composePath = tempDir.resolve("docker-compose.yml");
        String composeContent = """
            services:
              web:
                image: nginx:latest
            """;
        Files.writeString(composePath, composeContent);
        
        ValidationResult result = validationService.validateDockerCompose(composePath.toFile());
        
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getIssues().stream().anyMatch(issue -> issue.getType().equals("MISSING_VERSION")));
    }
    
    @Test
    void testValidateFilesWithMixedTypes() throws IOException {
        // Create files of different types
        Path dockerfile = tempDir.resolve("Dockerfile");
        Path manifest = tempDir.resolve("deployment.yaml");
        Path compose = tempDir.resolve("docker-compose.yml");
        
        Files.writeString(dockerfile, "FROM nginx:latest\nCMD [\"nginx\", \"-g\", \"daemon off;\"]");
        Files.writeString(manifest, "apiVersion: v1\nkind: Service\nmetadata:\n  name: test");
        Files.writeString(compose, "version: '3.8'\nservices:\n  web:\n    image: nginx");
        
        List<File> files = List.of(dockerfile.toFile(), manifest.toFile(), compose.toFile());
        ValidationResult result = validationService.validateFiles(files);
        
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getSummary().contains("All 3 files are valid"));
    }
    
    @Test
    void testBuildDockerImageWithoutDocker() {
        // This test assumes Docker is not available
        Path dockerfile = tempDir.resolve("Dockerfile");
        
        CompletableFuture<ValidationResult> future = validationService.buildDockerImage(dockerfile.toFile(), "test-image");
        ValidationResult result = future.join();
        
        assertNotNull(result);
        // Result depends on whether Docker is actually available in test environment
        // If Docker is not available, it should fail with DOCKER_NOT_AVAILABLE
        if (!validationService.isDockerAvailable()) {
            assertFalse(result.isValid());
            assertTrue(result.getIssues().stream().anyMatch(issue -> issue.getType().equals("DOCKER_NOT_AVAILABLE")));
        }
    }
    
    @Test
    void testToolAvailabilityChecks() {
        // Test tool availability methods
        // Results depend on what's actually installed in the test environment
        
        // These methods should not throw exceptions
        assertDoesNotThrow(() -> validationService.isDockerAvailable());
        assertDoesNotThrow(() -> validationService.isHelmAvailable());
        assertDoesNotThrow(() -> validationService.isKubectlAvailable());
        
        // Version methods should not throw exceptions
        assertDoesNotThrow(() -> validationService.getDockerVersion());
        assertDoesNotThrow(() -> validationService.getHelmVersion());
        assertDoesNotThrow(() -> validationService.getKubectlVersion());
    }
    
    @Test
    void testValidateEnvironment() {
        ValidationResult result = validationService.validateEnvironment();
        
        assertNotNull(result);
        assertTrue(result.isValid()); // Environment validation should always pass
        assertNotNull(result.getSummary());
        assertNotNull(result.getMetrics());
        
        // Check that metrics contain tool availability information
        assertTrue(result.getMetrics().containsKey("dockerAvailable"));
        assertTrue(result.getMetrics().containsKey("helmAvailable"));
        assertTrue(result.getMetrics().containsKey("kubectlAvailable"));
    }
    
    @Test
    void testValidateUnknownFileType() throws IOException {
        // Create a file with unknown extension
        Path unknownFile = tempDir.resolve("unknown.txt");
        Files.writeString(unknownFile, "This is an unknown file type");
        
        ValidationResult result = validationService.validateFiles(List.of(unknownFile.toFile()));
        
        assertNotNull(result);
        assertTrue(result.isValid()); // Unknown files should not fail validation
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.getType().equals("UNKNOWN_FILE_TYPE")));
    }
    
    @Test
    void testDockerfileWithWarnings() throws IOException {
        // Create a Dockerfile that should generate warnings
        Path dockerfilePath = tempDir.resolve("Dockerfile");
        String dockerfileContent = """
            FROM ubuntu:latest
            RUN apt-get update && apt-get install -y nginx
            ADD file.txt /app/
            WORKDIR /app
            """;
        Files.writeString(dockerfilePath, dockerfileContent);
        
        ValidationResult result = validationService.validateDockerfile(dockerfilePath.toFile());
        
        assertNotNull(result);
        // Should be valid but have warnings
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.getType().equals("PREFER_COPY")));
        assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.getType().equals("APT_CLEANUP")));
        assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.getType().equals("MISSING_CMD_ENTRYPOINT")));
    }
    
    @Test
    void testKubernetesManifestWithWarnings() throws IOException {
        // Create a manifest that should generate warnings
        Path manifestPath = tempDir.resolve("deployment.yaml");
        String manifestContent = """
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: test-deployment
            spec:
              template:
                spec:
                  containers:
                  - name: test
                    image: nginx:latest
            """;
        Files.writeString(manifestPath, manifestContent);
        
        ValidationResult result = validationService.validateKubernetesManifest(manifestPath.toFile());
        
        assertNotNull(result);
        assertTrue(result.isValid());
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.getType().equals("LATEST_TAG")));
    }
}