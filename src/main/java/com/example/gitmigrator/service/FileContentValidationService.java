package com.example.gitmigrator.service;

import com.example.gitmigrator.model.GeneratedFile;
import com.example.gitmigrator.model.ValidationIssue;
import com.example.gitmigrator.model.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for validating file content in real-time during editing
 */
public class FileContentValidationService {
    private static final Logger logger = LoggerFactory.getLogger(FileContentValidationService.class);
    
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    /**
     * Validate file content based on file type
     */
    public ValidationResult validateContent(GeneratedFile file) {
        logger.debug("Validating content for file: {} (type: {})", file.getFileName(), file.getFileType());
        
        List<ValidationIssue> issues = new ArrayList<>();
        
        try {
            switch (file.getFileType()) {
                case DOCKERFILE:
                    issues.addAll(validateDockerfile(file.getContent()));
                    break;
                case YAML:
                    issues.addAll(validateYaml(file.getContent()));
                    break;
                case JSON:
                    issues.addAll(validateJson(file.getContent()));
                    break;
                case XML:
                    issues.addAll(validateXml(file.getContent()));
                    break;
                case SHELL:
                    issues.addAll(validateShellScript(file.getContent()));
                    break;
                case PROPERTIES:
                    issues.addAll(validateProperties(file.getContent()));
                    break;
                default:
                    // No specific validation for plain text
                    break;
            }
        } catch (Exception e) {
            logger.error("Error validating file content: {}", e.getMessage(), e);
            issues.add(new ValidationIssue(
                ValidationIssue.Severity.ERROR,
                "Validation Error",
                "Failed to validate file: " + e.getMessage(),
                1, 1
            ));
        }
        
        boolean isValid = issues.stream().noneMatch(issue -> issue.getSeverity() == ValidationIssue.Severity.ERROR);
        

        ValidationResult result = new ValidationResult();
        result.setValid(isValid);
        result.setIssues(issues);
        result.setSummary(String.format("Found %d issues (%d errors, %d warnings)", 
            issues.size(),
            (int) issues.stream().filter(i -> i.getSeverity() == ValidationIssue.Severity.ERROR).count(),
            (int) issues.stream().filter(i -> i.getSeverity() == ValidationIssue.Severity.WARNING).count()
        ));
        
        logger.debug("Validation completed for {}: {} issues found", file.getFileName(), issues.size());
        return result;
    }
    
    private List<ValidationIssue> validateDockerfile(String content) {
        List<ValidationIssue> issues = new ArrayList<>();
        String[] lines = content.split("\n");
        
        boolean hasFrom = false;
        boolean hasExpose = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNumber = i + 1;
            
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // Check for FROM instruction
            if (line.toUpperCase().startsWith("FROM")) {
                hasFrom = true;
                
                // Validate FROM syntax
                if (!line.matches("(?i)^FROM\\s+\\S+(:\\S+)?\\s*(AS\\s+\\S+)?$")) {
                    issues.add(new ValidationIssue(
                        ValidationIssue.Severity.ERROR,
                        "Invalid FROM syntax",
                        "FROM instruction should be: FROM image[:tag] [AS name]",
                        lineNumber, 1
                    ));
                }
            }
            
            // Check for EXPOSE instruction
            if (line.toUpperCase().startsWith("EXPOSE")) {
                hasExpose = true;
                
                // Validate EXPOSE syntax
                if (!line.matches("(?i)^EXPOSE\\s+\\d+(\\s+\\d+)*$")) {
                    issues.add(new ValidationIssue(
                        ValidationIssue.Severity.WARNING,
                        "Invalid EXPOSE syntax",
                        "EXPOSE instruction should specify port numbers",
                        lineNumber, 1
                    ));
                }
            }
            
            // Check for RUN instruction best practices
            if (line.toUpperCase().startsWith("RUN")) {
                if (line.contains("apt-get update") && !line.contains("apt-get install")) {
                    issues.add(new ValidationIssue(
                        ValidationIssue.Severity.WARNING,
                        "Dockerfile best practice",
                        "Consider combining 'apt-get update' with 'apt-get install' in the same RUN instruction",
                        lineNumber, 1
                    ));
                }
            }
            
            // Check for COPY vs ADD
            if (line.toUpperCase().startsWith("ADD") && !line.contains("http")) {
                issues.add(new ValidationIssue(
                    ValidationIssue.Severity.WARNING,
                    "Dockerfile best practice",
                    "Consider using COPY instead of ADD for local files",
                    lineNumber, 1
                ));
            }
        }
        
        // Check for required instructions
        if (!hasFrom) {
            issues.add(new ValidationIssue(
                ValidationIssue.Severity.ERROR,
                "Missing FROM instruction",
                "Dockerfile must start with a FROM instruction",
                1, 1
            ));
        }
        
