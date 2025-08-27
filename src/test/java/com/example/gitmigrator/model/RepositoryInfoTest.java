package com.example.gitmigrator.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryInfoTest {

    @Test
    void shouldCreateRepositoryInfo() {
        // When
        RepositoryInfo repo = new RepositoryInfo();

        // Then
        assertThat(repo.getId()).isNull();
        assertThat(repo.getName()).isNull();
        assertThat(repo.getUrl()).isNull();
        assertThat(repo.getDetectedFramework()).isNull();
        assertThat(repo.getStatus()).isEqualTo(MigrationStatus.NOT_STARTED);
    }

    @Test
    void shouldSetBasicProperties() {
        // Given
        RepositoryInfo repo = new RepositoryInfo();

        // When
        repo.setId("repo-123");
        repo.setName("test-repository");
        repo.setUrl("https://github.com/test/repo.git");
        repo.setLocalPath("/tmp/test-repo");

        // Then
        assertThat(repo.getId()).isEqualTo("repo-123");
        assertThat(repo.getName()).isEqualTo("test-repository");
        assertThat(repo.getUrl()).isEqualTo("https://github.com/test/repo.git");
        assertThat(repo.getLocalPath()).isEqualTo("/tmp/test-repo");
    }

    @Test
    void shouldSetFrameworkAndMetadata() {
        // Given
        RepositoryInfo repo = new RepositoryInfo();
        LocalDateTime commitDate = LocalDateTime.now().minusDays(5);

        // When
        repo.setDetectedFramework(FrameworkType.SPRING_BOOT);
        repo.setLastCommitDate(commitDate);
        repo.setLastCommitMessage("Fix security vulnerability");
        repo.setEstimatedComplexity(3);
        repo.setRepositorySize(1024000L);
        repo.setLanguages(Arrays.asList("Java", "JavaScript"));

        // Then
        assertThat(repo.getDetectedFramework()).isEqualTo(FrameworkType.SPRING_BOOT);
        assertThat(repo.getLastCommitDate()).isEqualTo(commitDate);
        assertThat(repo.getLastCommitMessage()).isEqualTo("Fix security vulnerability");
        assertThat(repo.getEstimatedComplexity()).isEqualTo(3);
        assertThat(repo.getRepositorySize()).isEqualTo(1024000L);
        assertThat(repo.getLanguages()).containsExactly("Java", "JavaScript");
    }

    @Test
    void shouldManageMigrationConfiguration() {
        // Given
        RepositoryInfo repo = new RepositoryInfo();
        MigrationConfiguration config = new MigrationConfiguration();
        config.setTargetPlatform("kubernetes");

        // When
        repo.setMigrationConfig(config);

        // Then
        assertThat(repo.getMigrationConfig()).isEqualTo(config);
        assertThat(repo.getMigrationConfig().getTargetPlatform()).isEqualTo("kubernetes");
    }

    @Test
    void shouldUpdateMigrationStatus() {
        // Given
        RepositoryInfo repo = new RepositoryInfo();

        // When
        repo.setStatus(MigrationStatus.ANALYZING);

        // Then
        assertThat(repo.getStatus()).isEqualTo(MigrationStatus.ANALYZING);
        assertThat(repo.getStatus().isInProgress()).isTrue();

        // When
        repo.setStatus(MigrationStatus.COMPLETED);

        // Then
        assertThat(repo.getStatus()).isEqualTo(MigrationStatus.COMPLETED);
        assertThat(repo.getStatus().isCompleted()).isTrue();
    }

    @Test
    void shouldHandleNumericIds() {
        // Given
        RepositoryInfo repo = new RepositoryInfo();

        // When
        repo.setId(12345L);

        // Then
        assertThat(repo.getId()).isEqualTo("12345");
    }

    @Test
    void shouldHandleUrlFallback() {
        // Given
        RepositoryInfo repo = new RepositoryInfo();

        // When - only clone URL is set
        repo.setCloneUrl("https://github.com/test/repo.git");

        // Then - getUrl() should return clone URL
        assertThat(repo.getUrl()).isEqualTo("https://github.com/test/repo.git");

        // When - HTML URL is set
        repo.setHtmlUrl("https://github.com/test/repo");

        // Then - getUrl() should return HTML URL
        assertThat(repo.getUrl()).isEqualTo("https://github.com/test/repo");
    }

    @Test
    void shouldManageJavaFXSelectionProperty() {
        // Given
        RepositoryInfo repo = new RepositoryInfo();

        // When & Then - default not selected
        assertThat(repo.isSelected()).isFalse();

        // When - set selected
        repo.setSelected(true);

        // Then
        assertThat(repo.isSelected()).isTrue();
        assertThat(repo.selectedProperty().get()).isTrue();
    }

    @Test
    void shouldConvertToString() {
        // Given
        RepositoryInfo repo = new RepositoryInfo();
        repo.setName("test-repo");
        repo.setFullName("user/test-repo");
        repo.setCloneUrl("https://github.com/test/repo.git");

        // When
        String repoString = repo.toString();

        // Then
        assertThat(repoString).contains("test-repo");
        assertThat(repoString).contains("user/test-repo");
        assertThat(repoString).contains("https://github.com/test/repo.git");
    }

    @Test
    void shouldUseConstructorWithParameters() {
        // When
        RepositoryInfo repo = new RepositoryInfo("test-repo", "user/test-repo", "https://github.com/user/test-repo.git");

        // Then
        assertThat(repo.getName()).isEqualTo("test-repo");
        assertThat(repo.getFullName()).isEqualTo("user/test-repo");
        assertThat(repo.getCloneUrl()).isEqualTo("https://github.com/user/test-repo.git");
    }
}