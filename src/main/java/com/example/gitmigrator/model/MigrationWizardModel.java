package com.example.gitmigrator.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.Map;

/**
 * Data model that maintains state across all wizard steps.
 * Contains all information needed for the migration process.
 */
public class MigrationWizardModel {
    
    // Step 1: Repository Selection
    private ObservableList<RepositoryInfo> availableRepositories = FXCollections.observableArrayList();
    private ObservableList<RepositoryInfo> selectedRepositories = FXCollections.observableArrayList();
    
    // Step 2: Framework Detection Results
    private Map<RepositoryInfo, FrameworkDetectionResult> detectionResults = new HashMap<>();
    
    // Step 3: Migration Type Selection
    private MigrationType migrationType;
    private String sourceEnvironment; // PCF, Docker, etc.
    private String targetEnvironment; // OCP, Kubernetes, etc.
    
    // Step 4: Template Selection
    private Map<String, String> selectedTemplates = new HashMap<>(); // file type -> template name
    private MigrationConfiguration migrationConfiguration;
    
    // Step 5: Preview and Validation
    private Map<RepositoryInfo, ValidationResult> validationResults = new HashMap<>();
    private ObservableList<GeneratedFile> previewFiles = FXCollections.observableArrayList();
    
    // Step 6: Execution Results
    private Map<RepositoryInfo, MigrationResult> migrationResults = new HashMap<>();
    private boolean executionInProgress = false;
    
    // Git Configuration
    private GitCredentials gitCredentials;
    
    public MigrationWizardModel() {
        // Initialize with default values
        migrationConfiguration = new MigrationConfiguration();
        gitCredentials = new GitCredentials();
    }
    
    /**
     * Reset the wizard model to initial state.
     */
    public void reset() {
        availableRepositories.clear();
        selectedRepositories.clear();
        detectionResults.clear();
        migrationType = null;
        sourceEnvironment = null;
        targetEnvironment = null;
        selectedTemplates.clear();
        migrationConfiguration = new MigrationConfiguration();
        validationResults.clear();
        previewFiles.clear();
        migrationResults.clear();
        executionInProgress = false;
        gitCredentials = new GitCredentials();
    }
    
    // Getters and Setters
    
    public ObservableList<RepositoryInfo> getAvailableRepositories() {
        return availableRepositories;
    }
    
    public ObservableList<RepositoryInfo> getSelectedRepositories() {
        return selectedRepositories;
    }
    
    public Map<RepositoryInfo, FrameworkDetectionResult> getDetectionResults() {
        return detectionResults;
    }
    
    public void addDetectionResult(RepositoryInfo repository, FrameworkDetectionResult result) {
        detectionResults.put(repository, result);
    }
    
    public MigrationType getMigrationType() {
        return migrationType;
    }
    
    public void setMigrationType(MigrationType migrationType) {
        this.migrationType = migrationType;
    }
    
    public String getSourceEnvironment() {
        return sourceEnvironment;
    }
    
    public void setSourceEnvironment(String sourceEnvironment) {
        this.sourceEnvironment = sourceEnvironment;
    }
    
    public String getTargetEnvironment() {
        return targetEnvironment;
    }
    
    public void setTargetEnvironment(String targetEnvironment) {
        this.targetEnvironment = targetEnvironment;
    }
    
    public Map<String, String> getSelectedTemplates() {
        return selectedTemplates;
    }
    
    public void setSelectedTemplate(String fileType, String templateName) {
        selectedTemplates.put(fileType, templateName);
    }
    
    public MigrationConfiguration getMigrationConfiguration() {
        return migrationConfiguration;
    }
    
    public void setMigrationConfiguration(MigrationConfiguration migrationConfiguration) {
        this.migrationConfiguration = migrationConfiguration;
    }
    
    public Map<RepositoryInfo, ValidationResult> getValidationResults() {
        return validationResults;
    }
    
    public void addValidationResult(RepositoryInfo repository, ValidationResult result) {
        validationResults.put(repository, result);
    }
    
    public ObservableList<GeneratedFile> getPreviewFiles() {
        return previewFiles;
    }
    
    public Map<RepositoryInfo, MigrationResult> getMigrationResults() {
        return migrationResults;
    }
    
    public void addMigrationResult(RepositoryInfo repository, MigrationResult result) {
        migrationResults.put(repository, result);
    }
    
    public boolean isExecutionInProgress() {
        return executionInProgress;
    }
    
