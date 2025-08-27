package com.example.gitmigrator.model;

/**
 * Enumeration of migration steps for detailed progress tracking
 */
public enum MigrationStep {
    INITIALIZING("Initializing"),
    CLONING("Cloning repository"),
    ANALYZING("Analyzing project structure"),
    DETECTING_FRAMEWORK("Detecting framework"),
    EXTRACTING_METADATA("Extracting metadata"),
    GENERATING_DOCKERFILE("Generating Dockerfile"),
    GENERATING_HELM_CHART("Generating Helm chart"),
    GENERATING_KUBERNETES_MANIFESTS("Generating Kubernetes manifests"),
    GENERATING_CI_CD("Generating CI/CD pipeline"),
    VALIDATING_ARTIFACTS("Validating generated artifacts"),
    WRITING_FILES("Writing files to repository"),
    FINALIZING("Finalizing migration");

    private final String displayName;

    MigrationStep(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MigrationStep[] getOrderedSteps() {
        return values();
    }

    public int getStepNumber() {
        return ordinal() + 1;
    }

    public int getTotalSteps() {
        return values().length;
    }
}