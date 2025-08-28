package com.example.gitmigrator.controller.wizard;

import com.example.gitmigrator.model.*;
import com.example.gitmigrator.service.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * First wizard step: Repository Selection and Discovery.
 * Users can add repositories via URL, drag-drop, or discovery from Git providers.
 */
public class RepositorySelectionStep implements WizardStep {
    
    private static final Logger logger = LoggerFactory.getLogger(RepositorySelectionStep.class);
    
    private final MigrationWizardModel wizardModel;
    private final GitApiService gitApiService;
    private final TransformationService transformationService;
    
    private VBox stepContent;
    private TableView<RepositoryInfo> repositoryTable;
    private TextField urlTextField;
    private ComboBox<GitProviderType> providerComboBox;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField tokenField;
    private Button discoverButton;
    private Button addUrlButton;
    private Label statusLabel;
    private ProgressBar progressBar;
    
    private Runnable nextCallback;
    private Runnable previousCallback;
    private Runnable finishCallback;
    
    public RepositorySelectionStep(MigrationWizardModel wizardModel, GitApiService gitApiService, 
                                   TransformationService transformationService) {
        this.wizardModel = wizardModel;
        this.gitApiService = gitApiService;
        this.transformationService = transformationService;
        initializeUI();
    }
    
    private void initializeUI() {
        stepContent = new VBox(15);
        stepContent.setPadding(new Insets(20));
        
        // Title and description
        Label titleLabel = new Label("Select Repositories for Migration");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label descriptionLabel = new Label(
            "Add repositories by entering URLs, dragging folders, or discovering from Git providers. " +
            "The application will automatically detect the framework type for each repository.");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-text-fill: #666666;");
        
        // Git Provider Connection Section
        VBox connectionSection = createConnectionSection();
        
        // Repository Input Section
        VBox inputSection = createRepositoryInputSection();
        
        // Repository Table
        VBox tableSection = createRepositoryTableSection();
        
        // Status and Progress
        HBox statusSection = createStatusSection();
        
        stepContent.getChildren().addAll(
            titleLabel,
            descriptionLabel,
            new Separator(),
            connectionSection,
            new Separator(),
            inputSection,
            new Separator(),
            tableSection,
            statusSection
        );
    }
    
    private VBox createConnectionSection() {
        VBox connectionSection = new VBox(10);
        
        Label connectionLabel = new Label("🔗 Git Provider Connection (Optional)");
        connectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        GridPane connectionGrid = new GridPane();
        connectionGrid.setHgap(10);
        connectionGrid.setVgap(10);
        
        // Provider selection
        connectionGrid.add(new Label("Provider:"), 0, 0);
        providerComboBox = new ComboBox<>();
        providerComboBox.getItems().addAll(GitProviderType.values());
        providerComboBox.setValue(GitProviderType.GITHUB);
        providerComboBox.setPrefWidth(150);
        connectionGrid.add(providerComboBox, 1, 0);
        
        // Username
        connectionGrid.add(new Label("Username:"), 0, 1);
        usernameField = new TextField();
        usernameField.setPrefWidth(200);
        connectionGrid.add(usernameField, 1, 1);
        
        // Password
        connectionGrid.add(new Label("Password:"), 0, 2);
        passwordField = new PasswordField();
        passwordField.setPrefWidth(200);
        connectionGrid.add(passwordField, 1, 2);
        
        // Token (alternative)
        connectionGrid.add(new Label("Personal Access Token:"), 2, 1);
        tokenField = new TextField();
        tokenField.setPrefWidth(250);
        tokenField.setPromptText("Alternative to username/password");
        connectionGrid.add(tokenField, 3, 1);
        
        // Discover button
        discoverButton = new Button("🔍 Discover Repositories");
        discoverButton.getStyleClass().add("primary-button");
        discoverButton.setOnAction(e -> discoverRepositories());
        connectionGrid.add(discoverButton, 2, 2);
        
        connectionSection.getChildren().addAll(connectionLabel, connectionGrid);
        return connectionSection;
    }
    
