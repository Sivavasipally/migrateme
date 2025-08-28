package com.example.gitmigrator.controller.wizard;

import com.example.gitmigrator.model.*;
import com.example.gitmigrator.service.ValidationService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Fifth wizard step: Preview and Validation.
 * Users can preview all generated files and validate before execution.
 */
public class PreviewAndValidationStep implements WizardStep {
    
    private static final Logger logger = LoggerFactory.getLogger(PreviewAndValidationStep.class);
    
    private final MigrationWizardModel wizardModel;
    private final ValidationService validationService;
    
    private VBox stepContent;
    private TableView<ValidationResultInfo> validationTable;
    private TreeView<FileTreeItem> previewTree;
    private TextArea fileContentArea;
    private Label overallStatusLabel;
    private ProgressBar validationProgressBar;
    private Button validateAllButton;
    private Button refreshPreviewButton;
    private CheckBox showOnlyErrorsBox;
    private TextArea validationSummaryArea;
    
    private Runnable nextCallback;
    private Runnable previousCallback;
    private Runnable finishCallback;
    
    public PreviewAndValidationStep(MigrationWizardModel wizardModel, ValidationService validationService) {
        this.wizardModel = wizardModel;
        this.validationService = validationService;
        initializeUI();
    }
    
    private void initializeUI() {
        stepContent = new VBox(20);
        stepContent.setPadding(new Insets(20));
        
        // Title and description
        Label titleLabel = new Label("Preview and Validate");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label descriptionLabel = new Label(
            "Review the generated files and validation results before executing the migration. " +
            "Resolve any validation errors to ensure successful migration.");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-text-fill: #666666;");
        
        // Create split view for preview and validation
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        splitPane.setPrefHeight(500);
        
        // Left side - File preview
        VBox previewSection = createPreviewSection();
        
        // Right side - Validation results
        VBox validationSection = createValidationSection();
        
        splitPane.getItems().addAll(previewSection, validationSection);
        splitPane.setDividerPositions(0.5);
        
        // Bottom section - Overall status and controls
        VBox statusSection = createStatusSection();
        
        stepContent.getChildren().addAll(
            titleLabel,
            descriptionLabel,
            new Separator(),
            splitPane,
            new Separator(),
            statusSection
        );
    }
    
