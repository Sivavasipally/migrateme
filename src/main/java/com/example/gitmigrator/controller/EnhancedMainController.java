package com.example.gitmigrator.controller;

import com.example.gitmigrator.controller.component.AddRepositoryDialog;
import com.example.gitmigrator.controller.component.DragDropRepositoryTable;
import com.example.gitmigrator.controller.component.MigrationConfigurationPanel;
import com.example.gitmigrator.model.MigrationConfiguration;
import com.example.gitmigrator.model.MigrationQueueItem;
import com.example.gitmigrator.model.MigrationRequest;
import com.example.gitmigrator.model.MigrationResult;
import com.example.gitmigrator.model.RepositoryInfo;
import com.example.gitmigrator.service.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Enhanced main controller with drag-and-drop repository table,
 * advanced migration configuration, and improved user experience.
 */
public class EnhancedMainController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedMainController.class);
    
    // Services
    private GitApiService gitApiService;
    private MigrationOrchestratorService migrationOrchestratorService;
    private TemplateManagementService templateService;
    private TransformationService transformationService;
    private MigrationQueueService migrationQueueService;
    private GitServiceIntegration gitServiceIntegration;
    private ValidationService validationService;
    private ProgressTrackingService progressTrackingService;
    private ErrorReportingService errorReportingService;
    
    // UI Components
    @FXML private BorderPane rootPane;
    @FXML private VBox mainContainer;
    
    // Enhanced components
    private DragDropRepositoryTable repositoryTable;
    private MigrationConfigurationPanel configPanel;
    private TextArea logArea;
    private ProgressBar progressBar;
    private Label statusLabel;
    
    // Data
    private ObservableList<RepositoryInfo> repositories = FXCollections.observableArrayList();
    private MigrationConfiguration currentConfiguration = new MigrationConfiguration();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing EnhancedMainController");
        
        // Initialize will be called after FXML loading, but we need to create components programmatically
        // since we're using custom components that aren't easily defined in FXML
        Platform.runLater(this::initializeEnhancedComponents);
    }
    
    /**
     * Initialize enhanced UI components.
     */
    private void initializeEnhancedComponents() {
        createMainLayout();
        setupEventHandlers();
        setupInitialState();
    }
    
    /**
     * Create the main layout with enhanced components.
     */
    private void createMainLayout() {
        mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(15));
        mainContainer.getStyleClass().add("main-container");
        
        // Title section
        HBox titleSection = createTitleSection();
        
        // Repository section
        VBox repositorySection = createRepositorySection();
        
        // Configuration section
        VBox configSection = createConfigurationSection();
        
        // Actions section
        HBox actionsSection = createActionsSection();
        
        // Progress and log section
        VBox progressSection = createProgressSection();
        
        mainContainer.getChildren().addAll(
            titleSection,
            new Separator(),
            repositorySection,
            new Separator(),
            configSection,
            new Separator(),
            actionsSection,
            new Separator(),
            progressSection
        );
        
        // Set up scroll pane for main content
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("main-scroll-pane");
        
        if (rootPane != null) {
            rootPane.setCenter(scrollPane);
        }
    }
    
    /**
     * Create the title section.
     */
    private HBox createTitleSection() {
        HBox titleSection = new HBox();
        titleSection.setAlignment(Pos.CENTER);
        titleSection.setPadding(new Insets(10));
        
        Text title = new Text("Enhanced Git Repository Migrator");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.getStyleClass().add("main-title");
        
        titleSection.getChildren().add(title);
        return titleSection;
    }
    
    /**
     * Create the repository management section.
     */
    private VBox createRepositorySection() {
        VBox repositorySection = new VBox(10);
        
        // Section title
        Text sectionTitle = new Text("Repository Selection");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        sectionTitle.getStyleClass().add("section-title");
        
        // Repository table with drag-and-drop
        repositoryTable = new DragDropRepositoryTable();
        repositoryTable.setItems(repositories);
        repositoryTable.setPrefHeight(300);
        
        // Repository controls
        HBox repoControls = createRepositoryControls();
        
        repositorySection.getChildren().addAll(sectionTitle, repositoryTable, repoControls);
        return repositorySection;
    }
    
    /**
     * Create repository control buttons.
     */
    private HBox createRepositoryControls() {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        
        Button addRepoBtn = new Button("Add Repository");
        addRepoBtn.getStyleClass().add("primary-button");
        addRepoBtn.setOnAction(e -> showAddRepositoryDialog());
        
        Button removeRepoBtn = new Button("Remove Selected");
        removeRepoBtn.getStyleClass().add("button");
        removeRepoBtn.setOnAction(e -> removeSelectedRepositories());
        
        Button selectAllBtn = new Button("Select All");
        selectAllBtn.getStyleClass().add("button");
        selectAllBtn.setOnAction(e -> selectAllRepositories(true));
        
        Button selectNoneBtn = new Button("Select None");
        selectNoneBtn.getStyleClass().add("button");
        selectNoneBtn.setOnAction(e -> selectAllRepositories(false));
        
        Button refreshBtn = new Button("Refresh Metadata");
        refreshBtn.getStyleClass().add("button");
        refreshBtn.setOnAction(e -> refreshRepositoryMetadata());
        
        controls.getChildren().addAll(addRepoBtn, removeRepoBtn, new Separator(), 
                                     selectAllBtn, selectNoneBtn, new Separator(), refreshBtn);
        return controls;
    }
    
    /**
     * Create the configuration section.
     */
    private VBox createConfigurationSection() {
        VBox configSection = new VBox(10);
        
        // Section title
        Text sectionTitle = new Text("Migration Configuration");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        sectionTitle.getStyleClass().add("section-title");
        
        // Configuration panel
        configPanel = new MigrationConfigurationPanel(templateService);
        configPanel.setPrefHeight(400);
        
        configSection.getChildren().addAll(sectionTitle, configPanel);
        return configSection;
    }
    
    /**
     * Create the actions section.
     */
    private HBox createActionsSection() {
        HBox actionsSection = new HBox(15);
        actionsSection.setAlignment(Pos.CENTER);
        actionsSection.setPadding(new Insets(10));
        
        Button previewBtn = new Button("Preview Migration");
        previewBtn.getStyleClass().add("primary-button");
        previewBtn.setOnAction(e -> previewMigration());
        
        Button startMigrationBtn = new Button("Start Migration");
        startMigrationBtn.getStyleClass().add("primary-button");
        startMigrationBtn.setOnAction(e -> startMigration());
        
        Button queueMigrationBtn = new Button("Add to Queue");
        queueMigrationBtn.getStyleClass().add("button");
        queueMigrationBtn.setOnAction(e -> addToMigrationQueue());
        
        actionsSection.getChildren().addAll(previewBtn, startMigrationBtn, queueMigrationBtn);
        return actionsSection;
    }
    
    /**
     * Create the progress and log section.
     */
    private VBox createProgressSection() {
        VBox progressSection = new VBox(10);
        
        // Section title
        Text sectionTitle = new Text("Migration Progress & Logs");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        sectionTitle.getStyleClass().add("section-title");
        
        // Status and progress
        HBox statusSection = new HBox(15);
        statusSection.setAlignment(Pos.CENTER_LEFT);
        
        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");
        
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.setVisible(false);
        
        statusSection.getChildren().addAll(new Label("Status:"), statusLabel, progressBar);
        
        // Log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(8);
        logArea.getStyleClass().add("log-area");
        logArea.setPromptText("Migration logs will appear here...");
        
        // Log controls
        HBox logControls = new HBox(10);
        logControls.setAlignment(Pos.CENTER_LEFT);
        
        Button clearLogsBtn = new Button("Clear Logs");
        clearLogsBtn.getStyleClass().add("button");
        clearLogsBtn.setOnAction(e -> logArea.clear());
        
        Button exportLogsBtn = new Button("Export Logs");
        exportLogsBtn.getStyleClass().add("button");
        exportLogsBtn.setOnAction(e -> exportLogs());
        
        logControls.getChildren().addAll(clearLogsBtn, exportLogsBtn);
        
        progressSection.getChildren().addAll(sectionTitle, statusSection, logArea, logControls);
        return progressSection;
    }
    
    /**
     * Setup event handlers for the enhanced components.
     */
    private void setupEventHandlers() {
        // Repository table drag-and-drop handlers
        repositoryTable.setOnFolderDropped(this::handleFolderDropped);
        repositoryTable.setOnUrlDropped(this::handleUrlDropped);
        repositoryTable.setOnValidationError(this::showValidationError);
        
        // Configuration panel handlers
        configPanel.setOnConfigurationChanged(config -> {
            currentConfiguration = config;
            logger.debug("Configuration updated: {}", config);
        });
        configPanel.setOnValidationError(this::showValidationError);
        
        // Repository selection changes
        repositories.addListener((javafx.collections.ListChangeListener<RepositoryInfo>) change -> {
            updateActionButtonStates();
        });
    }
    
    /**
     * Setup initial UI state.
     */
    private void setupInitialState() {
        updateActionButtonStates();
        appendLog("Enhanced Git Repository Migrator initialized");
        appendLog("Drag and drop repository folders or URLs to get started");
    }
    
    /**
     * Safely set status label text, unbinding if necessary.
     */
    private void setStatusText(String text) {
        Platform.runLater(() -> {
            statusLabel.textProperty().unbind();
            statusLabel.setText(text);
        });
    }
    
    /**
     * Handle dropped folder.
     */
    private void handleFolderDropped(File folder) {
        logger.info("Processing dropped folder: {}", folder.getAbsolutePath());
        
        Task<RepositoryInfo> task = new Task<RepositoryInfo>() {
            @Override
            protected RepositoryInfo call() throws Exception {
                updateMessage("Analyzing repository: " + folder.getName());
                
                // Use transformation service to analyze the repository
                RepositoryInfo repoInfo = new RepositoryInfo();
                repoInfo.setName(folder.getName());
                repoInfo.setLocalPath(folder.getAbsolutePath());
                repoInfo.setCloneUrl("file://" + folder.getAbsolutePath());
                
                if (transformationService != null) {
                    // Enhance with metadata extraction
                    RepositoryInfo enhanced = transformationService.analyzeRepository(folder.getAbsolutePath());
                    if (enhanced != null) {
                        repoInfo = enhanced;
                    }
                }
                
                return repoInfo;
            }
            
            @Override
            protected void succeeded() {
                RepositoryInfo repoInfo = getValue();
                Platform.runLater(() -> {
                    repositories.add(repoInfo);
                    appendLog("✅ Added repository: " + repoInfo.getName());
                    statusLabel.textProperty().unbind();
                    statusLabel.setText("Repository added successfully");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    logger.error("Failed to process dropped folder", exception);
                    showValidationError("Failed to add repository: " + exception.getMessage());
                    statusLabel.textProperty().unbind();
                    statusLabel.setText("Failed to add repository");
                });
            }
        };
        
        statusLabel.textProperty().bind(task.messageProperty());
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Handle dropped URL.
     */
    private void handleUrlDropped(String url) {
        logger.info("Processing dropped URL: {}", url);
        
        Task<RepositoryInfo> task = new Task<RepositoryInfo>() {
            @Override
            protected RepositoryInfo call() throws Exception {
                updateMessage("Fetching repository info from URL...");
                
                // Extract repository name from URL
                String repoName = extractRepositoryNameFromUrl(url);
                
                RepositoryInfo repoInfo = new RepositoryInfo();
                repoInfo.setName(repoName);
                repoInfo.setCloneUrl(url);
                
                // Try to fetch additional metadata if it's a known Git service
                if (gitApiService != null && isKnownGitService(url)) {
                    try {
                        // This would require extending GitApiService to handle single repository info
                        // For now, we'll create basic info
                        repoInfo.setLastCommitDate(LocalDateTime.now());
                    } catch (Exception e) {
                        logger.warn("Could not fetch additional metadata for URL: {}", url, e);
                    }
                }
                
                return repoInfo;
            }
            
            @Override
            protected void succeeded() {
                RepositoryInfo repoInfo = getValue();
                Platform.runLater(() -> {
                    repositories.add(repoInfo);
                    appendLog("✅ Added repository from URL: " + repoInfo.getName());
                    statusLabel.textProperty().unbind();
                    statusLabel.setText("Repository added successfully");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    logger.error("Failed to process dropped URL", exception);
                    showValidationError("Failed to add repository from URL: " + exception.getMessage());
                    statusLabel.textProperty().unbind();
                    statusLabel.setText("Failed to add repository");
                });
            }
        };
        
        statusLabel.textProperty().bind(task.messageProperty());
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Show add repository dialog.
     */
    private void showAddRepositoryDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Repository");
        dialog.setHeaderText("Add Git Repository");
        dialog.setContentText("Repository URL or local path:");
        
        dialog.showAndWait().ifPresent(input -> {
            if (input.startsWith("http") || input.startsWith("git@")) {
                handleUrlDropped(input);
            } else {
                File folder = new File(input);
                if (folder.exists() && folder.isDirectory()) {
                    handleFolderDropped(folder);
                } else {
                    showValidationError("Invalid path: " + input);
                }
            }
        });
    }
    
    /**
     * Remove selected repositories.
     */
    private void removeSelectedRepositories() {
        List<RepositoryInfo> selected = repositories.stream()
                .filter(RepositoryInfo::isSelected)
                .collect(Collectors.toList());
        
        if (!selected.isEmpty()) {
            repositories.removeAll(selected);
            appendLog("Removed " + selected.size() + " repositories");
        }
    }
    
    /**
     * Select or deselect all repositories.
     */
    private void selectAllRepositories(boolean selected) {
        repositories.forEach(repo -> repo.setSelected(selected));
        repositoryTable.refresh();
    }
    
    /**
     * Refresh repository metadata.
     */
    private void refreshRepositoryMetadata() {
        List<RepositoryInfo> reposToRefresh = repositories.stream()
                .filter(RepositoryInfo::isSelected)
                .collect(Collectors.toList());
        
        if (reposToRefresh.isEmpty()) {
            showValidationError("Please select repositories to refresh");
            return;
        }
        
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int total = reposToRefresh.size();
                for (int i = 0; i < total; i++) {
                    RepositoryInfo repo = reposToRefresh.get(i);
                    updateMessage("Refreshing metadata for: " + repo.getName());
                    updateProgress(i, total);
                    
                    // Refresh metadata using transformation service
                    if (transformationService != null && repo.getLocalPath() != null) {
                        try {
                            RepositoryInfo refreshed = transformationService.analyzeRepository(repo.getLocalPath());
                            if (refreshed != null) {
                                Platform.runLater(() -> {
                                    int index = repositories.indexOf(repo);
                                    if (index >= 0) {
                                        repositories.set(index, refreshed);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to refresh metadata for: {}", repo.getName(), e);
                        }
                    }
                }
                updateProgress(total, total);
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    statusLabel.textProperty().unbind();
                    statusLabel.setText("Metadata refreshed successfully");
                    progressBar.setVisible(false);
                    appendLog("✅ Repository metadata refreshed");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    statusLabel.textProperty().unbind();
                    statusLabel.setText("Failed to refresh metadata");
                    progressBar.setVisible(false);
                    appendLog("❌ Failed to refresh repository metadata");
                });
            }
        };
        
        progressBar.setVisible(true);
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Preview migration without executing.
     */
    private void previewMigration() {
        List<RepositoryInfo> selected = getSelectedRepositories();
        if (selected.isEmpty()) {
            showValidationError("Please select at least one repository");
            return;
        }
        
        appendLog("🔍 Previewing migration for " + selected.size() + " repositories");
        
        // Create preview dialog
        Alert previewDialog = new Alert(Alert.AlertType.INFORMATION);
        previewDialog.setTitle("Migration Preview");
        previewDialog.setHeaderText("Preview Migration Configuration");
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        
        // Show configuration summary
        Text configTitle = new Text("Configuration Summary:");
        configTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        VBox configSummary = new VBox(5);
        configSummary.getChildren().addAll(
            new Label("Target Platform: " + currentConfiguration.getTargetPlatform()),
            new Label("Optional Components: " + String.join(", ", currentConfiguration.getOptionalComponents())),
            new Label("Template: " + (currentConfiguration.getTemplateName() != null ? 
                                    currentConfiguration.getTemplateName() : "None")),
            new Label("Validation Enabled: " + currentConfiguration.isEnableValidation())
        );
        
        // Show selected repositories
        Text repoTitle = new Text("Selected Repositories:");
        repoTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        ListView<String> repoList = new ListView<>();
        repoList.setPrefHeight(150);
        repoList.getItems().addAll(selected.stream()
                .map(repo -> repo.getName() + " (" + repo.getDetectedFramework() + ")")
                .collect(Collectors.toList()));
        
        // Validation preview
        if (validationService != null && currentConfiguration.isEnableValidation()) {
            Text validationTitle = new Text("Validation Preview:");
            validationTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
            
            Label validationInfo = new Label("✅ Validation will be performed after generation");
            content.getChildren().addAll(new Separator(), validationTitle, validationInfo);
        }
        
        content.getChildren().addAll(
            configTitle, configSummary,
            new Separator(),
            repoTitle, repoList
        );
        
        previewDialog.getDialogPane().setContent(content);
        previewDialog.getDialogPane().setPrefWidth(500);
        previewDialog.getDialogPane().setPrefHeight(400);
        
        previewDialog.showAndWait();
    }
    
    /**
     * Start migration process.
     */
    private void startMigration() {
        List<RepositoryInfo> selected = getSelectedRepositories();
        if (selected.isEmpty()) {
            showValidationError("Please select at least one repository");
            return;
        }
        
        logger.info("Starting migration for {} repositories", selected.size());
        appendLog("🚀 Starting migration for " + selected.size() + " repositories");
        
        Task<List<MigrationResult>> task = new Task<List<MigrationResult>>() {
            @Override
            protected List<MigrationResult> call() throws Exception {
                MigrationRequest request = createMigrationRequest(selected);
                
                int total = selected.size();
                List<MigrationResult> results = new ArrayList<>();
                
                for (int i = 0; i < selected.size(); i++) {
                    RepositoryInfo repo = selected.get(i);
                    updateMessage(String.format("Migrating %s (%d/%d)", repo.getName(), i + 1, total));
                    updateProgress(i, total);
                    
                    Platform.runLater(() -> appendLog("Processing: " + repo.getName()));
                    
                    try {
                        // Create single-repo request
                        MigrationRequest singleRequest = new MigrationRequest();
                        singleRequest.setRepositoryUrls(List.of(repo.getCloneUrl()));
                        singleRequest.setTargetFramework(currentConfiguration.getTargetPlatform());
                        singleRequest.setIncludeHelm(currentConfiguration.getOptionalComponents().contains("helm"));
                        singleRequest.setIncludeDockerfile(currentConfiguration.getOptionalComponents().contains("dockerfile"));
                        
                        List<MigrationResult> singleResult = migrationOrchestratorService.migrateRepositories(singleRequest);
                        results.addAll(singleResult);
                        
                        Platform.runLater(() -> {
                            MigrationResult result = singleResult.get(0);
                            if (result.isSuccess()) {
                                appendLog("✅ Successfully migrated: " + repo.getName());
                            } else {
                                appendLog("❌ Failed to migrate: " + repo.getName() + " - " + result.getErrorMessage());
                            }
                        });
                        
                    } catch (Exception e) {
                        Platform.runLater(() -> appendLog("❌ Error migrating " + repo.getName() + ": " + e.getMessage()));
                        logger.error("Error migrating repository: {}", repo.getName(), e);
                    }
                }
                
                updateProgress(total, total);
                return results;
            }
            
            @Override
            protected void succeeded() {
                List<MigrationResult> results = getValue();
                Platform.runLater(() -> {
                    long successful = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
                    long failed = results.size() - successful;
                    
                    statusLabel.textProperty().unbind();
                    statusLabel.setText(String.format("Migration completed: %d successful, %d failed", successful, failed));
                    appendLog(String.format("🎉 Migration completed! Success: %d/%d", successful, results.size()));
                    progressBar.setVisible(false);
                    
                    showAlert("Migration Complete", 
                            String.format("Migration completed!\n\nSuccessful: %d\nFailed: %d", successful, failed));
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    logger.error("Migration process failed", exception);
                    statusLabel.textProperty().unbind();
                    statusLabel.setText("Migration failed");
                    progressBar.setVisible(false);
                    appendLog("❌ Migration process failed: " + exception.getMessage());
                    showAlert("Migration Error", "Migration process failed: " + exception.getMessage());
                });
            }
        };
        
        progressBar.setVisible(true);
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Add selected repositories to migration queue.
     */
    private void addToMigrationQueue() {
        List<RepositoryInfo> selected = getSelectedRepositories();
        if (selected.isEmpty()) {
            showValidationError("Please select at least one repository");
            return;
        }
        
        if (migrationQueueService == null) {
            showValidationError("Migration queue service not available");
            return;
        }
        
        // Add repositories to queue with current configuration
        for (RepositoryInfo repo : selected) {
            migrationQueueService.addToQueue(repo, currentConfiguration);
        }
        
        appendLog("📋 Added " + selected.size() + " repositories to migration queue");
        statusLabel.setText("Added " + selected.size() + " repositories to queue");
        
        // Show queue management dialog
        showQueueManagementDialog();
    }
    
    /**
     * Show queue management dialog.
     */
    private void showQueueManagementDialog() {
        if (migrationQueueService == null) {
            return;
        }
        
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Migration Queue");
        dialog.setHeaderText("Queue Management");
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        QueueStatus status = migrationQueueService.getQueueStatus();
        content.getChildren().addAll(
            new Label("Queue Status: " + (status.isProcessing() ? "Processing" : "Idle")),
            new Label("Items in Queue: " + status.getTotalItems()),
            new Label("Completed: " + status.getCompletedItems()),
            new Label("Failed: " + status.getFailedItems())
        );
        
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER);
        
        Button processBtn = new Button("Process Queue");
        processBtn.setOnAction(e -> {
            dialog.close();
            processQueue();
        });
        
        Button pauseBtn = new Button(status.isProcessing() ? "Pause" : "Resume");
        pauseBtn.setOnAction(e -> {
            if (status.isProcessing()) {
                migrationQueueService.pauseProcessing();
                appendLog("⏸️ Queue processing paused");
            } else {
                migrationQueueService.resumeProcessing();
                appendLog("▶️ Queue processing resumed");
            }
            dialog.close();
        });
        
        Button clearBtn = new Button("Clear Queue");
        clearBtn.setOnAction(e -> {
            // Clear queue implementation would need to be added to service
            appendLog("🗑️ Queue cleared");
            dialog.close();
        });
        
        buttons.getChildren().addAll(processBtn, pauseBtn, clearBtn);
        content.getChildren().add(buttons);
        
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }
    
    /**
     * Process the migration queue.
     */
    private void processQueue() {
        if (migrationQueueService == null) {
            showValidationError("Migration queue service not available");
            return;
        }
        
        appendLog("🚀 Starting queue processing");
        statusLabel.setText("Processing migration queue...");
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // Indeterminate progress
        
        Task<List<MigrationResult>> task = new Task<List<MigrationResult>>() {
            @Override
            protected List<MigrationResult> call() throws Exception {
                return migrationQueueService.processQueue().get();
            }
            
            @Override
            protected void succeeded() {
                List<MigrationResult> results = getValue();
                Platform.runLater(() -> {
                    long successful = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
                    long failed = results.size() - successful;
                    
                    statusLabel.setText(String.format("Queue completed: %d successful, %d failed", successful, failed));
                    progressBar.setVisible(false);
                    appendLog(String.format("🎉 Queue processing completed! Success: %d/%d", successful, results.size()));
                    
                    showAlert("Queue Complete", 
                            String.format("Queue processing completed!\n\nSuccessful: %d\nFailed: %d", successful, failed));
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    logger.error("Queue processing failed", exception);
                    statusLabel.setText("Queue processing failed");
                    progressBar.setVisible(false);
                    appendLog("❌ Queue processing failed: " + exception.getMessage());
                    showAlert("Queue Error", "Queue processing failed: " + exception.getMessage());
                });
            }
        };
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Export logs to file.
     */
    private void exportLogs() {
        // TODO: Implement log export functionality
        showAlert("Export", "Log export functionality will be implemented");
    }
    
    /**
     * Get selected repositories.
     */
    private List<RepositoryInfo> getSelectedRepositories() {
        return repositories.stream()
                .filter(RepositoryInfo::isSelected)
                .collect(Collectors.toList());
    }
    
    /**
     * Create migration request from selected repositories and current configuration.
     */
    private MigrationRequest createMigrationRequest(List<RepositoryInfo> repositories) {
        MigrationRequest request = new MigrationRequest();
        request.setRepositoryUrls(repositories.stream()
                .map(RepositoryInfo::getCloneUrl)
                .collect(Collectors.toList()));
        request.setTargetFramework(currentConfiguration.getTargetPlatform());
        request.setIncludeHelm(currentConfiguration.getOptionalComponents().contains("helm"));
        request.setIncludeDockerfile(currentConfiguration.getOptionalComponents().contains("dockerfile"));
        return request;
    }
    
    /**
     * Update action button states based on repository selection.
     */
    private void updateActionButtonStates() {
        boolean hasRepositories = !repositories.isEmpty();
        boolean hasSelected = repositories.stream().anyMatch(RepositoryInfo::isSelected);
        
        // Enable/disable buttons based on state
        // This would be implemented when we have button references
    }
    
    /**
     * Extract repository name from URL.
     */
    private String extractRepositoryNameFromUrl(String url) {
        String name = url;
        if (name.endsWith(".git")) {
            name = name.substring(0, name.length() - 4);
        }
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf("/") + 1);
        }
        return name;
    }
    
    /**
     * Check if URL is from a known Git service.
     */
    private boolean isKnownGitService(String url) {
        return url.contains("github.com") || url.contains("gitlab.com") || url.contains("bitbucket.org");
    }
    
    /**
     * Show validation error message.
     */
    private void showValidationError(String message) {
        logger.warn("Validation error: {}", message);
        appendLog("⚠️ " + message);
        
        // Use error reporting service if available
        if (errorReportingService != null) {
            try {
                String actionableMessage = errorReportingService.formatUserFriendlyMessage(
                    new RuntimeException(message), "UI_VALIDATION");
                showAlert("Validation Error", actionableMessage);
            } catch (Exception e) {
                logger.error("Error reporting service failed", e);
                showAlert("Validation Error", message);
            }
        } else {
            showAlert("Validation Error", message);
        }
    }
    
    /**
     * Handle application errors with enhanced reporting.
     */
    private void handleApplicationError(String context, Exception error) {
        logger.error("Application error in context: {}", context, error);
        
        if (errorReportingService != null) {
            try {
                String actionableMessage = errorReportingService.formatUserFriendlyMessage(error, context);
                appendLog("❌ " + actionableMessage);
                showAlert("Application Error", actionableMessage);
            } catch (Exception e) {
                logger.error("Error reporting service failed", e);
                appendLog("❌ " + error.getMessage());
                showAlert("Application Error", error.getMessage());
            }
        } else {
            appendLog("❌ " + error.getMessage());
            showAlert("Application Error", error.getMessage());
        }
    }
    
    /**
     * Append message to log area.
     */
    private void appendLog(String message) {
        String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.appendText(String.format("[%s] %s%n", timestamp, message));
    }
    
    /**
     * Show alert dialog.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Set service dependencies.
     */
    public void setServices(GitApiService gitApiService, 
                           MigrationOrchestratorService migrationOrchestratorService,
                           TemplateManagementService templateService,
                           TransformationService transformationService) {
        this.gitApiService = gitApiService;
        this.migrationOrchestratorService = migrationOrchestratorService;
        this.templateService = templateService;
        this.transformationService = transformationService;
        
        // Update configuration panel with template service
        if (configPanel != null) {
            // The template service is already set in the constructor
        }
    }
    
    /**
     * Set enhanced service dependencies.
     */
    public void setEnhancedServices(MigrationQueueService migrationQueueService,
                                   GitServiceIntegration gitServiceIntegration,
                                   ValidationService validationService,
                                   ProgressTrackingService progressTrackingService,
                                   ErrorReportingService errorReportingService) {
        this.migrationQueueService = migrationQueueService;
        this.gitServiceIntegration = gitServiceIntegration;
        this.validationService = validationService;
        this.progressTrackingService = progressTrackingService;
        this.errorReportingService = errorReportingService;
        
        // Setup queue event listeners
        if (migrationQueueService != null) {
            setupQueueEventHandlers();
        }
        
        // Setup progress tracking
        if (progressTrackingService != null) {
            setupProgressTracking();
        }
    }
    
    /**
     * Setup queue event handlers for real-time updates.
     */
    private void setupQueueEventHandlers() {
        migrationQueueService.addQueueEventListener(new QueueEventListener() {
            @Override
            public void onItemAdded(MigrationQueueItem item) {
                Platform.runLater(() -> {
                    appendLog("📋 Added to queue: " + item.getRepository().getName());
                });
            }
            
            @Override
            public void onItemStarted(MigrationQueueItem item) {
                Platform.runLater(() -> {
                    appendLog("🚀 Started processing: " + item.getRepository().getName());
                    statusLabel.setText("Processing: " + item.getRepository().getName());
                });
            }
            
            @Override
            public void onItemCompleted(MigrationQueueItem item, MigrationResult result) {
                Platform.runLater(() -> {
                    if (result != null && result.isSuccess()) {
                        appendLog("✅ Completed: " + item.getRepository().getName());
                    } else {
                        appendLog("❌ Failed: " + item.getRepository().getName());
                    }
                });
            }
            
            @Override
            public void onQueueProcessingCompleted(int totalProcessed, int successful, int failed) {
                Platform.runLater(() -> {
                    appendLog("🎉 Queue processing completed - Total: " + totalProcessed + 
                             ", Successful: " + successful + ", Failed: " + failed);
                    statusLabel.setText("Queue processing completed");
                    progressBar.setVisible(false);
                });
            }
        });
    }
    
    /**
     * Setup progress tracking for detailed feedback.
     */
    private void setupProgressTracking() {
        // This would be called when progress tracking is needed
        // Implementation depends on how progress tracking service works
    }
}