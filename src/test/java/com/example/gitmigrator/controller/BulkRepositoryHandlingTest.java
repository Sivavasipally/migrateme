package com.example.gitmigrator.controller;

import com.example.gitmigrator.model.MigrationConfiguration;
import com.example.gitmigrator.model.RepositoryInfo;
import com.example.gitmigrator.service.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for bulk repository handling functionality in EnhancedMainController.
 * Tests the requirements: 5.6, 5.7, 5.8 from the bulk repository discovery spec.
 */
@ExtendWith(MockitoExtension.class)
class BulkRepositoryHandlingTest {

    @Mock
    private GitApiService gitApiService;
    @Mock
    private MigrationOrchestratorService migrationOrchestratorService;
    @Mock
    private TemplateManagementService templateService;
    @Mock
    private TransformationService transformationService;
    @Mock
    private MigrationQueueService migrationQueueService;
    @Mock
    private GitServiceIntegration gitServiceIntegration;
    @Mock
    private ValidationService validationService;
    @Mock
    private ProgressTrackingService progressTrackingService;
    @Mock
    private ErrorReportingService errorReportingService;
    @Mock
    private GitServiceFactory gitServiceFactory;

    private EnhancedMainController controller;
    private ObservableList<RepositoryInfo> repositories;

    @BeforeEach
    void setUp() {
        controller = new EnhancedMainController();
        repositories = FXCollections.observableArrayList();
        
        // Use reflection to set the repositories field
        try {
            var repositoriesField = EnhancedMainController.class.getDeclaredField("repositories");
            repositoriesField.setAccessible(true);
            repositoriesField.set(controller, repositories);
            
            // Set up services
            var gitApiServiceField = EnhancedMainController.class.getDeclaredField("gitApiService");
            gitApiServiceField.setAccessible(true);
            gitApiServiceField.set(controller, gitApiService);
            
            var transformationServiceField = EnhancedMainController.class.getDeclaredField("transformationService");
            transformationServiceField.setAccessible(true);
            transformationServiceField.set(controller, transformationService);
            
        } catch (Exception e) {
            fail("Failed to set up test: " + e.getMessage());
        }
    }

    @Test
    void testBulkRepositoryAddition() throws Exception {
        // Arrange
        List<RepositoryInfo> newRepositories = createTestRepositories();
        
        // Act
        var method = EnhancedMainController.class.getDeclaredMethod("addRepositoriesWithDuplicateDetection", List.class);
        method.setAccessible(true);
        method.invoke(controller, newRepositories);
        
        // Assert
        assertEquals(3, repositories.size(), "All repositories should be added");
        assertTrue(repositories.stream().allMatch(repo -> repo.getMigrationConfig() != null),
                  "All repositories should have migration configuration");
    }

    @Test
    void testDuplicateDetection() throws Exception {
        // Arrange
        RepositoryInfo existing = createRepositoryInfo("test-repo", "https://github.com/test/test-repo.git");
        repositories.add(existing);
        
        List<RepositoryInfo> newRepositories = Arrays.asList(
            createRepositoryInfo("test-repo", "https://github.com/test/test-repo.git"), // Duplicate
            createRepositoryInfo("new-repo", "https://github.com/test/new-repo.git")    // New
        );
        
        // Act
        var method = EnhancedMainController.class.getDeclaredMethod("addRepositoriesWithDuplicateDetection", List.class);
        method.setAccessible(true);
        method.invoke(controller, newRepositories);
        
        // Assert
        assertEquals(2, repositories.size(), "Only non-duplicate repository should be added");
        assertTrue(repositories.stream().anyMatch(repo -> "new-repo".equals(repo.getName())),
                  "New repository should be added");
    }

