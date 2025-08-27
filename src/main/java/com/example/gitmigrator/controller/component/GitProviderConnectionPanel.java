package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.GitProviderConnection;
import com.example.gitmigrator.model.GitProviderType;
import com.example.gitmigrator.service.RepositoryDiscoveryServiceFactory;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * UI component for Git provider connection configuration.
 * Handles provider selection, URL input with validation, authentication,
 * and connection testing with progress indication.
 */
public class GitProviderConnectionPanel extends VBox {
    
    // Connection status enumeration
    public enum ConnectionStatus {
        NOT_TESTED("Not tested", "connection-not-tested"),
        TESTING("Testing connection...", "connection-testing"),
        SUCCESS("Connection successful", "connection-success"),
        FAILED("Connection failed", "connection-failed");
        
        private final String message;
        private final String styleClass;
        
        ConnectionStatus(String message, String styleClass) {
            this.message = message;
            this.styleClass = styleClass;
        }
        
        public String getMessage() { return message; }
        public String getStyleClass() { return styleClass; }
    }
    
    // UI Components
    private ComboBox<GitProviderType> providerSelector;
    private TextField urlField;
    private TextField usernameField;
    private PasswordField passwordField;
    private Button testConnectionButton;
    private Button cancelTestButton;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    
    // Properties
    private final BooleanProperty connectionTested = new SimpleBooleanProperty(false);
    private final BooleanProperty connectionValid = new SimpleBooleanProperty(false);
    private final BooleanProperty isConnectionTesting = new SimpleBooleanProperty(false);
    private final StringProperty statusMessage = new SimpleStringProperty("Not tested");
    
    // Services
    private final RepositoryDiscoveryServiceFactory serviceFactory;
    
