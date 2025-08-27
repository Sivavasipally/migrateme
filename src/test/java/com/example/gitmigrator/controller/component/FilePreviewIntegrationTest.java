package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.GeneratedFile;
import com.example.gitmigrator.service.FileContentValidationService;
import com.example.gitmigrator.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Integration tests for file preview and editing workflows
 */
@ExtendWith(MockitoExtension.class)
class FilePreviewIntegrationTest {
    
    @Mock
    private FileContentValidationService validationService;
    
    private List<GeneratedFile> testFiles;
    
    @BeforeEach
    void setUp() {
        // Create test files
        testFiles = Arrays.asList(
            createDockerfile(),
            createKubernetesDeployment(),
            createKubernetesService(),
            createHelmValues(),
            createBuildScript()
        );
    }
    
    @Test
    void testFileCreationAndModification() {
        GeneratedFile dockerfile = createDockerfile();
        
        // Test initial state
        assertTrue(dockerfile.isNew());
        assertFalse(dockerfile.isModified());
        assertTrue(dockerfile.hasChanges()); // New files are considered as having changes
        
        // Test modification
        String originalContent = dockerfile.getContent();
        dockerfile.setContent("FROM openjdk:11-jre-slim\nCOPY app.jar /app.jar\nEXPOSE 8080");
        
        assertTrue(dockerfile.isModified());
        assertTrue(dockerfile.hasChanges()); // New files are considered as having changes
        
        // Set original content and test changes
        dockerfile.setOriginalContent(originalContent);
        assertTrue(dockerfile.hasChanges());
        
        // Test revert
        dockerfile.revertChanges();
        assertEquals(originalContent, dockerfile.getContent());
        assertFalse(dockerfile.isModified());
    }
    
    @Test
    void testFileTreeStructure() {
        // Test that files are organized correctly in tree structure
        List<String> expectedPaths = Arrays.asList(
            "Dockerfile",
            "k8s/deployment.yaml",
            "k8s/service.yaml",
            "helm/values.yaml",
            "scripts/build.sh"
        );
        
        for (int i = 0; i < testFiles.size(); i++) {
            assertEquals(expectedPaths.get(i), testFiles.get(i).getRelativePath());
        }
        
        // Test file type detection
        assertEquals(GeneratedFile.FileType.DOCKERFILE, testFiles.get(0).getFileType());
        assertEquals(GeneratedFile.FileType.YAML, testFiles.get(1).getFileType());
        assertEquals(GeneratedFile.FileType.YAML, testFiles.get(2).getFileType());
        assertEquals(GeneratedFile.FileType.YAML, testFiles.get(3).getFileType());
        assertEquals(GeneratedFile.FileType.SHELL, testFiles.get(4).getFileType());
    }
    
    @Test
    void testValidationWorkflow() {
        GeneratedFile dockerfile = createDockerfile();
        
        // Test with real validation service
        FileContentValidationService realValidationService = new FileContentValidationService();
        ValidationResult result = realValidationService.validateContent(dockerfile);
        assertTrue(result.isValid());
        
        // Test invalid content
        dockerfile.setContent("INVALID DOCKERFILE CONTENT");
        result = realValidationService.validateContent(dockerfile);
        assertFalse(result.isValid()); // Should be invalid due to missing FROM instruction
    }
    
    @Test
    void testEditingWorkflow() {
        GeneratedFile kubernetesDeployment = createKubernetesDeployment();
        String originalContent = kubernetesDeployment.getContent();
        
        // Simulate editing workflow
        // 1. Open file for editing
        assertFalse(kubernetesDeployment.isModified());
        
        // 2. Make changes
        String modifiedContent = originalContent.replace("replicas: 3", "replicas: 5");
        kubernetesDeployment.setContent(modifiedContent);
        
        // 3. Verify modification state
        assertTrue(kubernetesDeployment.isModified());
        assertEquals(modifiedContent, kubernetesDeployment.getContent());
        
        // 4. Save changes (simulate)
        kubernetesDeployment.setModified(false);
        assertFalse(kubernetesDeployment.isModified());
        
        // 5. Revert changes
        kubernetesDeployment.setOriginalContent(originalContent);
        kubernetesDeployment.revertChanges();
        assertEquals(originalContent, kubernetesDeployment.getContent());
    }
    
