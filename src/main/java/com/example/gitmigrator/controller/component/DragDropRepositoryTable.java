package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.FrameworkType;
import com.example.gitmigrator.model.MigrationStatus;
import com.example.gitmigrator.model.RepositoryInfo;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Enhanced TableView with drag-and-drop support for repositories.
 * Supports dropping local repository folders and Git URLs.
 */
public class DragDropRepositoryTable extends TableView<RepositoryInfo> {
    
    private static final Logger logger = LoggerFactory.getLogger(DragDropRepositoryTable.class);
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    
    // Callbacks for handling dropped items
    private Consumer<File> onFolderDropped;
    private Consumer<String> onUrlDropped;
    private Consumer<String> onValidationError;
    
    // Table columns
    private TableColumn<RepositoryInfo, Boolean> selectColumn;
    private TableColumn<RepositoryInfo, String> nameColumn;
    private TableColumn<RepositoryInfo, String> frameworkColumn;
    private TableColumn<RepositoryInfo, String> urlColumn;
    private TableColumn<RepositoryInfo, String> lastCommitColumn;
    private TableColumn<RepositoryInfo, String> complexityColumn;
    private TableColumn<RepositoryInfo, String> statusColumn;
    
    public DragDropRepositoryTable() {
        super();
        initializeTable();
        setupDragAndDrop();
        setupStyling();
    }
    
    /**
     * Initialize table columns and cell factories.
     */
    private void initializeTable() {
        // Select column with checkboxes
        selectColumn = new TableColumn<>("Select");
        selectColumn.setPrefWidth(60);
        selectColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);
        selectColumn.setSortable(false);
        
        // Name column with repository icon
        nameColumn = new TableColumn<>("Repository");
        nameColumn.setPrefWidth(200);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setCellFactory(column -> new RepositoryNameCell());
        
        // Framework column with framework icons
        frameworkColumn = new TableColumn<>("Framework");
        frameworkColumn.setPrefWidth(120);
        frameworkColumn.setCellValueFactory(cellData -> {
            FrameworkType framework = cellData.getValue().getDetectedFramework();
            return new SimpleStringProperty(framework != null ? framework.getDisplayName() : "Unknown");
        });
        frameworkColumn.setCellFactory(column -> new FrameworkCell());
        
