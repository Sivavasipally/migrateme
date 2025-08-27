package com.example.gitmigrator.model;

import javafx.scene.control.TreeItem;

/**
 * Custom TreeItem for file tree representation
 */
public class FileTreeItem extends TreeItem<String> {
    private final boolean isDirectory;
    private final GeneratedFile generatedFile;
    private final String fullPath;
    
    // Constructor for directory
    public FileTreeItem(String name, String fullPath) {
        super(name);
        this.isDirectory = true;
        this.generatedFile = null;
        this.fullPath = fullPath;
    }
    
    // Constructor for file
    public FileTreeItem(GeneratedFile file) {
        super(file.getFileName());
        this.isDirectory = false;
        this.generatedFile = file;
        this.fullPath = file.getRelativePath();
    }
    
    public boolean isDirectory() {
        return isDirectory;
    }
    
    public GeneratedFile getGeneratedFile() {
        return generatedFile;
    }
    
    public String getFullPath() {
        return fullPath;
    }
    
    public boolean isModified() {
        return !isDirectory && generatedFile != null && generatedFile.isModified();
    }
    
    public boolean isNew() {
        return !isDirectory && generatedFile != null && generatedFile.isNew();
    }
}