    // URL validation patterns
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
        "^https?://(www\\.)?(github\\.com|[\\w.-]+/github)(/.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GITLAB_URL_PATTERN = Pattern.compile(
        "^https?://(www\\.)?(gitlab\\.com|[\\w.-]+)(/.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BITBUCKET_URL_PATTERN = Pattern.compile(
        "^https?://(www\\.)?(bitbucket\\.org|[\\w.-]+/bitbucket)(/.*)?$", Pattern.CASE_INSENSITIVE);
    
    public GitProviderConnectionPanel() {
        this.serviceFactory = new RepositoryDiscoveryServiceFactory();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupValidation();
    }
    
    public GitProviderConnectionPanel(RepositoryDiscoveryServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupValidation();
    }
    
    private void initializeComponents() {
        // Provider selector
        providerSelector = new ComboBox<>();
        providerSelector.getItems().addAll(GitProviderType.values());
        providerSelector.setConverter(new StringConverter<GitProviderType>() {
            @Override
            public String toString(GitProviderType provider) {
                return provider != null ? provider.getDisplayName() : "";
            }
            
            @Override
            public GitProviderType fromString(String string) {
                return GitProviderType.valueOf(string.toUpperCase());
            }
        });
        providerSelector.setPromptText("Select Git Provider");
        
        // URL field
        urlField = new TextField();
        urlField.setPromptText("e.g., https://github.com/myorg or https://gitlab.company.com/group");
        
        // Username field
        usernameField = new TextField();
        usernameField.setPromptText("Username");
        
        // Password field
        passwordField = new PasswordField();
        passwordField.setPromptText("Password or Token");
        
        // Test connection button
        testConnectionButton = new Button("Test Connection");
        testConnectionButton.setDefaultButton(true);
        
        // Cancel test button
        cancelTestButton = new Button("Cancel");
        cancelTestButton.setVisible(false);
        cancelTestButton.setOnAction(e -> cancelConnectionTest());
        
        // Progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(16, 16);
        progressIndicator.setVisible(false);
        
        // Status label
        statusLabel = new Label("Not tested");
        statusLabel.getStyleClass().add("connection-not-tested");
    }
    
    private void setupLayout() {
        setSpacing(10);
        setPadding(new Insets(10));
        
        // Provider selection
        Label providerLabel = new Label("Git Provider:");
        providerLabel.getStyleClass().add("field-label");
        getChildren().addAll(providerLabel, providerSelector);
        
        // URL input
        Label urlLabel = new Label("Repository URL:");
        urlLabel.getStyleClass().add("field-label");
        getChildren().addAll(urlLabel, urlField);
        
        // Credentials section
        Label credentialsLabel = new Label("Authentication:");
        credentialsLabel.getStyleClass().add("field-label");
        getChildren().add(credentialsLabel);
        
        GridPane credentialsGrid = new GridPane();
        credentialsGrid.setHgap(10);
        credentialsGrid.setVgap(5);
        
        credentialsGrid.add(new Label("Username:"), 0, 0);
        credentialsGrid.add(usernameField, 1, 0);
        credentialsGrid.add(new Label("Password:"), 0, 1);
        credentialsGrid.add(passwordField, 1, 1);
        
        GridPane.setHgrow(usernameField, Priority.ALWAYS);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);
        
        getChildren().add(credentialsGrid);
        
        // Test connection section
        HBox testSection = new HBox(10);
        testSection.setAlignment(Pos.CENTER_LEFT);
        testSection.getChildren().addAll(testConnectionButton, cancelTestButton, progressIndicator);
        
        getChildren().add(testSection);
        
        // Status section
        HBox statusSection = new HBox(5);
        statusSection.setAlignment(Pos.CENTER_LEFT);
        statusSection.getChildren().add(statusLabel);
        
        getChildren().add(statusSection);
        
        // Apply styles
        getStyleClass().add("git-provider-connection-panel");
    }
    
    private void setupEventHandlers() {
        // Provider selection changes
        providerSelector.setOnAction(e -> {
            GitProviderType selected = providerSelector.getValue();
            if (selected != null) {
                updateUrlFieldForProvider(selected);
                resetConnectionStatus();
            }
        });
        
        // URL field changes
        urlField.textProperty().addListener((obs, oldVal, newVal) -> {
            autoDetectProvider(newVal);
            resetConnectionStatus();
        });
        
        // Username/password changes
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> resetConnectionStatus());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> resetConnectionStatus());
        
        // Test connection button
        testConnectionButton.setOnAction(e -> testConnection());
    }
    
    private void setupValidation() {
        // Enable/disable test button based on input validation and testing state
        testConnectionButton.disableProperty().bind(
            providerSelector.valueProperty().isNull()
            .or(urlField.textProperty().isEmpty())
            .or(usernameField.textProperty().isEmpty())
            .or(passwordField.textProperty().isEmpty())
            .or(isConnectionTesting)
        );
    }
    
    private void updateUrlFieldForProvider(GitProviderType provider) {
        if (urlField.getText().isEmpty()) {
            urlField.setText(provider.getDefaultWebUrl());
        }
    }
    
    private void autoDetectProvider(String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        
        GitProviderType detectedProvider = null;
        
        if (GITHUB_URL_PATTERN.matcher(url).matches()) {
            detectedProvider = GitProviderType.GITHUB;
        } else if (GITLAB_URL_PATTERN.matcher(url).matches()) {
            detectedProvider = GitProviderType.GITLAB;
        } else if (BITBUCKET_URL_PATTERN.matcher(url).matches()) {
            detectedProvider = GitProviderType.BITBUCKET;
        }
        
        if (detectedProvider != null && !detectedProvider.equals(providerSelector.getValue())) {
            providerSelector.setValue(detectedProvider);
        }
    }
    
    private void resetConnectionStatus() {
        setConnectionStatus(ConnectionStatus.NOT_TESTED, null);
        connectionTested.set(false);
        connectionValid.set(false);
    }
    
    // Current connection test task for cancellation support
    private Task<Boolean> currentTestTask;
    
    private void testConnection() {
        GitProviderConnection connection = getConnection();
        if (connection == null || !connection.isValid()) {
            setConnectionStatus(ConnectionStatus.FAILED, "Invalid connection configuration");
            return;
        }
        
        // Cancel any existing test
        if (currentTestTask != null && !currentTestTask.isDone()) {
            currentTestTask.cancel(true);
        }
        
        setConnectionStatus(ConnectionStatus.TESTING, null);
        
        currentTestTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return serviceFactory.getService(connection.getProviderType())
                    .map(service -> {
                        try {
                            CompletableFuture<Boolean> testFuture = service.testConnection(connection);
                            
                            // Check for cancellation periodically
                            while (!testFuture.isDone() && !isCancelled()) {
                                Thread.sleep(100);
                            }
                            
                            if (isCancelled()) {
                                testFuture.cancel(true);
                                return false;
                            }
                            
                            return testFuture.get();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        } catch (Exception e) {
                            throw new RuntimeException("Connection test failed: " + e.getMessage(), e);
                        }
                    })
                    .orElse(false);
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    boolean success = getValue();
                    connectionTested.set(true);
                    connectionValid.set(success);
                    
                    if (success) {
                        setConnectionStatus(ConnectionStatus.SUCCESS, null);
                    } else {
                        setConnectionStatus(ConnectionStatus.FAILED, "Authentication failed");
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    connectionTested.set(true);
                    connectionValid.set(false);
                    
                    Throwable exception = getException();
                    String errorMessage = exception != null ? exception.getMessage() : "Unknown error";
                    setConnectionStatus(ConnectionStatus.FAILED, errorMessage);
                });
            }
            
            @Override
            protected void cancelled() {
                Platform.runLater(() -> {
                    setConnectionStatus(ConnectionStatus.NOT_TESTED, "Connection test cancelled");
                });
            }
        };
        
        Thread testThread = new Thread(currentTestTask);
        testThread.setDaemon(true);
        testThread.start();
    }
    
    public void setConnectionStatus(ConnectionStatus status, String customMessage) {
        // Remove previous style classes
        statusLabel.getStyleClass().removeIf(style -> style.startsWith("connection-"));
        
        // Set new status
        String message = customMessage != null ? customMessage : status.getMessage();
        statusLabel.setText(message);
        statusLabel.getStyleClass().add(status.getStyleClass());
        statusMessage.set(message);
        
        // Show/hide progress indicator and cancel button
        boolean isTesting = status == ConnectionStatus.TESTING;
        progressIndicator.setVisible(isTesting);
        cancelTestButton.setVisible(isTesting);
        
        // Update testing state property (this will automatically disable/enable the button via binding)
        isConnectionTesting.set(isTesting);
    }
    
    private void cancelConnectionTest() {
        if (currentTestTask != null && !currentTestTask.isDone()) {
            currentTestTask.cancel(true);
        }
    }
    
    /**
     * Gets the current connection configuration.
     * 
     * @return GitProviderConnection or null if configuration is invalid
     */
    public GitProviderConnection getConnection() {
        GitProviderType provider = providerSelector.getValue();
        String url = urlField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (provider == null || url == null || url.trim().isEmpty() ||
            username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            return null;
        }
        
        return new GitProviderConnection(provider, url.trim(), username.trim(), password);
    }
    
    /**
     * Sets the connection configuration.
     * 
     * @param connection The connection to set
     */
    public void setConnection(GitProviderConnection connection) {
        if (connection == null) {
            clearConnection();
            return;
        }
        
        providerSelector.setValue(connection.getProviderType());
        urlField.setText(connection.getBaseUrl());
        usernameField.setText(connection.getUsername());
        
        // Note: We don't set the password for security reasons
        passwordField.clear();
        
        resetConnectionStatus();
    }
    
    /**
     * Clears all connection fields.
     */
    public void clearConnection() {
        providerSelector.setValue(null);
        urlField.clear();
        usernameField.clear();
        passwordField.clear();
        resetConnectionStatus();
    }
    
    /**
     * Tests the current connection asynchronously.
     * 
     * @return CompletableFuture that resolves to true if connection is successful
     */
    public CompletableFuture<Boolean> testConnectionAsync() {
        GitProviderConnection connection = getConnection();
        if (connection == null || !connection.isValid()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return serviceFactory.getService(connection.getProviderType())
            .map(service -> service.testConnection(connection))
            .orElse(CompletableFuture.completedFuture(false));
    }
    
    // Property getters
    public BooleanProperty connectionTestedProperty() { return connectionTested; }
    public BooleanProperty connectionValidProperty() { return connectionValid; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    
    public boolean isConnectionTested() { return connectionTested.get(); }
    public boolean isConnectionValid() { return connectionValid.get(); }
    public String getStatusMessage() { return statusMessage.get(); }
}