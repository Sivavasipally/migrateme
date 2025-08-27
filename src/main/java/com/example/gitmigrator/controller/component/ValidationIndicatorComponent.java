package com.example.gitmigrator.controller.component;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * Component for displaying validation issues and warnings
 */
public class ValidationIndicatorComponent extends VBox {
    
    private final Label statusLabel;
    private final ListView<String> issuesList;
    private final TitledPane issuesPane;
    private final ObservableList<String> issues;
    
    public ValidationIndicatorComponent() {
        // Initialize components
        this.statusLabel = new Label("No issues");
        this.issues = FXCollections.observableArrayList();
        this.issuesList = new ListView<>(issues);
        this.issuesPane = new TitledPane("Validation Issues", issuesList);
        
        setupComponents();
        setupLayout();
    }
    
    private void setupComponents() {
        // Configure status label
        statusLabel.getStyleClass().add("validation-success");
        
        // Configure issues list
        issuesList.setPrefHeight(100);
        issuesList.setVisible(false);
        issuesList.setManaged(false);
        
        // Configure issues pane
        issuesPane.setExpanded(false);
        issuesPane.setVisible(false);
        issuesPane.setManaged(false);
    }
    
    private void setupLayout() {
        setPadding(new Insets(5));
        setSpacing(5);
        getChildren().addAll(statusLabel, issuesPane);
    }
    
    public void setIssues(List<String> newIssues) {
        issues.clear();
        
        if (newIssues == null || newIssues.isEmpty()) {
            // No issues
            statusLabel.setText("No issues");
            statusLabel.getStyleClass().removeAll("validation-error", "validation-warning");
            statusLabel.getStyleClass().add("validation-success");
            
            issuesPane.setVisible(false);
            issuesPane.setManaged(false);
        } else {
            // Has issues
            issues.addAll(newIssues);
            
            statusLabel.setText(String.format("%d issue%s found", 
                newIssues.size(), newIssues.size() == 1 ? "" : "s"));
            statusLabel.getStyleClass().removeAll("validation-success", "validation-warning");
            statusLabel.getStyleClass().add("validation-error");
            
            issuesPane.setVisible(true);
            issuesPane.setManaged(true);
            issuesPane.setExpanded(true);
        }
    }
    
    public void clear() {
        setIssues(null);
    }
}