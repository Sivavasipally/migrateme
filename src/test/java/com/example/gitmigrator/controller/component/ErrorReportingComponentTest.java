package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.ErrorCategory;
import com.example.gitmigrator.model.ErrorReport;
import com.example.gitmigrator.model.ErrorSeverity;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.testfx.assertions.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
class ErrorReportingComponentTest {

    private ErrorReportingComponent errorReportingComponent;

    @Start
    void start(Stage stage) {
        errorReportingComponent = new ErrorReportingComponent();
        VBox root = new VBox(errorReportingComponent);
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void shouldDisplayErrorReports(FxRobot robot) {
        // Given
        List<ErrorReport> errors = Arrays.asList(
            createErrorReport("Git clone failed", ErrorSeverity.ERROR, ErrorCategory.GIT_OPERATION),
            createErrorReport("Template not found", ErrorSeverity.WARNING, ErrorCategory.TEMPLATE)
        );

        // When
        Platform.runLater(() -> errorReportingComponent.displayErrors(errors));
        robot.sleep(100);

        // Then
        assertThat(robot.lookup(".error-list").queryListView()).hasListCell("Git clone failed");
        assertThat(robot.lookup(".error-list").queryListView()).hasListCell("Template not found");
    }

    @Test
    void shouldFilterErrorsBySeverity(FxRobot robot) {
        // Given
        List<ErrorReport> errors = Arrays.asList(
            createErrorReport("Critical error", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
            createErrorReport("Minor warning", ErrorSeverity.WARNING, ErrorCategory.TEMPLATE)
        );

        Platform.runLater(() -> errorReportingComponent.displayErrors(errors));
        robot.sleep(100);

        // When
        robot.clickOn(".severity-filter");
        robot.clickOn("ERROR");

        // Then
        assertThat(robot.lookup(".error-list").queryListView()).hasListCell("Critical error");
        assertThat(robot.lookup(".error-list").queryListView()).doesNotHaveListCell("Minor warning");
    }

    @Test
    void shouldShowErrorDetails(FxRobot robot) {
        // Given
        ErrorReport error = createErrorReport("Detailed error", ErrorSeverity.ERROR, ErrorCategory.GIT_OPERATION);
        error.setDetails("This is a detailed error message with stack trace");

        Platform.runLater(() -> errorReportingComponent.displayErrors(Arrays.asList(error)));
        robot.sleep(100);

        // When
        robot.clickOn("Detailed error");

        // Then
        assertThat(robot.lookup(".error-details").queryTextArea()).hasText("This is a detailed error message with stack trace");
    }

    private ErrorReport createErrorReport(String message, ErrorSeverity severity, ErrorCategory category) {
        ErrorReport report = new ErrorReport();
        report.setMessage(message);
        report.setSeverity(severity);
        report.setCategory(category);
        report.setTimestamp(LocalDateTime.now());
        return report;
    }
}