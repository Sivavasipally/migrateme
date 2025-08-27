package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.ErrorCategory;
import com.example.gitmigrator.model.ErrorReport;
import com.example.gitmigrator.model.ErrorSeverity;
import com.example.gitmigrator.service.ErrorRecoveryHelper;
import com.example.gitmigrator.service.ErrorReportingService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * JavaFX component for displaying comprehensive error reports with recovery options
 */
public class ErrorReportingComponent extends VBox {
    
    private final ErrorReportingService errorReportingService;
    private final ErrorRecoveryHelper errorRecoveryHelper;
    private final Consumer<String> onRecoveryAction;
    
    private Label titleLabel;
    private Label categoryLabel;
    private Label severityLabel;
    private TextArea descriptionArea;
    private TextArea technicalDetailsArea;
    private ListView<String> suggestedActionsList;
    private ListView<String> documentationList;
    private VBox recoveryOptionsBox;
    private ProgressIndicator recoveryProgress;
    private Label recoveryStatusLabel;
    
    public ErrorReportingComponent(ErrorReportingService errorReportingService, 
                                 ErrorRecoveryHelper errorRecoveryHelper,
                                 Consumer<String> onRecoveryAction) {
        this.errorReportingService = errorReportingService;
        this.errorRecoveryHelper = errorRecoveryHelper;
        this.onRecoveryAction = onRecoveryAction;
        
        initializeComponents();
        setupLayout();
        applyStyles();
    }
    
    private void initializeComponents() {
        titleLabel = new Label();
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        categoryLabel = new Label();
        categoryLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        
        severityLabel = new Label();
        severityLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        
        descriptionArea = new TextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(3);
        
        technicalDetailsArea = new TextArea();
        technicalDetailsArea.setEditable(false);
        technicalDetailsArea.setWrapText(true);
        technicalDetailsArea.setPrefRowCount(4);
        
        suggestedActionsList = new ListView<>();
        suggestedActionsList.setPrefHeight(120);
        
        documentationList = new ListView<>();
        documentationList.setPrefHeight(80);
        
        recoveryOptionsBox = new VBox(5);
        recoveryProgress = new ProgressIndicator();
        recoveryProgress.setVisible(false);
        recoveryProgress.setPrefSize(30, 30);
        
        recoveryStatusLabel = new Label();
        recoveryStatusLabel.setVisible(false);
    }
    
    private void setupLayout() {
        setSpacing(10);
        setPadding(new Insets(15));
        
        // Header section
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getChildren().addAll(titleLabel);
        
        HBox metadataBox = new HBox(20);
        metadataBox.setAlignment(Pos.CENTER_LEFT);
        metadataBox.getChildren().addAll(categoryLabel, severityLabel);
        
        // Description section
        Label descLabel = new Label("Description:");
        descLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        // Technical details section (collapsible)
        TitledPane technicalPane = new TitledPane("Technical Details", technicalDetailsArea);
        technicalPane.setExpanded(false);
        technicalPane.setCollapsible(true);
        
        // Suggested actions section
        Label actionsLabel = new Label("Suggested Actions:");
        actionsLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        // Documentation section
        Label docsLabel = new Label("Related Documentation:");
        docsLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        // Recovery options section
        Label recoveryLabel = new Label("Recovery Options:");
        recoveryLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        HBox recoveryStatusBox = new HBox(10);
        recoveryStatusBox.setAlignment(Pos.CENTER_LEFT);
        recoveryStatusBox.getChildren().addAll(recoveryProgress, recoveryStatusLabel);
        
        // Add all components
        getChildren().addAll(
                headerBox,
                metadataBox,
                new Separator(),
                descLabel,
                descriptionArea,
                technicalPane,
                new Separator(),
                actionsLabel,
                suggestedActionsList,
                new Separator(),
                docsLabel,
                documentationList,
                new Separator(),
                recoveryLabel,
                recoveryOptionsBox,
                recoveryStatusBox
        );
    }
    
