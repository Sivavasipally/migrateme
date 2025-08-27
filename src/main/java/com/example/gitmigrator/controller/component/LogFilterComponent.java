package com.example.gitmigrator.controller.component;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Predicate;

/**
 * Component for filtering and searching through migration logs
 */
public class LogFilterComponent extends VBox {
    
    public enum LogLevel {
        ALL("All"),
        INFO("Info"),
        WARNING("Warning"),
        ERROR("Error");
        
        private final String displayName;
        
        LogLevel(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private TextField searchField;
    private ComboBox<LogLevel> levelFilter;
    private ComboBox<String> repositoryFilter;
    private CheckBox caseSensitive;
    private CheckBox useRegex;
    private Button clearFilters;
    
    private Predicate<String> currentFilter;
    private Runnable onFilterChanged;
    
    public LogFilterComponent() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        updateFilter();
    }
    
    private void initializeComponents() {
        searchField = new TextField();
        searchField.setPromptText("Search logs...");
        searchField.getStyleClass().add("search-field");
        
        levelFilter = new ComboBox<>();
        levelFilter.getItems().addAll(LogLevel.values());
        levelFilter.setValue(LogLevel.ALL);
        levelFilter.getStyleClass().add("level-filter");
        
        repositoryFilter = new ComboBox<>();
        repositoryFilter.setPromptText("All repositories");
        repositoryFilter.getStyleClass().add("repository-filter");
        
        caseSensitive = new CheckBox("Case sensitive");
        useRegex = new CheckBox("Use regex");
        
        clearFilters = new Button("Clear");
        clearFilters.getStyleClass().add("clear-button");
    }
    
    private void setupLayout() {
        setSpacing(5);
        setPadding(new Insets(5));
        getStyleClass().add("log-filter-component");
        
        // Search row
        HBox searchRow = new HBox(5);
        searchRow.getChildren().addAll(
            new Label("Search:"), searchField, 
            new Label("Level:"), levelFilter,
            new Label("Repository:"), repositoryFilter
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        // Options row
        HBox optionsRow = new HBox(10);
        optionsRow.getChildren().addAll(caseSensitive, useRegex, clearFilters);
        
        getChildren().addAll(searchRow, optionsRow);
    }
    
    private void setupEventHandlers() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        levelFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        repositoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        caseSensitive.selectedProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        useRegex.selectedProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        
        clearFilters.setOnAction(e -> clearAllFilters());
    }
    
    private void updateFilter() {
        String searchText = searchField.getText();
        LogLevel level = levelFilter.getValue();
        String repository = repositoryFilter.getValue();
        boolean isCaseSensitive = caseSensitive.isSelected();
        boolean isRegex = useRegex.isSelected();
        
        currentFilter = createFilter(searchText, level, repository, isCaseSensitive, isRegex);
        
        if (onFilterChanged != null) {
            onFilterChanged.run();
        }
    }
    
    private Predicate<String> createFilter(String searchText, LogLevel level, String repository, 
                                         boolean caseSensitive, boolean useRegex) {
        return logLine -> {
            // Repository filter
            if (repository != null && !repository.isEmpty()) {
                if (!logLine.contains("[" + repository + "]")) {
                    return false;
                }
            }
            
            // Level filter
            if (level != LogLevel.ALL) {
                String levelName = level.name();
                if (!logLine.toUpperCase().contains(levelName)) {
                    return false;
                }
            }
            
            // Search text filter
            if (searchText != null && !searchText.trim().isEmpty()) {
                String searchTarget = caseSensitive ? logLine : logLine.toLowerCase();
                String searchPattern = caseSensitive ? searchText : searchText.toLowerCase();
                
                if (useRegex) {
                    try {
                        return searchTarget.matches(".*" + searchPattern + ".*");
                    } catch (Exception e) {
                        // Invalid regex, fall back to simple contains
                        return searchTarget.contains(searchPattern);
                    }
                } else {
                    return searchTarget.contains(searchPattern);
                }
            }
            
            return true;
        };
    }
    
    public void setRepositoryOptions(java.util.List<String> repositories) {
        String currentSelection = repositoryFilter.getValue();
        repositoryFilter.getItems().clear();
        repositoryFilter.getItems().add(null); // "All repositories" option
        repositoryFilter.getItems().addAll(repositories);
        
        // Restore selection if it still exists
        if (currentSelection != null && repositories.contains(currentSelection)) {
            repositoryFilter.setValue(currentSelection);
        } else {
            repositoryFilter.setValue(null);
        }
    }
    
    public Predicate<String> getCurrentFilter() {
        return currentFilter;
    }
    
    public void setOnFilterChanged(Runnable callback) {
        this.onFilterChanged = callback;
    }
    
    public void clearAllFilters() {
        searchField.clear();
        levelFilter.setValue(LogLevel.ALL);
        repositoryFilter.setValue(null);
        caseSensitive.setSelected(false);
        useRegex.setSelected(false);
    }
    
    public boolean hasActiveFilters() {
        return !searchField.getText().trim().isEmpty() ||
               levelFilter.getValue() != LogLevel.ALL ||
               repositoryFilter.getValue() != null ||
               caseSensitive.isSelected() ||
               useRegex.isSelected();
    }
}