package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.*;
import com.example.gitmigrator.service.GitServiceFactory;
import com.example.gitmigrator.service.GitServiceIntegration;
import com.example.gitmigrator.service.RepositoryDiscoveryService;
import com.example.gitmigrator.service.RepositoryDiscoveryServiceFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Enhanced dialog for adding repositories with support for both single repository entry
 * and bulk discovery from Git providers (GitHub, GitLab, Bitbucket).
 * Features a tabbed interface with "Single Repository" and "Bulk Discovery" modes.
 */
public class AddRepositoryDialog {
    
    private static final Logger logger = LoggerFactory.getLogger(AddRepositoryDialog.class);
    
    // Dialog components
    private Stage dialog;
    private TabPane tabPane;
    
    // Single repository tab components (existing functionality)
    private Tab singleRepositoryTab;
    private VBox singleRepositoryPanel;
    private ComboBox<String> providerComboBox;
    private TextField serverUrlField;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField organizationField;
    private ListView<RepositoryInfo> repositoryListView;
    private Button discoverButton;
    private Button addSelectedButton;
    private ProgressIndicator progressIndicator;
    private Label statusLabel;
    
    // Bulk discovery tab components (new functionality)
    private Tab bulkDiscoveryTab;
    private VBox bulkDiscoveryPanel;
    private GitProviderConnectionPanel connectionPanel;
    private RepositoryDiscoveryPanel discoveryPanel;
    private Button discoverRepositoriesButton;
    private Button addSelectedRepositoriesButton;
    
    // Services
    private GitServiceFactory gitServiceFactory;
    private RepositoryDiscoveryServiceFactory discoveryServiceFactory;
    
    // Callback
    private Consumer<List<RepositoryInfo>> onRepositoriesAdded;
    
    public AddRepositoryDialog(GitServiceFactory gitServiceFactory) {
        this.gitServiceFactory = gitServiceFactory;
        this.discoveryServiceFactory = new RepositoryDiscoveryServiceFactory();
        createDialog();
    }
    
    public AddRepositoryDialog(GitServiceFactory gitServiceFactory, RepositoryDiscoveryServiceFactory discoveryServiceFactory) {
        this.gitServiceFactory = gitServiceFactory;
        this.discoveryServiceFactory = discoveryServiceFactory;
        createDialog();
    }
    
    /**
     * Shows the dialog and calls the callback when repositories are added.
     */
    public void show(Consumer<List<RepositoryInfo>> onRepositoriesAdded) {
        this.onRepositoriesAdded = onRepositoriesAdded;
        dialog.show();
    }
    
    /**
     * Switches to single repository mode.
     */
    public void switchToSingleMode() {
        tabPane.getSelectionModel().select(singleRepositoryTab);
    }
    
    /**
     * Switches to bulk discovery mode.
     */
    public void switchToBulkMode() {
        tabPane.getSelectionModel().select(bulkDiscoveryTab);
    }
    
    private void createDialog() {
        dialog = new Stage();
        dialog.setTitle("Add Repository");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(true);
        dialog.setWidth(900);
        dialog.setHeight(700);
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        
        // Create tabbed interface
        createTabbedInterface();
        
        // Button section (shared between tabs)
        HBox buttonSection = createSharedButtonSection();
        
        root.getChildren().addAll(tabPane, buttonSection);
        
        Scene scene = new Scene(root);
        dialog.setScene(scene);
    }
    
