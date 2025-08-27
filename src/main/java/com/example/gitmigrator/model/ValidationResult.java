package com.example.gitmigrator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of validation operations for generated artifacts.
 */
public class ValidationResult {
    
    private boolean isValid;
    private List<ValidationIssue> issues;
    private List<ValidationWarning> warnings;
    private String summary;
    private Map<String, Object> metrics;
    
    // Default constructor
    public ValidationResult() {
        this.issues = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.metrics = new HashMap<>();
        this.isValid = true;
    }
    
    // Constructor with validity
    public ValidationResult(boolean isValid) {
        this();
        this.isValid = isValid;
    }
    
    // Constructor with validity and summary
    public ValidationResult(boolean isValid, String summary) {
        this(isValid);
        this.summary = summary;
    }
    
    // Getters and Setters
    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }
    
    public List<ValidationIssue> getIssues() { return issues; }
    public void setIssues(List<ValidationIssue> issues) { this.issues = issues; }
    
    public List<ValidationWarning> getWarnings() { return warnings; }
    public void setWarnings(List<ValidationWarning> warnings) { this.warnings = warnings; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
    
    // Convenience methods
    public void addIssue(ValidationIssue issue) {
        this.issues.add(issue);
        this.isValid = false;
    }
    
    public void addIssue(String message, String file, int line) {
        addIssue(new ValidationIssue(message, file, line));
    }
    
    public void addWarning(ValidationWarning warning) {
        this.warnings.add(warning);
    }
    
    public void addWarning(String message, String file, int line) {
        addWarning(new ValidationWarning(message, file, line));
    }
    
    public void addMetric(String key, Object value) {
        this.metrics.put(key, value);
    }
    
    public boolean hasIssues() {
        return !issues.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    public int getIssueCount() {
        return issues.size();
    }
    
    public int getWarningCount() {
        return warnings.size();
    }
    
    @Override
    public String toString() {
        return "ValidationResult{" +
                "isValid=" + isValid +
                ", issues=" + issues.size() +
                ", warnings=" + warnings.size() +
                ", summary='" + summary + '\'' +
                '}';
    }
}