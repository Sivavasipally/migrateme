package com.example.gitmigrator.model;

import java.time.LocalDateTime;

/**
 * Represents a generated file with its content and metadata
 */
public class GeneratedFile {
    private String fileName;
    private String relativePath;
    private String content;
    private String originalContent; // For diff comparison
    private FileType fileType;
    private boolean isModified;
    private boolean isNew;
    private LocalDateTime generatedAt;
    
    public GeneratedFile() {
        this.generatedAt = LocalDateTime.now();
        this.isModified = false;
        this.isNew = true;
    }
    
    public GeneratedFile(String fileName, String relativePath, String content, FileType fileType) {
        this();
        this.fileName = fileName;
        this.relativePath = relativePath;
        this.content = content;
        this.fileType = fileType;
    }
    
    // Getters and setters
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getRelativePath() {
        return relativePath;
    }
    
    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
        this.isModified = true;
    }
    
    public String getOriginalContent() {
        return originalContent;
    }
    
    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
        this.isNew = false;
    }
    
    public FileType getFileType() {
        return fileType;
    }
    
    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }
    
    public boolean isModified() {
        return isModified;
    }
    
    public void setModified(boolean modified) {
        this.isModified = modified;
    }
    
    public boolean isNew() {
        return isNew;
    }
    
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    /**
     * Check if this file has changes compared to original
     */
    public boolean hasChanges() {
        if (isNew) {
            return true;
        }
        return originalContent != null && !originalContent.equals(content);
    }
    
    /**
     * Reset content to original state
     */
    public void revertChanges() {
        if (originalContent != null) {
            this.content = originalContent;
            this.isModified = false;
        }
    }
    
    public enum FileType {
        DOCKERFILE("dockerfile", "text/x-dockerfile"),
        YAML("yaml", "text/x-yaml"),
        JSON("json", "application/json"),
        XML("xml", "text/xml"),
        SHELL("sh", "text/x-sh"),
        PROPERTIES("properties", "text/x-properties"),
        TEXT("txt", "text/plain");
        
        private final String extension;
        private final String mimeType;
        
        FileType(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }
        
        public String getExtension() {
            return extension;
        }
        
        public String getMimeType() {
            return mimeType;
        }
        
        public static FileType fromFileName(String fileName) {
            if (fileName == null) return TEXT;
            
            String lowerName = fileName.toLowerCase();
            if (lowerName.equals("dockerfile") || lowerName.endsWith(".dockerfile")) {
                return DOCKERFILE;
            } else if (lowerName.endsWith(".yaml") || lowerName.endsWith(".yml")) {
                return YAML;
            } else if (lowerName.endsWith(".json")) {
                return JSON;
            } else if (lowerName.endsWith(".xml")) {
                return XML;
            } else if (lowerName.endsWith(".sh") || lowerName.endsWith(".bash")) {
                return SHELL;
            } else if (lowerName.endsWith(".properties")) {
                return PROPERTIES;
            }
            return TEXT;
        }
    }
}