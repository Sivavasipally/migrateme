package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.GitServiceConfig;
import com.example.gitmigrator.model.RepositoryInfo;
import com.example.gitmigrator.service.GitServiceFactory;
import com.example.gitmigrator.service.GitServiceIntegration;
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
import java.util.function.Consumer;

/**
 * Dialog for adding repositories from Git providers (GitHub, GitLab, Bitbucket).
 * Allows users to authenticate and discover all repositories from an organization or user.
 */
public class AddRepositoryDialog {
    
    private static final Logger logger = LoggerFactory.getLogger(AddRepositoryDialog.class);
    
    private Stage dialog;
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
    
    private GitServiceFactory gitServiceFactory;
    private Consumer<List<RepositoryInfo>> onRepositoriesAdded;
    
    public AddRepositoryDialog(GitServiceFactory gitServiceFactory) {
        this.gitServiceFactory = gitServiceFactory;
        createDialog();
    }
    
    /**
     * Shows the dialog and calls the callback when repositories are added.
     */
    public void show(Consumer<List<RepositoryInfo>> onRepositoriesAdded) {
        this.onRepositoriesAdded = onRepositoriesAdded;
        dialog.show();
    }
    
    private void createDialog() {
        dialog = new Stage();
        dialog.setTitle("Add Repositories from Git Provider");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(true);
        dialog.setWidth(800);
        dialog.setHeight(600);
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        
        // Connection section
        VBox connectionSection = createConnectionSection();
        
        // Repository discovery section
        VBox discoverySection = createDiscoverySection();
        
        // Repository list section
        VBox repositorySection = createRepositorySection();
        
        // Button section
        HBox buttonSection = createButtonSection();
        
        root.getChildren().addAll(connectionSection, discoverySection, repositorySection, buttonSection);
        
        Scene scene = new Scene(root);
        dialog.setScene(scene);
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
    
    private HBox createButtonSection() {
        HBox section = new HBox(10);
        section.setAlignment(Pos.CENTER_RIGHT);
        
        addSelectedButton = new Button("Add Selected Repositories");
        addSelectedButton.setOnAction(e -> addSelectedRepositories());
        addSelectedButton.setDisable(true);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> dialog.close());
        
        section.getChildren().addAll(cancelButton, addSelectedButton);
        return section;
    }
    
    private void onProviderChanged() {
        String provider = providerComboBox.getValue();
        boolean showServerUrl = !"GitHub".equals(provider);
        
        // Find the server URL row and show/hide it
        VBox connectionSection = (VBox) ((VBox) dialog.getScene().getRoot()).getChildren().get(0);
        HBox serverRow = (HBox) connectionSection.getChildren().get(2);
        serverRow.setVisible(showServerUrl);
        serverRow.setManaged(showServerUrl);
        
        // Set default URLs
        if ("GitLab".equals(provider)) {
            serverUrlField.setText("https://gitlab.com");
        } else if ("Bitbucket".equals(provider)) {
            serverUrlField.setText("https://bitbucket.org");
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
        config.setProvider(provider.toLowerCase());
        config.setServerUrl("GitHub".equals(provider) ? "https://api.github.com" : serverUrl);
        config.setUsername(username);
        config.setToken(password); // Using token field for password
        
        // Start discovery task
        Task<List<RepositoryInfo>> task = new Task<List<RepositoryInfo>>() {
            @Override
            protected List<RepositoryInfo> call() throws Exception {
                updateMessage("Connecting to " + provider + "...");
                
                GitServiceIntegration service = gitServiceFactory.createService(config);
                
                updateMessage("Discovering repositories...");
                
                if (organization.isEmpty()) {
                    return service.getUserRepositories();
                } else {
                    return service.getOrganizationRepositories(organization);
                }
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    List<RepositoryInfo> repositories = getValue();
                    repositoryListView.getItems().setAll(repositories);
                    addSelectedButton.setDisable(repositories.isEmpty());
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
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}