package com.example.gitmigrator.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive error report with actionable suggestions
 */
public class ErrorReport {
    private final String id;
    private final LocalDateTime timestamp;
    private final ErrorCategory category;
    private final String title;
    private final String description;
    private final String technicalDetails;
    private final List<String> suggestedActions;
    private final List<String> relatedDocumentation;
    private final String context;
    private final Throwable originalException;
    private final ErrorSeverity severity;

    private ErrorReport(Builder builder) {
        this.id = builder.id;
        this.timestamp = builder.timestamp;
        this.category = builder.category;
        this.title = builder.title;
        this.description = builder.description;
        this.technicalDetails = builder.technicalDetails;
        this.suggestedActions = new ArrayList<>(builder.suggestedActions);
        this.relatedDocumentation = new ArrayList<>(builder.relatedDocumentation);
        this.context = builder.context;
        this.originalException = builder.originalException;
        this.severity = builder.severity;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTechnicalDetails() {
        return technicalDetails;
    }

    public List<String> getSuggestedActions() {
        return new ArrayList<>(suggestedActions);
    }

    public List<String> getRelatedDocumentation() {
        return new ArrayList<>(relatedDocumentation);
    }

    public String getContext() {
        return context;
    }

    public Throwable getOriginalException() {
        return originalException;
    }

    public ErrorSeverity getSeverity() {
        return severity;
    }

    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(category.getDisplayName()).append("] ").append(title);
        if (description != null && !description.isEmpty()) {
            sb.append("\n\n").append(description);
        }
        return sb.toString();
    }

    public String getDetailedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Error Report: ").append(title).append("\n");
        sb.append("Category: ").append(category.getDisplayName()).append("\n");
        sb.append("Severity: ").append(severity.getDisplayName()).append("\n");
        sb.append("Time: ").append(timestamp).append("\n");
        
        if (context != null && !context.isEmpty()) {
            sb.append("Context: ").append(context).append("\n");
        }
        
        sb.append("\nDescription:\n").append(description).append("\n");
        
        if (technicalDetails != null && !technicalDetails.isEmpty()) {
            sb.append("\nTechnical Details:\n").append(technicalDetails).append("\n");
        }
        
        if (!suggestedActions.isEmpty()) {
            sb.append("\nSuggested Actions:\n");
            for (int i = 0; i < suggestedActions.size(); i++) {
                sb.append((i + 1)).append(". ").append(suggestedActions.get(i)).append("\n");
            }
        }
        
        if (!relatedDocumentation.isEmpty()) {
            sb.append("\nRelated Documentation:\n");
            for (String doc : relatedDocumentation) {
                sb.append("- ").append(doc).append("\n");
            }
        }
        
        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private LocalDateTime timestamp = LocalDateTime.now();
        private ErrorCategory category = ErrorCategory.UNKNOWN;
        private String title;
        private String description;
        private String technicalDetails;
        private List<String> suggestedActions = new ArrayList<>();
        private List<String> relatedDocumentation = new ArrayList<>();
        private String context;
        private Throwable originalException;
        private ErrorSeverity severity = ErrorSeverity.MEDIUM;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder category(ErrorCategory category) {
            this.category = category;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder technicalDetails(String technicalDetails) {
            this.technicalDetails = technicalDetails;
            return this;
        }

        public Builder addSuggestedAction(String action) {
            this.suggestedActions.add(action);
            return this;
        }

        public Builder suggestedActions(List<String> actions) {
            this.suggestedActions = new ArrayList<>(actions);
            return this;
        }

        public Builder addRelatedDocumentation(String documentation) {
            this.relatedDocumentation.add(documentation);
            return this;
        }

        public Builder relatedDocumentation(List<String> documentation) {
            this.relatedDocumentation = new ArrayList<>(documentation);
            return this;
        }

        public Builder context(String context) {
            this.context = context;
            return this;
        }

        public Builder originalException(Throwable exception) {
            this.originalException = exception;
            return this;
        }

        public Builder severity(ErrorSeverity severity) {
            this.severity = severity;
            return this;
        }

        public ErrorReport build() {
            if (id == null) {
                id = "error-" + System.currentTimeMillis();
            }
            if (title == null) {
                title = "Unknown Error";
            }
            return new ErrorReport(this);
        }
    }
}