    @Test
    void testConfigurationPreservation() throws Exception {
        // Arrange
        MigrationConfiguration currentConfig = new MigrationConfiguration();
        currentConfig.setTargetPlatform("kubernetes");
        Set<String> components = new HashSet<>(Arrays.asList("helm", "dockerfile"));
        currentConfig.setOptionalComponents(components);
        currentConfig.setTemplateName("spring-boot-template");
        currentConfig.setEnableValidation(true);
        
        // Set current configuration
        var configField = EnhancedMainController.class.getDeclaredField("currentConfiguration");
        configField.setAccessible(true);
        configField.set(controller, currentConfig);
        
        RepositoryInfo newRepo = createRepositoryInfo("test-repo", "https://github.com/test/test-repo.git");
        
        // Act
        var method = EnhancedMainController.class.getDeclaredMethod("preserveExistingConfigurations", RepositoryInfo.class);
        method.setAccessible(true);
        method.invoke(controller, newRepo);
        
        // Assert
        assertNotNull(newRepo.getMigrationConfig(), "Repository should have migration configuration");
        assertEquals("kubernetes", newRepo.getMigrationConfig().getTargetPlatform(),
                    "Target platform should be preserved");
        assertEquals(components, newRepo.getMigrationConfig().getOptionalComponents(),
                    "Optional components should be preserved");
        assertEquals("spring-boot-template", newRepo.getMigrationConfig().getTemplateName(),
                    "Template name should be preserved");
        assertTrue(newRepo.getMigrationConfig().isEnableValidation(),
                  "Validation setting should be preserved");
    }

    @Test
    void testRepositoryTableRefresh() throws Exception {
        // Arrange
        List<RepositoryInfo> addedRepositories = createTestRepositories();
        repositories.addAll(addedRepositories);
        
        // Act
        var method = EnhancedMainController.class.getDeclaredMethod("refreshRepositoryTableAfterBulkAddition", List.class);
        method.setAccessible(true);
        method.invoke(controller, addedRepositories);
        
        // Assert - This test mainly verifies the method doesn't throw exceptions
        // In a real JavaFX environment, we would verify UI updates
        assertTrue(true, "Repository table refresh should complete without errors");
    }

    @Test
    void testBulkAdditionFeedback() throws Exception {
        // Act
        var method = EnhancedMainController.class.getDeclaredMethod("showBulkAdditionFeedback", int.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(controller, 5, 2, 7); // 5 added, 2 duplicates, 7 total attempted
        
        // Assert - This test mainly verifies the method doesn't throw exceptions
        // In a real JavaFX environment, we would verify the dialog content
        assertTrue(true, "Bulk addition feedback should be shown without errors");
    }

    @Test
    void testIsDuplicateRepository() throws Exception {
        // Arrange
        RepositoryInfo repo1 = createRepositoryInfo("test-repo", "https://github.com/test/test-repo.git");
        RepositoryInfo repo2 = createRepositoryInfo("test-repo", "https://github.com/test/test-repo.git");
        RepositoryInfo repo3 = createRepositoryInfo("different-repo", "https://github.com/test/different-repo.git");
        
        // Act
        var method = EnhancedMainController.class.getDeclaredMethod("isDuplicateRepository", RepositoryInfo.class, RepositoryInfo.class);
        method.setAccessible(true);
        boolean isDuplicate1 = (Boolean) method.invoke(controller, repo1, repo2);
        boolean isDuplicate2 = (Boolean) method.invoke(controller, repo1, repo3);
        
        // Assert
        assertTrue(isDuplicate1, "Repositories with same clone URL should be considered duplicates");
        assertFalse(isDuplicate2, "Repositories with different clone URLs should not be considered duplicates");
    }

    private List<RepositoryInfo> createTestRepositories() {
        return Arrays.asList(
            createRepositoryInfo("repo1", "https://github.com/test/repo1.git"),
            createRepositoryInfo("repo2", "https://github.com/test/repo2.git"),
            createRepositoryInfo("repo3", "https://github.com/test/repo3.git")
        );
    }

    private RepositoryInfo createRepositoryInfo(String name, String cloneUrl) {
        RepositoryInfo repo = new RepositoryInfo();
        repo.setName(name);
        repo.setCloneUrl(cloneUrl);
        repo.setUrl(cloneUrl);
        repo.setDescription("Test repository: " + name);
        repo.setLastCommitDate(LocalDateTime.now());
        return repo;
    }
}