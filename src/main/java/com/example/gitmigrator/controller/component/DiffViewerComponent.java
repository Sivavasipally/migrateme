package com.example.gitmigrator.controller.component;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Component for displaying differences between original and modified file content
 */
public class DiffViewerComponent extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(DiffViewerComponent.class);
    
    private final Label titleLabel;
    private final HBox diffContainer;
    private final CodeArea originalArea;
    private final CodeArea modifiedArea;
    private final ScrollPane scrollPane;
    
    public DiffViewerComponent() {
        logger.debug("Initializing DiffViewerComponent");
        
        // Initialize components
        this.titleLabel = new Label("Diff Viewer");
        this.titleLabel.getStyleClass().add("section-title");
        
        this.originalArea = new CodeArea();
        this.modifiedArea = new CodeArea();
        
        setupCodeAreas();
        
        // Create diff container
        this.diffContainer = new HBox();
        setupDiffContainer();
        
        // Wrap in scroll pane
        this.scrollPane = new ScrollPane(diffContainer);
        setupScrollPane();
        
        // Add to layout
        getChildren().addAll(titleLabel, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Initially hidden
        setVisible(false);
        setManaged(false);
        
        logger.debug("DiffViewerComponent initialized successfully");
    }
    
    private void setupCodeAreas() {
        // Configure original area
        originalArea.setEditable(false);
        originalArea.getStyleClass().addAll("code-editor", "diff-original");
        
        // Configure modified area
        modifiedArea.setEditable(false);
        modifiedArea.getStyleClass().addAll("code-editor", "diff-modified");
        
        // Synchronize scrolling (RichTextFX doesn't have scrollYProperty, we'll handle this differently)
        // For now, we'll skip scroll synchronization as it requires more complex implementation
    }
    
    private void setupDiffContainer() {
        // Create labels for each side
        VBox originalContainer = new VBox();
        Label originalLabel = new Label("Original");
        originalLabel.getStyleClass().add("diff-label");
        originalContainer.getChildren().addAll(originalLabel, originalArea);
        VBox.setVgrow(originalArea, Priority.ALWAYS);
        
        VBox modifiedContainer = new VBox();
        Label modifiedLabel = new Label("Modified");
        modifiedLabel.getStyleClass().add("diff-label");
        modifiedContainer.getChildren().addAll(modifiedLabel, modifiedArea);
        VBox.setVgrow(modifiedArea, Priority.ALWAYS);
        
        // Add to diff container
        diffContainer.getChildren().addAll(originalContainer, modifiedContainer);
        HBox.setHgrow(originalContainer, Priority.ALWAYS);
        HBox.setHgrow(modifiedContainer, Priority.ALWAYS);
        
        diffContainer.setSpacing(5);
        diffContainer.setPadding(new Insets(5));
    }
    
    private void setupScrollPane() {
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("diff-scroll-pane");
    }
    
    public void showDiff(String originalContent, String modifiedContent, String fileName) {
        logger.debug("Showing diff for file: {}", fileName);
        
        if (originalContent == null) {
            originalContent = "";
        }
        if (modifiedContent == null) {
            modifiedContent = "";
        }
        
        // Update title
        titleLabel.setText("Diff Viewer - " + fileName);
        
        // Compute and display diff
        DiffResult diffResult = computeDiff(originalContent, modifiedContent);
        displayDiff(diffResult);
        
        // Show the component
        setVisible(true);
        setManaged(true);
        
        logger.debug("Diff displayed for file: {}", fileName);
    }
    
    public void clear() {
        logger.debug("Clearing diff viewer");
        
        titleLabel.setText("Diff Viewer");
        originalArea.clear();
        modifiedArea.clear();
        
        // Hide the component
        setVisible(false);
        setManaged(false);
    }
    
    private DiffResult computeDiff(String original, String modified) {
        // Simple line-by-line diff implementation
        String[] originalLines = original.split("\n", -1);
        String[] modifiedLines = modified.split("\n", -1);
        
        List<DiffLine> originalDiffLines = new ArrayList<>();
        List<DiffLine> modifiedDiffLines = new ArrayList<>();
        
        // Simple diff algorithm - can be enhanced with more sophisticated algorithms
        int maxLines = Math.max(originalLines.length, modifiedLines.length);
        
        for (int i = 0; i < maxLines; i++) {
            String originalLine = i < originalLines.length ? originalLines[i] : "";
            String modifiedLine = i < modifiedLines.length ? modifiedLines[i] : "";
            
            DiffLineType type;
            if (i >= originalLines.length) {
                type = DiffLineType.ADDED;
            } else if (i >= modifiedLines.length) {
                type = DiffLineType.REMOVED;
            } else if (originalLine.equals(modifiedLine)) {
                type = DiffLineType.UNCHANGED;
            } else {
                type = DiffLineType.MODIFIED;
            }
            
            originalDiffLines.add(new DiffLine(originalLine, type, i + 1));
            modifiedDiffLines.add(new DiffLine(modifiedLine, type, i + 1));
        }
        
        return new DiffResult(originalDiffLines, modifiedDiffLines);
    }
    
    private void displayDiff(DiffResult diffResult) {
        // Build styled content for original area
        StringBuilder originalContent = new StringBuilder();
        StringBuilder modifiedContent = new StringBuilder();
        
        for (int i = 0; i < diffResult.originalLines.size(); i++) {
            DiffLine originalLine = diffResult.originalLines.get(i);
            DiffLine modifiedLine = diffResult.modifiedLines.get(i);
            
            originalContent.append(originalLine.content);
            modifiedContent.append(modifiedLine.content);
            
            if (i < diffResult.originalLines.size() - 1) {
                originalContent.append("\n");
                modifiedContent.append("\n");
            }
        }
        
        // Set content
        originalArea.replaceText(originalContent.toString());
        modifiedArea.replaceText(modifiedContent.toString());
        
        // Apply diff styling
        applyDiffStyling(originalArea, diffResult.originalLines);
        applyDiffStyling(modifiedArea, diffResult.modifiedLines);
    }
    
    private void applyDiffStyling(CodeArea codeArea, List<DiffLine> diffLines) {
        // Apply paragraph styles based on diff type
        for (int i = 0; i < diffLines.size(); i++) {
            DiffLine line = diffLines.get(i);
            String styleClass = getDiffStyleClass(line.type);
            
            if (!styleClass.isEmpty()) {
                // Apply style to the paragraph using Collections.singleton
                codeArea.setParagraphStyle(i, java.util.Collections.singleton(styleClass));
            }
        }
    }
    
    private String getDiffStyleClass(DiffLineType type) {
        switch (type) {
            case ADDED:
                return "diff-added";
            case REMOVED:
                return "diff-removed";
            case MODIFIED:
                return "diff-modified";
            case UNCHANGED:
            default:
                return "";
        }
    }
    
    /**
     * Represents the result of a diff computation
     */
    private static class DiffResult {
        final List<DiffLine> originalLines;
        final List<DiffLine> modifiedLines;
        
        DiffResult(List<DiffLine> originalLines, List<DiffLine> modifiedLines) {
            this.originalLines = originalLines;
            this.modifiedLines = modifiedLines;
        }
    }
    
    /**
     * Represents a single line in a diff
     */
    private static class DiffLine {
        final String content;
        final DiffLineType type;
        final int lineNumber;
        
        DiffLine(String content, DiffLineType type, int lineNumber) {
            this.content = content;
            this.type = type;
            this.lineNumber = lineNumber;
        }
    }
    
    /**
     * Types of diff lines
     */
    private enum DiffLineType {
        UNCHANGED,
        ADDED,
        REMOVED,
        MODIFIED
    }
}