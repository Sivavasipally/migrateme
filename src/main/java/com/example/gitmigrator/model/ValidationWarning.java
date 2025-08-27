package com.example.gitmigrator.model;

/**
 * Represents a validation warning found during artifact validation.
 */
public class ValidationWarning {
    
    private String type;
    private String message;
    private String file;
    private int line;
    private String category;
    private String suggestion;
    
    // Default constructor
    public ValidationWarning() {}
    
    // Constructor with type and message
    public ValidationWarning(String type, String message) {
        this.type = type;
        this.message = message;
    }
    
    // Constructor with message only
    public ValidationWarning(String message) {
        this.message = message;
    }
    
    // Constructor with message, file, and line
    public ValidationWarning(String message, String file, int line) {
        this(message);
        this.file = file;
        this.line = line;
    }
    
    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }
    
    public int getLine() { return line; }
    public void setLine(int line) { this.line = line; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WARNING: ").append(message);
        if (file != null) {
            sb.append(" in ").append(file);
            if (line > 0) {
                sb.append(" at line ").append(line);
            }
        }
        return sb.toString();
    }
}