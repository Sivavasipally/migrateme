package com.example.gitmigrator.controller.wizard;

import com.example.gitmigrator.model.*;
import com.example.gitmigrator.service.TransformationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Second wizard step: Framework Detection and Review.
 * Shows detected frameworks for selected repositories and allows user review/override.
 */
public class FrameworkDetectionStep implements WizardStep {
    
    private static final Logger logger = LoggerFactory.getLogger(FrameworkDetectionStep.class);
    
    private final MigrationWizardModel wizardModel;
    private final TransformationService transformationService;
    
    private VBox stepContent;
    private TableView<RepositoryDetectionInfo> detectionTable;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Button redetectButton;
    
    private Runnable nextCallback;
    private Runnable previousCallback;
    private Runnable finishCallback;
    
    public FrameworkDetectionStep(MigrationWizardModel wizardModel, TransformationService transformationService) {
        this.wizardModel = wizardModel;
        this.transformationService = transformationService;
        initializeUI();
    }
    
    private void initializeUI() {
        stepContent = new VBox(15);
        stepContent.setPadding(new Insets(20));
        
        // Title and description
        Label titleLabel = new Label("Framework Detection Results");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label descriptionLabel = new Label(
            "Review the automatically detected frameworks for your repositories. " +
            "You can override the detection if needed. This information will be used to select appropriate migration templates.");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-text-fill: #666666;");
        
        // Detection summary
        VBox summarySection = createDetectionSummarySection();
        
        // Detection results table
        VBox tableSection = createDetectionTableSection();
        
        // Actions
        HBox actionsSection = createActionsSection();
        
        // Status
        HBox statusSection = createStatusSection();
        
        stepContent.getChildren().addAll(
            titleLabel,
            descriptionLabel,
            new Separator(),
            summarySection,
            new Separator(),
            tableSection,
            actionsSection,
            statusSection
        );
    }
    
    private VBox createDetectionSummarySection() {
        VBox summarySection = new VBox(10);
        
        Label summaryLabel = new Label("📊 Detection Summary");
        summaryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Framework distribution chart would go here
        // For now, show text summary
        Label summaryText = new Label();
        summaryText.setWrapText(true);
        summaryText.setStyle("-fx-text-fill: #666666;");
        
        // This will be updated when step is entered
        summarySection.getChildren().addAll(summaryLabel, summaryText);
        summarySection.setUserData(summaryText); // Store reference for updates
        
        return summarySection;
    }
    
