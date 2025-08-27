package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.GitProviderConnection;
import com.example.gitmigrator.model.RepositoryFilter;
import com.example.gitmigrator.model.RepositoryMetadata;
import com.example.gitmigrator.service.RepositoryDiscoveryService;
import com.example.gitmigrator.service.RepositoryDiscoveryServiceFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UI component for repository discovery and selection.
 * Displays discovered repositories with search, filtering, and bulk selection capabilities.
 */
public class RepositoryDiscoveryPanel extends VBox {
    
    // UI Components
    private TextField searchField;
    private ComboBox<String> languageFilter;
    private ComboBox<String> visibilityFilter;
    private DatePicker updatedAfterPicker;
    private Button clearFiltersButton;
    private TableView<RepositoryMetadata> repositoryTable;
    private Label selectionSummary;
    private Button selectAllButton;
    private Button selectNoneButton;
    private Button cancelDiscoveryButton;
    private ProgressIndicator loadingIndicator;
    private Label statusLabel;
    
    // Data
    private final ObservableList<RepositoryMetadata> allRepositories = FXCollections.observableArrayList();
    private final FilteredList<RepositoryMetadata> filteredRepositories = new FilteredList<>(allRepositories);
    private final Map<RepositoryMetadata, BooleanProperty> selectionMap = new HashMap<>();
    
