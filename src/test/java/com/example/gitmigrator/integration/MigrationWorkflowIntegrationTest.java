package com.example.gitmigrator.integration;

import com.example.gitmigrator.model.*;
import com.example.gitmigrator.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MigrationWorkflowIntegrationTest {

    @Mock
    private GitOperationService gitOperationService;
    
    @Mock
    private TransformationService transformationService;
    
    @Mock
    private ValidationService validationService;
    
    @Mock
    private TemplateManagementService templateManagementService;

    private MigrationOrchestratorService orchestratorService;
    private MigrationQueueService queueService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        queueService = new MigrationQueueServiceImpl();
        orchestratorService = new MigrationOrchestratorService(
            gitOperationService, 
            transformationService, 
            validationService,
            templateManagementService
        );
    }

    @Test
    void shouldCompleteFullMigrationWorkflow() throws Exception {
        // Given
        RepositoryInfo repository = createTestRepository();
        MigrationConfiguration config = createTestConfiguration();
        
        when(gitOperationService.cloneRepository(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(tempDir.toFile()));
        
        when(transformationService.analyzeRepository(any()))
            .thenReturn(CompletableFuture.completedFuture(createAnalysisResult()));
        
        when(transformationService.generateArtifacts(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(createGeneratedFiles()));
        
        when(validationService.validateDockerfile(any()))
            .thenReturn(createValidationResult(true));

        // When
        queueService.addToQueue(repository, config);
        CompletableFuture<List<MigrationResult>> resultsFuture = queueService.processQueue();
        List<MigrationResult> results = resultsFuture.get();

        // Then
        assertThat(results).hasSize(1);
        MigrationResult result = results.get(0);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getGeneratedFiles()).isNotEmpty();
        
        verify(gitOperationService).cloneRepository(repository.getUrl(), any());
        verify(transformationService).analyzeRepository(any());
        verify(transformationService).generateArtifacts(any(), eq(config));
        verify(validationService).validateDockerfile(any());
    }

    @Test
    void shouldHandleMultipleRepositoriesInQueue() throws Exception {
        // Given
        List<RepositoryInfo> repositories = Arrays.asList(
            createTestRepository("repo1", "https://github.com/test/repo1.git"),
            createTestRepository("repo2", "https://github.com/test/repo2.git"),
            createTestRepository("repo3", "https://github.com/test/repo3.git")
        );
        
        MigrationConfiguration config = createTestConfiguration();
        
        when(gitOperationService.cloneRepository(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(tempDir.toFile()));
        
        when(transformationService.analyzeRepository(any()))
            .thenReturn(CompletableFuture.completedFuture(createAnalysisResult()));
        
        when(transformationService.generateArtifacts(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(createGeneratedFiles()));

        // When
        repositories.forEach(repo -> queueService.addToQueue(repo, config));
        CompletableFuture<List<MigrationResult>> resultsFuture = queueService.processQueue();
        List<MigrationResult> results = resultsFuture.get();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(MigrationResult::isSuccess);
        
        verify(gitOperationService, times(3)).cloneRepository(any(), any());
        verify(transformationService, times(3)).analyzeRepository(any());
        verify(transformationService, times(3)).generateArtifacts(any(), any());
    }

    @Test
    void shouldContinueProcessingAfterFailure() throws Exception {
        // Given
        List<RepositoryInfo> repositories = Arrays.asList(
            createTestRepository("success-repo", "https://github.com/test/success.git"),
            createTestRepository("failure-repo", "https://github.com/test/failure.git"),
            createTestRepository("another-success", "https://github.com/test/success2.git")
        );
        
        MigrationConfiguration config = createTestConfiguration();
        
        when(gitOperationService.cloneRepository(contains("success"), any()))
            .thenReturn(CompletableFuture.completedFuture(tempDir.toFile()));
        
        when(gitOperationService.cloneRepository(contains("failure"), any()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Clone failed")));
        
        when(transformationService.analyzeRepository(any()))
            .thenReturn(CompletableFuture.completedFuture(createAnalysisResult()));
        
        when(transformationService.generateArtifacts(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(createGeneratedFiles()));

        // When
        repositories.forEach(repo -> queueService.addToQueue(repo, config));
        CompletableFuture<List<MigrationResult>> resultsFuture = queueService.processQueue();
        List<MigrationResult> results = resultsFuture.get();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.stream().filter(MigrationResult::isSuccess)).hasSize(2);
        assertThat(results.stream().filter(r -> !r.isSuccess())).hasSize(1);
        
        MigrationResult failedResult = results.stream()
            .filter(r -> !r.isSuccess())
            .findFirst()
            .orElseThrow();
        assertThat(failedResult.getErrorMessage()).contains("Clone failed");
    }

    @Test
    void shouldApplyTemplateConfiguration() throws Exception {
        // Given
        RepositoryInfo repository = createTestRepository();
        MigrationConfiguration templateConfig = createTestConfiguration();
        templateConfig.setTemplateName("microservice-template");
        
        when(templateManagementService.loadTemplate("microservice-template"))
            .thenReturn(templateConfig);
        
        when(gitOperationService.cloneRepository(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(tempDir.toFile()));
        
        when(transformationService.analyzeRepository(any()))
            .thenReturn(CompletableFuture.completedFuture(createAnalysisResult()));
        
        when(transformationService.generateArtifacts(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(createGeneratedFiles()));

        // When
        queueService.addToQueue(repository, templateConfig);
        CompletableFuture<List<MigrationResult>> resultsFuture = queueService.processQueue();
        List<MigrationResult> results = resultsFuture.get();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).isSuccess()).isTrue();
        
        verify(templateManagementService).loadTemplate("microservice-template");
        verify(transformationService).generateArtifacts(any(), eq(templateConfig));
    }

    private RepositoryInfo createTestRepository() {
        return createTestRepository("test-repo", "https://github.com/test/repo.git");
    }

    private RepositoryInfo createTestRepository(String name, String url) {
        RepositoryInfo repo = new RepositoryInfo();
        repo.setName(name);
        repo.setUrl(url);
        repo.setDetectedFramework(FrameworkType.SPRING_BOOT);
        return repo;
    }

    private MigrationConfiguration createTestConfiguration() {
        MigrationConfiguration config = new MigrationConfiguration();
        config.setTargetPlatform("kubernetes");
        config.setOptionalComponents(Arrays.asList("dockerfile", "helm"));
        config.setEnableValidation(true);
        return config;
    }

    private MigrationResult createAnalysisResult() {
        MigrationResult result = new MigrationResult();
        result.setSuccess(true);
        return result;
    }

    private List<GeneratedFile> createGeneratedFiles() {
        GeneratedFile dockerfile = new GeneratedFile();
        dockerfile.setPath("Dockerfile");
        dockerfile.setContent("FROM openjdk:17-jre-slim\nCOPY app.jar /app.jar\nEXPOSE 8080\nCMD [\"java\", \"-jar\", \"/app.jar\"]");
        
        return Arrays.asList(dockerfile);
    }

    private ValidationResult createValidationResult(boolean isValid) {
        ValidationResult result = new ValidationResult();
        result.setValid(isValid);
        return result;
    }
}