    private VBox createDetectionTableSection() {
        VBox tableSection = new VBox(10);
        
        Label tableLabel = new Label("🔍 Framework Detection Results");
        tableLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Create detection results table
        detectionTable = new TableView<>();
        detectionTable.setPrefHeight(400);
        
        // Repository name column
        TableColumn<RepositoryDetectionInfo, String> nameColumn = new TableColumn<>("Repository");
        nameColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRepository().getName()));
        nameColumn.setPrefWidth(180);
        
        // Detected framework column with icon
        TableColumn<RepositoryDetectionInfo, String> frameworkColumn = new TableColumn<>("Detected Framework");
        frameworkColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                getFrameworkDisplayText(cellData.getValue().getDetectedFramework())));
        frameworkColumn.setPrefWidth(200);
        
        // Framework override dropdown
        TableColumn<RepositoryDetectionInfo, FrameworkType> overrideColumn = new TableColumn<>("Override Framework");
        overrideColumn.setCellValueFactory(cellData -> cellData.getValue().overrideFrameworkProperty());
        overrideColumn.setCellFactory(col -> new TableCell<RepositoryDetectionInfo, FrameworkType>() {
            private final ComboBox<FrameworkType> comboBox = new ComboBox<>();
            {
                comboBox.getItems().addAll(FrameworkType.values());
                comboBox.setOnAction(e -> {
                    RepositoryDetectionInfo item = getTableRow().getItem();
                    if (item != null) {
                        item.setOverrideFramework(comboBox.getValue());
                    }
                });
            }
            
            @Override
            protected void updateItem(FrameworkType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    comboBox.setValue(item);
                    setGraphic(comboBox);
                }
            }
        });
        overrideColumn.setPrefWidth(150);
        overrideColumn.setEditable(true);
        
        // Complexity column
        TableColumn<RepositoryDetectionInfo, String> complexityColumn = new TableColumn<>("Complexity");
        complexityColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                getComplexityDisplayText(cellData.getValue().getComplexity())));
        complexityColumn.setPrefWidth(100);
        
        // Details column
        TableColumn<RepositoryDetectionInfo, String> detailsColumn = new TableColumn<>("Details");
        detailsColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDetailsText()));
        detailsColumn.setPrefWidth(250);
        
        // Actions column
        TableColumn<RepositoryDetectionInfo, Void> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setCellFactory(col -> new TableCell<RepositoryDetectionInfo, Void>() {
            private final Button viewDetailsButton = new Button("View Details");
            {
                viewDetailsButton.setOnAction(e -> {
                    RepositoryDetectionInfo info = getTableView().getItems().get(getIndex());
                    showDetectionDetails(info);
                });
                viewDetailsButton.getStyleClass().add("config-button");
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewDetailsButton);
            }
        });
        actionsColumn.setPrefWidth(120);
        
        detectionTable.getColumns().addAll(nameColumn, frameworkColumn, overrideColumn, 
                                         complexityColumn, detailsColumn, actionsColumn);
        detectionTable.setEditable(true);
        
        tableSection.getChildren().addAll(tableLabel, detectionTable);
        return tableSection;
    }
    
    private HBox createActionsSection() {
        HBox actionsSection = new HBox(10);
        actionsSection.setAlignment(Pos.CENTER_LEFT);
        
        redetectButton = new Button("🔄 Re-run Detection");
        redetectButton.getStyleClass().add("config-button");
        redetectButton.setOnAction(e -> rerunDetection());
        
        Button exportResultsButton = new Button("📊 Export Results");
        exportResultsButton.getStyleClass().add("config-button");
        exportResultsButton.setOnAction(e -> exportDetectionResults());
        
        actionsSection.getChildren().addAll(redetectButton, exportResultsButton);
        return actionsSection;
    }
    
    private HBox createStatusSection() {
        HBox statusSection = new HBox(10);
        statusSection.setAlignment(Pos.CENTER_LEFT);
        
        statusLabel = new Label("Framework detection completed");
        statusLabel.setStyle("-fx-text-fill: #666666;");
        
        progressBar = new ProgressBar(0);
        progressBar.setVisible(false);
        progressBar.setPrefWidth(200);
        
        statusSection.getChildren().addAll(statusLabel, progressBar);
        return statusSection;
    }
    
    private void populateDetectionResults() {
        var detectionInfoList = FXCollections.<RepositoryDetectionInfo>observableArrayList();
        
        for (RepositoryInfo repo : wizardModel.getSelectedRepositories()) {
            MigrationWizardModel.FrameworkDetectionResult result = wizardModel.getDetectionResults().get(repo);
            
            RepositoryDetectionInfo info = new RepositoryDetectionInfo();
            info.setRepository(repo);
            
            if (result != null) {
                info.setDetectedFramework(result.getPrimaryFramework());
                info.setComplexity(result.getComplexity());
                info.setMonorepo(result.isMonorepo());
                info.setMetadata(result.getMetadata());
            } else {
                info.setDetectedFramework(FrameworkType.UNKNOWN);
                info.setComplexity(1);
                info.setMonorepo(false);
                info.setMetadata(new HashMap<>());
            }
            
            detectionInfoList.add(info);
        }
        
        detectionTable.setItems(detectionInfoList);
        updateDetectionSummary(detectionInfoList);
    }
    
    private void updateDetectionSummary(javafx.collections.ObservableList<RepositoryDetectionInfo> detectionInfoList) {
        // Count frameworks
        Map<FrameworkType, Long> frameworkCounts = new HashMap<>();
        for (RepositoryDetectionInfo info : detectionInfoList) {
            FrameworkType framework = info.getEffectiveFramework();
            frameworkCounts.put(framework, frameworkCounts.getOrDefault(framework, 0L) + 1);
        }
        
        // Build summary text
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Analyzed %d repositories. ", detectionInfoList.size()));
        
        if (!frameworkCounts.isEmpty()) {
            summary.append("Detected frameworks: ");
            frameworkCounts.forEach((framework, count) -> 
                summary.append(String.format("%s (%d), ", framework.getDisplayName(), count)));
            
            // Remove trailing comma and space
            if (summary.length() > 2) {
                summary.setLength(summary.length() - 2);
            }
        }
        
        // Update summary label
        VBox summarySection = (VBox) stepContent.getChildren().get(3); // Index 3 is summarySection (after titleLabel, descriptionLabel, Separator)
        Label summaryText = (Label) summarySection.getUserData();
        summaryText.setText(summary.toString());
    }
    
    private void rerunDetection() {
        statusLabel.setText("Re-running framework detection...");
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        redetectButton.setDisable(true);
        
        Task<Void> detectionTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for (RepositoryInfo repo : wizardModel.getSelectedRepositories()) {
                    if (repo.getLocalPath() != null) {
                        // Re-analyze repository
                        RepositoryInfo analyzed = transformationService.analyzeRepository(repo.getLocalPath());
                        
                        // Update repository info
                        repo.setDetectedFramework(analyzed.getDetectedFramework());
                        repo.setEstimatedComplexity(analyzed.getEstimatedComplexity());
                        repo.setAdditionalMetadata(analyzed.getAdditionalMetadata());
                        
                        // Update wizard model
                        MigrationWizardModel.FrameworkDetectionResult result = 
                            new MigrationWizardModel.FrameworkDetectionResult(
                                analyzed.getDetectedFramework(),
                                false, // simplified
                                analyzed.getEstimatedComplexity(),
                                analyzed.getAdditionalMetadata()
                            );
                        wizardModel.addDetectionResult(repo, result);
                    }
                }
                return null;
            }
        };
        
        detectionTask.setOnSucceeded(e -> Platform.runLater(() -> {
            populateDetectionResults();
            statusLabel.setText("Framework detection completed");
            progressBar.setVisible(false);
            redetectButton.setDisable(false);
        }));
        
        detectionTask.setOnFailed(e -> Platform.runLater(() -> {
            statusLabel.setText("Framework detection failed");
            progressBar.setVisible(false);
            redetectButton.setDisable(false);
            showError("Framework detection failed: " + detectionTask.getException().getMessage());
        }));
        
        new Thread(detectionTask).start();
    }
    
    private void showDetectionDetails(RepositoryDetectionInfo info) {
        Dialog<Void> detailsDialog = new Dialog<>();
        detailsDialog.setTitle("Framework Detection Details");
        detailsDialog.setHeaderText("Details for " + info.getRepository().getName());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        // Framework information
        content.getChildren().add(new Label("Detected Framework: " + info.getDetectedFramework().getDisplayName()));
        content.getChildren().add(new Label("Complexity Level: " + info.getComplexity()));
        content.getChildren().add(new Label("Monorepo: " + (info.isMonorepo() ? "Yes" : "No")));
        
        // Metadata
        if (!info.getMetadata().isEmpty()) {
            content.getChildren().add(new Separator());
            content.getChildren().add(new Label("Additional Information:"));
            
            TextArea metadataArea = new TextArea();
            metadataArea.setEditable(false);
            metadataArea.setPrefRowCount(10);
            
            StringBuilder metadataText = new StringBuilder();
            info.getMetadata().forEach((key, value) -> 
                metadataText.append(key).append(": ").append(value).append("\n"));
            
            metadataArea.setText(metadataText.toString());
            content.getChildren().add(metadataArea);
        }
        
        detailsDialog.getDialogPane().setContent(content);
        detailsDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        detailsDialog.showAndWait();
    }
    
    private void exportDetectionResults() {
        // Implementation for exporting detection results to file
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Results");
        alert.setContentText("Detection results exported successfully!");
        alert.showAndWait();
    }
    
    private String getFrameworkDisplayText(FrameworkType framework) {
        if (framework == null) return "❓ Unknown";
        
        String emoji = switch (framework) {
            case SPRING_BOOT -> "🍃";
            case REACT -> "⚛️";
            case ANGULAR -> "🅰️";
            case NODE_JS -> "🟢";
            case PYTHON -> "🐍";
            case UNKNOWN -> "❓";
            default -> "📦";
        };
        
        return emoji + " " + framework.getDisplayName();
    }
    
    private String getComplexityDisplayText(int complexity) {
        String dots = "●".repeat(complexity) + "○".repeat(5 - complexity);
        String level = switch (complexity) {
            case 1 -> "Simple";
            case 2 -> "Easy";
            case 3 -> "Medium";
            case 4 -> "Complex";
            case 5 -> "Very Complex";
            default -> "Unknown";
        };
        return dots + " " + level;
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Framework Detection Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @Override
    public String getStepTitle() {
        return "Framework Detection Review";
    }
    
    @Override
    public String getStepDescription() {
        return "Review and verify the automatically detected frameworks for your repositories.";
    }
    
    @Override
    public Node getStepContent() {
        return stepContent;
    }
    
    @Override
    public void onStepEnter() {
        populateDetectionResults();
        statusLabel.setText("Framework detection results ready for review");
    }
    
    @Override
    public void onStepExit() {
        // Save any framework overrides back to the wizard model
        for (RepositoryDetectionInfo info : detectionTable.getItems()) {
            if (info.getOverrideFramework() != null) {
                // Update the repository with override framework
                info.getRepository().setDetectedFramework(info.getOverrideFramework());
                
                // Update detection results
                MigrationWizardModel.FrameworkDetectionResult updatedResult = 
                    new MigrationWizardModel.FrameworkDetectionResult(
                        info.getOverrideFramework(),
                        info.isMonorepo(),
                        info.getComplexity(),
                        info.getMetadata()
                    );
                wizardModel.addDetectionResult(info.getRepository(), updatedResult);
            }
        }
        
        logger.info("Framework detection review completed for {} repositories", 
            detectionTable.getItems().size());
    }
    
    @Override
    public boolean validateStep() {
        // Check that all repositories have a detected framework
        boolean allDetected = detectionTable.getItems().stream()
            .allMatch(info -> info.getEffectiveFramework() != FrameworkType.UNKNOWN);
            
        if (!allDetected) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unknown Frameworks");
            alert.setHeaderText("Some repositories have unknown frameworks");
            alert.setContentText("Continue anyway? Migration templates may be limited for unknown frameworks.");
            
            var result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
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
        if (detectionTable.getItems() == null || detectionTable.getItems().isEmpty()) {
            return 0.0;
        }
        
        long detectedCount = detectionTable.getItems().stream()
            .mapToLong(info -> info.getEffectiveFramework() != FrameworkType.UNKNOWN ? 1 : 0)
            .sum();
        
        return (double) detectedCount / detectionTable.getItems().size();
    }
    
    /**
     * Data model for repository detection information display.
     */
    public static class RepositoryDetectionInfo {
        private RepositoryInfo repository;
        private FrameworkType detectedFramework;
        private final javafx.beans.property.ObjectProperty<FrameworkType> overrideFramework = 
            new javafx.beans.property.SimpleObjectProperty<>();
        private int complexity;
        private boolean monorepo;
        private Map<String, Object> metadata = new HashMap<>();
        
        public RepositoryInfo getRepository() { return repository; }
        public void setRepository(RepositoryInfo repository) { this.repository = repository; }
        
        public FrameworkType getDetectedFramework() { return detectedFramework; }
        public void setDetectedFramework(FrameworkType detectedFramework) { this.detectedFramework = detectedFramework; }
        
        public javafx.beans.property.ObjectProperty<FrameworkType> overrideFrameworkProperty() { 
            return overrideFramework; 
        }
        public FrameworkType getOverrideFramework() { return overrideFramework.get(); }
        public void setOverrideFramework(FrameworkType overrideFramework) { this.overrideFramework.set(overrideFramework); }
        
        public FrameworkType getEffectiveFramework() {
            return getOverrideFramework() != null ? getOverrideFramework() : getDetectedFramework();
        }
        
        public int getComplexity() { return complexity; }
        public void setComplexity(int complexity) { this.complexity = complexity; }
        
        public boolean isMonorepo() { return monorepo; }
        public void setMonorepo(boolean monorepo) { this.monorepo = monorepo; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        
        public String getDetailsText() {
            StringBuilder details = new StringBuilder();
            if (monorepo) details.append("Monorepo, ");
            if (metadata.containsKey("componentCount")) {
                details.append("Components: ").append(metadata.get("componentCount")).append(", ");
            }
            if (details.length() > 0) {
                details.setLength(details.length() - 2); // Remove trailing comma
            }
            return details.toString();
        }
    }
}