        return issues;
    }
    
    private List<ValidationIssue> validateYaml(String content) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        try {
            yamlMapper.readTree(content);
        } catch (Exception e) {
            issues.add(new ValidationIssue(
                ValidationIssue.Severity.ERROR,
                "YAML Syntax Error",
                e.getMessage(),
                getLineNumberFromException(e), 1
            ));
        }
        
        // Additional YAML-specific validations
        if (content.contains("apiVersion:")) {
            issues.addAll(validateKubernetesYaml(content));
        }
        
        return issues;
    }
    
    private List<ValidationIssue> validateKubernetesYaml(String content) {
        List<ValidationIssue> issues = new ArrayList<>();
        String[] lines = content.split("\n");
        
        boolean hasApiVersion = false;
        boolean hasKind = false;
        boolean hasMetadata = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNumber = i + 1;
            
            if (line.startsWith("apiVersion:")) {
                hasApiVersion = true;
            } else if (line.startsWith("kind:")) {
                hasKind = true;
            } else if (line.startsWith("metadata:")) {
                hasMetadata = true;
            }
            
            // Check for common Kubernetes resource requirements
            if (line.startsWith("kind: Deployment") || line.startsWith("kind: StatefulSet")) {
                // Look for spec section
                boolean hasSpec = false;
                for (int j = i; j < lines.length; j++) {
                    if (lines[j].trim().startsWith("spec:")) {
                        hasSpec = true;
                        break;
                    }
                }
                if (!hasSpec) {
                    issues.add(new ValidationIssue(
                        ValidationIssue.Severity.ERROR,
                        "Missing spec section",
                        "Deployment/StatefulSet requires a spec section",
                        lineNumber, 1
                    ));
                }
            }
        }
        
        if (!hasApiVersion) {
            issues.add(new ValidationIssue(
                ValidationIssue.Severity.ERROR,
                "Missing apiVersion",
                "Kubernetes resources must have an apiVersion",
                1, 1
            ));
        }
        
        if (!hasKind) {
            issues.add(new ValidationIssue(
                ValidationIssue.Severity.ERROR,
                "Missing kind",
                "Kubernetes resources must have a kind",
                1, 1
            ));
        }
        
        if (!hasMetadata) {
            issues.add(new ValidationIssue(
                ValidationIssue.Severity.ERROR,
                "Missing metadata",
                "Kubernetes resources must have metadata",
                1, 1
            ));
        }
        
        return issues;
    }
    
    private List<ValidationIssue> validateJson(String content) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        try {
            jsonMapper.readTree(content);
        } catch (Exception e) {
            issues.add(new ValidationIssue(
                ValidationIssue.Severity.ERROR,
                "JSON Syntax Error",
                e.getMessage(),
                getLineNumberFromException(e), 1
            ));
        }
        
        return issues;
    }
    
    private List<ValidationIssue> validateXml(String content) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(content.getBytes()));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            issues.add(new ValidationIssue(
                ValidationIssue.Severity.ERROR,
                "XML Syntax Error",
                e.getMessage(),
                getLineNumberFromException(e), 1
            ));
        }
        
        return issues;
    }
    
    private List<ValidationIssue> validateShellScript(String content) {
        List<ValidationIssue> issues = new ArrayList<>();
        String[] lines = content.split("\n");
        
        boolean hasShebang = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNumber = i + 1;
            
            if (i == 0 && line.startsWith("#!")) {
                hasShebang = true;
            }
            
            // Check for common shell script issues
            if (line.contains("rm -rf /") && !line.contains("$")) {
                issues.add(new ValidationIssue(
                    ValidationIssue.Severity.ERROR,
                    "Dangerous command",
                    "Potentially dangerous rm command detected",
                    lineNumber, 1
                ));
            }
            
            // Check for unquoted variables
            Pattern unquotedVar = Pattern.compile("\\$\\w+(?![\"'])");
            Matcher matcher = unquotedVar.matcher(line);
            if (matcher.find()) {
                issues.add(new ValidationIssue(
                    ValidationIssue.Severity.WARNING,
                    "Unquoted variable",
                    "Consider quoting variables to prevent word splitting",
                    lineNumber, matcher.start() + 1
                ));
            }
        }
        
        if (!hasShebang && lines.length > 0) {
            issues.add(new ValidationIssue(
                ValidationIssue.Severity.WARNING,
                "Missing shebang",
                "Shell scripts should start with a shebang (#!/bin/bash)",
                1, 1
            ));
        }
        
        return issues;
    }
    
    private List<ValidationIssue> validateProperties(String content) {
        List<ValidationIssue> issues = new ArrayList<>();
        String[] lines = content.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNumber = i + 1;
            
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // Check for valid property format
            if (!line.contains("=") && !line.contains(":")) {
                issues.add(new ValidationIssue(
                    ValidationIssue.Severity.ERROR,
                    "Invalid property format",
                    "Properties should be in key=value or key:value format",
                    lineNumber, 1
                ));
            }
            
            // Check for duplicate keys
            String key = line.split("[=:]")[0].trim();
            for (int j = i + 1; j < lines.length; j++) {
                String otherLine = lines[j].trim();
                if (!otherLine.isEmpty() && !otherLine.startsWith("#")) {
                    String otherKey = otherLine.split("[=:]")[0].trim();
                    if (key.equals(otherKey)) {
                        issues.add(new ValidationIssue(
                            ValidationIssue.Severity.WARNING,
                            "Duplicate property key",
                            "Property key '" + key + "' is defined multiple times",
                            j + 1, 1
                        ));
                        break;
                    }
                }
            }
        }
        
        return issues;
    }
    
    private int getLineNumberFromException(Exception e) {
        String message = e.getMessage();
        if (message != null) {
            Pattern linePattern = Pattern.compile("line (\\d+)");
            Matcher matcher = linePattern.matcher(message);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 1; // Default to line 1 if we can't extract line number
    }
}