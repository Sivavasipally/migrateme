package com.example.gitmigrator.controller.component;

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
class LogFilterComponentTest {

    private LogFilterComponent logFilterComponent;

    @Start
    void start(Stage stage) {
        logFilterComponent = new LogFilterComponent();
        VBox root = new VBox(logFilterComponent);
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void shouldFilterLogsByLevel(FxRobot robot) {
        // Given
        List<String> logEntries = Arrays.asList(
            "[INFO] Starting migration process",
            "[ERROR] Failed to clone repository",
            "[DEBUG] Analyzing project structure",
            "[WARN] Template not found, using default"
        );

        Platform.runLater(() -> logFilterComponent.setLogEntries(logEntries));
        robot.sleep(100);

        // When
        robot.clickOn(".level-filter");
        robot.clickOn("ERROR");

        // Then
        assertThat(robot.lookup(".filtered-logs").queryListView()).hasListCell("[ERROR] Failed to clone repository");
        assertThat(robot.lookup(".filtered-logs").queryListView()).doesNotHaveListCell("[INFO] Starting migration process");
    }

    @Test
    void shouldFilterLogsByRepository(FxRobot robot) {
        // Given
        List<String> logEntries = Arrays.asList(
            "[repo1] [INFO] Cloning repository",
            "[repo2] [INFO] Analyzing structure",
            "[repo1] [ERROR] Build failed",
            "[repo3] [INFO] Migration completed"
        );

        Platform.runLater(() -> logFilterComponent.setLogEntries(logEntries));
        robot.sleep(100);

        // When
        robot.clickOn(".repository-filter");
        robot.clickOn("repo1");

        // Then
        assertThat(robot.lookup(".filtered-logs").queryListView()).hasListCell("[repo1] [INFO] Cloning repository");
        assertThat(robot.lookup(".filtered-logs").queryListView()).hasListCell("[repo1] [ERROR] Build failed");
        assertThat(robot.lookup(".filtered-logs").queryListView()).doesNotHaveListCell("[repo2] [INFO] Analyzing structure");
    }

    @Test
    void shouldSearchLogsWithTextFilter(FxRobot robot) {
        // Given
        List<String> logEntries = Arrays.asList(
            "[INFO] Starting Docker build",
            "[INFO] Generating Helm charts",
            "[ERROR] Docker build failed",
            "[INFO] Kubernetes manifests created"
        );

        Platform.runLater(() -> logFilterComponent.setLogEntries(logEntries));
        robot.sleep(100);

        // When
        robot.clickOn(".search-field");
        robot.write("Docker");

        // Then
        assertThat(robot.lookup(".filtered-logs").queryListView()).hasListCell("[INFO] Starting Docker build");
        assertThat(robot.lookup(".filtered-logs").queryListView()).hasListCell("[ERROR] Docker build failed");
        assertThat(robot.lookup(".filtered-logs").queryListView()).doesNotHaveListCell("[INFO] Generating Helm charts");
    }

    @Test
    void shouldCombineMultipleFilters(FxRobot robot) {
        // Given
        List<String> logEntries = Arrays.asList(
            "[repo1] [ERROR] Docker build failed",
            "[repo1] [INFO] Docker build started",
            "[repo2] [ERROR] Template error",
            "[repo1] [ERROR] Helm validation failed"
        );

        Platform.runLater(() -> logFilterComponent.setLogEntries(logEntries));
        robot.sleep(100);

        // When
        robot.clickOn(".repository-filter");
        robot.clickOn("repo1");
        robot.clickOn(".level-filter");
        robot.clickOn("ERROR");

        // Then
        assertThat(robot.lookup(".filtered-logs").queryListView()).hasListCell("[repo1] [ERROR] Docker build failed");
        assertThat(robot.lookup(".filtered-logs").queryListView()).hasListCell("[repo1] [ERROR] Helm validation failed");
        assertThat(robot.lookup(".filtered-logs").queryListView()).doesNotHaveListCell("[repo1] [INFO] Docker build started");
        assertThat(robot.lookup(".filtered-logs").queryListView()).doesNotHaveListCell("[repo2] [ERROR] Template error");
    }

    @Test
    void shouldClearFilters(FxRobot robot) {
        // Given
        List<String> logEntries = Arrays.asList(
            "[INFO] Log entry 1",
            "[ERROR] Log entry 2"
        );

        Platform.runLater(() -> logFilterComponent.setLogEntries(logEntries));
        robot.sleep(100);

        robot.clickOn(".level-filter");
        robot.clickOn("ERROR");

        // When
        robot.clickOn(".clear-filters");

        // Then
        assertThat(robot.lookup(".filtered-logs").queryListView()).hasListCell("[INFO] Log entry 1");
        assertThat(robot.lookup(".filtered-logs").queryListView()).hasListCell("[ERROR] Log entry 2");
    }
}