    private VBox createRepositoryInputSection() {
        VBox inputSection = new VBox(10);
        
        Label inputLabel = new Label("📂 Add Repository");
        inputLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        HBox urlInputBox = new HBox(10);
        urlInputBox.setAlignment(Pos.CENTER_LEFT);
        
        urlTextField = new TextField();
        urlTextField.setPromptText("Enter Git repository URL or drag a folder here...");
        urlTextField.setPrefWidth(400);
        HBox.setHgrow(urlTextField, Priority.ALWAYS);
        
        addUrlButton = new Button("Add Repository");
        addUrlButton.getStyleClass().add("config-button");
        addUrlButton.setOnAction(e -> addRepositoryFromUrl());
        
        urlInputBox.getChildren().addAll(new Label("Repository URL:"), urlTextField, addUrlButton);
        
        // Drag and drop support
        setupDragAndDrop();
        
        inputSection.getChildren().addAll(inputLabel, urlInputBox);
        return inputSection;
    }
    
    private VBox createRepositoryTableSection() {
        VBox tableSection = new VBox(10);
        
        Label tableLabel = new Label("📋 Available Repositories");
        tableLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Create repository table
        repositoryTable = new TableView<>();
        repositoryTable.setPrefHeight(300);
        repositoryTable.setItems(wizardModel.getAvailableRepositories());
        
        // Selection column
        TableColumn<RepositoryInfo, Boolean> selectColumn = new TableColumn<>("Select");
        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setPrefWidth(60);
        selectColumn.setEditable(true);
        
        // Name column
        TableColumn<RepositoryInfo, String> nameColumn = new TableColumn<>("Repository Name");
        nameColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
        nameColumn.setPrefWidth(200);
        
        // URL column
        TableColumn<RepositoryInfo, String> urlColumn = new TableColumn<>("Repository URL");
        urlColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCloneUrl()));
        urlColumn.setPrefWidth(300);
        
        // Framework column (will be populated after detection)
        TableColumn<RepositoryInfo, String> frameworkColumn = new TableColumn<>("Framework");
        frameworkColumn.setCellValueFactory(cellData -> {
            FrameworkType framework = cellData.getValue().getDetectedFramework();
            return new javafx.beans.property.SimpleStringProperty(
                framework != null ? framework.getDisplayName() : "Detecting...");
        });
        frameworkColumn.setPrefWidth(150);
        
        // Actions column
        TableColumn<RepositoryInfo, Void> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setCellFactory(col -> new TableCell<RepositoryInfo, Void>() {
            private final Button removeButton = new Button("Remove");
            {
                removeButton.setOnAction(e -> {
                    RepositoryInfo repo = getTableView().getItems().get(getIndex());
                    wizardModel.getSelectedRepositories().remove(repo);
                    wizardModel.getAvailableRepositories().remove(repo);
                });
                removeButton.getStyleClass().add("config-button-danger");
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeButton);
            }
        });
        actionsColumn.setPrefWidth(100);
        
        repositoryTable.getColumns().addAll(selectColumn, nameColumn, urlColumn, frameworkColumn, actionsColumn);
        repositoryTable.setEditable(true);
        
        tableSection.getChildren().addAll(tableLabel, repositoryTable);
        return tableSection;
    }
    
    private HBox createStatusSection() {
        HBox statusSection = new HBox(10);
        statusSection.setAlignment(Pos.CENTER_LEFT);
        
        statusLabel = new Label("Ready to add repositories");
        statusLabel.setStyle("-fx-text-fill: #666666;");
        
        progressBar = new ProgressBar(0);
        progressBar.setVisible(false);
        progressBar.setPrefWidth(200);
        
        statusSection.getChildren().addAll(statusLabel, progressBar);
        return statusSection;
    }
    
    private void setupDragAndDrop() {
        urlTextField.setOnDragOver(event -> {
            if (event.getGestureSource() != urlTextField && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        
        urlTextField.setOnDragDropped(event -> {
            var dragboard = event.getDragboard();
            boolean success = false;
            
            if (dragboard.hasFiles()) {
                List<File> files = dragboard.getFiles();
                for (File file : files) {
                    if (file.isDirectory()) {
                        addRepositoryFromFolder(file);
                        success = true;
                    }
                }
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
    }
    
    private void discoverRepositories() {
        // Setup credentials
        MigrationWizardModel.GitCredentials credentials = wizardModel.getGitCredentials();
        credentials.setProviderType(providerComboBox.getValue());
        credentials.setUsername(usernameField.getText());
        credentials.setPassword(passwordField.getText());
        credentials.setPersonalAccessToken(tokenField.getText());
        
        if (!credentials.hasCredentials()) {
            showError("Please provide either username/password or personal access token");
            return;
        }
        
        setOperationInProgress("Discovering repositories...");
        
        Task<List<RepositoryInfo>> discoveryTask = new Task<List<RepositoryInfo>>() {
            @Override
            protected List<RepositoryInfo> call() throws Exception {
                // Use existing GitApiService method with appropriate API URL
                String apiUrl = buildApiUrl(credentials.getProviderType());
                String token = credentials.getPersonalAccessToken() != null ? 
                    credentials.getPersonalAccessToken() : 
                    credentials.getPassword(); // Fallback to password if no token
                return gitApiService.fetchRepositories(apiUrl, token);
            }
        };
        
        discoveryTask.setOnSucceeded(e -> {
            List<RepositoryInfo> discovered = discoveryTask.getValue();
            Platform.runLater(() -> {
                // Add discovered repositories to available list
                wizardModel.getAvailableRepositories().addAll(discovered);
                
                // Start framework detection for each discovered repository
                for (RepositoryInfo repo : discovered) {
                    repo.setSelected(false); // User can select which ones to use
                    detectFrameworkForRepository(repo);
                }
                
                setOperationComplete(String.format("Discovered %d repositories", discovered.size()));
            });
        });
        
        discoveryTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                setOperationComplete("Failed to discover repositories");
                showError("Repository discovery failed: " + discoveryTask.getException().getMessage());
            });
        });
        
        new Thread(discoveryTask).start();
    }
    
    private String buildApiUrl(GitProviderType providerType) {
        switch (providerType) {
            case GITHUB:
                return "https://api.github.com/user/repos";
            case GITLAB:
                return "https://gitlab.com/api/v4/projects";
            case BITBUCKET:
                return "https://api.bitbucket.org/2.0/repositories";
            default:
                return "https://api.github.com/user/repos";
        }
    }
    
    private void addRepositoryFromUrl() {
        String url = urlTextField.getText().trim();
        if (url.isEmpty()) {
            showError("Please enter a repository URL");
            return;
        }
        
        setOperationInProgress("Adding repository...");
        
        // Create repository info from URL
        RepositoryInfo repo = createRepositoryFromUrl(url);
        
        // Add to wizard model and start framework detection
        wizardModel.getAvailableRepositories().add(repo);
        wizardModel.getSelectedRepositories().add(repo);
        repo.setSelected(true);
        
        // Clear input
        urlTextField.clear();
        
        // Start framework detection
        detectFrameworkForRepository(repo);
        
        setOperationComplete("Repository added successfully");
    }
    
    private void addRepositoryFromFolder(File folder) {
        setOperationInProgress("Adding repository from folder...");
        
        RepositoryInfo repo = new RepositoryInfo();
        repo.setName(folder.getName());
        repo.setLocalPath(folder.getAbsolutePath());
        repo.setCloneUrl("file://" + folder.getAbsolutePath());
        repo.setSelected(true);
        
        wizardModel.getAvailableRepositories().add(repo);
        wizardModel.getSelectedRepositories().add(repo);
        
        // Start framework detection
        detectFrameworkForRepository(repo);
        
        setOperationComplete("Repository added from folder");
    }
    
    private RepositoryInfo createRepositoryFromUrl(String url) {
        RepositoryInfo repo = new RepositoryInfo();
        
        // Extract repository name from URL
        String name = url.substring(url.lastIndexOf('/') + 1);
        if (name.endsWith(".git")) {
            name = name.substring(0, name.length() - 4);
        }
        
        repo.setName(name);
        repo.setCloneUrl(url);
        repo.setHtmlUrl(url);
        repo.setSelected(true);
        
        return repo;
    }
    
    private void detectFrameworkForRepository(RepositoryInfo repo) {
        CompletableFuture.supplyAsync(() -> {
            try {
                if (repo.getLocalPath() != null) {
                    // Local repository - analyze directly
                    return transformationService.analyzeRepository(repo.getLocalPath());
                } else {
                    // Remote repository - clone and analyze
                    logger.info("Starting framework detection for remote repository: {}", repo.getName());
                    
                    // Get token from wizard model credentials
                    String token = null;
                    if (wizardModel.getGitCredentials() != null) {
                        token = wizardModel.getGitCredentials().getPersonalAccessToken();
                        if (token == null || token.trim().isEmpty()) {
                            token = wizardModel.getGitCredentials().getPassword();
                        }
                    }
                    
                    // Clone and analyze repository
                    RepositoryInfo analyzed = gitApiService.cloneAndAnalyzeRepository(
                        repo.getCloneUrl(), token, transformationService);
                    
                    // Update the original repository with analysis results
                    repo.setDetectedFramework(analyzed.getDetectedFramework());
                    repo.setEstimatedComplexity(analyzed.getEstimatedComplexity());
                    repo.setLanguage(analyzed.getLanguage());
                    repo.setRepositorySize(analyzed.getRepositorySize());
                    if (analyzed.getAdditionalMetadata() != null) {
                        repo.setAdditionalMetadata(analyzed.getAdditionalMetadata());
                    }
                    
                    return repo;
                }
            } catch (Exception e) {
                logger.error("Framework detection failed for {}: {}", repo.getName(), e.getMessage());
                repo.setDetectedFramework(FrameworkType.UNKNOWN);
                repo.setEstimatedComplexity(1);
                return repo;
            }
        }).thenAccept(analyzedRepo -> {
            Platform.runLater(() -> {
                // Update table to show detected framework
                repositoryTable.refresh();
                
                // Store detection results
                MigrationWizardModel.FrameworkDetectionResult result = 
                    new MigrationWizardModel.FrameworkDetectionResult(
                        analyzedRepo.getDetectedFramework(),
                        false, // simplified
                        analyzedRepo.getEstimatedComplexity(),
                        analyzedRepo.getAdditionalMetadata()
                    );
                wizardModel.addDetectionResult(repo, result);
                
                logger.info("Framework detected for {}: {}", 
                    repo.getName(), analyzedRepo.getDetectedFramework());
            });
        });
    }
    
    private void setOperationInProgress(String message) {
        statusLabel.setText(message);
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // Indeterminate
        discoverButton.setDisable(true);
        addUrlButton.setDisable(true);
    }
    
    private void setOperationComplete(String message) {
        statusLabel.setText(message);
        progressBar.setVisible(false);
        discoverButton.setDisable(false);
        addUrlButton.setDisable(false);
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Repository Selection Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @Override
    public String getStepTitle() {
        return "Repository Selection";
    }
    
    @Override
    public String getStepDescription() {
        return "Select repositories for migration by adding URLs, dragging folders, or discovering from Git providers.";
    }
    
    @Override
    public Node getStepContent() {
        return stepContent;
    }
    
    @Override
    public void onStepEnter() {
        // Refresh table when entering step
        repositoryTable.refresh();
        setOperationComplete("Ready to add repositories");
    }
    
    @Override
    public void onStepExit() {
        // Save selected repositories
        wizardModel.getSelectedRepositories().clear();
        wizardModel.getAvailableRepositories().stream()
            .filter(RepositoryInfo::isSelected)
            .forEach(repo -> wizardModel.getSelectedRepositories().add(repo));
        
        logger.info("Repository selection completed. Selected {} repositories", 
            wizardModel.getSelectedRepositories().size());
    }
    
    @Override
    public boolean validateStep() {
        boolean hasSelectedRepositories = wizardModel.getAvailableRepositories().stream()
            .anyMatch(RepositoryInfo::isSelected);
            
        if (!hasSelectedRepositories) {
            showError("Please select at least one repository to proceed.");
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
        return wizardModel.getSelectedRepositories().isEmpty() ? 0.0 : 0.5;
    }
}