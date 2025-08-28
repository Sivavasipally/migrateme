package com.example.gitmigrator.model;

import javafx.scene.control.TreeItem;

/**
 * Custom TreeItem for file tree representation
 */
public class FileTreeItem extends TreeItem<String> {
    private final boolean isDirectory;
    private GeneratedFile generatedFile;
    private final String fullPath;
    
    // Constructor for directory
    public FileTreeItem(String name, String fullPath) {
        super(name);
        this.isDirectory = true;
        this.generatedFile = null;
        this.fullPath = fullPath;
    }
    
    // Constructor for directory (wizard)
    public FileTreeItem(String name, boolean isDirectory) {
        super(name);
        this.isDirectory = isDirectory;
        this.generatedFile = null;
        this.fullPath = name;
    }
    
    // Constructor for file
    public FileTreeItem(GeneratedFile file) {
        super(file.getFileName());
        this.isDirectory = false;
        this.generatedFile = file;
        this.fullPath = file.getRelativePath();
    }
    
    public String getName() {
        return getValue();
    }
    
    public boolean isDirectory() {
        return isDirectory;
    }
    
    public GeneratedFile getGeneratedFile() {
        return generatedFile;
    }
    
    public void setGeneratedFile(GeneratedFile generatedFile) {
        this.generatedFile = generatedFile;
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