    private void applyStyles() {
        getStyleClass().add("error-report-component");
        titleLabel.getStyleClass().add("error-title");
        categoryLabel.getStyleClass().add("error-category");
        severityLabel.getStyleClass().add("error-severity");
        descriptionArea.getStyleClass().add("error-description");
        technicalDetailsArea.getStyleClass().add("technical-details");
        suggestedActionsList.getStyleClass().add("suggested-actions");
        documentationList.getStyleClass().add("documentation-list");
        recoveryOptionsBox.getStyleClass().add("recovery-options");
    }
    
    public void displayErrorReport(ErrorReport errorReport) {
        Platform.runLater(() -> {
            // Update basic information
            titleLabel.setText(errorReport.getTitle());
            categoryLabel.setText("Category: " + errorReport.getCategory().getDisplayName());
            severityLabel.setText("Severity: " + errorReport.getSeverity().getDisplayName());
            
            // Apply severity-specific styling
            applySeverityStyle(errorReport.getSeverity());
            
            // Update description
            descriptionArea.setText(errorReport.getDescription());
            
            // Update technical details
            if (errorReport.getTechnicalDetails() != null && !errorReport.getTechnicalDetails().isEmpty()) {
                technicalDetailsArea.setText(errorReport.getTechnicalDetails());
            } else {
                technicalDetailsArea.setText("No technical details available");
            }
            
            // Update suggested actions
            suggestedActionsList.getItems().clear();
            suggestedActionsList.getItems().addAll(errorReport.getSuggestedActions());
            
            // Update documentation
            documentationList.getItems().clear();
            documentationList.getItems().addAll(errorReport.getRelatedDocumentation());
            
            // Load recovery options
            loadRecoveryOptions(errorReport);
        });
    }
    
    private void applySeverityStyle(ErrorSeverity severity) {
        // Remove existing severity styles
        severityLabel.getStyleClass().removeIf(style -> style.startsWith("severity-"));
        
        // Apply new severity style
        switch (severity) {
            case LOW:
                severityLabel.getStyleClass().add("severity-low");
                break;
            case MEDIUM:
                severityLabel.getStyleClass().add("severity-medium");
                break;
            case HIGH:
                severityLabel.getStyleClass().add("severity-high");
                break;
            case CRITICAL:
                severityLabel.getStyleClass().add("severity-critical");
                break;
        }
    }
    
    private void loadRecoveryOptions(ErrorReport errorReport) {
        recoveryOptionsBox.getChildren().clear();
        
        // Check if auto-recovery is possible
        if (errorRecoveryHelper.canAutoRecover(errorReport.getCategory(), errorReport.getOriginalException())) {
            Button autoRecoveryButton = new Button("Attempt Auto Recovery");
            autoRecoveryButton.getStyleClass().add("auto-recovery-button");
            autoRecoveryButton.setOnAction(e -> attemptAutoRecovery(errorReport));
            recoveryOptionsBox.getChildren().add(autoRecoveryButton);
        }
        
        // Load interactive recovery options
        CompletableFuture.supplyAsync(() -> errorRecoveryHelper.getInteractiveRecoveryOptions(errorReport))
                .thenAccept(options -> Platform.runLater(() -> {
                    if (!options.isEmpty()) {
                        Label interactiveLabel = new Label("Interactive Recovery Options:");
                        interactiveLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
                        recoveryOptionsBox.getChildren().add(interactiveLabel);
                        
                        for (ErrorRecoveryHelper.RecoveryOption option : options) {
                            Button optionButton = createRecoveryOptionButton(option, errorReport);
                            recoveryOptionsBox.getChildren().add(optionButton);
                        }
                    }
                }));
    }
    
