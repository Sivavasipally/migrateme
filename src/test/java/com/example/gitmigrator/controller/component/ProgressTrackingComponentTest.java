package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.MigrationProgress;
import com.example.gitmigrator.model.MigrationStep;
import com.example.gitmigrator.model.RepositoryProgress;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Arrays;
import java.util.List;

import static org.testfx.assertions.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
class ProgressTrackingComponentTest {

    private ProgressTrackingComponent progressTrackingComponent;

    @Start
    void start(Stage stage) {
        progressTrackingComponent = new ProgressTrackingComponent();
        VBox root = new VBox(progressTrackingComponent);
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void shouldDisplayOverallProgress(FxRobot robot) {
        // Given
        MigrationProgress progress = new MigrationProgress();
        progress.setTotalRepositories(3);
        progress.setCompletedRepositories(1);
        progress.setCurrentStep("Processing repositories");

        // When
        Platform.runLater(() -> progressTrackingComponent.updateProgress(progress));
        robot.sleep(100);

        // Then
        assertThat(robot.lookup(".overall-progress").queryProgressBar()).hasProgress(1.0/3.0, 0.01);
        assertThat(robot.lookup(".current-step").queryLabeled()).hasText("Processing repositories");
    }

    @Test
    void shouldDisplayRepositoryProgress(FxRobot robot) {
        // Given
        List<RepositoryProgress> repoProgresses = Arrays.asList(
            createRepositoryProgress("repo1", MigrationStep.CLONING, 0.5),
            createRepositoryProgress("repo2", MigrationStep.ANALYZING, 0.8),
            createRepositoryProgress("repo3", MigrationStep.COMPLETED, 1.0)
        );

        // When
        Platform.runLater(() -> progressTrackingComponent.updateRepositoryProgress(repoProgresses));
        robot.sleep(100);

        // Then
        assertThat(robot.lookup(".repository-progress-list").queryListView()).hasListCell("repo1");
        assertThat(robot.lookup(".repository-progress-list").queryListView()).hasListCell("repo2");
        assertThat(robot.lookup(".repository-progress-list").queryListView()).hasListCell("repo3");
    }

    @Test
    void shouldShowDetailedStepProgress(FxRobot robot) {
        // Given
        RepositoryProgress repoProgress = createRepositoryProgress("test-repo", MigrationStep.GENERATING_FILES, 0.6);
        repoProgress.setCurrentOperation("Generating Dockerfile");

        Platform.runLater(() -> progressTrackingComponent.updateRepositoryProgress(Arrays.asList(repoProgress)));
        robot.sleep(100);

        // When
        robot.clickOn("test-repo");

        // Then
        assertThat(robot.lookup(".current-operation").queryLabeled()).hasText("Generating Dockerfile");
        assertThat(robot.lookup(".step-progress").queryProgressBar()).hasProgress(0.6, 0.01);
    }

    @Test
    void shouldUpdateProgressInRealTime(FxRobot robot) {
        // Given
        MigrationProgress initialProgress = new MigrationProgress();
        initialProgress.setTotalRepositories(2);
        initialProgress.setCompletedRepositories(0);

        Platform.runLater(() -> progressTrackingComponent.updateProgress(initialProgress));
        robot.sleep(100);

        // When - simulate progress update
        MigrationProgress updatedProgress = new MigrationProgress();
        updatedProgress.setTotalRepositories(2);
        updatedProgress.setCompletedRepositories(1);

        Platform.runLater(() -> progressTrackingComponent.updateProgress(updatedProgress));
        robot.sleep(100);

        // Then
        assertThat(robot.lookup(".overall-progress").queryProgressBar()).hasProgress(0.5, 0.01);
    }

    @Test
    void shouldShowElapsedTime(FxRobot robot) {
        // Given
        MigrationProgress progress = new MigrationProgress();
        progress.setStartTime(System.currentTimeMillis() - 60000); // 1 minute ago

        // When
        Platform.runLater(() -> progressTrackingComponent.updateProgress(progress));
        robot.sleep(100);

        // Then
        assertThat(robot.lookup(".elapsed-time").queryLabeled()).hasText("00:01:00");
    }

    private RepositoryProgress createRepositoryProgress(String repoName, MigrationStep step, double progress) {
        RepositoryProgress repoProgress = new RepositoryProgress();
        repoProgress.setRepositoryName(repoName);
        repoProgress.setCurrentStep(step);
        repoProgress.setProgress(progress);
        return repoProgress;
    }
}