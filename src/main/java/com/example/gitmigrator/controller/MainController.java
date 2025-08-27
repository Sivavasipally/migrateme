package com.example.gitmigrator.controller;

import com.example.gitmigrator.model.MigrationRequest;
import com.example.gitmigrator.model.MigrationResult;
import com.example.gitmigrator.model.RepositoryInfo;
import com.example.gitmigrator.service.GitApiService;
import com.example.gitmigrator.service.MigrationOrchestratorService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * JavaFX Controller for the main Git Repository Migrator interface.
 * Replaces the Spring Boot REST controller with a desktop UI.
 */
public class MainController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // Service dependencies
    private GitApiService gitApiService;
    private MigrationOrchestratorService migrationOrchestratorService;

    // FXML Controls
    @FXML private TextField apiUrlField;
    @FXML private TextField tokenField;
    @FXML private Button fetchReposButton;
    @FXML private TableView<RepositoryInfo> repositoryTable;
    @FXML private TableColumn<RepositoryInfo, Boolean> selectColumn;
    @FXML private TableColumn<RepositoryInfo, String> nameColumn;
    @FXML private TableColumn<RepositoryInfo, String> urlColumn;
    @FXML private TableColumn<RepositoryInfo, String> languageColumn;
    @FXML private Button migrateButton;
    @FXML private TextArea logArea;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Button selectAllButton;
    @FXML private Button selectNoneButton;

    private ObservableList<RepositoryInfo> repositories = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainController");
        
        setupRepositoryTable();
        setupEventHandlers();
        setupInitialState();
    }

    /**
     * Set service dependencies (called from main application).
     */
    public void setServices(GitApiService gitApiService, MigrationOrchestratorService migrationOrchestratorService) {
        this.gitApiService = gitApiService;
        this.migrationOrchestratorService = migrationOrchestratorService;
    }

    /**
     * Configure the repository table columns.
     */
    private void setupRepositoryTable() {
        // Select column with checkboxes
        selectColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        // Name column
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        // URL column  
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("cloneUrl"));

        // Language column
        languageColumn.setCellValueFactory(cellData -> {
            String language = cellData.getValue().getLanguage();
            return new SimpleStringProperty(language != null ? language : "Unknown");
        });

        // Set table data
        repositoryTable.setItems(repositories);
        repositoryTable.setEditable(true);
    }

    /**
     * Setup event handlers for UI controls.
     */
    private void setupEventHandlers() {
        fetchReposButton.setOnAction(e -> fetchRepositories());
        migrateButton.setOnAction(e -> startMigration());
        selectAllButton.setOnAction(e -> selectAllRepositories(true));
        selectNoneButton.setOnAction(e -> selectAllRepositories(false));
        
        // Enable/disable migrate button based on selection
        repositories.addListener((javafx.collections.ListChangeListener<RepositoryInfo>) change -> {
            boolean hasSelected = repositories.stream().anyMatch(RepositoryInfo::isSelected);
            migrateButton.setDisable(!hasSelected);
        });
    }

    /**
     * Setup initial UI state.
     */
    private void setupInitialState() {
        migrateButton.setDisable(true);
        progressBar.setVisible(false);
        statusLabel.setText("Ready");
        
        // Add placeholder text
        apiUrlField.setPromptText("https://api.github.com/user/repos");
        tokenField.setPromptText("Enter your authentication token");
        logArea.setPromptText("Migration logs will appear here...");
        
        // Set default API URL for GitHub user repositories
        apiUrlField.setText("https://api.github.com/user/repos");
    }

    /**
     * Fetch repositories from the specified Git API.
     */
    @FXML
    private void fetchRepositories() {
        String apiUrl = apiUrlField.getText().trim();
        String token = tokenField.getText().trim();

        if (apiUrl.isEmpty() || token.isEmpty()) {
            showAlert("Validation Error", "Please enter both API URL and authentication token.");
            return;
        }

        logger.info("Fetching repositories from: {}", apiUrl);
        
        Task<List<RepositoryInfo>> task = new Task<List<RepositoryInfo>>() {
            @Override
            protected List<RepositoryInfo> call() throws Exception {
                updateMessage("Fetching repositories...");
                return gitApiService.fetchRepositories(apiUrl, token);
            }

            @Override
            protected void succeeded() {
                List<RepositoryInfo> fetchedRepos = getValue();
                Platform.runLater(() -> {
                    repositories.clear();
                    repositories.addAll(fetchedRepos);
                    statusLabel.textProperty().unbind();
                    statusLabel.setText(String.format("Fetched %d repositories", fetchedRepos.size()));
                    appendLog(String.format("Successfully fetched %d repositories", fetchedRepos.size()));
                    progressBar.setVisible(false);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    logger.error("Failed to fetch repositories", exception);
                    showAlert("Fetch Error", "Failed to fetch repositories: " + exception.getMessage());
                    statusLabel.textProperty().unbind();
                    statusLabel.setText("Fetch failed");
                    progressBar.setVisible(false);
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
     * Start the migration process for selected repositories.
     */
    @FXML
    private void startMigration() {
        List<RepositoryInfo> selectedRepos = repositories.stream()
                .filter(RepositoryInfo::isSelected)
                .collect(Collectors.toList());

        if (selectedRepos.isEmpty()) {
            showAlert("Selection Error", "Please select at least one repository to migrate.");
            return;
        }

        logger.info("Starting migration for {} repositories", selectedRepos.size());

        Task<List<MigrationResult>> task = new Task<List<MigrationResult>>() {
            @Override
            protected List<MigrationResult> call() throws Exception {
                MigrationRequest request = new MigrationRequest();
                request.setRepositoryUrls(selectedRepos.stream()
                        .map(RepositoryInfo::getCloneUrl)
                        .collect(Collectors.toList()));
                request.setTargetFramework("kubernetes");
                request.setIncludeHelm(true);
                request.setIncludeDockerfile(true);

                int totalRepos = selectedRepos.size();
                List<MigrationResult> results = new ArrayList<>();

                for (int i = 0; i < selectedRepos.size(); i++) {
                    RepositoryInfo repo = selectedRepos.get(i);
                    updateMessage(String.format("Migrating %s (%d/%d)", repo.getName(), i + 1, totalRepos));
                    updateProgress(i, totalRepos);

                    Platform.runLater(() -> appendLog(String.format("Starting migration of: %s", repo.getName())));

                    try {
                        // Create single-repo request
                        MigrationRequest singleRequest = new MigrationRequest();
                        singleRequest.setRepositoryUrls(List.of(repo.getCloneUrl()));
                        singleRequest.setTargetFramework(request.getTargetFramework());
                        singleRequest.setIncludeHelm(request.isIncludeHelm());
                        singleRequest.setIncludeDockerfile(request.isIncludeDockerfile());

                        List<MigrationResult> singleResult = migrationOrchestratorService.migrateRepositories(singleRequest);
                        results.addAll(singleResult);

                        Platform.runLater(() -> {
                            MigrationResult result = singleResult.get(0);
                            if (result.isSuccess()) {
                                appendLog(String.format("✅ Successfully migrated: %s", repo.getName()));
                            } else {
                                appendLog(String.format("❌ Failed to migrate: %s - %s", repo.getName(), result.getErrorMessage()));
                            }
                        });

                    } catch (Exception e) {
                        Platform.runLater(() -> appendLog(String.format("❌ Error migrating %s: %s", repo.getName(), e.getMessage())));
                        logger.error("Error migrating repository: {}", repo.getName(), e);
                    }
                }

                updateProgress(totalRepos, totalRepos);
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
                    appendLog(String.format("\n🎉 Migration process completed! Success: %d/%d", successful, results.size()));
                    progressBar.setVisible(false);
                    
                    showAlert("Migration Complete", 
                            String.format("Migration completed successfully!\n\nSuccessful: %d\nFailed: %d", successful, failed));
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    logger.error("Migration process failed", exception);
                    showAlert("Migration Error", "Migration process failed: " + exception.getMessage());
                    statusLabel.textProperty().unbind();
                    statusLabel.setText("Migration failed");
                    progressBar.setVisible(false);
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
     * Select or deselect all repositories.
     */
    private void selectAllRepositories(boolean selected) {
        repositories.forEach(repo -> repo.setSelected(selected));
        repositoryTable.refresh();
    }

    /**
     * Append text to the log area.
     */
    private void appendLog(String message) {
        logArea.appendText(message + "\n");
    }

    /**
     * Show an alert dialog.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}