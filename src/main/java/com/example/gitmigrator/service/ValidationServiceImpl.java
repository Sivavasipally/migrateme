package com.example.gitmigrator.service;

import com.example.gitmigrator.model.ValidationResult;
import com.example.gitmigrator.model.ValidationIssue;
import com.example.gitmigrator.model.ValidationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Implementation of ValidationService for validating migration artifacts.
 * Uses external tools like Docker, Helm, and kubectl for validation when available.
 */
public class ValidationServiceImpl implements ValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationServiceImpl.class);
    
    // Dockerfile validation patterns
    private static final Pattern FROM_PATTERN = Pattern.compile("^FROM\\s+\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern COPY_PATTERN = Pattern.compile("^COPY\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADD_PATTERN = Pattern.compile("^ADD\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern RUN_PATTERN = Pattern.compile("^RUN\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPOSE_PATTERN = Pattern.compile("^EXPOSE\\s+\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_PATTERN = Pattern.compile("^CMD\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENTRYPOINT_PATTERN = Pattern.compile("^ENTRYPOINT\\s+", Pattern.CASE_INSENSITIVE);
    
    // Tool availability cache
    private Boolean dockerAvailable = null;
    private Boolean helmAvailable = null;
    private Boolean kubectlAvailable = null;
    
    @Override
    public ValidationResult validateDockerfile(File dockerfile) {
        logger.debug("Validating Dockerfile: {}", dockerfile.getAbsolutePath());
        
        List<ValidationIssue> issues = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        if (!dockerfile.exists()) {
            issues.add(new ValidationIssue("FILE_NOT_FOUND", "Dockerfile not found: " + dockerfile.getAbsolutePath()));
            return createValidationResult(false, issues, warnings, "Dockerfile validation failed");
        }
        
        if (!dockerfile.canRead()) {
            issues.add(new ValidationIssue("FILE_NOT_READABLE", "Cannot read Dockerfile: " + dockerfile.getAbsolutePath()));
            return createValidationResult(false, issues, warnings, "Dockerfile validation failed");
        }
        
        try {
            List<String> lines = Files.readAllLines(dockerfile.toPath());
            validateDockerfileContent(lines, issues, warnings);
            
            // Try to use Docker CLI for additional validation if available
            if (isDockerAvailable()) {
                validateDockerfileWithCli(dockerfile, issues, warnings);
            }
            
        } catch (IOException e) {
            logger.error("Error reading Dockerfile: {}", dockerfile.getAbsolutePath(), e);
            issues.add(new ValidationIssue("READ_ERROR", "Error reading Dockerfile: " + e.getMessage()));
        }
        
        boolean isValid = issues.isEmpty();
        String summary = isValid ? "Dockerfile validation passed" : "Dockerfile validation failed with " + issues.size() + " issues";
        
        return createValidationResult(isValid, issues, warnings, summary);
    }
    
    @Override
    public ValidationResult validateHelmChart(File chartDirectory) {
        logger.debug("Validating Helm chart: {}", chartDirectory.getAbsolutePath());
        
        List<ValidationIssue> issues = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        if (!chartDirectory.exists()) {
            issues.add(new ValidationIssue("CHART_NOT_FOUND", "Helm chart directory not found: " + chartDirectory.getAbsolutePath()));
            return createValidationResult(false, issues, warnings, "Helm chart validation failed");
        }
        
        if (!chartDirectory.isDirectory()) {
            issues.add(new ValidationIssue("NOT_DIRECTORY", "Path is not a directory: " + chartDirectory.getAbsolutePath()));
            return createValidationResult(false, issues, warnings, "Helm chart validation failed");
        }
        
        // Check for required Helm chart files
        validateHelmChartStructure(chartDirectory, issues, warnings);
        
        // Try to use Helm CLI for additional validation if available
        if (isHelmAvailable()) {
            validateHelmChartWithCli(chartDirectory, issues, warnings);
        }
        
        boolean isValid = issues.isEmpty();
        String summary = isValid ? "Helm chart validation passed" : "Helm chart validation failed with " + issues.size() + " issues";
        
        return createValidationResult(isValid, issues, warnings, summary);
    }
    
    @Override
    public ValidationResult validateKubernetesManifests(List<File> manifests) {
        logger.debug("Validating {} Kubernetes manifests", manifests.size());
        
        List<ValidationIssue> allIssues = new ArrayList<>();
        List<ValidationWarning> allWarnings = new ArrayList<>();
        
        for (File manifest : manifests) {
            ValidationResult result = validateKubernetesManifest(manifest);
            allIssues.addAll(result.getIssues());
            allWarnings.addAll(result.getWarnings());
        }
        
        boolean isValid = allIssues.isEmpty();
        String summary = isValid ? 
            "All " + manifests.size() + " Kubernetes manifests are valid" : 
            "Kubernetes manifest validation failed with " + allIssues.size() + " issues";
        
        return createValidationResult(isValid, allIssues, allWarnings, summary);
    }
    
    @Override
    public ValidationResult validateKubernetesManifest(File manifest) {
        logger.debug("Validating Kubernetes manifest: {}", manifest.getAbsolutePath());
        
        List<ValidationIssue> issues = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        if (!manifest.exists()) {
            issues.add(new ValidationIssue("FILE_NOT_FOUND", "Manifest file not found: " + manifest.getAbsolutePath()));
            return createValidationResult(false, issues, warnings, "Kubernetes manifest validation failed");
        }
        
        try {
            String content = Files.readString(manifest.toPath());
            validateKubernetesManifestContent(content, issues, warnings);
            
            // Try to use kubectl for additional validation if available
            if (isKubectlAvailable()) {
                validateKubernetesManifestWithCli(manifest, issues, warnings);
            }
            
        } catch (IOException e) {
            logger.error("Error reading Kubernetes manifest: {}", manifest.getAbsolutePath(), e);
            issues.add(new ValidationIssue("READ_ERROR", "Error reading manifest: " + e.getMessage()));
        }
        
        boolean isValid = issues.isEmpty();
        String summary = isValid ? "Kubernetes manifest validation passed" : "Kubernetes manifest validation failed";
        
        return createValidationResult(isValid, issues, warnings, summary);
    }
    
    @Override
    public CompletableFuture<ValidationResult> buildDockerImage(File dockerfile, String imageName) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Building Docker image from: {} with name: {}", dockerfile.getAbsolutePath(), imageName);
            
            List<ValidationIssue> issues = new ArrayList<>();
            List<ValidationWarning> warnings = new ArrayList<>();
            
            if (!isDockerAvailable()) {
                issues.add(new ValidationIssue("DOCKER_NOT_AVAILABLE", "Docker is not available for image building"));
                return createValidationResult(false, issues, warnings, "Docker build validation failed");
            }
            
            try {
                ProcessBuilder pb = new ProcessBuilder("docker", "build", "-t", imageName, "-f", dockerfile.getAbsolutePath(), dockerfile.getParent());
                pb.redirectErrorStream(true);
                
                Process process = pb.start();
                
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        logger.debug("Docker build output: {}", line);
                    }
                }
                
                boolean success = process.waitFor(5, TimeUnit.MINUTES) && process.exitValue() == 0;
                
                if (!success) {
                    issues.add(new ValidationIssue("BUILD_FAILED", "Docker build failed: " + output.toString()));
                    return createValidationResult(false, issues, warnings, "Docker build validation failed");
                }
                
                // Clean up the built image
                cleanupDockerImage(imageName);
                
                return createValidationResult(true, issues, warnings, "Docker build validation passed");
                
            } catch (Exception e) {
                logger.error("Error building Docker image", e);
                issues.add(new ValidationIssue("BUILD_ERROR", "Error building Docker image: " + e.getMessage()));
                return createValidationResult(false, issues, warnings, "Docker build validation failed");
            }
        });
    }
    
    @Override
    public ValidationResult validateDockerCompose(File composeFile) {
        logger.debug("Validating Docker Compose file: {}", composeFile.getAbsolutePath());
        
        List<ValidationIssue> issues = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        if (!composeFile.exists()) {
            issues.add(new ValidationIssue("FILE_NOT_FOUND", "Docker Compose file not found: " + composeFile.getAbsolutePath()));
            return createValidationResult(false, issues, warnings, "Docker Compose validation failed");
        }
        
        try {
            String content = Files.readString(composeFile.toPath());
            validateDockerComposeContent(content, issues, warnings);
            
            // Try to use docker-compose for additional validation if available
            if (isDockerAvailable()) {
                validateDockerComposeWithCli(composeFile, issues, warnings);
            }
            
        } catch (IOException e) {
            logger.error("Error reading Docker Compose file: {}", composeFile.getAbsolutePath(), e);
            issues.add(new ValidationIssue("READ_ERROR", "Error reading Docker Compose file: " + e.getMessage()));
        }
        
        boolean isValid = issues.isEmpty();
        String summary = isValid ? "Docker Compose validation passed" : "Docker Compose validation failed";
        
        return createValidationResult(isValid, issues, warnings, summary);
    }
    
    @Override
    public ValidationResult validateFiles(List<File> files) {
        logger.debug("Validating {} files", files.size());
        
        List<ValidationIssue> allIssues = new ArrayList<>();
        List<ValidationWarning> allWarnings = new ArrayList<>();
        
        for (File file : files) {
            ValidationResult result = validateFileByType(file);
            allIssues.addAll(result.getIssues());
            allWarnings.addAll(result.getWarnings());
        }
        
        boolean isValid = allIssues.isEmpty();
        String summary = isValid ? 
            "All " + files.size() + " files are valid" : 
            "File validation failed with " + allIssues.size() + " issues";
        
        return createValidationResult(isValid, allIssues, allWarnings, summary);
    }
    
    @Override
    public boolean isDockerAvailable() {
        if (dockerAvailable == null) {
            dockerAvailable = checkToolAvailability("docker", "--version");
        }
        return dockerAvailable;
    }
    
    @Override
    public boolean isHelmAvailable() {
        if (helmAvailable == null) {
            helmAvailable = checkToolAvailability("helm", "version", "--short");
        }
        return helmAvailable;
    }
    
    @Override
    public boolean isKubectlAvailable() {
        if (kubectlAvailable == null) {
            kubectlAvailable = checkToolAvailability("kubectl", "version", "--client");
        }
        return kubectlAvailable;
    }
    
    @Override
    public String getDockerVersion() {
        return getToolVersion("docker", "--version");
    }
    
    @Override
    public String getHelmVersion() {
        return getToolVersion("helm", "version", "--short");
    }
    
    @Override
    public String getKubectlVersion() {
        return getToolVersion("kubectl", "version", "--client", "--short");
    }
    
    @Override
    public ValidationResult validateEnvironment() {
        logger.debug("Validating validation environment");
        
        List<ValidationIssue> issues = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        Map<String, Object> metrics = new HashMap<>();
        
        // Check tool availability
        boolean dockerAvail = isDockerAvailable();
        boolean helmAvail = isHelmAvailable();
        boolean kubectlAvail = isKubectlAvailable();
        
        metrics.put("dockerAvailable", dockerAvail);
        metrics.put("helmAvailable", helmAvail);
        metrics.put("kubectlAvailable", kubectlAvail);
        
        if (!dockerAvail) {
            warnings.add(new ValidationWarning("DOCKER_NOT_AVAILABLE", "Docker is not available. Docker validation will be limited."));
        } else {
            metrics.put("dockerVersion", getDockerVersion());
        }
        
        if (!helmAvail) {
            warnings.add(new ValidationWarning("HELM_NOT_AVAILABLE", "Helm is not available. Helm chart validation will be limited."));
        } else {
            metrics.put("helmVersion", getHelmVersion());
        }
        
        if (!kubectlAvail) {
            warnings.add(new ValidationWarning("KUBECTL_NOT_AVAILABLE", "kubectl is not available. Kubernetes manifest validation will be limited."));
        } else {
            metrics.put("kubectlVersion", getKubectlVersion());
        }
        
        boolean isValid = true; // Environment validation doesn't fail, just warns
        String summary = String.format("Environment validation completed. Docker: %s, Helm: %s, kubectl: %s", 
            dockerAvail ? "available" : "not available",
            helmAvail ? "available" : "not available", 
            kubectlAvail ? "available" : "not available");
        
        ValidationResult result = createValidationResult(isValid, issues, warnings, summary);
        result.setMetrics(metrics);
        
        return result;
    }
    
    // Private helper methods
    
    private void validateDockerfileContent(List<String> lines, List<ValidationIssue> issues, List<ValidationWarning> warnings) {
        boolean hasFrom = false;
        boolean hasCmd = false;
        boolean hasEntrypoint = false;
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            int lineNumber = i + 1;
            
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            if (FROM_PATTERN.matcher(line).find()) {
                hasFrom = true;
                if (i > 0) {
                    warnings.add(new ValidationWarning("FROM_NOT_FIRST", "FROM instruction should be the first non-comment instruction (line " + lineNumber + ")"));
                }
            } else if (CMD_PATTERN.matcher(line).find()) {
                hasCmd = true;
            } else if (ENTRYPOINT_PATTERN.matcher(line).find()) {
                hasEntrypoint = true;
            }
            
            // Check for common issues
            if (line.toUpperCase().contains("ADD") && !ADD_PATTERN.matcher(line).find()) {
                warnings.add(new ValidationWarning("PREFER_COPY", "Consider using COPY instead of ADD for simple file copying (line " + lineNumber + ")"));
            }
            
            if (line.toUpperCase().contains("RUN") && line.contains("apt-get update") && !line.contains("apt-get clean")) {
                warnings.add(new ValidationWarning("APT_CLEANUP", "Consider cleaning apt cache after update (line " + lineNumber + ")"));
            }
        }
        
        if (!hasFrom) {
            issues.add(new ValidationIssue("MISSING_FROM", "Dockerfile must contain a FROM instruction"));
        }
        
        if (!hasCmd && !hasEntrypoint) {
            warnings.add(new ValidationWarning("MISSING_CMD_ENTRYPOINT", "Dockerfile should contain either CMD or ENTRYPOINT instruction"));
        }
    }
    
    private void validateDockerfileWithCli(File dockerfile, List<ValidationIssue> issues, List<ValidationWarning> warnings) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "build", "--no-cache", "-f", dockerfile.getAbsolutePath(), dockerfile.getParent());
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0) {
                issues.add(new ValidationIssue("DOCKER_BUILD_FAILED", "Docker build validation failed: " + output.toString()));
            }
            
        } catch (Exception e) {
            logger.warn("Could not validate Dockerfile with Docker CLI", e);
            warnings.add(new ValidationWarning("CLI_VALIDATION_FAILED", "Could not validate with Docker CLI: " + e.getMessage()));
        }
    }
    
    private void validateHelmChartStructure(File chartDirectory, List<ValidationIssue> issues, List<ValidationWarning> warnings) {
        // Check for Chart.yaml
        File chartYaml = new File(chartDirectory, "Chart.yaml");
        if (!chartYaml.exists()) {
            issues.add(new ValidationIssue("MISSING_CHART_YAML", "Chart.yaml file is missing"));
        }
        
        // Check for templates directory
        File templatesDir = new File(chartDirectory, "templates");
        if (!templatesDir.exists() || !templatesDir.isDirectory()) {
            warnings.add(new ValidationWarning("MISSING_TEMPLATES", "templates directory is missing"));
        }
        
        // Check for values.yaml
        File valuesYaml = new File(chartDirectory, "values.yaml");
        if (!valuesYaml.exists()) {
            warnings.add(new ValidationWarning("MISSING_VALUES", "values.yaml file is missing"));
        }
    }
    
    private void validateHelmChartWithCli(File chartDirectory, List<ValidationIssue> issues, List<ValidationWarning> warnings) {
        try {
            ProcessBuilder pb = new ProcessBuilder("helm", "lint", chartDirectory.getAbsolutePath());
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0) {
                issues.add(new ValidationIssue("HELM_LINT_FAILED", "Helm lint failed: " + output.toString()));
            }
            
        } catch (Exception e) {
            logger.warn("Could not validate Helm chart with Helm CLI", e);
            warnings.add(new ValidationWarning("CLI_VALIDATION_FAILED", "Could not validate with Helm CLI: " + e.getMessage()));
        }
    }
    
    private void validateKubernetesManifestContent(String content, List<ValidationIssue> issues, List<ValidationWarning> warnings) {
        // Basic YAML structure validation
        if (!content.contains("apiVersion:")) {
            issues.add(new ValidationIssue("MISSING_API_VERSION", "Kubernetes manifest must contain apiVersion"));
        }
        
        if (!content.contains("kind:")) {
            issues.add(new ValidationIssue("MISSING_KIND", "Kubernetes manifest must contain kind"));
        }
        
        if (!content.contains("metadata:")) {
            issues.add(new ValidationIssue("MISSING_METADATA", "Kubernetes manifest must contain metadata"));
        }
        
        // Check for common issues
        if (content.contains("latest") && content.contains("image:")) {
            warnings.add(new ValidationWarning("LATEST_TAG", "Consider using specific image tags instead of 'latest'"));
        }
    }
    
    private void validateKubernetesManifestWithCli(File manifest, List<ValidationIssue> issues, List<ValidationWarning> warnings) {
        try {
            ProcessBuilder pb = new ProcessBuilder("kubectl", "apply", "--dry-run=client", "-f", manifest.getAbsolutePath());
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0) {
                issues.add(new ValidationIssue("KUBECTL_VALIDATION_FAILED", "kubectl validation failed: " + output.toString()));
            }
            
        } catch (Exception e) {
            logger.warn("Could not validate Kubernetes manifest with kubectl", e);
            warnings.add(new ValidationWarning("CLI_VALIDATION_FAILED", "Could not validate with kubectl: " + e.getMessage()));
        }
    }
    
    private void validateDockerComposeContent(String content, List<ValidationIssue> issues, List<ValidationWarning> warnings) {
        if (!content.contains("version:")) {
            issues.add(new ValidationIssue("MISSING_VERSION", "Docker Compose file must contain version"));
        }
        
        if (!content.contains("services:")) {
            issues.add(new ValidationIssue("MISSING_SERVICES", "Docker Compose file must contain services"));
        }
    }
    
    private void validateDockerComposeWithCli(File composeFile, List<ValidationIssue> issues, List<ValidationWarning> warnings) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker-compose", "-f", composeFile.getAbsolutePath(), "config");
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0) {
                issues.add(new ValidationIssue("COMPOSE_VALIDATION_FAILED", "Docker Compose validation failed: " + output.toString()));
            }
            
        } catch (Exception e) {
            logger.warn("Could not validate Docker Compose file with CLI", e);
            warnings.add(new ValidationWarning("CLI_VALIDATION_FAILED", "Could not validate with docker-compose CLI: " + e.getMessage()));
        }
    }
    
    private ValidationResult validateFileByType(File file) {
        String fileName = file.getName().toLowerCase();
        
        if (fileName.equals("dockerfile") || fileName.endsWith(".dockerfile")) {
            return validateDockerfile(file);
        } else if (fileName.equals("docker-compose.yml") || fileName.equals("docker-compose.yaml")) {
            return validateDockerCompose(file);
        } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            // Assume Kubernetes manifest
            return validateKubernetesManifest(file);
        } else if (file.isDirectory() && new File(file, "Chart.yaml").exists()) {
            return validateHelmChart(file);
        } else {
            List<ValidationWarning> warnings = new ArrayList<>();
            warnings.add(new ValidationWarning("UNKNOWN_FILE_TYPE", "Unknown file type, skipping validation: " + file.getName()));
            return createValidationResult(true, new ArrayList<>(), warnings, "File type not recognized");
        }
    }
    
    private boolean checkToolAvailability(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            
            return finished && process.exitValue() == 0;
            
        } catch (Exception e) {
            logger.debug("Tool not available: {}", command[0], e);
            return false;
        }
    }
    
    private String getToolVersion(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(" ");
                }
            }
            
            if (process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0) {
                return output.toString().trim();
            }
            
        } catch (Exception e) {
            logger.debug("Could not get version for tool: {}", command[0], e);
        }
        
        return null;
    }
    
    private void cleanupDockerImage(String imageName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "rmi", imageName);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            process.waitFor(30, TimeUnit.SECONDS);
            
            logger.debug("Cleaned up Docker image: {}", imageName);
            
        } catch (Exception e) {
            logger.warn("Could not clean up Docker image: {}", imageName, e);
        }
    }
    
    /**
     * Helper method to create ValidationResult with all fields.
     */
    private ValidationResult createValidationResult(boolean isValid, List<ValidationIssue> issues, 
                                                   List<ValidationWarning> warnings, String summary) {
        ValidationResult result = new ValidationResult(isValid, summary);
        result.setIssues(issues != null ? issues : new ArrayList<>());
        result.setWarnings(warnings != null ? warnings : new ArrayList<>());
        return result;
    }
}