    // Properties
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);
    private final StringProperty statusMessage = new SimpleStringProperty("No repositories loaded");
    private final IntegerProperty selectedCount = new SimpleIntegerProperty(0);
    private final IntegerProperty totalCount = new SimpleIntegerProperty(0);
    private final IntegerProperty filteredCount = new SimpleIntegerProperty(0);
    
    // Filter
    private final RepositoryFilter currentFilter = new RepositoryFilter();
    
    // Services and tasks
    private RepositoryDiscoveryServiceFactory serviceFactory;
    private Task<List<RepositoryMetadata>> currentDiscoveryTask;
    
    public RepositoryDiscoveryPanel() {
        this.serviceFactory = new RepositoryDiscoveryServiceFactory();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupTableColumns();
        setupFiltering();
        updateSelectionSummary();
    }
    
    public RepositoryDiscoveryPanel(RepositoryDiscoveryServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupTableColumns();
        setupFiltering();
        updateSelectionSummary();
    }
    
    private void initializeComponents() {
        // Search field
        searchField = new TextField();
        searchField.setPromptText("Search repositories by name or description...");
        searchField.setPrefWidth(300);
        
        // Language filter
        languageFilter = new ComboBox<>();
        languageFilter.setPromptText("All Languages");
        languageFilter.setPrefWidth(150);
        
        // Visibility filter
        visibilityFilter = new ComboBox<>();
        visibilityFilter.getItems().addAll("All", "Public", "Private", "Non-Forks", "Non-Archived");
        visibilityFilter.setValue("All");
        visibilityFilter.setPrefWidth(120);
        
        // Date filter
        updatedAfterPicker = new DatePicker();
        updatedAfterPicker.setPromptText("Updated after...");
        updatedAfterPicker.setPrefWidth(150);
        
        // Clear filters button
        clearFiltersButton = new Button("Clear Filters");
        clearFiltersButton.getStyleClass().add("button");
        
        // Repository table
        repositoryTable = new TableView<>();
        repositoryTable.setRowFactory(tv -> {
            TableRow<RepositoryMetadata> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (oldItem != null) {
                    row.getStyleClass().removeAll("repository-row-selected", "repository-row-private", "repository-row-fork");
                }
                if (newItem != null) {
                    BooleanProperty selected = selectionMap.get(newItem);
                    if (selected != null && selected.get()) {
                        row.getStyleClass().add("repository-row-selected");
                    }
                    if (newItem.isPrivate()) {
                        row.getStyleClass().add("repository-row-private");
                    }
                    if (newItem.isFork()) {
                        row.getStyleClass().add("repository-row-fork");
                    }
                }
            });
            return row;
        });
        
        // Selection controls
        selectAllButton = new Button("Select All");
        selectAllButton.getStyleClass().add("button");
        
        selectNoneButton = new Button("Select None");
        selectNoneButton.getStyleClass().add("button");
        
        // Cancel discovery button
        cancelDiscoveryButton = new Button("Cancel Discovery");
        cancelDiscoveryButton.getStyleClass().add("button");
        cancelDiscoveryButton.setVisible(false);
        cancelDiscoveryButton.setOnAction(e -> cancelDiscovery());
        
        // Selection summary
        selectionSummary = new Label();
        selectionSummary.getStyleClass().add("selection-summary");
        
        // Loading indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(24, 24);
        loadingIndicator.setVisible(false);
        
        // Status label
        statusLabel = new Label("No repositories loaded");
        statusLabel.getStyleClass().add("status-label");
    }
    
    private void setupLayout() {
        setSpacing(10);
        setPadding(new Insets(10));
        getStyleClass().add("repository-discovery-panel");
        
        // Search and filter section
        VBox filterSection = new VBox(8);
        filterSection.getStyleClass().add("filter-section");
        
        Label filterLabel = new Label("Search and Filter:");
        filterLabel.getStyleClass().add("section-label");
        
        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.getChildren().addAll(
            new Label("Search:"), searchField,
            new Label("Language:"), languageFilter,
            new Label("Type:"), visibilityFilter
        );
        
        HBox dateRow = new HBox(10);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        dateRow.getChildren().addAll(
            new Label("Updated after:"), updatedAfterPicker,
            clearFiltersButton
        );
        
        filterSection.getChildren().addAll(filterLabel, searchRow, dateRow);
        
        // Selection controls section
        HBox selectionControls = new HBox(10);
        selectionControls.setAlignment(Pos.CENTER_LEFT);
        selectionControls.getChildren().addAll(
            selectAllButton, selectNoneButton, 
            new Separator(), selectionSummary
        );
        
        // Status section
        HBox statusSection = new HBox(10);
        statusSection.setAlignment(Pos.CENTER_LEFT);
        statusSection.getChildren().addAll(loadingIndicator, statusLabel, cancelDiscoveryButton);
        
        // Repository table
        VBox.setVgrow(repositoryTable, Priority.ALWAYS);
        
        getChildren().addAll(filterSection, selectionControls, statusSection, repositoryTable);
    }
    
    private void setupEventHandlers() {
        // Search field real-time filtering
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            currentFilter.setSearchQuery(newVal);
            applyFilters();
        });
        
        // Language filter
        languageFilter.setOnAction(e -> {
            String selected = languageFilter.getValue();
            if (selected == null || selected.equals("All Languages")) {
                currentFilter.setLanguages(null);
            } else {
                currentFilter.setLanguages(Set.of(selected));
            }
            applyFilters();
        });
        
        // Visibility filter
        visibilityFilter.setOnAction(e -> {
            String selected = visibilityFilter.getValue();
            switch (selected) {
                case "Public":
                    currentFilter.setIncludePrivate(false);
                    currentFilter.setIncludeForks(null);
                    currentFilter.setIncludeArchived(null);
                    break;
                case "Private":
                    currentFilter.setIncludePrivate(true);
                    currentFilter.setIncludeForks(null);
                    currentFilter.setIncludeArchived(null);
                    break;
                case "Non-Forks":
                    currentFilter.setIncludePrivate(null);
                    currentFilter.setIncludeForks(false);
                    currentFilter.setIncludeArchived(null);
                    break;
                case "Non-Archived":
                    currentFilter.setIncludePrivate(null);
                    currentFilter.setIncludeForks(null);
                    currentFilter.setIncludeArchived(false);
                    break;
                default: // "All"
                    currentFilter.setIncludePrivate(null);
                    currentFilter.setIncludeForks(null);
                    currentFilter.setIncludeArchived(null);
                    break;
            }
            applyFilters();
        });
        
        // Date filter
        updatedAfterPicker.setOnAction(e -> {
            if (updatedAfterPicker.getValue() != null) {
                currentFilter.setUpdatedAfter(updatedAfterPicker.getValue().atStartOfDay());
            } else {
                currentFilter.setUpdatedAfter(null);
            }
            applyFilters();
        });
        
        // Clear filters
        clearFiltersButton.setOnAction(e -> clearFilters());
        
        // Selection controls
        selectAllButton.setOnAction(e -> selectAll());
        selectNoneButton.setOnAction(e -> selectNone());
        
        // Disable selection controls when loading
        selectAllButton.disableProperty().bind(isLoading);
        selectNoneButton.disableProperty().bind(isLoading);
        clearFiltersButton.disableProperty().bind(isLoading);
    }
    
    private void setupTableColumns() {
        // Selection column
        TableColumn<RepositoryMetadata, Boolean> selectColumn = new TableColumn<>("");
        selectColumn.setPrefWidth(40);
        selectColumn.setMinWidth(40);
        selectColumn.setMaxWidth(40);
        selectColumn.setResizable(false);
        selectColumn.setSortable(false);
        selectColumn.setCellValueFactory(param -> {
            RepositoryMetadata repo = param.getValue();
            return selectionMap.computeIfAbsent(repo, k -> {
                BooleanProperty selected = new SimpleBooleanProperty(false);
                selected.addListener((obs, oldVal, newVal) -> {
                    Platform.runLater(this::updateSelectionSummary);
                    // Update row styling
                    repositoryTable.refresh();
                });
                return selected;
            });
        });
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        
        // Name column
        TableColumn<RepositoryMetadata, String> nameColumn = new TableColumn<>("Repository");
        nameColumn.setPrefWidth(200);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setCellFactory(column -> new TableCell<RepositoryMetadata, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    RepositoryMetadata repo = getTableView().getItems().get(getIndex());
                    VBox content = new VBox(2);
                    
                    Label nameLabel = new Label(item);
                    nameLabel.getStyleClass().add("repository-name");
                    
                    if (repo.getDescription() != null && !repo.getDescription().trim().isEmpty()) {
                        Label descLabel = new Label(repo.getDescription());
                        descLabel.getStyleClass().add("repository-description");
                        descLabel.setWrapText(true);
                        content.getChildren().addAll(nameLabel, descLabel);
                    } else {
                        content.getChildren().add(nameLabel);
                    }
                    
                    setGraphic(content);
                    setText(null);
                }
            }
        });
        
        // Language column
        TableColumn<RepositoryMetadata, String> languageColumn = new TableColumn<>("Language");
        languageColumn.setPrefWidth(100);
        languageColumn.setCellValueFactory(new PropertyValueFactory<>("language"));
        languageColumn.setCellFactory(column -> new TableCell<RepositoryMetadata, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.trim().isEmpty()) {
                    setText("—");
                    getStyleClass().removeAll("language-label");
                } else {
                    setText(item);
                    getStyleClass().add("language-label");
                }
            }
        });
        
        // Visibility column
        TableColumn<RepositoryMetadata, String> visibilityColumn = new TableColumn<>("Visibility");
        visibilityColumn.setPrefWidth(80);
        visibilityColumn.setCellValueFactory(param -> {
            RepositoryMetadata repo = param.getValue();
            String visibility = repo.isPrivate() ? "Private" : "Public";
            if (repo.isFork()) visibility += " (Fork)";
            if (repo.isArchived()) visibility += " (Archived)";
            return new SimpleStringProperty(visibility);
        });
        visibilityColumn.setCellFactory(column -> new TableCell<RepositoryMetadata, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().removeAll("visibility-private", "visibility-public", "visibility-fork");
                } else {
                    setText(item);
                    getStyleClass().removeAll("visibility-private", "visibility-public", "visibility-fork");
                    if (item.contains("Private")) {
                        getStyleClass().add("visibility-private");
                    } else {
                        getStyleClass().add("visibility-public");
                    }
                    if (item.contains("Fork")) {
                        getStyleClass().add("visibility-fork");
                    }
                }
            }
        });
        
        // Size column
        TableColumn<RepositoryMetadata, String> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setPrefWidth(80);
        sizeColumn.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getFormattedSize()));
        
        // Last Updated column
        TableColumn<RepositoryMetadata, String> updatedColumn = new TableColumn<>("Last Updated");
        updatedColumn.setPrefWidth(120);
        updatedColumn.setCellValueFactory(param -> {
            LocalDateTime updated = param.getValue().getMostRecentActivity();
            if (updated != null) {
                return new SimpleStringProperty(updated.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            } else {
                return new SimpleStringProperty("—");
            }
        });
        
        // Stars column
        TableColumn<RepositoryMetadata, Integer> starsColumn = new TableColumn<>("Stars");
        starsColumn.setPrefWidth(60);
        starsColumn.setCellValueFactory(new PropertyValueFactory<>("starCount"));
        starsColumn.setCellFactory(column -> new TableCell<RepositoryMetadata, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText("—");
                } else {
                    setText(item.toString());
                }
            }
        });
        
        repositoryTable.getColumns().addAll(
            selectColumn, nameColumn, languageColumn, 
            visibilityColumn, sizeColumn, updatedColumn, starsColumn
        );
        
        // Set table properties
        repositoryTable.setItems(filteredRepositories);
        repositoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        repositoryTable.getStyleClass().add("repository-table");
    }
    
    private void setupFiltering() {
        filteredRepositories.setPredicate(repository -> {
            if (repository == null) return false;
            return currentFilter.matches(repository);
        });
        
        // Update filtered count when predicate changes
        filteredRepositories.addListener((javafx.collections.ListChangeListener<RepositoryMetadata>) change -> {
            filteredCount.set(filteredRepositories.size());
            updateStatusMessage();
        });
    }
    
    private void applyFilters() {
        // Trigger predicate re-evaluation
        filteredRepositories.setPredicate(null);
        filteredRepositories.setPredicate(repository -> {
            if (repository == null) return false;
            return currentFilter.matches(repository);
        });
    }
    
    private void clearFilters() {
        searchField.clear();
        languageFilter.setValue(null);
        visibilityFilter.setValue("All");
        updatedAfterPicker.setValue(null);
        
        currentFilter.clear();
        applyFilters();
    }
    
    private void selectAll() {
        filteredRepositories.forEach(repo -> {
            BooleanProperty selected = selectionMap.get(repo);
            if (selected != null) {
                selected.set(true);
            }
        });
    }
    
    private void selectNone() {
        selectionMap.values().forEach(selected -> selected.set(false));
    }
    
    private void updateSelectionSummary() {
        int selected = (int) selectionMap.values().stream()
            .mapToInt(prop -> prop.get() ? 1 : 0)
            .sum();
        
        selectedCount.set(selected);
        
        String summary = String.format("%d selected", selected);
        if (filteredCount.get() != totalCount.get()) {
            summary += String.format(" (%d filtered, %d total)", filteredCount.get(), totalCount.get());
        } else {
            summary += String.format(" of %d", totalCount.get());
        }
        
        selectionSummary.setText(summary);
    }
    
    private void updateStatusMessage() {
        if (isLoading.get()) {
            statusMessage.set("Loading repositories...");
        } else if (totalCount.get() == 0) {
            statusMessage.set("No repositories loaded");
        } else if (filteredCount.get() == 0) {
            statusMessage.set("No repositories match current filters");
        } else {
            statusMessage.set(String.format("Showing %d of %d repositories", 
                filteredCount.get(), totalCount.get()));
        }
        statusLabel.setText(statusMessage.get());
    }
    
    /**
     * Sets the repositories to display.
     */
    public void setRepositories(List<RepositoryMetadata> repositories) {
        Platform.runLater(() -> {
            // Clear existing data
            allRepositories.clear();
            selectionMap.clear();
            
            if (repositories != null && !repositories.isEmpty()) {
                allRepositories.addAll(repositories);
                
                // Initialize selection properties
                repositories.forEach(repo -> {
                    BooleanProperty selected = new SimpleBooleanProperty(false);
                    selected.addListener((obs, oldVal, newVal) -> {
                        Platform.runLater(this::updateSelectionSummary);
                        repositoryTable.refresh();
                    });
                    selectionMap.put(repo, selected);
                });
                
                // Update language filter options
                updateLanguageFilterOptions(repositories);
            }
            
            totalCount.set(repositories != null ? repositories.size() : 0);
            filteredCount.set(filteredRepositories.size());
            updateSelectionSummary();
            updateStatusMessage();
        });
    }
    
    private void updateLanguageFilterOptions(List<RepositoryMetadata> repositories) {
        Set<String> languages = repositories.stream()
            .map(RepositoryMetadata::getLanguage)
            .filter(Objects::nonNull)
            .filter(lang -> !lang.trim().isEmpty())
            .collect(Collectors.toSet());
        
        String currentSelection = languageFilter.getValue();
        languageFilter.getItems().clear();
        languageFilter.getItems().add("All Languages");
        languageFilter.getItems().addAll(languages.stream().sorted().collect(Collectors.toList()));
        
        // Restore selection if it still exists
        if (currentSelection != null && languageFilter.getItems().contains(currentSelection)) {
            languageFilter.setValue(currentSelection);
        } else {
            languageFilter.setValue("All Languages");
        }
    }
    
    /**
     * Gets the currently selected repositories.
     */
    public List<RepositoryMetadata> getSelectedRepositories() {
        return selectionMap.entrySet().stream()
            .filter(entry -> entry.getValue().get())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the count of currently selected repositories.
     */
    public int getSelectedCount() {
        return selectedCount.get();
    }
    
    /**
     * Property for binding to the selected count.
     */
    public IntegerProperty selectedCountProperty() {
        return selectedCount;
    }
    
    /**
     * Property for binding to the loading state.
     */
    public BooleanProperty isLoadingProperty() {
        return isLoading;
    }
    
    /**
     * Sets the loading state.
     */
    public void setLoading(boolean loading) {
        isLoading.set(loading);
        loadingIndicator.setVisible(loading);
        cancelDiscoveryButton.setVisible(loading);
        updateStatusMessage();
    }
    
    /**
     * Discovers repositories from the given connection.
     */
    public void discoverRepositories(GitProviderConnection connection) {
        if (connection == null || !connection.isValid()) {
            setStatusMessage("Invalid connection configuration");
            return;
        }
        
        // Cancel any existing discovery
        if (currentDiscoveryTask != null && !currentDiscoveryTask.isDone()) {
            currentDiscoveryTask.cancel(true);
        }
        
        setLoading(true);
        setStatusMessage("Connecting to " + connection.getProviderType().getDisplayName() + "...");
        
        currentDiscoveryTask = new Task<List<RepositoryMetadata>>() {
            @Override
            protected List<RepositoryMetadata> call() throws Exception {
                RepositoryDiscoveryService service = serviceFactory
                    .getService(connection.getProviderType())
                    .orElseThrow(() -> new RuntimeException("No service available for " + connection.getProviderType()));
                
                updateMessage("Discovering repositories...");
                
                // Use the service to discover repositories
                return service.discoverAllRepositories(connection).get();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    List<RepositoryMetadata> repositories = getValue();
                    setRepositories(repositories);
                    setLoading(false);
                    setStatusMessage(String.format("Discovered %d repositories", repositories.size()));
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    setLoading(false);
                    Throwable exception = getException();
                    String errorMessage = exception != null ? exception.getMessage() : "Unknown error";
                    setStatusMessage("Discovery failed: " + errorMessage);
                });
            }
            
            @Override
            protected void cancelled() {
                Platform.runLater(() -> {
                    setLoading(false);
                    setStatusMessage("Repository discovery cancelled");
                });
            }
        };
        
        // Bind status message to task message
        statusLabel.textProperty().bind(currentDiscoveryTask.messageProperty());
        
        Thread discoveryThread = new Thread(currentDiscoveryTask);
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }
    
    /**
     * Discovers repositories with filtering.
     */
    public void discoverRepositoriesWithFilter(GitProviderConnection connection, RepositoryFilter filter) {
        if (connection == null || !connection.isValid()) {
            setStatusMessage("Invalid connection configuration");
            return;
        }
        
        // Cancel any existing discovery
        if (currentDiscoveryTask != null && !currentDiscoveryTask.isDone()) {
            currentDiscoveryTask.cancel(true);
        }
        
        setLoading(true);
        setStatusMessage("Connecting to " + connection.getProviderType().getDisplayName() + "...");
        
        currentDiscoveryTask = new Task<List<RepositoryMetadata>>() {
            @Override
            protected List<RepositoryMetadata> call() throws Exception {
                RepositoryDiscoveryService service = serviceFactory
                    .getService(connection.getProviderType())
                    .orElseThrow(() -> new RuntimeException("No service available for " + connection.getProviderType()));
                
                updateMessage("Discovering repositories with filters...");
                
                // Use filtered discovery if supported, otherwise discover all and filter locally
                if (service.supportsAdvancedFiltering()) {
                    return service.discoverRepositoriesWithFilter(connection, filter).get();
                } else {
                    List<RepositoryMetadata> allRepos = service.discoverAllRepositories(connection).get();
                    return allRepos.stream()
                        .filter(filter::matches)
                        .collect(java.util.stream.Collectors.toList());
                }
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    List<RepositoryMetadata> repositories = getValue();
                    setRepositories(repositories);
                    setLoading(false);
                    setStatusMessage(String.format("Discovered %d repositories (filtered)", repositories.size()));
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    setLoading(false);
                    Throwable exception = getException();
                    String errorMessage = exception != null ? exception.getMessage() : "Unknown error";
                    setStatusMessage("Discovery failed: " + errorMessage);
                });
            }
            
            @Override
            protected void cancelled() {
                Platform.runLater(() -> {
                    setLoading(false);
                    setStatusMessage("Repository discovery cancelled");
                });
            }
        };
        
        // Bind status message to task message
        statusLabel.textProperty().bind(currentDiscoveryTask.messageProperty());
        
        Thread discoveryThread = new Thread(currentDiscoveryTask);
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }
    
    /**
     * Cancels the current repository discovery operation.
     */
    public void cancelDiscovery() {
        if (currentDiscoveryTask != null && !currentDiscoveryTask.isDone()) {
            currentDiscoveryTask.cancel(true);
        }
    }
    
    /**
     * Sets a custom status message.
     */
    public void setStatusMessage(String message) {
        // Unbind from task message if bound
        if (statusLabel.textProperty().isBound()) {
            statusLabel.textProperty().unbind();
        }
        statusMessage.set(message);
        statusLabel.setText(message);
    }
    
    /**
     * Gets the current filter being applied.
     */
    public RepositoryFilter getCurrentFilter() {
        return currentFilter;
    }
    
    /**
     * Applies the specified filter.
     */
    public void applyFilter(RepositoryFilter filter) {
        if (filter == null) {
            clearFilters();
            return;
        }
        
        // Update UI controls to match filter
        searchField.setText(filter.getSearchQuery() != null ? filter.getSearchQuery() : "");
        
        if (filter.getLanguages() != null && !filter.getLanguages().isEmpty()) {
            String language = filter.getLanguages().iterator().next();
            if (languageFilter.getItems().contains(language)) {
                languageFilter.setValue(language);
            }
        } else {
            languageFilter.setValue("All Languages");
        }
        
        // Set visibility filter based on filter properties
        if (filter.getIncludePrivate() != null && !filter.getIncludePrivate()) {
            visibilityFilter.setValue("Public");
        } else if (filter.getIncludeForks() != null && !filter.getIncludeForks()) {
            visibilityFilter.setValue("Non-Forks");
        } else if (filter.getIncludeArchived() != null && !filter.getIncludeArchived()) {
            visibilityFilter.setValue("Non-Archived");
        } else {
            visibilityFilter.setValue("All");
        }
        
        if (filter.getUpdatedAfter() != null) {
            updatedAfterPicker.setValue(filter.getUpdatedAfter().toLocalDate());
        } else {
            updatedAfterPicker.setValue(null);
        }
        
        // Apply the filter
        applyFilters();
    }
    
    // Property getters (non-duplicates)
    public StringProperty statusMessageProperty() { return statusMessage; }
    public IntegerProperty totalCountProperty() { return totalCount; }
    public IntegerProperty filteredCountProperty() { return filteredCount; }
    
    public boolean isLoading() { return isLoading.get(); }
    public String getStatusMessage() { return statusMessage.get(); }
    public int getTotalCount() { return totalCount.get(); }
    public int getFilteredCount() { return filteredCount.get(); }
}