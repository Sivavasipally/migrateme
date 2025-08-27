package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.GeneratedFile;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Tabbed editor component with syntax highlighting for different file types
 */
public class TabbedFileEditorComponent extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(TabbedFileEditorComponent.class);
    
    private final TabPane tabPane;
    private final Map<String, EditorTab> openTabs = new HashMap<>();
    private BiConsumer<GeneratedFile, String> onContentChanged;
    
    public TabbedFileEditorComponent() {
        logger.debug("Initializing TabbedFileEditorComponent");
        
        // Initialize tab pane
        this.tabPane = new TabPane();
        setupTabPane();
        
        // Add to layout
        Label titleLabel = new Label("File Editor");
        titleLabel.getStyleClass().add("section-title");
        
        getChildren().addAll(titleLabel, tabPane);
        
        logger.debug("TabbedFileEditorComponent initialized successfully");
    }
    
    private void setupTabPane() {
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        
        // Handle tab close requests
        tabPane.getTabs().addListener((javafx.collections.ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    for (Tab removedTab : change.getRemoved()) {
                        if (removedTab instanceof EditorTab) {
                            EditorTab editorTab = (EditorTab) removedTab;
                            openTabs.remove(editorTab.getFile().getRelativePath());
                            logger.debug("Closed tab for file: {}", editorTab.getFile().getFileName());
                        }
                    }
                }
            }
        });
    }
    
    public void openFile(GeneratedFile file) {
        logger.debug("Opening file: {}", file.getFileName());
        
        String filePath = file.getRelativePath();
        EditorTab existingTab = openTabs.get(filePath);
        
        if (existingTab != null) {
            // Tab already exists, just select it
            tabPane.getSelectionModel().select(existingTab);
            return;
        }
        
        // Create new tab
        EditorTab newTab = new EditorTab(file);
        newTab.setOnContentChanged(this::handleContentChanged);
        
        openTabs.put(filePath, newTab);
        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().select(newTab);
        
        logger.debug("Created new tab for file: {}", file.getFileName());
    }
    
    public void closeFile(String relativePath) {
        EditorTab tab = openTabs.get(relativePath);
        if (tab != null) {
            tabPane.getTabs().remove(tab);
        }
    }
    
    public void closeAllTabs() {
        logger.debug("Closing all tabs");
        tabPane.getTabs().clear();
        openTabs.clear();
    }
    
    public void refreshAllTabs() {
        logger.debug("Refreshing all tabs");
        openTabs.values().forEach(EditorTab::refreshContent);
    }
    
    private void handleContentChanged(GeneratedFile file, String newContent) {
        if (onContentChanged != null) {
            onContentChanged.accept(file, newContent);
        }
    }
    
    public void setOnContentChanged(BiConsumer<GeneratedFile, String> handler) {
        this.onContentChanged = handler;
    }
    
    /**
     * Individual editor tab with syntax highlighting
     */
    private static class EditorTab extends Tab {
        private final GeneratedFile file;
        private final CodeArea codeArea;
        private final ValidationIndicatorComponent validationIndicator;
        private BiConsumer<GeneratedFile, String> onContentChanged;
        private boolean ignoreChanges = false;
        private String savedContent;
        
        public EditorTab(GeneratedFile file) {
            super(file.getFileName());
            this.file = file;
            this.codeArea = new CodeArea();
            this.validationIndicator = new ValidationIndicatorComponent();
            this.savedContent = file.getContent();
            
            setupEditor();
            setupLayout();
            
            // Update tab title if file is modified
            updateTabTitle();
        }
        
        private void setupEditor() {
            // Add line numbers
            codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
            
            // Set initial content
            codeArea.replaceText(0, 0, file.getContent());
            
            // Apply syntax highlighting based on file type
            applySyntaxHighlighting();
            
            // Listen for content changes
            codeArea.textProperty().addListener((obs, oldText, newText) -> {
                if (!ignoreChanges) {
                    // Update file content
                    if (onContentChanged != null) {
                        onContentChanged.accept(file, newText);
                    }
                    
                    // Update tab title
                    updateTabTitle();
                    
                    // Validate content in real-time
                    validateContent();
                }
            });
            
            // Configure editor behavior
            codeArea.setWrapText(false);
            codeArea.getStyleClass().add("code-editor");
            
            // Add keyboard shortcuts
            setupKeyboardShortcuts();
        }
        
        private void setupLayout() {
            VBox container = new VBox();
            
            // Create toolbar
            ToolBar toolbar = createToolbar();
            
            // Add components to container
            container.getChildren().addAll(toolbar, codeArea, validationIndicator);
            VBox.setVgrow(codeArea, Priority.ALWAYS);
            
            setContent(container);
        }
        
        private ToolBar createToolbar() {
            ToolBar toolbar = new ToolBar();
            
            Button saveBtn = new Button("Save");
            saveBtn.setOnAction(e -> saveContent());
            saveBtn.getStyleClass().add("config-button");
            
            Button revertBtn = new Button("Revert");
            revertBtn.setOnAction(e -> revertContent());
            revertBtn.getStyleClass().add("config-button-danger");
            
            Label statusLabel = new Label();
            statusLabel.textProperty().bind(
                javafx.beans.binding.Bindings.createStringBinding(
                    () -> hasUnsavedChanges() ? "Modified" : "Saved",
                    codeArea.textProperty()
                )
            );
            statusLabel.getStyleClass().add("status-label");
            
            toolbar.getItems().addAll(saveBtn, revertBtn, new Separator(), statusLabel);
            return toolbar;
        }
        
        private void setupKeyboardShortcuts() {
            codeArea.setOnKeyPressed(event -> {
                if (event.isControlDown()) {
                    switch (event.getCode()) {
                        case S:
                            saveContent();
                            event.consume();
                            break;
                        case Z:
                            if (event.isShiftDown()) {
                                codeArea.redo();
                            } else {
                                codeArea.undo();
                            }
                            event.consume();
                            break;
                    }
                }
            });
        }
        
        private void validateContent() {
            // This would integrate with FileContentValidationService
            // For now, we'll do basic validation
            String content = codeArea.getText();
            
            // Simple validation example
            List<String> issues = new ArrayList<>();
            
            if (content.trim().isEmpty()) {
                issues.add("File is empty");
            }
            
            // File-type specific validation
            switch (file.getFileType()) {
                case DOCKERFILE:
                    if (!content.toUpperCase().contains("FROM")) {
                        issues.add("Dockerfile should contain a FROM instruction");
                    }
                    break;
                case JSON:
                    try {
                        new com.fasterxml.jackson.databind.ObjectMapper().readTree(content);
                    } catch (Exception e) {
                        issues.add("Invalid JSON syntax: " + e.getMessage());
                    }
                    break;
            }
            
            validationIndicator.setIssues(issues);
        }
        
        private void saveContent() {
            savedContent = codeArea.getText();
            file.setModified(false);
            updateTabTitle();
            logger.debug("Saved content for file: {}", file.getFileName());
        }
        
        private void revertContent() {
            ignoreChanges = true;
            codeArea.replaceText(0, codeArea.getLength(), savedContent);
            file.setContent(savedContent);
            file.setModified(false);
            ignoreChanges = false;
            updateTabTitle();
            validateContent();
            logger.debug("Reverted content for file: {}", file.getFileName());
        }
        
        private boolean hasUnsavedChanges() {
            return !codeArea.getText().equals(savedContent);
        }
        
        private void applySyntaxHighlighting() {
            // Basic syntax highlighting based on file type
            switch (file.getFileType()) {
                case DOCKERFILE:
                    applyDockerfileSyntaxHighlighting();
                    break;
                case YAML:
                    applyYamlSyntaxHighlighting();
                    break;
                case JSON:
                    applyJsonSyntaxHighlighting();
                    break;
                case XML:
                    applyXmlSyntaxHighlighting();
                    break;
                case SHELL:
                    applyShellSyntaxHighlighting();
                    break;
                default:
                    // No specific highlighting for plain text
                    break;
            }
        }
        
        private void applyDockerfileSyntaxHighlighting() {
            // Simple Dockerfile syntax highlighting
            codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .subscribe(change -> {
                    codeArea.setStyleSpans(0, SyntaxHighlighter.computeDockerfileHighlighting(codeArea.getText()));
                });
        }
        
        private void applyYamlSyntaxHighlighting() {
            // Simple YAML syntax highlighting
            codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .subscribe(change -> {
                    codeArea.setStyleSpans(0, SyntaxHighlighter.computeYamlHighlighting(codeArea.getText()));
                });
        }
        
        private void applyJsonSyntaxHighlighting() {
            // Simple JSON syntax highlighting
            codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .subscribe(change -> {
                    codeArea.setStyleSpans(0, SyntaxHighlighter.computeJsonHighlighting(codeArea.getText()));
                });
        }
        
        private void applyXmlSyntaxHighlighting() {
            // Simple XML syntax highlighting
            codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .subscribe(change -> {
                    codeArea.setStyleSpans(0, SyntaxHighlighter.computeXmlHighlighting(codeArea.getText()));
                });
        }
        
        private void applyShellSyntaxHighlighting() {
            // Simple shell script syntax highlighting
            codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .subscribe(change -> {
                    codeArea.setStyleSpans(0, SyntaxHighlighter.computeShellHighlighting(codeArea.getText()));
                });
        }
        
        private void updateTabTitle() {
            String title = file.getFileName();
            if (file.isModified()) {
                title += " *";
            }
            setText(title);
        }
        
        public void refreshContent() {
            ignoreChanges = true;
            codeArea.replaceText(0, codeArea.getLength(), file.getContent());
            ignoreChanges = false;
            updateTabTitle();
        }
        
        public GeneratedFile getFile() {
            return file;
        }
        
        public void setOnContentChanged(BiConsumer<GeneratedFile, String> handler) {
            this.onContentChanged = handler;
        }
    }
}