    private VBox createPreviewSection() {
        VBox section = new VBox(10);
        section.setPrefWidth(400);
        
        Label sectionLabel = new Label("📁 Generated Files Preview");
        sectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Control buttons for preview
        HBox previewControls = new HBox(10);
        previewControls.setAlignment(Pos.CENTER_LEFT);
        
        refreshPreviewButton = new Button("🔄 Refresh Preview");
        refreshPreviewButton.getStyleClass().add("config-button");
        refreshPreviewButton.setOnAction(e -> refreshPreview());
        
        previewControls.getChildren().add(refreshPreviewButton);
        
        // File tree view
        previewTree = new TreeView<>();
        previewTree.setPrefHeight(200);
        previewTree.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                displayFileContent(newSelection.getValue());
            }
        });
        
        // File content area
        Label contentLabel = new Label("File Content:");
        contentLabel.setStyle("-fx-font-weight: bold;");
        
        fileContentArea = new TextArea();
        fileContentArea.setPrefRowCount(12);
        fileContentArea.setWrapText(false);
        fileContentArea.setEditable(false);
        fileContentArea.getStyleClass().add("wizard-preview-area");
        fileContentArea.setText("Select a file from the tree above to preview its content.");
        
        section.getChildren().addAll(sectionLabel, previewControls, previewTree, contentLabel, fileContentArea);
        return section;
    }
    
    private VBox createValidationSection() {
        VBox section = new VBox(10);
        section.setPrefWidth(400);
        
        Label sectionLabel = new Label("✅ Validation Results");
        sectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Control buttons for validation
        HBox validationControls = new HBox(10);
        validationControls.setAlignment(Pos.CENTER_LEFT);
        
        validateAllButton = new Button("🔍 Validate All");
        validateAllButton.getStyleClass().add("primary-button");
        validateAllButton.setOnAction(e -> validateAll());
        
        showOnlyErrorsBox = new CheckBox("Show only errors");
        showOnlyErrorsBox.setOnAction(e -> filterValidationResults());
        
        validationControls.getChildren().addAll(validateAllButton, showOnlyErrorsBox);
        
        // Validation results table
        validationTable = createValidationTable();
        
        // Validation summary
        Label summaryLabel = new Label("Validation Summary:");
        summaryLabel.setStyle("-fx-font-weight: bold;");
        
        validationSummaryArea = new TextArea();
        validationSummaryArea.setPrefRowCount(6);
        validationSummaryArea.setWrapText(true);
        validationSummaryArea.setEditable(false);
        validationSummaryArea.getStyleClass().add("wizard-preview-area");
        validationSummaryArea.setText("Click 'Validate All' to run validation checks on generated files.");
        
        section.getChildren().addAll(sectionLabel, validationControls, validationTable, summaryLabel, validationSummaryArea);
        return section;
    }
    
    private TableView<ValidationResultInfo> createValidationTable() {
        TableView<ValidationResultInfo> table = new TableView<>();
        table.setPrefHeight(200);
        table.getStyleClass().add("wizard-table-view");
        
        // File column
        TableColumn<ValidationResultInfo, String> fileColumn = new TableColumn<>("File");
        fileColumn.setCellValueFactory(cellData -> cellData.getValue().fileNameProperty());
        fileColumn.setPrefWidth(120);
        
        // Type column
        TableColumn<ValidationResultInfo, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cellData -> cellData.getValue().validationTypeProperty());
        typeColumn.setPrefWidth(80);
        
        // Status column
        TableColumn<ValidationResultInfo, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusColumn.setPrefWidth(80);
        statusColumn.setCellFactory(col -> new TableCell<ValidationResultInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "PASS":
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            break;
                        case "FAIL":
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            break;
                        case "WARNING":
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        // Message column
        TableColumn<ValidationResultInfo, String> messageColumn = new TableColumn<>("Message");
        messageColumn.setCellValueFactory(cellData -> cellData.getValue().messageProperty());
        messageColumn.setPrefWidth(200);
        
        table.getColumns().addAll(fileColumn, typeColumn, statusColumn, messageColumn);
        return table;
    }
    
    private VBox createStatusSection() {
        VBox section = new VBox(10);
        
        Label statusSectionLabel = new Label("📊 Overall Status");
        statusSectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        HBox statusBox = new HBox(15);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        
        validationProgressBar = new ProgressBar(0.0);
        validationProgressBar.setPrefWidth(200);
        validationProgressBar.getStyleClass().add("wizard-progress-small");
        validationProgressBar.setVisible(false);
        
        overallStatusLabel = new Label("Ready for validation");
        overallStatusLabel.getStyleClass().add("wizard-status-label");
        
        statusBox.getChildren().addAll(validationProgressBar, overallStatusLabel);
        section.getChildren().addAll(statusSectionLabel, statusBox);
        return section;
    }
    
    private void refreshPreview() {
        setOperationInProgress("Refreshing file preview...");
        
        Task<TreeItem<FileTreeItem>> previewTask = new Task<TreeItem<FileTreeItem>>() {
            @Override
            protected TreeItem<FileTreeItem> call() throws Exception {
                return buildPreviewTree();
            }
        };
        
        previewTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                previewTree.setRoot(previewTask.getValue());
                setOperationComplete("Preview refreshed successfully");
            });
        });
        
        previewTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                setOperationComplete("Failed to refresh preview");
                showError("Failed to refresh preview: " + previewTask.getException().getMessage());
            });
        });
        
        new Thread(previewTask).start();
    }
    
    private TreeItem<FileTreeItem> buildPreviewTree() {
        TreeItem<FileTreeItem> root = new TreeItem<>(new FileTreeItem("Generated Files", true));
        root.setExpanded(true);
        
        // Build tree structure with generated files
        if (!wizardModel.getPreviewFiles().isEmpty()) {
            TreeItem<FileTreeItem> repoItem = new TreeItem<>(new FileTreeItem("Repository Files", true));
            repoItem.setExpanded(true);
            
            for (GeneratedFile file : wizardModel.getPreviewFiles()) {
                FileTreeItem fileItem = new FileTreeItem(file.getFileName(), false);
                fileItem.setGeneratedFile(file);
                TreeItem<FileTreeItem> fileTreeItem = new TreeItem<>(fileItem);
                repoItem.getChildren().add(fileTreeItem);
            }
            
            root.getChildren().add(repoItem);
        }
        
        return root;
    }
    
    private void displayFileContent(FileTreeItem item) {
        if (item.isDirectory()) {
            fileContentArea.setText("Select a file to view its content.");
            return;
        }
        
        GeneratedFile file = item.getGeneratedFile();
        if (file != null) {
            String content = file.getContent();
            if (content != null && !content.trim().isEmpty()) {
                fileContentArea.setText(content);
            } else {
                fileContentArea.setText("// File content is empty or not available");
            }
        } else {
            fileContentArea.setText("// File content not available");
        }
    }
    
    private void validateAll() {
        if (wizardModel.getPreviewFiles().isEmpty()) {
            showError("No files to validate. Please go back and generate preview files first.");
            return;
        }
        
        setOperationInProgress("Validating generated files...");
        
        Task<List<ValidationResultInfo>> validationTask = new Task<List<ValidationResultInfo>>() {
            @Override
            protected List<ValidationResultInfo> call() throws Exception {
                return performValidation();
            }
        };
        
        validationTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                List<ValidationResultInfo> results = validationTask.getValue();
                updateValidationResults(results);
                setOperationComplete("Validation completed");
            });
        });
        
        validationTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                setOperationComplete("Validation failed");
                showError("Validation failed: " + validationTask.getException().getMessage());
            });
        });
        
        new Thread(validationTask).start();
    }
    
    private List<ValidationResultInfo> performValidation() {
        List<ValidationResultInfo> results = new ArrayList<>();
        
        for (GeneratedFile file : wizardModel.getPreviewFiles()) {
            try {
                ValidationResult result = validateFile(file);
                
                // Convert to display format
                ValidationResultInfo info = new ValidationResultInfo(
                    file.getFileName(),
                    determineValidationType(file.getFileName()),
                    result.isValid() ? "PASS" : "FAIL",
                    result.isValid() ? "Valid" : formatValidationMessage(result)
                );
                results.add(info);
                
                // Store in wizard model
                RepositoryInfo repo = wizardModel.getSelectedRepositories().get(0); // Simplified
                wizardModel.addValidationResult(repo, result);
                
            } catch (Exception e) {
                logger.error("Validation failed for file: " + file.getFileName(), e);
                ValidationResultInfo errorInfo = new ValidationResultInfo(
                    file.getFileName(),
                    "ERROR",
                    "FAIL",
                    "Validation error: " + e.getMessage()
                );
                results.add(errorInfo);
            }
        }
        
        return results;
    }
    
    private ValidationResult validateFile(GeneratedFile file) {
        String fileName = file.getFileName();
        String content = file.getContent();
        
        ValidationResult result = new ValidationResult();
        result.setValid(true);
        
        // Basic validation rules
        if (content == null || content.trim().isEmpty()) {
            result.setValid(false);
            ValidationIssue issue = new ValidationIssue();
            issue.setType("EMPTY_CONTENT");
            issue.setMessage("File content is empty");
            issue.setSeverity(ValidationIssue.Severity.ERROR);
            result.getIssues().add(issue);
        }
        
        // File-specific validations
        if (fileName.equals("Dockerfile")) {
            validateDockerfile(content, result);
        } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            validateYamlFile(content, result);
        } else if (fileName.endsWith(".json")) {
            validateJsonFile(content, result);
        }
        
        return result;
    }
    
    private void validateDockerfile(String content, ValidationResult result) {
        if (!content.contains("FROM ")) {
            result.setValid(false);
            ValidationIssue issue = new ValidationIssue();
            issue.setType("MISSING_FROM");
            issue.setMessage("Dockerfile must contain a FROM instruction");
            issue.setSeverity(ValidationIssue.Severity.ERROR);
            result.getIssues().add(issue);
        }
        
        if (!content.contains("WORKDIR ")) {
            ValidationWarning warning = new ValidationWarning();
            warning.setType("MISSING_WORKDIR");
            warning.setMessage("Consider adding WORKDIR instruction for better practices");
            result.getWarnings().add(warning);
        }
    }
    
    private void validateYamlFile(String content, ValidationResult result) {
        try {
            // Basic YAML structure validation
            if (!content.contains(":")) {
                result.setValid(false);
                ValidationIssue issue = new ValidationIssue();
                issue.setType("INVALID_YAML");
                issue.setMessage("YAML file appears to have invalid structure");
                issue.setSeverity(ValidationIssue.Severity.ERROR);
                result.getIssues().add(issue);
            }
            
            // Kubernetes specific validations
            if (content.contains("apiVersion:") && content.contains("kind:")) {
                if (!content.contains("metadata:")) {
                    ValidationWarning warning = new ValidationWarning();
                    warning.setType("MISSING_METADATA");
                    warning.setMessage("Kubernetes resource should have metadata section");
                    result.getWarnings().add(warning);
                }
            }
        } catch (Exception e) {
            result.setValid(false);
            ValidationIssue issue = new ValidationIssue();
            issue.setType("YAML_PARSE_ERROR");
            issue.setMessage("YAML parsing error: " + e.getMessage());
            issue.setSeverity(ValidationIssue.Severity.ERROR);
            result.getIssues().add(issue);
        }
    }
    
    private void validateJsonFile(String content, ValidationResult result) {
        try {
            // Basic JSON validation - just check if it's parseable structure
            if (!(content.trim().startsWith("{") && content.trim().endsWith("}"))) {
                result.setValid(false);
                ValidationIssue issue = new ValidationIssue();
                issue.setType("INVALID_JSON");
                issue.setMessage("JSON file appears to have invalid structure");
                issue.setSeverity(ValidationIssue.Severity.ERROR);
                result.getIssues().add(issue);
            }
        } catch (Exception e) {
            result.setValid(false);
            ValidationIssue issue = new ValidationIssue();
            issue.setType("JSON_PARSE_ERROR");
            issue.setMessage("JSON parsing error: " + e.getMessage());
            issue.setSeverity(ValidationIssue.Severity.ERROR);
            result.getIssues().add(issue);
        }
    }
    
    private String determineValidationType(String fileName) {
        if (fileName.equals("Dockerfile")) {
            return "Docker";
        } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            if (fileName.contains("deployment") || fileName.contains("service")) {
                return "Kubernetes";
            } else {
                return "YAML";
            }
        } else if (fileName.endsWith(".json")) {
            return "JSON";
        } else {
            return "General";
        }
    }
    
    private String formatValidationMessage(ValidationResult result) {
        if (!result.getIssues().isEmpty()) {
            return result.getIssues().get(0).getMessage();
        } else if (!result.getWarnings().isEmpty()) {
            return "Warning: " + result.getWarnings().get(0).getMessage();
        } else {
            return "Unknown validation failure";
        }
    }
    
    private void updateValidationResults(List<ValidationResultInfo> results) {
        validationTable.getItems().clear();
        validationTable.getItems().addAll(results);
        
        // Update summary
        long passCount = results.stream().filter(r -> "PASS".equals(r.getStatus())).count();
        long failCount = results.stream().filter(r -> "FAIL".equals(r.getStatus())).count();
        long warningCount = results.stream().filter(r -> "WARNING".equals(r.getStatus())).count();
        
        StringBuilder summary = new StringBuilder();
        summary.append("Validation Summary:\n");
        summary.append("• Total Files: ").append(results.size()).append("\n");
        summary.append("• Passed: ").append(passCount).append("\n");
        summary.append("• Failed: ").append(failCount).append("\n");
        summary.append("• Warnings: ").append(warningCount).append("\n\n");
        
        if (failCount > 0) {
            summary.append("❌ Migration cannot proceed due to validation errors.\n");
            summary.append("Please review and fix the issues listed in the table above.\n");
        } else if (warningCount > 0) {
            summary.append("⚠️ Migration can proceed but review warnings.\n");
            summary.append("Some files have warnings that should be addressed.\n");
        } else {
            summary.append("✅ All validations passed successfully!\n");
            summary.append("Migration is ready to proceed.\n");
        }
        
        validationSummaryArea.setText(summary.toString());
        
        // Apply filter if enabled
        filterValidationResults();
    }
    
    private void filterValidationResults() {
        if (showOnlyErrorsBox.isSelected()) {
            List<ValidationResultInfo> errorResults = validationTable.getItems().stream()
                .filter(r -> "FAIL".equals(r.getStatus()) || "WARNING".equals(r.getStatus()))
                .collect(Collectors.toList());
            validationTable.getItems().clear();
            validationTable.getItems().addAll(errorResults);
        } else {
            // Would need to store original results to restore full list
            // For now, just show current items
        }
    }
    
    private void setOperationInProgress(String message) {
        overallStatusLabel.setText(message);
        validationProgressBar.setVisible(true);
        validationProgressBar.setProgress(-1);
        validateAllButton.setDisable(true);
        refreshPreviewButton.setDisable(true);
    }
    
    private void setOperationComplete(String message) {
        overallStatusLabel.setText(message);
        validationProgressBar.setVisible(false);
        validateAllButton.setDisable(false);
        refreshPreviewButton.setDisable(false);
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Preview and Validation Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @Override
    public String getStepTitle() {
        return "Preview and Validation";
    }
    
    @Override
    public String getStepDescription() {
        return "Preview generated files and validate them before executing the migration.";
    }
    
    @Override
    public Node getStepContent() {
        return stepContent;
    }
    
    @Override
    public void onStepEnter() {
        refreshPreview();
        logger.info("Preview and validation step entered");
    }
    
    @Override
    public void onStepExit() {
        logger.info("Preview and validation step completed");
    }
    
    @Override
    public boolean validateStep() {
        if (wizardModel.getValidationResults().isEmpty()) {
            showError("Please run validation before proceeding to ensure all files are valid.");
            return false;
        }
        
        // Check if there are any critical validation errors
        boolean hasErrors = wizardModel.getValidationResults().values().stream()
            .anyMatch(result -> !result.isValid());
        
        if (hasErrors) {
            showError("There are validation errors that must be resolved before proceeding. " +
                     "Please review the validation results and fix the issues.");
            return false;
        }
        
        return true;
    }
    
    @Override
    public void setWizardNavigation(Runnable nextCallback, Runnable previousCallback, Runnable finishCallback) {
        this.nextCallback = nextCallback;
        this.previousCallback = previousCallback;
        this.finishCallback = finishCallback;
    }
    
    @Override
    public double getStepProgress() {
        if (wizardModel.getValidationResults().isEmpty()) {
            return 0.0;
        } else {
            boolean allValid = wizardModel.getValidationResults().values().stream()
                .allMatch(ValidationResult::isValid);
            return allValid ? 1.0 : 0.5;
        }
    }
    
    /**
     * Data class for displaying validation results in the table.
     */
    public static class ValidationResultInfo {
        private final SimpleStringProperty fileName;
        private final SimpleStringProperty validationType;
        private final SimpleStringProperty status;
        private final SimpleStringProperty message;
        
        public ValidationResultInfo(String fileName, String validationType, String status, String message) {
            this.fileName = new SimpleStringProperty(fileName);
            this.validationType = new SimpleStringProperty(validationType);
            this.status = new SimpleStringProperty(status);
            this.message = new SimpleStringProperty(message);
        }
        
        // Property getters for JavaFX binding
        public SimpleStringProperty fileNameProperty() { return fileName; }
        public SimpleStringProperty validationTypeProperty() { return validationType; }
        public SimpleStringProperty statusProperty() { return status; }
        public SimpleStringProperty messageProperty() { return message; }
        
        // Value getters
        public String getFileName() { return fileName.get(); }
        public String getValidationType() { return validationType.get(); }
        public String getStatus() { return status.get(); }
        public String getMessage() { return message.get(); }
    }
}