    public void setExecutionInProgress(boolean executionInProgress) {
        this.executionInProgress = executionInProgress;
    }
    
    public GitCredentials getGitCredentials() {
        return gitCredentials;
    }
    
    public void setGitCredentials(GitCredentials gitCredentials) {
        this.gitCredentials = gitCredentials;
    }
    
    /**
     * Check if the wizard has enough data to proceed to a specific step.
     */
    public boolean canProceedToStep(int stepIndex) {
        switch (stepIndex) {
            case 0: // Repository Selection
                return true;
            case 1: // Framework Detection
                return !selectedRepositories.isEmpty();
            case 2: // Migration Type Selection
                return !detectionResults.isEmpty();
            case 3: // Template Selection
                return migrationType != null;
            case 4: // Preview and Validation
                return !selectedTemplates.isEmpty();
            case 5: // Execution
                return !validationResults.isEmpty() && 
                       validationResults.values().stream().allMatch(ValidationResult::isValid);
            default:
                return false;
        }
    }
    
    /**
     * Get a summary of the current wizard state for debugging.
     */
    public String getWizardSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Wizard State Summary:\n");
        summary.append("- Available Repositories: ").append(availableRepositories.size()).append("\n");
        summary.append("- Selected Repositories: ").append(selectedRepositories.size()).append("\n");
        summary.append("- Detection Results: ").append(detectionResults.size()).append("\n");
        summary.append("- Migration Type: ").append(migrationType).append("\n");
        summary.append("- Source Environment: ").append(sourceEnvironment).append("\n");
        summary.append("- Target Environment: ").append(targetEnvironment).append("\n");
        summary.append("- Selected Templates: ").append(selectedTemplates.size()).append("\n");
        summary.append("- Validation Results: ").append(validationResults.size()).append("\n");
        summary.append("- Migration Results: ").append(migrationResults.size()).append("\n");
        return summary.toString();
    }
    
    /**
     * Inner class for git credentials management.
     */
    public static class GitCredentials {
        private String username;
        private String password;
        private String personalAccessToken;
        private GitProviderType providerType = GitProviderType.GITHUB;
        
        public GitCredentials() {}
        
        public GitCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getPersonalAccessToken() { return personalAccessToken; }
        public void setPersonalAccessToken(String personalAccessToken) { this.personalAccessToken = personalAccessToken; }
        
        public GitProviderType getProviderType() { return providerType; }
        public void setProviderType(GitProviderType providerType) { this.providerType = providerType; }
        
        public boolean hasCredentials() {
            return (username != null && password != null) || personalAccessToken != null;
        }
    }
    
    /**
     * Enum for different types of migrations supported.
     */
    public enum MigrationType {
        PCF_TO_OPENSHIFT("PCF to OpenShift", "Migrate from Pivotal Cloud Foundry to Red Hat OpenShift"),
        DOCKER_TO_KUBERNETES("Docker to Kubernetes", "Containerize and migrate to Kubernetes"),
        MONOLITH_TO_MICROSERVICES("Monolith to Microservices", "Break down monolithic application"),
        FRAMEWORK_MODERNIZATION("Framework Modernization", "Update to modern framework versions"),
        CLOUD_NATIVE_MIGRATION("Cloud Native Migration", "Transform to cloud-native architecture"),
        CUSTOM("Custom Migration", "Custom migration with selected templates");
        
        private final String displayName;
        private final String description;
        
        MigrationType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        
        @Override
        public String toString() { return displayName; }
    }
    
    /**
     * Framework detection result wrapper.
     */
    public static class FrameworkDetectionResult {
        private final FrameworkType primaryFramework;
        private final boolean isMonorepo;
        private final int complexity;
        private final Map<String, Object> metadata;
        
        public FrameworkDetectionResult(FrameworkType primaryFramework, boolean isMonorepo, 
                                      int complexity, Map<String, Object> metadata) {
            this.primaryFramework = primaryFramework;
            this.isMonorepo = isMonorepo;
            this.complexity = complexity;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        }
        
        public FrameworkType getPrimaryFramework() { return primaryFramework; }
        public boolean isMonorepo() { return isMonorepo; }
        public int getComplexity() { return complexity; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        @Override
        public String toString() {
            return String.format("%s (Complexity: %d, Monorepo: %s)", 
                primaryFramework.getDisplayName(), complexity, isMonorepo);
        }
    }
}