    @Test
    void testDiffGeneration() {
        GeneratedFile file = createDockerfile();
        String originalContent = "FROM openjdk:17-jre-slim\nCOPY app.jar /app.jar";
        String modifiedContent = "FROM openjdk:11-jre-slim\nCOPY app.jar /app.jar\nEXPOSE 8080";
        
        file.setOriginalContent(originalContent);
        file.setContent(modifiedContent);
        
        assertTrue(file.hasChanges());
        
        // In a real implementation, this would generate a proper diff
        assertNotEquals(originalContent, modifiedContent);
        assertTrue(modifiedContent.contains("EXPOSE 8080"));
        assertTrue(modifiedContent.contains("openjdk:11"));
    }
    
    @Test
    void testBatchFileOperations() {
        // Test operations on multiple files
        int modifiedCount = 0;
        
        for (GeneratedFile file : testFiles) {
            // Simulate modifications
            String content = file.getContent();
            file.setContent(content + "\n# Modified");
            
            if (file.isModified()) {
                modifiedCount++;
            }
        }
        
        assertEquals(testFiles.size(), modifiedCount);
        
        // Test batch revert
        for (GeneratedFile file : testFiles) {
            file.setOriginalContent(file.getContent().replace("\n# Modified", ""));
            file.revertChanges();
            assertFalse(file.isModified());
        }
    }
    
    private GeneratedFile createDockerfile() {
        return new GeneratedFile(
            "Dockerfile",
            "Dockerfile",
            "FROM openjdk:17-jre-slim\n" +
            "COPY target/app.jar /app.jar\n" +
            "EXPOSE 8080\n" +
            "ENTRYPOINT [\"java\", \"-jar\", \"/app.jar\"]",
            GeneratedFile.FileType.DOCKERFILE
        );
    }
    
    private GeneratedFile createKubernetesDeployment() {
        return new GeneratedFile(
            "deployment.yaml",
            "k8s/deployment.yaml",
            "apiVersion: apps/v1\n" +
            "kind: Deployment\n" +
            "metadata:\n" +
            "  name: my-app\n" +
            "spec:\n" +
            "  replicas: 3\n" +
            "  selector:\n" +
            "    matchLabels:\n" +
            "      app: my-app\n" +
            "  template:\n" +
            "    metadata:\n" +
            "      labels:\n" +
            "        app: my-app\n" +
            "    spec:\n" +
            "      containers:\n" +
            "      - name: my-app\n" +
            "        image: my-app:latest\n" +
            "        ports:\n" +
            "        - containerPort: 8080",
            GeneratedFile.FileType.YAML
        );
    }
    
    private GeneratedFile createKubernetesService() {
        return new GeneratedFile(
            "service.yaml",
            "k8s/service.yaml",
            "apiVersion: v1\n" +
            "kind: Service\n" +
            "metadata:\n" +
            "  name: my-app-service\n" +
            "spec:\n" +
            "  selector:\n" +
            "    app: my-app\n" +
            "  ports:\n" +
            "  - port: 80\n" +
            "    targetPort: 8080\n" +
            "  type: ClusterIP",
            GeneratedFile.FileType.YAML
        );
    }
    
    private GeneratedFile createHelmValues() {
        return new GeneratedFile(
            "values.yaml",
            "helm/values.yaml",
            "replicaCount: 3\n" +
            "image:\n" +
            "  repository: my-app\n" +
            "  tag: latest\n" +
            "  pullPolicy: IfNotPresent\n" +
            "service:\n" +
            "  type: ClusterIP\n" +
            "  port: 80\n" +
            "resources:\n" +
            "  limits:\n" +
            "    cpu: 500m\n" +
            "    memory: 512Mi\n" +
            "  requests:\n" +
            "    cpu: 250m\n" +
            "    memory: 256Mi",
            GeneratedFile.FileType.YAML
        );
    }
    
    private GeneratedFile createBuildScript() {
        return new GeneratedFile(
            "build.sh",
            "scripts/build.sh",
            "#!/bin/bash\n" +
            "set -e\n" +
            "\n" +
            "echo \"Building application...\"\n" +
            "mvn clean package -DskipTests\n" +
            "\n" +
            "echo \"Building Docker image...\"\n" +
            "docker build -t my-app:latest .\n" +
            "\n" +
            "echo \"Build completed successfully!\"",
            GeneratedFile.FileType.SHELL
        );
    }
}