    private Button createRecoveryOptionButton(ErrorRecoveryHelper.RecoveryOption option, ErrorReport errorReport) {
        Button button = new Button(option.getTitle());
        button.setTooltip(new Tooltip(option.getDescription()));
        button.getStyleClass().add("recovery-option-button");
        
        // Add time estimate if available
        if (option.getEstimatedTime() > 0) {
            long seconds = option.getEstimatedTime() / 1000;
            button.setText(button.getText() + " (~" + seconds + "s)");
        }
        
        // Add indicator for user input required
        if (option.requiresUserInput()) {
            button.setText(button.getText() + " *");
            button.setTooltip(new Tooltip(option.getDescription() + "\n* Requires user input"));
        }
        
        button.setOnAction(e -> executeRecoveryOption(option, errorReport));
        
        return button;
    }
    
    private void attemptAutoRecovery(ErrorReport errorReport) {
        showRecoveryProgress("Attempting automatic recovery...");
        
        errorRecoveryHelper.attemptAutoRecovery(errorReport)
                .thenAccept(success -> Platform.runLater(() -> {
                    hideRecoveryProgress();
                    if (success) {
                        showRecoveryStatus("Auto recovery successful", true);
                        if (onRecoveryAction != null) {
                            onRecoveryAction.accept("auto_recovery_success");
                        }
                    } else {
                        showRecoveryStatus("Auto recovery failed - try manual options", false);
                    }
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        hideRecoveryProgress();
                        showRecoveryStatus("Auto recovery error: " + throwable.getMessage(), false);
                    });
                    return null;
                });
    }
    
    private void executeRecoveryOption(ErrorRecoveryHelper.RecoveryOption option, ErrorReport errorReport) {
        showRecoveryProgress("Executing: " + option.getTitle());
        
        errorRecoveryHelper.executeRecoveryOption(option, errorReport)
                .thenAccept(result -> Platform.runLater(() -> {
                    hideRecoveryProgress();
                    showRecoveryStatus(result.getMessage(), result.isSuccessful());
                    
                    if (result.isSuccessful() && onRecoveryAction != null) {
                        onRecoveryAction.accept(option.getId());
                    }
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        hideRecoveryProgress();
                        showRecoveryStatus("Recovery failed: " + throwable.getMessage(), false);
                    });
                    return null;
                });
    }
    
    private void showRecoveryProgress(String message) {
        recoveryProgress.setVisible(true);
        recoveryStatusLabel.setText(message);
        recoveryStatusLabel.setVisible(true);
        recoveryStatusLabel.getStyleClass().removeAll("recovery-success", "recovery-error");
    }
    
    private void hideRecoveryProgress() {
        recoveryProgress.setVisible(false);
    }
    
    private void showRecoveryStatus(String message, boolean success) {
        recoveryStatusLabel.setText(message);
        recoveryStatusLabel.setVisible(true);
        recoveryStatusLabel.getStyleClass().removeAll("recovery-success", "recovery-error");
        recoveryStatusLabel.getStyleClass().add(success ? "recovery-success" : "recovery-error");
        
        // Auto-hide after 5 seconds
        CompletableFuture.delayedExecutor(5, java.util.concurrent.TimeUnit.SECONDS)
                .execute(() -> Platform.runLater(() -> {
                    recoveryStatusLabel.setVisible(false);
                }));
    }
    
    public void displayError(Throwable error, String context) {
        ErrorReport report = errorReportingService.createErrorReport(error, context);
        displayErrorReport(report);
    }
    
    public void displayCustomError(String title, String description, ErrorCategory category, String context) {
        ErrorReport report = errorReportingService.createErrorReport(title, description, category, context);
        displayErrorReport(report);
    }
    
    public void clear() {
        Platform.runLater(() -> {
            titleLabel.setText("");
            categoryLabel.setText("");
            severityLabel.setText("");
            descriptionArea.clear();
            technicalDetailsArea.clear();
            suggestedActionsList.getItems().clear();
            documentationList.getItems().clear();
            recoveryOptionsBox.getChildren().clear();
            recoveryStatusLabel.setVisible(false);
            recoveryProgress.setVisible(false);
        });
    }
}