    private void createTabbedInterface() {
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Create single repository tab (existing functionality)
        createSingleRepositoryTab();
        
        // Create bulk discovery tab (new functionality)
        createBulkDiscoveryTab();
        
        tabPane.getTabs().addAll(singleRepositoryTab, bulkDiscoveryTab);
        
        // Set up tab change handling
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            onTabChanged(newTab);
        });
        
        // Default to single repository tab
        tabPane.getSelectionModel().select(singleRepositoryTab);
    }
    
    private void createSingleRepositoryTab() {
        singleRepositoryTab = new Tab("Single Repository");
        singleRepositoryPanel = new VBox(15);
        singleRepositoryPanel.setPadding(new Insets(15));
        
        // Connection section
        VBox connectionSection = createConnectionSection();
        
        // Repository discovery section
        VBox discoverySection = createDiscoverySection();
        
        // Repository list section
        VBox repositorySection = createRepositorySection();
        
        singleRepositoryPanel.getChildren().addAll(connectionSection, discoverySection, repositorySection);
        singleRepositoryTab.setContent(singleRepositoryPanel);
    }
    
    private void createBulkDiscoveryTab() {
        bulkDiscoveryTab = new Tab("Bulk Discovery");
        bulkDiscoveryPanel = new VBox(15);
        bulkDiscoveryPanel.setPadding(new Insets(15));
        
        // Connection panel
        connectionPanel = new GitProviderConnectionPanel(discoveryServiceFactory);
        
        // Discovery panel
        discoveryPanel = new RepositoryDiscoveryPanel(discoveryServiceFactory);
        
        // Discover repositories button
        discoverRepositoriesButton = new Button("Discover Repositories");
        discoverRepositoriesButton.setDefaultButton(true);
        discoverRepositoriesButton.setOnAction(e -> discoverRepositoriesFromProvider());
        
        // Bind button state to connection validity and loading state
        discoverRepositoriesButton.disableProperty().bind(
            connectionPanel.connectionValidProperty().not()
            .or(discoveryPanel.isLoadingProperty())
        );
        
        // Add selected repositories button
        addSelectedRepositoriesButton = new Button("Add Selected Repositories");
        addSelectedRepositoriesButton.setOnAction(e -> addSelectedRepositoriesFromBulkDiscovery());
        addSelectedRepositoriesButton.disableProperty().bind(
            discoveryPanel.selectedCountProperty().isEqualTo(0)
        );
        
        // Layout
        VBox connectionSection = new VBox(10);
        connectionSection.getChildren().addAll(
            new Label("Git Provider Connection:") {{ getStyleClass().add("section-title"); }},
            connectionPanel,
            discoverRepositoriesButton
        );
        
        VBox discoverySection = new VBox(10);
        discoverySection.getChildren().addAll(
            new Label("Repository Discovery:") {{ getStyleClass().add("section-title"); }},
            discoveryPanel
        );
        
        VBox.setVgrow(discoverySection, Priority.ALWAYS);
        
        bulkDiscoveryPanel.getChildren().addAll(connectionSection, discoverySection);
        bulkDiscoveryTab.setContent(bulkDiscoveryPanel);
    }
    
    private VBox createConnectionSection() {
        VBox section = new VBox(10);
        
        Label title = new Label("Git Provider Connection");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Provider selection
        HBox providerRow = new HBox(10);
        providerRow.setAlignment(Pos.CENTER_LEFT);
        providerRow.getChildren().addAll(
            new Label("Provider:"),
            providerComboBox = new ComboBox<>()
        );
        providerComboBox.getItems().addAll("GitHub", "GitLab", "Bitbucket");
        providerComboBox.setValue("GitHub");
        providerComboBox.setOnAction(e -> onProviderChanged());
        
        // Server URL (for GitLab/Bitbucket)
        HBox serverRow = new HBox(10);
        serverRow.setAlignment(Pos.CENTER_LEFT);
        serverUrlField = new TextField();
        serverUrlField.setPromptText("https://gitlab.com or https://bitbucket.org");
        serverUrlField.setPrefWidth(300);
        serverRow.getChildren().addAll(
            new Label("Server URL:"),
            serverUrlField
        );
        
        // Username
        HBox usernameRow = new HBox(10);
        usernameRow.setAlignment(Pos.CENTER_LEFT);
        usernameField = new TextField();
        usernameField.setPromptText("Username or email");
        usernameField.setPrefWidth(200);
        usernameRow.getChildren().addAll(
            new Label("Username:"),
            usernameField
        );
        
        // Password/Token
        HBox passwordRow = new HBox(10);
        passwordRow.setAlignment(Pos.CENTER_LEFT);
        passwordField = new PasswordField();
        passwordField.setPromptText("Password or personal access token");
        passwordField.setPrefWidth(200);
        passwordRow.getChildren().addAll(
            new Label("Password/Token:"),
            passwordField
        );
        
        section.getChildren().addAll(title, providerRow, serverRow, usernameRow, passwordRow);
        
        // Initially hide server URL for GitHub
        onProviderChanged();
        
        return section;
    }
    
    private VBox createDiscoverySection() {
        VBox section = new VBox(10);
        
        Label title = new Label("Repository Discovery");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Organization/User field
        HBox orgRow = new HBox(10);
        orgRow.setAlignment(Pos.CENTER_LEFT);
        organizationField = new TextField();
        organizationField.setPromptText("Organization or username (leave empty for all accessible repos)");
        organizationField.setPrefWidth(300);
        orgRow.getChildren().addAll(
            new Label("Organization/User:"),
            organizationField
        );
        
        // Discover button and progress
        HBox discoverRow = new HBox(10);
        discoverRow.setAlignment(Pos.CENTER_LEFT);
        discoverButton = new Button("Discover Repositories");
        discoverButton.setOnAction(e -> discoverRepositories());
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(20, 20);
        statusLabel = new Label();
        discoverRow.getChildren().addAll(discoverButton, progressIndicator, statusLabel);
        
        section.getChildren().addAll(title, orgRow, discoverRow);
        return section;
    }
    
    private VBox createRepositorySection() {
        VBox section = new VBox(10);
        
        Label title = new Label("Discovered Repositories");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        repositoryListView = new ListView<>();
        repositoryListView.setPrefHeight(200);
        repositoryListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        repositoryListView.setCellFactory(listView -> new ListCell<RepositoryInfo>() {
            @Override
            protected void updateItem(RepositoryInfo repo, boolean empty) {
                super.updateItem(repo, empty);
                if (empty || repo == null) {
                    setText(null);
                } else {
                    setText(String.format("%s (%s) - %s", 
                        repo.getName(), 
                        repo.getDetectedFramework() != null ? repo.getDetectedFramework() : "Unknown",
                        repo.getDescription() != null ? repo.getDescription() : "No description"
                    ));
                }
            }
        });
        
        // Select all button
        HBox selectRow = new HBox(10);
        Button selectAllButton = new Button("Select All");
        selectAllButton.setOnAction(e -> repositoryListView.getSelectionModel().selectAll());
        Button selectNoneButton = new Button("Select None");
        selectNoneButton.setOnAction(e -> repositoryListView.getSelectionModel().clearSelection());
        selectRow.getChildren().addAll(selectAllButton, selectNoneButton);
        
        section.getChildren().addAll(title, repositoryListView, selectRow);
        return section;
    }
    
    private HBox createSharedButtonSection() {
        HBox section = new HBox(10);
        section.setAlignment(Pos.CENTER_RIGHT);
        section.setPadding(new Insets(10, 0, 0, 0));
        
        // Add selected button (for single repository tab)
        addSelectedButton = new Button("Add Selected Repositories");
        addSelectedButton.setOnAction(e -> addSelectedRepositories());
        addSelectedButton.setDisable(true);
        
        // Cancel button
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> dialog.close());
        
        section.getChildren().addAll(cancelButton, addSelectedButton, addSelectedRepositoriesButton);
        
        // Show/hide buttons based on selected tab
        updateButtonVisibility();
        
        return section;
    }
    
    private void onTabChanged(Tab newTab) {
        updateButtonVisibility();
    }
    
    private void updateButtonVisibility() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        
        if (selectedTab == singleRepositoryTab) {
            addSelectedButton.setVisible(true);
            addSelectedButton.setManaged(true);
            addSelectedRepositoriesButton.setVisible(false);
            addSelectedRepositoriesButton.setManaged(false);
        } else if (selectedTab == bulkDiscoveryTab) {
            addSelectedButton.setVisible(false);
            addSelectedButton.setManaged(false);
            addSelectedRepositoriesButton.setVisible(true);
            addSelectedRepositoriesButton.setManaged(true);
        }
    }
    
    private void onProviderChanged() {
        String provider = providerComboBox.getValue();
        boolean showServerUrl = !"GitHub".equals(provider);
        
        // Only proceed if the UI structure is fully initialized
        if (singleRepositoryPanel != null && !singleRepositoryPanel.getChildren().isEmpty()) {
            try {
                // Find the server URL row and show/hide it
                // Navigate through the tab structure to find the connection section
                VBox connectionSection = (VBox) singleRepositoryPanel.getChildren().get(0);
                if (connectionSection.getChildren().size() > 2) {
                    HBox serverRow = (HBox) connectionSection.getChildren().get(2);
                    serverRow.setVisible(showServerUrl);
                    serverRow.setManaged(showServerUrl);
                }
            } catch (IndexOutOfBoundsException | ClassCastException e) {
                // Log the error but don't crash the application
                System.err.println("Warning: UI structure not yet initialized for provider change: " + e.getMessage());
            }
        }
        
        // Set default URLs (this can be done regardless of UI state)
        if (serverUrlField != null) {
            if ("GitLab".equals(provider)) {
                serverUrlField.setText("https://gitlab.com");
            } else if ("Bitbucket".equals(provider)) {
                serverUrlField.setText("https://bitbucket.org");
            }
        }
    }
    
    private void discoverRepositories() {
        String provider = providerComboBox.getValue();
        String serverUrl = serverUrlField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String organization = organizationField.getText().trim();
        
        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password/token");
            return;
        }
        
        if (!"GitHub".equals(provider) && serverUrl.isEmpty()) {
            showError("Please enter server URL for " + provider);
            return;
        }
        
        // Create configuration
        GitServiceConfig config = new GitServiceConfig();
        config.setServiceName(provider.toLowerCase());
        config.setApiUrl("GitHub".equals(provider) ? "https://api.github.com" : serverUrl);
        config.setUsername(username);
        config.setToken(password); // Using token field for password
        
        // Start discovery task
        Task<List<RepositoryInfo>> task = new Task<List<RepositoryInfo>>() {
            @Override
            protected List<RepositoryInfo> call() throws Exception {
                updateMessage("Connecting to " + provider + "...");
                
                GitServiceIntegration service = gitServiceFactory.createService(config);
                
                updateMessage("Discovering repositories...");
                
                config.setOrganization(organization.isEmpty() ? null : organization);
                return service.fetchRepositories(config).get();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    List<RepositoryInfo> repositories = getValue();
                    repositoryListView.getItems().setAll(repositories);
                    addSelectedButton.setDisable(repositories.isEmpty());
                    // Unbind from task message property before setting text directly
                    statusLabel.textProperty().unbind();
                    statusLabel.setText(String.format("Found %d repositories", repositories.size()));
                    progressIndicator.setVisible(false);
                    discoverButton.setDisable(false);
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    logger.error("Failed to discover repositories", exception);
                    // Unbind from task message property
                    statusLabel.textProperty().unbind();
                    statusLabel.setText("Discovery failed");
                    showError("Failed to discover repositories: " + exception.getMessage());
                    progressIndicator.setVisible(false);
                    discoverButton.setDisable(false);
                });
            }
        };
        
        // Bind UI to task
        statusLabel.textProperty().bind(task.messageProperty());
        progressIndicator.setVisible(true);
        discoverButton.setDisable(true);
        
        // Run task
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void addSelectedRepositories() {
        List<RepositoryInfo> selectedRepos = repositoryListView.getSelectionModel().getSelectedItems();
        if (selectedRepos.isEmpty()) {
            showError("Please select at least one repository");
            return;
        }
        
        if (onRepositoriesAdded != null) {
            onRepositoriesAdded.accept(selectedRepos);
        }
        
        dialog.close();
    }
    
    private void discoverRepositoriesFromProvider() {
        GitProviderConnection connection = connectionPanel.getConnection();
        if (connection == null || !connection.isValid()) {
            showError("Please configure a valid Git provider connection");
            return;
        }
        
        // Use the enhanced discovery panel integration
        discoveryPanel.discoverRepositories(connection);
    }
    
    private void addSelectedRepositoriesFromBulkDiscovery() {
        List<RepositoryMetadata> selectedMetadata = discoveryPanel.getSelectedRepositories();
        if (selectedMetadata.isEmpty()) {
            showError("Please select at least one repository");
            return;
        }
        
        // Convert RepositoryMetadata to RepositoryInfo
        List<RepositoryInfo> repositories = selectedMetadata.stream()
            .map(metadata -> metadata.toRepositoryInfo())
            .collect(Collectors.toList());
        
        if (onRepositoriesAdded != null) {
            onRepositoriesAdded.accept(repositories);
        }
        
        dialog.close();
    }
    
    private RepositoryInfo convertToRepositoryInfo(RepositoryMetadata metadata) {
        RepositoryInfo info = new RepositoryInfo();
        info.setName(metadata.getName());
        info.setUrl(metadata.getCloneUrl());
        info.setDescription(metadata.getDescription());
        
        // Set additional fields
        info.setFullName(metadata.getFullName());
        info.setDefaultBranch(metadata.getDefaultBranch());
        info.setLanguage(metadata.getLanguage());
        info.setRepositorySize(metadata.getSize());
        info.setLastCommitDate(metadata.getMostRecentActivity());
        
        // Try to detect framework from language
        if (metadata.getLanguage() != null) {
            FrameworkType framework = detectFrameworkFromLanguage(metadata.getLanguage());
            if (framework != null) {
                info.setDetectedFramework(framework);
            }
        }
        
        return info;
    }
    
    private FrameworkType detectFrameworkFromLanguage(String language) {
        if (language == null) return FrameworkType.UNKNOWN;
        
        switch (language.toLowerCase()) {
            case "java":
                return FrameworkType.SPRING_BOOT; // Default assumption for Java
            case "javascript":
            case "typescript":
                return FrameworkType.NODE_JS;
            default:
                return FrameworkType.UNKNOWN;
        }
    }
    
    /**
     * Tests the connection for the bulk discovery tab.
     * 
     * @return CompletableFuture that resolves when connection test is complete
     */
    public CompletableFuture<Void> testConnection(GitProviderConnection connection) {
        return connectionPanel.testConnectionAsync()
            .thenApply(result -> null); // Convert to Void
    }
    
    /**
     * Discovers repositories using the bulk discovery functionality.
     * 
     * @return CompletableFuture that resolves to the list of discovered repositories
     */
    public CompletableFuture<List<RepositoryMetadata>> discoverRepositoriesAsync() {
        GitProviderConnection connection = connectionPanel.getConnection();
        if (connection == null || !connection.isValid()) {
            return CompletableFuture.completedFuture(List.of());
        }
        
        return discoveryServiceFactory.getService(connection.getProviderType())
            .map(service -> service.discoverAllRepositories(connection))
            .orElse(CompletableFuture.completedFuture(List.of()));
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}