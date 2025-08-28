package com.example.gitmigrator.service;

import com.example.gitmigrator.model.ErrorCategory;
import com.example.gitmigrator.model.ErrorReport;
import com.example.gitmigrator.model.ErrorSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorReportingServiceImplTest {

    @Mock
    private ErrorRecoveryHelper errorRecoveryHelper;

    private ErrorReportingServiceImpl errorReportingService;

    @BeforeEach
    void setUp() {
        errorReportingService = new ErrorReportingServiceImpl(errorRecoveryHelper);
    }

    @Test
    void shouldReportError() {
        // Given
        String message = "Test error message";
        ErrorCategory category = ErrorCategory.GIT_OPERATION;
        Exception cause = new RuntimeException("Root cause");

        // When
        errorReportingService.reportError(message, category, cause);

        // Then
        List<ErrorReport> errors = errorReportingService.getErrorReports();
        assertThat(errors).hasSize(1);
        
        ErrorReport error = errors.get(0);
        assertThat(error.getTitle()).isEqualTo(message);
        assertThat(error.getCategory()).isEqualTo(category);
        assertThat(error.getSeverity()).isEqualTo(ErrorSeverity.ERROR);
        assertThat(error.getTechnicalDetails()).contains("Root cause");
    }

    @Test
    void shouldReportWarning() {
        // Given
        String message = "Test warning message";
        ErrorCategory category = ErrorCategory.TEMPLATE;

        // When
        errorReportingService.reportWarning(message, category);

        // Then
        List<ErrorReport> errors = errorReportingService.getErrorReports();
        assertThat(errors).hasSize(1);
        
        ErrorReport error = errors.get(0);
        assertThat(error.getTitle()).isEqualTo(message);
        assertThat(error.getCategory()).isEqualTo(category);
        assertThat(error.getSeverity()).isEqualTo(ErrorSeverity.WARNING);
    }

    @Test
    void shouldFilterErrorsByCategory() {
        // Given
        errorReportingService.reportError("Git error", ErrorCategory.GIT_OPERATION, null);
        errorReportingService.reportError("Template error", ErrorCategory.TEMPLATE, null);
        errorReportingService.reportError("Validation error", ErrorCategory.VALIDATION, null);

        // When
        List<ErrorReport> gitErrors = errorReportingService.getErrorsByCategory(ErrorCategory.GIT_OPERATION);
        List<ErrorReport> templateErrors = errorReportingService.getErrorsByCategory(ErrorCategory.TEMPLATE);

        // Then
        assertThat(gitErrors).hasSize(1);
        assertThat(gitErrors.get(0).getTitle()).isEqualTo("Git error");
        
        assertThat(templateErrors).hasSize(1);
        assertThat(templateErrors.get(0).getTitle()).isEqualTo("Template error");
    }

    @Test
    void shouldFilterErrorsBySeverity() {
        // Given
        errorReportingService.reportError("Error message", ErrorCategory.GIT_OPERATION, null);
        errorReportingService.reportWarning("Warning message", ErrorCategory.TEMPLATE);

        // When
        List<ErrorReport> errors = errorReportingService.getErrorsBySeverity(ErrorSeverity.ERROR);
        List<ErrorReport> warnings = errorReportingService.getErrorsBySeverity(ErrorSeverity.WARNING);

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getTitle()).isEqualTo("Error message");
        
        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0).getTitle()).isEqualTo("Warning message");
    }

    @Test
    void shouldClearErrors() {
        // Given
        errorReportingService.reportError("Error 1", ErrorCategory.GIT_OPERATION, null);
        errorReportingService.reportError("Error 2", ErrorCategory.TEMPLATE, null);
        assertThat(errorReportingService.getErrorReports()).hasSize(2);

        // When
        errorReportingService.clearErrors();

        // Then
        assertThat(errorReportingService.getErrorReports()).isEmpty();
    }

    @Test
    void shouldNotifyListenersOnNewError() throws Exception {
        // Given
        CompletableFuture<ErrorReport> notificationFuture = new CompletableFuture<>();
        errorReportingService.addErrorListener(notificationFuture::complete);

        // When
        errorReportingService.reportError("Test error", ErrorCategory.GIT_OPERATION, null);

        // Then
        ErrorReport notifiedError = notificationFuture.get(1, TimeUnit.SECONDS);
        assertThat(notifiedError.getTitle()).isEqualTo("Test error");
        assertThat(notifiedError.getCategory()).isEqualTo(ErrorCategory.GIT_OPERATION);
    }

    @Test
    void shouldGetRecoverySuggestions() {
        // Given
        when(errorRecoveryHelper.getRecoverySuggestions(any(ErrorReport.class)))
            .thenReturn(List.of("Suggestion 1", "Suggestion 2"));

        errorReportingService.reportError("Test error", ErrorCategory.GIT_OPERATION, null);
        ErrorReport error = errorReportingService.getErrorReports().get(0);

        // When
        List<String> suggestions = errorReportingService.getRecoverySuggestions(error);

        // Then
        assertThat(suggestions).containsExactly("Suggestion 1", "Suggestion 2");
        verify(errorRecoveryHelper).getRecoverySuggestions(error);
    }
}