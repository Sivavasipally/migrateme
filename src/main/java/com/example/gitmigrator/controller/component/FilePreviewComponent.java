package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.FileTreeItem;
import com.example.gitmigrator.model.GeneratedFile;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main component for file preview functionality
 * Combines file tree, tabbed editor, and diff viewer
 */
public class FilePreviewComponent extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(FilePreviewComponent.class);
    
    private final FileTreeComponent fileTree;
    private final TabbedFileEditorComponent tabbedEditor;
    private final DiffViewerComponent diffViewer;
    private SplitPane mainSplitPane;
    private SplitPane rightSplitPane;
    
    private final ObjectProperty<List<GeneratedFile>> generatedFiles = new SimpleObjectProperty<>();
    private final Map<String, GeneratedFile> fileMap = new HashMap<>();
    
    public FilePreviewComponent() {
        logger.debug("Initializing FilePreviewComponent");
        
        // Initialize components
        this.fileTree = new FileTreeComponent();
        this.tabbedEditor = new TabbedFileEditorComponent();
        this.diffViewer = new DiffViewerComponent();
        
        // Setup layout
        setupLayout();
        
        // Setup event handlers
        setupEventHandlers();
        
        logger.debug("FilePreviewComponent initialized successfully");
    }
    
    private void setupLayout() {
        // Create split panes
        rightSplitPane = new SplitPane();
        rightSplitPane.setOrientation(Orientation.VERTICAL);
        rightSplitPane.getItems().addAll(tabbedEditor, diffViewer);
        rightSplitPane.setDividerPositions(0.7);
        
        mainSplitPane = new SplitPane();
        mainSplitPane.setOrientation(Orientation.HORIZONTAL);
        mainSplitPane.getItems().addAll(fileTree, rightSplitPane);
        mainSplitPane.setDividerPositions(0.3);
        
        // Add toolbar
        ToolBar toolbar = createToolbar();
        
        // Set layout
        setTop(toolbar);
        setCenter(mainSplitPane);
    }
    
    private ToolBar createToolbar() {
        ToolBar toolbar = new ToolBar();
        
        Button expandAllBtn = new Button("Expand All");
        expandAllBtn.setOnAction(e -> fileTree.expandAll());
        
        Button collapseAllBtn = new Button("Collapse All");
        collapseAllBtn.setOnAction(e -> fileTree.collapseAll());
        
        Separator separator1 = new Separator();
        
        Button saveAllBtn = new Button("Save All");
        saveAllBtn.setOnAction(e -> saveAllFiles());
        
        Button revertAllBtn = new Button("Revert All");
        revertAllBtn.setOnAction(e -> revertAllFiles());
        
        Separator separator2 = new Separator();
        
        Label statusLabel = new Label("Ready");
        statusLabel.setId("status-label");
        
        toolbar.getItems().addAll(
            expandAllBtn, collapseAllBtn, separator1,
            saveAllBtn, revertAllBtn, separator2,
            statusLabel
        );
        
        return toolbar;
    }
    
    private void setupEventHandlers() {
        // File tree selection handler
        fileTree.selectedFileProperty().addListener((obs, oldFile, newFile) -> {
            if (newFile != null) {
                openFileInEditor(newFile);
                showDiffForFile(newFile);
            }
        });
        
        // Editor content change handler
        tabbedEditor.setOnContentChanged(this::handleContentChanged);
        
        // Generated files property listener
        generatedFiles.addListener((obs, oldFiles, newFiles) -> {
            updateFileMap(newFiles);
            fileTree.setFiles(newFiles);
        });
    }
    
    private void updateFileMap(List<GeneratedFile> files) {
        fileMap.clear();
        if (files != null) {
            files.forEach(file -> fileMap.put(file.getRelativePath(), file));
        }
    }
    
    private void openFileInEditor(GeneratedFile file) {
        logger.debug("Opening file in editor: {}", file.getFileName());
        tabbedEditor.openFile(file);
    }
    
    private void showDiffForFile(GeneratedFile file) {
        if (file.hasChanges()) {
            logger.debug("Showing diff for file: {}", file.getFileName());
            diffViewer.showDiff(file.getOriginalContent(), file.getContent(), file.getFileName());
        } else {
            diffViewer.clear();
        }
    }
    
    private void handleContentChanged(GeneratedFile file, String newContent) {
        logger.debug("Content changed for file: {}", file.getFileName());
        file.setContent(newContent);
        
        // Update diff viewer
        showDiffForFile(file);
        
        // Update file tree to show modified status
        fileTree.refreshFile(file);
        
        // Notify listeners about the change
        fireFileModified(file);
    }
    
    private void saveAllFiles() {
        logger.info("Saving all modified files");
        fileMap.values().stream()
            .filter(GeneratedFile::isModified)
            .forEach(file -> {
                // In a real implementation, this would save to disk
                file.setModified(false);
                logger.debug("Saved file: {}", file.getFileName());
            });
        
        // Refresh UI
        fileTree.refresh();
        diffViewer.clear();
    }
    
    private void revertAllFiles() {
        logger.info("Reverting all modified files");
        fileMap.values().stream()
            .filter(GeneratedFile::isModified)
            .forEach(file -> {
                file.revertChanges();
                logger.debug("Reverted file: {}", file.getFileName());
            });
        
        // Refresh UI
        tabbedEditor.refreshAllTabs();
        fileTree.refresh();
        diffViewer.clear();
    }
    
    private void fireFileModified(GeneratedFile file) {
        // This could be extended to notify external listeners
        logger.debug("File modified event fired for: {}", file.getFileName());
    }
    
    // Public API methods
    
    public void setGeneratedFiles(List<GeneratedFile> files) {
        this.generatedFiles.set(files);
    }
    
    public List<GeneratedFile> getGeneratedFiles() {
        return generatedFiles.get();
    }
    
    public ObjectProperty<List<GeneratedFile>> generatedFilesProperty() {
        return generatedFiles;
    }
    
    public void clear() {
        logger.debug("Clearing file preview component");
        fileTree.clear();
        tabbedEditor.closeAllTabs();
        diffViewer.clear();
        fileMap.clear();
    }
    
    public boolean hasModifiedFiles() {
        return fileMap.values().stream().anyMatch(GeneratedFile::isModified);
    }
    
    public void selectFile(String relativePath) {
        GeneratedFile file = fileMap.get(relativePath);
        if (file != null) {
            fileTree.selectFile(file);
        }
    }
}