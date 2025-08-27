package com.example.gitmigrator.service;

import com.example.gitmigrator.model.ErrorCategory;
import com.example.gitmigrator.model.ErrorReport;
import com.example.gitmigrator.model.ErrorSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ErrorRecoveryHelperImplTest {

    private ErrorRecoveryHelperImpl errorRecoveryHelper;

    @BeforeEach
    void setUp() {
        errorRecoveryHelper = new ErrorRecoveryHelperImpl();
    }

    @Test
    void shouldProvideRecoverySuggestionsForGitErrors() {
        // Given
        ErrorReport gitError = createErrorReport("Authentication failed", ErrorCategory.GIT_OPERATION);

        // When
        List<String> suggestions = errorRecoveryHelper.getRecoverySuggestions(gitError);

        // Then
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions).anyMatch(s -> s.contains("Check your credentials"));
        assertThat(suggestions).anyMatch(s -> s.contains("Verify repository access"));
    }

    @Test
    void shouldProvideRecoverySuggestionsForValidationErrors() {
        // Given
        ErrorReport validationError = createErrorReport("Invalid Dockerfile syntax", ErrorCategory.VALIDATION);

        // When
        List<String> suggestions = errorRecoveryHelper.getRecoverySuggestions(validationError);

        // Then
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions).anyMatch(s -> s.contains("Check Dockerfile syntax"));
        assertThat(suggestions).anyMatch(s -> s.contains("Validate base image"));
    }

    @Test
    void shouldProvideRecoverySuggestionsForTemplateErrors() {
        // Given
        ErrorReport templateError = createErrorReport("Template not found", ErrorCategory.TEMPLATE);

        // When
        List<String> suggestions = errorRecoveryHelper.getRecoverySuggestions(templateError);

        // Then
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions).anyMatch(s -> s.contains("Check template path"));
        assertThat(suggestions).anyMatch(s -> s.contains("Verify template exists"));
    }

    @Test
    void shouldProvideGenericSuggestionsForUnknownErrors() {
        // Given
        ErrorReport unknownError = createErrorReport("Unknown error", ErrorCategory.UNKNOWN);

        // When
        List<String> suggestions = errorRecoveryHelper.getRecoverySuggestions(unknownError);

        // Then
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions).anyMatch(s -> s.contains("Check logs"));
        assertThat(suggestions).anyMatch(s -> s.contains("Retry operation"));
    }

    @Test
    void shouldDetermineIfErrorIsRecoverable() {
        // Given
        ErrorReport recoverableError = createErrorReport("Network timeout", ErrorCategory.GIT_OPERATION);
        ErrorReport nonRecoverableError = createErrorReport("Invalid project structure", ErrorCategory.VALIDATION);

        // When & Then
        assertThat(errorRecoveryHelper.isRecoverable(recoverableError)).isTrue();
        assertThat(errorRecoveryHelper.isRecoverable(nonRecoverableError)).isFalse();
    }

    private ErrorReport createErrorReport(String message, ErrorCategory category) {
        ErrorReport report = new ErrorReport();
        report.setMessage(message);
        report.setCategory(category);
        report.setSeverity(ErrorSeverity.ERROR);
        report.setTimestamp(LocalDateTime.now());
        return report;
    }
}