package com.example.gitmigrator.model;

/**
 * Represents a validation issue (error) found during artifact validation.
 */
public class ValidationIssue {
    
    public enum Severity {
        ERROR, WARNING, INFO
    }
    
    private String type;
    private String message;
    private String file;
    private int line;
    private int column;
    private Severity severity;
    private String suggestion;
    
    // Default constructor
    public ValidationIssue() {
        this.severity = Severity.ERROR;
    }
    
    // Constructor with severity, type, message, line, and column
    public ValidationIssue(Severity severity, String type, String message, int line, int column) {
        this.severity = severity;
        this.type = type;
        this.message = message;
        this.line = line;
        this.column = column;
    }
    
    // Constructor with type and message
    public ValidationIssue(String type, String message) {
        this();
        this.type = type;
        this.message = message;
    }
    
    // Constructor with message only
    public ValidationIssue(String message) {
        this();
        this.message = message;
    }
    
    // Constructor with message, file, and line
    public ValidationIssue(String message, String file, int line) {
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
    
    public int getColumn() { return column; }
    public void setColumn(int column) { this.column = column; }
    
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(severity).append(": ").append(message);
        if (file != null) {
            sb.append(" in ").append(file);
            if (line > 0) {
                sb.append(" at line ").append(line);
            }
        }
        return sb.toString();
    }
}