        // URL column
        urlColumn = new TableColumn<>("Clone URL");
        urlColumn.setPrefWidth(300);
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("cloneUrl"));
        
        // Last commit column
        lastCommitColumn = new TableColumn<>("Last Commit");
        lastCommitColumn.setPrefWidth(120);
        lastCommitColumn.setCellValueFactory(cellData -> {
            var lastCommit = cellData.getValue().getLastCommitDate();
            String dateStr = lastCommit != null ? lastCommit.format(DATE_FORMATTER) : "Unknown";
            return new SimpleStringProperty(dateStr);
        });
        
        // Complexity column with visual indicators
        complexityColumn = new TableColumn<>("Complexity");
        complexityColumn.setPrefWidth(100);
        complexityColumn.setCellValueFactory(cellData -> {
            int complexity = cellData.getValue().getEstimatedComplexity();
            return new SimpleStringProperty(getComplexityText(complexity));
        });
        complexityColumn.setCellFactory(column -> new ComplexityCell());
        
        // Status column with colored indicators
        statusColumn = new TableColumn<>("Status");
        statusColumn.setPrefWidth(100);
        statusColumn.setCellValueFactory(cellData -> {
            MigrationStatus status = cellData.getValue().getStatus();
            return new SimpleStringProperty(status != null ? status.getDisplayName() : "Ready");
        });
        statusColumn.setCellFactory(column -> new StatusCell());
        
        // Add columns to table
        getColumns().addAll(selectColumn, nameColumn, frameworkColumn, urlColumn, 
                           lastCommitColumn, complexityColumn, statusColumn);
        
        // Enable editing and set resize policy
        setEditable(true);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Set placeholder for empty table
        setPlaceholder(createEmptyTablePlaceholder());
    }
    
    /**
     * Setup drag and drop functionality.
     */
    private void setupDragAndDrop() {
        // Accept drag over events
        setOnDragOver(event -> {
            if (event.getGestureSource() != this && 
                (event.getDragboard().hasFiles() || event.getDragboard().hasString())) {
                event.acceptTransferModes(TransferMode.COPY);
                getStyleClass().add("drag-over");
            }
            event.consume();
        });
        
        // Handle drag entered
        setOnDragEntered(event -> {
            if (event.getGestureSource() != this) {
                getStyleClass().add("drag-over");
            }
            event.consume();
        });
        
        // Handle drag exited
        setOnDragExited(event -> {
            getStyleClass().remove("drag-over");
            event.consume();
        });
        
        // Handle dropped items
        setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            
            try {
                if (dragboard.hasFiles()) {
                    // Handle dropped files/folders
                    List<File> files = dragboard.getFiles();
                    for (File file : files) {
                        if (file.isDirectory() && isGitRepository(file)) {
                            if (onFolderDropped != null) {
                                onFolderDropped.accept(file);
                            }
                            success = true;
                        } else if (file.isDirectory()) {
                            showValidationError("Dropped folder is not a Git repository: " + file.getName());
                        }
                    }
                } else if (dragboard.hasString()) {
                    // Handle dropped URLs
                    String content = dragboard.getString();
                    if (isValidGitUrl(content)) {
                        if (onUrlDropped != null) {
                            onUrlDropped.accept(content);
                        }
                        success = true;
                    } else {
                        showValidationError("Invalid Git URL: " + content);
                    }
                }
            } catch (Exception e) {
                logger.error("Error handling drag and drop", e);
                showValidationError("Error processing dropped item: " + e.getMessage());
            } finally {
                getStyleClass().remove("drag-over");
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
    }
    
    /**
     * Setup visual styling for the table.
     */
    private void setupStyling() {
        getStyleClass().add("drag-drop-table");
        
        // Add hover effects for rows
        setRowFactory(tv -> {
            TableRow<RepositoryInfo> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    row.getStyleClass().removeAll("status-ready", "status-processing", 
                                                 "status-completed", "status-failed");
                    if (newItem.getStatus() != null) {
                        row.getStyleClass().add("status-" + newItem.getStatus().name().toLowerCase());
                    }
                }
            });
            return row;
        });
    }
    
    /**
     * Create placeholder content for empty table.
     */
    private VBox createEmptyTablePlaceholder() {
        VBox placeholder = new VBox(10);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.getStyleClass().add("empty-table-placeholder");
        
        Text title = new Text("No repositories added yet");
        title.getStyleClass().add("placeholder-title");
        
        Text instruction = new Text("Drag and drop repository folders or Git URLs here");
        instruction.getStyleClass().add("placeholder-instruction");
        
        Text examples = new Text("Supported: Local folders, GitHub/GitLab/Bitbucket URLs");
        examples.getStyleClass().add("placeholder-examples");
        
        placeholder.getChildren().addAll(title, instruction, examples);
        return placeholder;
    }
    
    /**
     * Check if a directory is a Git repository.
     */
    private boolean isGitRepository(File directory) {
        if (!directory.isDirectory()) {
            return false;
        }
        
        File gitDir = new File(directory, ".git");
        return gitDir.exists() && (gitDir.isDirectory() || gitDir.isFile());
    }
    
    /**
     * Validate if a string is a valid Git URL.
     */
    private boolean isValidGitUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        url = url.trim();
        
        // Check for common Git URL patterns
        return url.matches("^(https?|git|ssh)://.*\\.git$") ||
               url.matches("^git@[\\w.-]+:[\\w.-]+/[\\w.-]+\\.git$") ||
               url.matches("^https?://github\\.com/[\\w.-]+/[\\w.-]+/?$") ||
               url.matches("^https?://gitlab\\.com/[\\w.-]+/[\\w.-]+/?$") ||
               url.matches("^https?://bitbucket\\.org/[\\w.-]+/[\\w.-]+/?$");
    }
    
    /**
     * Get complexity text representation.
     */
    private String getComplexityText(int complexity) {
        switch (complexity) {
            case 1: return "Very Low";
            case 2: return "Low";
            case 3: return "Medium";
            case 4: return "High";
            case 5: return "Very High";
            default: return "Unknown";
        }
    }
    
    /**
     * Show validation error message.
     */
    private void showValidationError(String message) {
        if (onValidationError != null) {
            Platform.runLater(() -> onValidationError.accept(message));
        } else {
            logger.warn("Validation error: {}", message);
        }
    }
    
    // Setter methods for callbacks
    public void setOnFolderDropped(Consumer<File> callback) {
        this.onFolderDropped = callback;
    }
    
    public void setOnUrlDropped(Consumer<String> callback) {
        this.onUrlDropped = callback;
    }
    
    public void setOnValidationError(Consumer<String> callback) {
        this.onValidationError = callback;
    }
    
    /**
     * Custom cell for repository names with icons.
     */
    private static class RepositoryNameCell extends TableCell<RepositoryInfo, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                HBox container = new HBox(8);
                container.setAlignment(Pos.CENTER_LEFT);
                
                // Repository icon
                ImageView icon = new ImageView();
                icon.setFitWidth(16);
                icon.setFitHeight(16);
                try {
                    icon.setImage(new Image(getClass().getResourceAsStream("/icons/repository.png")));
                } catch (Exception e) {
                    // Fallback to text if icon not found
                    Text iconText = new Text("📁");
                    container.getChildren().add(iconText);
                }
                
                if (icon.getImage() != null) {
                    container.getChildren().add(icon);
                }
                
                // Repository name
                Label nameLabel = new Label(item);
                nameLabel.getStyleClass().add("repository-name");
                container.getChildren().add(nameLabel);
                
                setGraphic(container);
                setText(null);
            }
        }
    }
    
    /**
     * Custom cell for framework types with icons.
     */
    private static class FrameworkCell extends TableCell<RepositoryInfo, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                HBox container = new HBox(5);
                container.setAlignment(Pos.CENTER_LEFT);
                
                // Framework icon
                String iconText = getFrameworkIcon(item);
                Text icon = new Text(iconText);
                icon.getStyleClass().add("framework-icon");
                
                // Framework name
                Label nameLabel = new Label(item);
                nameLabel.getStyleClass().add("framework-name");
                
                container.getChildren().addAll(icon, nameLabel);
                setGraphic(container);
                setText(null);
            }
        }
        
        private String getFrameworkIcon(String framework) {
            switch (framework.toLowerCase()) {
                case "spring boot": return "🍃";
                case "react": return "⚛️";
                case "angular": return "🅰️";
                case "node.js": return "🟢";
                case "vue.js": return "💚";
                case "django": return "🐍";
                case "flask": return "🌶️";
                case "express": return "🚂";
                default: return "📦";
            }
        }
    }
    
    /**
     * Custom cell for complexity with visual indicators.
     */
    private static class ComplexityCell extends TableCell<RepositoryInfo, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                HBox container = new HBox(3);
                container.setAlignment(Pos.CENTER_LEFT);
                
                // Get complexity level from repository info
                RepositoryInfo repo = getTableRow().getItem();
                int complexity = repo != null ? repo.getEstimatedComplexity() : 0;
                
                // Add complexity dots
                for (int i = 1; i <= 5; i++) {
                    Text dot = new Text("●");
                    if (i <= complexity) {
                        dot.setFill(getComplexityColor(complexity));
                    } else {
                        dot.setFill(Color.LIGHTGRAY);
                    }
                    container.getChildren().add(dot);
                }
                
                // Add text label
                Label label = new Label(" " + item);
                label.getStyleClass().add("complexity-label");
                container.getChildren().add(label);
                
                setGraphic(container);
                setText(null);
            }
        }
        
        private Color getComplexityColor(int complexity) {
            switch (complexity) {
                case 1: return Color.GREEN;
                case 2: return Color.LIGHTGREEN;
                case 3: return Color.ORANGE;
                case 4: return Color.DARKORANGE;
                case 5: return Color.RED;
                default: return Color.GRAY;
            }
        }
    }
    
    /**
     * Custom cell for migration status with colored indicators.
     */
    private static class StatusCell extends TableCell<RepositoryInfo, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                HBox container = new HBox(5);
                container.setAlignment(Pos.CENTER_LEFT);
                
                // Status indicator
                Text indicator = new Text("●");
                indicator.setFill(getStatusColor(item));
                
                // Status text
                Label statusLabel = new Label(item);
                statusLabel.getStyleClass().add("status-label");
                
                container.getChildren().addAll(indicator, statusLabel);
                setGraphic(container);
                setText(null);
            }
        }
        
        private Color getStatusColor(String status) {
            switch (status.toLowerCase()) {
                case "ready": return Color.GRAY;
                case "processing": return Color.BLUE;
                case "completed": return Color.GREEN;
                case "failed": return Color.RED;
                case "queued": return Color.ORANGE;
                default: return Color.GRAY;
            }
        }
    }
}