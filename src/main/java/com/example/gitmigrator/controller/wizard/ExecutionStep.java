package com.example.gitmigrator.controller.wizard;

import com.example.gitmigrator.model.*;
import com.example.gitmigrator.service.GitOperationService;
import com.example.gitmigrator.service.TransformationService;
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

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Final wizard step: Execution and Results.
 * Handles git checkout, template application, and commit process.
 */
public class ExecutionStep implements WizardStep {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecutionStep.class);
    
    private final MigrationWizardModel wizardModel;
    private final GitOperationService gitOperationService;
    private final TransformationService transformationService;
    
    private VBox stepContent;
    private TableView<ExecutionStatus> statusTable;
    private TextArea logArea;
    private ProgressBar overallProgressBar;
    private Label overallStatusLabel;
    private Button startExecutionButton;
    private Button pauseExecutionButton;
    private Button cancelExecutionButton;
    private Button exportResultsButton;
    private CheckBox autoCommitBox;
    private TextField commitMessageField;
    private ComboBox<String> branchStrategyCombo;
    
    private boolean executionInProgress = false;
    private boolean executionCancelled = false;
    private Task<Void> currentExecutionTask;
    
    private Runnable nextCallback;
    private Runnable previousCallback;
    private Runnable finishCallback;
    
    public ExecutionStep(MigrationWizardModel wizardModel, GitOperationService gitOperationService,
                        TransformationService transformationService) {
        this.wizardModel = wizardModel;
        this.gitOperationService = gitOperationService;
        this.transformationService = transformationService;
        initializeUI();
    }
    
    private void initializeUI() {
        stepContent = new VBox(20);
        stepContent.setPadding(new Insets(20));
        
        // Title and description
        Label titleLabel = new Label("Execute Migration");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label descriptionLabel = new Label(
            "Ready to execute the migration process. The application will checkout repositories, " +
            "apply selected templates, and commit the changes. Monitor progress and review results below.");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-text-fill: #666666;");
        
        // Execution configuration section
        VBox configSection = createConfigurationSection();
        
        // Progress tracking section
        VBox progressSection = createProgressSection();
        
        // Status table section
        VBox statusSection = createStatusTableSection();
        
        // Log section
        VBox logSection = createLogSection();
        
        // Control buttons section
        HBox controlsSection = createControlsSection();
        
        stepContent.getChildren().addAll(
            titleLabel,
            descriptionLabel,
            new Separator(),
            configSection,
            new Separator(),
            progressSection,
            statusSection,
            new Separator(),
            logSection,
            controlsSection
        );
    }
    
    private VBox createConfigurationSection() {
        VBox section = new VBox(15);
        
        Label sectionLabel = new Label("⚙️ Execution Configuration");
        sectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        GridPane configGrid = new GridPane();
        configGrid.setHgap(15);
        configGrid.setVgap(10);
        
        // Branch strategy
        Label branchLabel = new Label("Branch Strategy:");
        branchStrategyCombo = new ComboBox<>();
        branchStrategyCombo.getItems().addAll(
            "Create new migration branch",
            "Use existing main/master branch",
            "Create feature branch with timestamp",
            "Ask for each repository"
        );
        branchStrategyCombo.setValue("Create new migration branch");
        branchStrategyCombo.setPrefWidth(250);
        
        // Auto-commit option
        autoCommitBox = new CheckBox("Auto-commit changes");
        autoCommitBox.setSelected(true);
        Label autoCommitDescription = new Label("Automatically commit changes after applying templates");
        autoCommitDescription.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        
        // Commit message
        Label commitLabel = new Label("Commit Message:");
        commitMessageField = new TextField();
        commitMessageField.setText(generateDefaultCommitMessage());
        commitMessageField.setPrefWidth(400);
        
        configGrid.add(branchLabel, 0, 0);
        configGrid.add(branchStrategyCombo, 1, 0);
        configGrid.add(autoCommitBox, 0, 1);
        configGrid.add(autoCommitDescription, 1, 1);
        configGrid.add(commitLabel, 0, 2);
        configGrid.add(commitMessageField, 1, 2);
        
        section.getChildren().addAll(sectionLabel, configGrid);
        return section;
    }
    
    private VBox createProgressSection() {
        VBox section = new VBox(10);
        
        Label sectionLabel = new Label("📊 Overall Progress");
        sectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        HBox progressBox = new HBox(15);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        
        overallProgressBar = new ProgressBar(0.0);
        overallProgressBar.setPrefWidth(300);
        
        overallStatusLabel = new Label("Ready to begin migration");
        overallStatusLabel.setStyle("-fx-text-fill: #666666;");
        
        progressBox.getChildren().addAll(overallProgressBar, overallStatusLabel);
        section.getChildren().addAll(sectionLabel, progressBox);
        return section;
    }
    
    private VBox createStatusTableSection() {
        VBox section = new VBox(10);
        
        Label sectionLabel = new Label("📋 Repository Status");
        sectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        statusTable = createStatusTable();
        
        section.getChildren().addAll(sectionLabel, statusTable);
        return section;
    }
    
    private TableView<ExecutionStatus> createStatusTable() {
        TableView<ExecutionStatus> table = new TableView<>();
        table.setPrefHeight(200);
        
        // Repository name column
        TableColumn<ExecutionStatus, String> nameColumn = new TableColumn<>("Repository");
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().repositoryNameProperty());
        nameColumn.setPrefWidth(150);
        
        // Current step column
        TableColumn<ExecutionStatus, String> stepColumn = new TableColumn<>("Current Step");
        stepColumn.setCellValueFactory(cellData -> cellData.getValue().currentStepProperty());
        stepColumn.setPrefWidth(200);
        
        // Status column
        TableColumn<ExecutionStatus, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusColumn.setPrefWidth(100);
        statusColumn.setCellFactory(col -> new TableCell<ExecutionStatus, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "SUCCESS":
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            break;
                        case "ERROR":
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            break;
                        case "IN_PROGRESS":
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: #666666;");
                    }
                }
            }
        });
        
        // Progress column
        TableColumn<ExecutionStatus, String> progressColumn = new TableColumn<>("Progress");
        progressColumn.setCellValueFactory(cellData -> cellData.getValue().progressProperty());
        progressColumn.setPrefWidth(100);
        
        // Message column
        TableColumn<ExecutionStatus, String> messageColumn = new TableColumn<>("Message");
        messageColumn.setCellValueFactory(cellData -> cellData.getValue().messageProperty());
        messageColumn.setPrefWidth(300);
        
        table.getColumns().addAll(nameColumn, stepColumn, statusColumn, progressColumn, messageColumn);
        return table;
    }
    
    private VBox createLogSection() {
        VBox section = new VBox(10);
        
        Label sectionLabel = new Label("📝 Execution Log");
        sectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        logArea = new TextArea();
        logArea.setPrefRowCount(8);
        logArea.setWrapText(true);
        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px;");
        logArea.setText("Migration execution log will appear here...\n");
        
        section.getChildren().addAll(sectionLabel, logArea);
        return section;
    }
    
    private HBox createControlsSection() {
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);
        
        startExecutionButton = new Button("▶️ Start Migration");
        startExecutionButton.getStyleClass().add("primary-button");
        startExecutionButton.setOnAction(e -> startExecution());
        
        pauseExecutionButton = new Button("⏸️ Pause");
        pauseExecutionButton.getStyleClass().add("config-button");
        pauseExecutionButton.setOnAction(e -> pauseExecution());
        pauseExecutionButton.setDisable(true);
        
        cancelExecutionButton = new Button("⏹️ Cancel");
        cancelExecutionButton.getStyleClass().add("config-button-danger");
        cancelExecutionButton.setOnAction(e -> cancelExecution());
        cancelExecutionButton.setDisable(true);
        
        exportResultsButton = new Button("📤 Export Results");
        exportResultsButton.getStyleClass().add("config-button");
        exportResultsButton.setOnAction(e -> exportResults());
        exportResultsButton.setDisable(true);
        
        controls.getChildren().addAll(startExecutionButton, pauseExecutionButton, 
                                     cancelExecutionButton, exportResultsButton);
        return controls;
    }
    
    private void startExecution() {
        if (executionInProgress) {
            return;
        }
        
        // Validate configuration
        if (commitMessageField.getText().trim().isEmpty()) {
            showError("Please provide a commit message.");
            return;
        }
        
        executionInProgress = true;
        executionCancelled = false;
        wizardModel.setExecutionInProgress(true);
        
        // Update UI state
        startExecutionButton.setDisable(true);
        pauseExecutionButton.setDisable(false);
        cancelExecutionButton.setDisable(false);
        
        // Initialize status table
        initializeStatusTable();
        
        // Start execution task
        currentExecutionTask = createExecutionTask();
        new Thread(currentExecutionTask).start();
        
        appendLog("🚀 Migration execution started at " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
    }
    
    private Task<Void> createExecutionTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    executeMigration();
                    return null;
                } catch (Exception e) {
                    logger.error("Migration execution failed", e);
                    throw e;
                }
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    executionCompleted(true);
                    appendLog("✅ Migration execution completed successfully!");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    executionCompleted(false);
                    appendLog("❌ Migration execution failed: " + getException().getMessage());
                });
            }
            
            @Override
            protected void cancelled() {
                Platform.runLater(() -> {
                    executionCompleted(false);
                    appendLog("⏹️ Migration execution cancelled by user");
                });
            }
        };
    }
    
    private void executeMigration() throws Exception {
        List<RepositoryInfo> repositories = new ArrayList<>(wizardModel.getSelectedRepositories());
        AtomicInteger completedCount = new AtomicInteger(0);
        String branchStrategy = branchStrategyCombo.getValue();
        String commitMessage = commitMessageField.getText().trim();
        boolean autoCommit = autoCommitBox.isSelected();
        
        for (RepositoryInfo repo : repositories) {
            if (executionCancelled) {
                break;
            }
            
            try {
                migrateRepository(repo, branchStrategy, commitMessage, autoCommit);
                completedCount.incrementAndGet();
                
                Platform.runLater(() -> {
                    double progress = (double) completedCount.get() / repositories.size();
                    overallProgressBar.setProgress(progress);
                    overallStatusLabel.setText(String.format("Completed %d of %d repositories", 
                        completedCount.get(), repositories.size()));
                });
                
            } catch (Exception e) {
                logger.error("Failed to migrate repository: " + repo.getName(), e);
                Platform.runLater(() -> {
                    updateRepositoryStatus(repo.getName(), "ERROR", "100%", 
                        "Migration failed: " + e.getMessage());
                    appendLog("❌ Failed to migrate " + repo.getName() + ": " + e.getMessage());
                });
            }
        }
    }
    
    private void migrateRepository(RepositoryInfo repo, String branchStrategy, 
                                 String commitMessage, boolean autoCommit) throws Exception {
        
        String repoName = repo.getName();
        appendLog("🔄 Starting migration for " + repoName);
        
        // Step 1: Checkout/Clone repository
        updateRepositoryStatus(repoName, "IN_PROGRESS", "10%", "Cloning repository...");
        String localPath = cloneOrCheckoutRepository(repo, branchStrategy);
        
        // Step 2: Analyze repository structure
        updateRepositoryStatus(repoName, "IN_PROGRESS", "20%", "Analyzing repository structure...");
        Map<String, Object> repoContext = analyzeRepository(localPath, repo);
        
        // Step 3: Apply templates
        updateRepositoryStatus(repoName, "IN_PROGRESS", "50%", "Applying templates...");
        List<GeneratedFile> generatedFiles = applyTemplates(localPath, repo, repoContext);
        
        // Step 4: Write generated files
        updateRepositoryStatus(repoName, "IN_PROGRESS", "70%", "Writing generated files...");
        writeGeneratedFiles(localPath, generatedFiles);
        
        // Step 5: Commit changes (if enabled)
        if (autoCommit) {
            updateRepositoryStatus(repoName, "IN_PROGRESS", "90%", "Committing changes...");
            commitChanges(localPath, commitMessage);
        }
        
        // Step 6: Complete
        updateRepositoryStatus(repoName, "SUCCESS", "100%", "Migration completed successfully");
        
        // Store results
        MigrationResult result = new MigrationResult();
        result.setRepository(repo);
        result.setStatus(MigrationStatus.COMPLETED);
        result.setLocalPath(localPath);
        result.setGeneratedFiles(generatedFiles);
        result.setCommitMessage(commitMessage);
        result.setExecutionTime(LocalDateTime.now());
        wizardModel.addMigrationResult(repo, result);
        
        appendLog("✅ Successfully migrated " + repoName);
    }
    
    private String cloneOrCheckoutRepository(RepositoryInfo repo, String branchStrategy) throws Exception {
        if (repo.getLocalPath() != null && !repo.getLocalPath().isEmpty()) {
            // Local repository - use as is for now
            return repo.getLocalPath();
        } else {
            // Remote repository - clone 
            File clonedRepo = gitOperationService.cloneRepository(repo.getCloneUrl(), null);
            return clonedRepo.getAbsolutePath();
        }
    }
    
    private String generateBranchName(String strategy) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
        String migrationType = wizardModel.getMigrationType().toString().toLowerCase().replace("_", "-");
        
        if (strategy.contains("timestamp")) {
            return "feature/migration-" + timestamp;
        } else {
            return "migration/" + migrationType + "-" + timestamp;
        }
    }
    
    private Map<String, Object> analyzeRepository(String localPath, RepositoryInfo repo) {
        Map<String, Object> context = new HashMap<>();
        context.put("repositoryName", repo.getName());
        context.put("repositoryPath", localPath);
        context.put("framework", repo.getDetectedFramework());
        context.put("migrationType", wizardModel.getMigrationType());
        context.put("sourceEnvironment", wizardModel.getSourceEnvironment());
        context.put("targetEnvironment", wizardModel.getTargetEnvironment());
        context.put("configuration", wizardModel.getMigrationConfiguration());
        return context;
    }
    
    private List<GeneratedFile> applyTemplates(String localPath, RepositoryInfo repo, 
                                             Map<String, Object> context) throws Exception {
        List<GeneratedFile> generatedFiles = new ArrayList<>();
        
        for (Map.Entry<String, String> template : wizardModel.getSelectedTemplates().entrySet()) {
            String templateType = template.getKey();
            String templateName = template.getValue();
            
            try {
                // Create a simple generated file for demo
                GeneratedFile file = new GeneratedFile();
                file.setFileName(templateName);
                file.setContent("# Generated content for " + templateName + "\n# Template type: " + templateType);
                file.setFileType(GeneratedFile.FileType.fromFileName(templateName));
                generatedFiles.add(file);
                
                appendLog("  📄 Generated " + file.getFileName() + " from " + templateName);
                
            } catch (Exception e) {
                logger.error("Failed to generate file from template: " + templateName, e);
                appendLog("  ❌ Failed to generate from template " + templateName + ": " + e.getMessage());
            }
        }
        
        return generatedFiles;
    }
    
    private void writeGeneratedFiles(String localPath, List<GeneratedFile> generatedFiles) throws Exception {
        for (GeneratedFile file : generatedFiles) {
            String filePath = localPath + "/" + file.getFileName();
            // Simple file writing for demo
            java.nio.file.Files.write(
                java.nio.file.Paths.get(filePath), 
                file.getContent().getBytes(),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
            );
            appendLog("  💾 Wrote " + file.getFileName());
        }
    }
    
    private void commitChanges(String localPath, String commitMessage) throws Exception {
        // Simplified git operations for demo
        appendLog("  📝 Would commit changes with message: " + commitMessage);
        appendLog("  📝 (Git operations simplified for demonstration)");
    }
    
    private void pauseExecution() {
        if (currentExecutionTask != null && !currentExecutionTask.isDone()) {
            // In a real implementation, you'd implement pause functionality
            appendLog("⏸️ Pause functionality - implementation depends on specific requirements");
        }
    }
    
    private void cancelExecution() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Cancel Migration");
        confirmAlert.setHeaderText("Are you sure you want to cancel the migration?");
        confirmAlert.setContentText("This will stop all ongoing operations. Partial changes may remain.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            executionCancelled = true;
            if (currentExecutionTask != null) {
                currentExecutionTask.cancel();
            }
            appendLog("🛑 Migration cancellation requested");
        }
    }
    
    private void exportResults() {
        // Implementation for exporting migration results
        appendLog("📤 Export functionality - would save results to file");
        showInfo("Export feature will be implemented to save detailed migration results.");
    }
    
    private void executionCompleted(boolean successful) {
        executionInProgress = false;
        wizardModel.setExecutionInProgress(false);
        
        startExecutionButton.setDisable(false);
        pauseExecutionButton.setDisable(true);
        cancelExecutionButton.setDisable(true);
        exportResultsButton.setDisable(false);
        
        if (successful) {
            overallStatusLabel.setText("Migration completed successfully");
            overallProgressBar.setProgress(1.0);
        } else {
            overallStatusLabel.setText("Migration completed with errors");
        }
    }
    
    private void initializeStatusTable() {
        statusTable.getItems().clear();
        for (RepositoryInfo repo : wizardModel.getSelectedRepositories()) {
            ExecutionStatus status = new ExecutionStatus(repo.getName());
            statusTable.getItems().add(status);
        }
    }
    
    private void updateRepositoryStatus(String repoName, String status, String progress, String message) {
        for (ExecutionStatus execStatus : statusTable.getItems()) {
            if (execStatus.getRepositoryName().equals(repoName)) {
                execStatus.setStatus(status);
                execStatus.setProgress(progress);
                execStatus.setMessage(message);
                
                // Update current step based on message
                if (message.contains("Cloning")) {
                    execStatus.setCurrentStep("Repository Checkout");
                } else if (message.contains("Analyzing")) {
                    execStatus.setCurrentStep("Analysis");
                } else if (message.contains("Applying")) {
                    execStatus.setCurrentStep("Template Application");
                } else if (message.contains("Writing")) {
                    execStatus.setCurrentStep("File Generation");
                } else if (message.contains("Committing")) {
                    execStatus.setCurrentStep("Git Commit");
                } else if (message.contains("completed")) {
                    execStatus.setCurrentStep("Completed");
                }
                
                Platform.runLater(() -> statusTable.refresh());
                break;
            }
        }
    }
    
    private void appendLog(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.appendText("[" + timestamp + "] " + message + "\n");
        });
    }
    
    private String generateDefaultCommitMessage() {
        String migrationType = wizardModel.getMigrationType() != null ? 
            wizardModel.getMigrationType().getDisplayName() : "Migration";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return "feat: " + migrationType + " - Generated at " + timestamp + " via Git Migrator";
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Execution Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @Override
    public String getStepTitle() {
        return "Execute Migration";
    }
    
    @Override
    public String getStepDescription() {
        return "Execute the migration process and monitor progress for all selected repositories.";
    }
    
    @Override
    public Node getStepContent() {
        return stepContent;
    }
    
    @Override
    public void onStepEnter() {
        // Prepare execution summary
        int repoCount = wizardModel.getSelectedRepositories().size();
        int templateCount = wizardModel.getSelectedTemplates().size();
        
        overallStatusLabel.setText(String.format("Ready to migrate %d repositories with %d templates", 
            repoCount, templateCount));
        
        appendLog("📋 Migration Summary:");
        appendLog("  • Repositories: " + repoCount);
        appendLog("  • Templates: " + templateCount);
        appendLog("  • Migration Type: " + wizardModel.getMigrationType().getDisplayName());
        appendLog("  • Target Environment: " + wizardModel.getTargetEnvironment());
        appendLog("");
        
        logger.info("Execution step entered - ready to migrate {} repositories", repoCount);
    }
    
    @Override
    public void onStepExit() {
        logger.info("Execution step completed");
    }
    
    @Override
    public boolean validateStep() {
        // Final step - validation happens during execution
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
        if (!executionInProgress && wizardModel.getMigrationResults().isEmpty()) {
            return 0.0;
        } else if (executionInProgress) {
            return overallProgressBar.getProgress();
        } else {
            return 1.0;
        }
    }
    
    /**
     * Data class for tracking execution status of individual repositories.
     */
    public static class ExecutionStatus {
        private final SimpleStringProperty repositoryName;
        private final SimpleStringProperty currentStep;
        private final SimpleStringProperty status;
        private final SimpleStringProperty progress;
        private final SimpleStringProperty message;
        
        public ExecutionStatus(String repositoryName) {
            this.repositoryName = new SimpleStringProperty(repositoryName);
            this.currentStep = new SimpleStringProperty("Pending");
            this.status = new SimpleStringProperty("PENDING");
            this.progress = new SimpleStringProperty("0%");
            this.message = new SimpleStringProperty("Waiting to start...");
        }
        
        // Property getters for JavaFX binding
        public SimpleStringProperty repositoryNameProperty() { return repositoryName; }
        public SimpleStringProperty currentStepProperty() { return currentStep; }
        public SimpleStringProperty statusProperty() { return status; }
        public SimpleStringProperty progressProperty() { return progress; }
        public SimpleStringProperty messageProperty() { return message; }
        
        // Value getters
        public String getRepositoryName() { return repositoryName.get(); }
        public String getCurrentStep() { return currentStep.get(); }
        public String getStatus() { return status.get(); }
        public String getProgress() { return progress.get(); }
        public String getMessage() { return message.get(); }
        
        // Setters
        public void setCurrentStep(String step) { this.currentStep.set(step); }
        public void setStatus(String status) { this.status.set(status); }
        public void setProgress(String progress) { this.progress.set(progress); }
        public void setMessage(String message) { this.message.set(message); }
    }
}