package com.example.gitmigrator.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of credential validation operations.
 * Contains validation status, errors, warnings, and informational messages.
 */
public class CredentialValidationResult {
    
    private final boolean isValid;
    private final List<String> errors;
    private final List<String> warnings;
    private final List<String> infoMessages;
    
    private CredentialValidationResult(Builder builder) {
        this.errors = new ArrayList<>(builder.errors);
        this.warnings = new ArrayList<>(builder.warnings);
        this.infoMessages = new ArrayList<>(builder.infoMessages);
        this.isValid = this.errors.isEmpty();
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    public List<String> getInfoMessages() {
        return new ArrayList<>(infoMessages);
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    public boolean hasInfoMessages() {
        return !infoMessages.isEmpty();
    }
    
    /**
     * Gets the first error message, if any.
     */
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }
    
    /**
     * Gets all messages combined into a single string.
     */
    public String getAllMessages() {
        StringBuilder sb = new StringBuilder();
        
        if (hasErrors()) {
            sb.append("Errors:\n");
            for (String error : errors) {
                sb.append("• ").append(error).append("\n");
            }
        }
        
        if (hasWarnings()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Warnings:\n");
            for (String warning : warnings) {
                sb.append("• ").append(warning).append("\n");
            }
        }
        
        if (hasInfoMessages()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Information:\n");
            for (String info : infoMessages) {
                sb.append("• ").append(info).append("\n");
            }
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Gets a summary of the validation result.
     */
    public String getSummary() {
        if (isValid && !hasWarnings()) {
            return "Validation successful";
        } else if (isValid && hasWarnings()) {
            return "Validation successful with " + warnings.size() + " warning(s)";
        } else {
            return "Validation failed with " + errors.size() + " error(s)";
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static CredentialValidationResult valid() {
        return new Builder().build();
    }
    
    public static CredentialValidationResult invalid(String error) {
        return new Builder().addError(error).build();
    }
    
    public static CredentialValidationResult withWarning(String warning) {
        return new Builder().addWarning(warning).build();
    }
    
    public static class Builder {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> infoMessages = new ArrayList<>();
        
        public Builder addError(String error) {
            if (error != null && !error.trim().isEmpty()) {
                errors.add(error.trim());
            }
            return this;
        }
        
        public Builder addWarning(String warning) {
            if (warning != null && !warning.trim().isEmpty()) {
                warnings.add(warning.trim());
            }
            return this;
        }
        
        public Builder addInfo(String info) {
            if (info != null && !info.trim().isEmpty()) {
                infoMessages.add(info.trim());
            }
            return this;
        }
        
        public Builder addErrors(List<String> errors) {
            if (errors != null) {
                for (String error : errors) {
                    addError(error);
                }
            }
            return this;
        }
        
        public Builder addWarnings(List<String> warnings) {
            if (warnings != null) {
                for (String warning : warnings) {
                    addWarning(warning);
                }
            }
            return this;
        }
        
        public Builder addInfoMessages(List<String> infoMessages) {
            if (infoMessages != null) {
                for (String info : infoMessages) {
                    addInfo(info);
                }
            }
            return this;
        }
        
        public CredentialValidationResult build() {
            return new CredentialValidationResult(this);
        }
    }
    
    @Override
    public String toString() {
        return "CredentialValidationResult{" +
                "isValid=" + isValid +
                ", errors=" + errors.size() +
                ", warnings=" + warnings.size() +
                ", infoMessages=" + infoMessages.size() +
                '}';
    }
}