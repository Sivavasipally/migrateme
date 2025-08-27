package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.FileTreeItem;
import com.example.gitmigrator.model.GeneratedFile;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * File tree component for displaying generated files in a hierarchical structure
 */
public class FileTreeComponent extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(FileTreeComponent.class);
    
    private final TreeView<String> treeView;
    private final ObjectProperty<GeneratedFile> selectedFile = new SimpleObjectProperty<>();
    private final Map<String, FileTreeItem> itemMap = new HashMap<>();
    
    public FileTreeComponent() {
        logger.debug("Initializing FileTreeComponent");
        
        // Initialize tree view
        this.treeView = new TreeView<>();
        setupTreeView();
        
        // Add to layout
        Label titleLabel = new Label("Generated Files");
        titleLabel.getStyleClass().add("section-title");
        
        getChildren().addAll(titleLabel, treeView);
        
        logger.debug("FileTreeComponent initialized successfully");
    }
    
    private void setupTreeView() {
        // Create root item
        FileTreeItem rootItem = new FileTreeItem("Generated Files", "");
        rootItem.setExpanded(true);
        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
        
        // Setup cell factory for custom rendering
        treeView.setCellFactory(tv -> new FileTreeCell());
        
        // Selection handler
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem instanceof FileTreeItem) {
                FileTreeItem fileItem = (FileTreeItem) newItem;
                if (!fileItem.isDirectory() && fileItem.getGeneratedFile() != null) {
                    selectedFile.set(fileItem.getGeneratedFile());
                }
            }
        });
    }
    
    public void setFiles(List<GeneratedFile> files) {
        logger.debug("Setting {} files in tree", files != null ? files.size() : 0);
        
        clear();
        
        if (files == null || files.isEmpty()) {
            return;
        }
        
        // Build tree structure
        Map<String, FileTreeItem> directoryItems = new HashMap<>();
        FileTreeItem root = (FileTreeItem) treeView.getRoot();
        
        for (GeneratedFile file : files) {
            String relativePath = file.getRelativePath();
            String[] pathParts = relativePath.split("/");
            
            FileTreeItem parentItem = root;
            StringBuilder currentPath = new StringBuilder();
            
            // Create directory structure
            for (int i = 0; i < pathParts.length - 1; i++) {
                if (currentPath.length() > 0) {
                    currentPath.append("/");
                }
                currentPath.append(pathParts[i]);
                
                String dirPath = currentPath.toString();
                FileTreeItem dirItem = directoryItems.get(dirPath);
                
                if (dirItem == null) {
                    dirItem = new FileTreeItem(pathParts[i], dirPath);
                    directoryItems.put(dirPath, dirItem);
                    parentItem.getChildren().add(dirItem);
                    dirItem.setExpanded(true);
                }
                
                parentItem = dirItem;
            }
            
            // Add file item
            FileTreeItem fileItem = new FileTreeItem(file);
            parentItem.getChildren().add(fileItem);
            itemMap.put(relativePath, fileItem);
        }
        
        // Sort items
        sortTreeItems(root);
        
        logger.debug("File tree populated with {} files", files.size());
    }
    
    private void sortTreeItems(FileTreeItem item) {
        if (item.getChildren().isEmpty()) {
            return;
        }
        
        // Sort children: directories first, then files, both alphabetically
        item.getChildren().sort((a, b) -> {
            FileTreeItem itemA = (FileTreeItem) a;
            FileTreeItem itemB = (FileTreeItem) b;
            
            if (itemA.isDirectory() && !itemB.isDirectory()) {
                return -1;
            } else if (!itemA.isDirectory() && itemB.isDirectory()) {
                return 1;
            } else {
                return itemA.getValue().compareToIgnoreCase(itemB.getValue());
            }
        });
        
        // Recursively sort children
        for (TreeItem<String> child : item.getChildren()) {
            sortTreeItems((FileTreeItem) child);
        }
    }
    
    public void clear() {
        logger.debug("Clearing file tree");
        FileTreeItem root = (FileTreeItem) treeView.getRoot();
        root.getChildren().clear();
        itemMap.clear();
        selectedFile.set(null);
    }
    
    public void expandAll() {
        logger.debug("Expanding all tree items");
        expandTreeItem((FileTreeItem) treeView.getRoot());
    }
    
    public void collapseAll() {
        logger.debug("Collapsing all tree items");
        collapseTreeItem((FileTreeItem) treeView.getRoot());
    }
    
    private void expandTreeItem(FileTreeItem item) {
        item.setExpanded(true);
        for (TreeItem<String> child : item.getChildren()) {
            expandTreeItem((FileTreeItem) child);
        }
    }
    
    private void collapseTreeItem(FileTreeItem item) {
        item.setExpanded(false);
        for (TreeItem<String> child : item.getChildren()) {
            collapseTreeItem((FileTreeItem) child);
        }
    }
    
    public void selectFile(GeneratedFile file) {
        FileTreeItem item = itemMap.get(file.getRelativePath());
        if (item != null) {
            treeView.getSelectionModel().select(item);
            
            // Ensure item is visible
            TreeItem<String> parent = item.getParent();
            while (parent != null) {
                parent.setExpanded(true);
                parent = parent.getParent();
            }
        }
    }
    
    public void refreshFile(GeneratedFile file) {
        FileTreeItem item = itemMap.get(file.getRelativePath());
        if (item != null) {
            // Trigger cell refresh by updating the item
            treeView.refresh();
        }
    }
    
    public void refresh() {
        treeView.refresh();
    }
    
    public ObjectProperty<GeneratedFile> selectedFileProperty() {
        return selectedFile;
    }
    
    /**
     * Custom tree cell for rendering file items with icons and status indicators
     */
    private static class FileTreeCell extends TreeCell<String> {
        
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                getStyleClass().removeAll("file-modified", "file-new");
                return;
            }
            
            FileTreeItem treeItem = (FileTreeItem) getTreeItem();
            if (treeItem == null) {
                setText(item);
                setGraphic(null);
                return;
            }
            
            setText(item);
            
            // Set icon based on type
            ImageView icon = getIconForItem(treeItem);
            setGraphic(icon);
            
            // Apply styles based on file status
            getStyleClass().removeAll("file-modified", "file-new");
            if (!treeItem.isDirectory()) {
                if (treeItem.isModified()) {
                    getStyleClass().add("file-modified");
                } else if (treeItem.isNew()) {
                    getStyleClass().add("file-new");
                }
            }
        }
        
        private ImageView getIconForItem(FileTreeItem item) {
            String iconPath;
            
            if (item.isDirectory()) {
                iconPath = "/icons/folder.png";
            } else {
                GeneratedFile file = item.getGeneratedFile();
                if (file != null) {
                    switch (file.getFileType()) {
                        case DOCKERFILE:
                            iconPath = "/icons/docker.png";
                            break;
                        case YAML:
                            iconPath = "/icons/yaml.png";
                            break;
                        case JSON:
                            iconPath = "/icons/json.png";
                            break;
                        case XML:
                            iconPath = "/icons/xml.png";
                            break;
                        case SHELL:
                            iconPath = "/icons/shell.png";
                            break;
                        default:
                            iconPath = "/icons/file.png";
                            break;
                    }
                } else {
                    iconPath = "/icons/file.png";
                }
            }
            
            try {
                // For now, create a simple colored rectangle as placeholder
                // In a real implementation, you would load actual icon images
                ImageView imageView = new ImageView();
                imageView.setFitWidth(16);
                imageView.setFitHeight(16);
                return imageView;
            } catch (Exception e) {
                // Return empty ImageView if icon loading fails
                return new ImageView();
            }
        }
    }
}