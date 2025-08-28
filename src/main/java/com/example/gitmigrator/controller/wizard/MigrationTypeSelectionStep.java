package com.example.gitmigrator.controller.wizard;

import com.example.gitmigrator.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Third wizard step: Migration Type Selection.
 * Users select the type of migration based on detected frameworks and target platform.
 */
public class MigrationTypeSelectionStep implements WizardStep {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationTypeSelectionStep.class);
    
    private final MigrationWizardModel wizardModel;
    
    private VBox stepContent;
    private RadioButton pcfToOpenshiftRadio;
    private RadioButton dockerToKubernetesRadio;
    private RadioButton frameworkModernizationRadio;
    private RadioButton monolithToMicroservicesRadio;
    private RadioButton cloudNativeMigrationRadio;
    private RadioButton customMigrationRadio;
    private ComboBox<String> sourceEnvironmentCombo;
    private ComboBox<String> targetEnvironmentCombo;
    private TextArea migrationDescriptionArea;
    private VBox customOptionsBox;
    private CheckBox enableCIBox;
    private CheckBox enableMonitoringBox;
    private CheckBox enableSecurityBox;
    private CheckBox enableDatabaseMigrationBox;
    
    private Runnable nextCallback;
    private Runnable previousCallback;
    private Runnable finishCallback;
    
    public MigrationTypeSelectionStep(MigrationWizardModel wizardModel) {
        this.wizardModel = wizardModel;
        initializeUI();
    }
    
    private void initializeUI() {
        stepContent = new VBox(20);
        stepContent.setPadding(new Insets(20));
        
        // Title and description
        Label titleLabel = new Label("Select Migration Type");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label descriptionLabel = new Label(
            "Choose the migration strategy that best matches your requirements based on the detected frameworks. " +
            "The application will configure templates and processes accordingly.");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-text-fill: #666666;");
        
        // Migration type selection
        VBox migrationTypeSection = createMigrationTypeSection();
        
        // Environment selection
        VBox environmentSection = createEnvironmentSection();
        
        // Custom options
        VBox customSection = createCustomOptionsSection();
        
        // Migration description
        VBox descriptionSection = createDescriptionSection();
        
        stepContent.getChildren().addAll(
            titleLabel,
            descriptionLabel,
            new Separator(),
            migrationTypeSection,
            new Separator(),
            environmentSection,
            new Separator(),
            customSection,
            new Separator(),
            descriptionSection
        );
    }
    
    private VBox createMigrationTypeSection() {
        VBox section = new VBox(15);
        
        Label sectionLabel = new Label("🎯 Migration Strategy");
        sectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        ToggleGroup migrationGroup = new ToggleGroup();
        
        // PCF to OpenShift
        pcfToOpenshiftRadio = new RadioButton("PCF to OpenShift Migration");
        pcfToOpenshiftRadio.setToggleGroup(migrationGroup);
        Label pcfDescription = new Label("Migrate applications from Pivotal Cloud Foundry to Red Hat OpenShift");
        pcfDescription.setStyle("-fx-text-fill: #666666; -fx-padding: 0 0 0 25;");
        
        // Docker to Kubernetes
        dockerToKubernetesRadio = new RadioButton("Docker to Kubernetes Migration");
        dockerToKubernetesRadio.setToggleGroup(migrationGroup);
        Label dockerDescription = new Label("Containerize applications and generate Kubernetes deployment manifests");
        dockerDescription.setStyle("-fx-text-fill: #666666; -fx-padding: 0 0 0 25;");
        
        // Framework Modernization
        frameworkModernizationRadio = new RadioButton("Framework Modernization");
        frameworkModernizationRadio.setToggleGroup(migrationGroup);
        Label frameworkDescription = new Label("Update framework versions and modernize application architecture");
        frameworkDescription.setStyle("-fx-text-fill: #666666; -fx-padding: 0 0 0 25;");
        
        // Monolith to Microservices
        monolithToMicroservicesRadio = new RadioButton("Monolith to Microservices");
        monolithToMicroservicesRadio.setToggleGroup(migrationGroup);
        Label monolithDescription = new Label("Break down monolithic applications into microservices architecture");
        monolithDescription.setStyle("-fx-text-fill: #666666; -fx-padding: 0 0 0 25;");
        
        // Cloud Native Migration
        cloudNativeMigrationRadio = new RadioButton("Cloud Native Migration");
        cloudNativeMigrationRadio.setToggleGroup(migrationGroup);
        Label cloudNativeDescription = new Label("Transform applications to cloud-native patterns with observability");
        cloudNativeDescription.setStyle("-fx-text-fill: #666666; -fx-padding: 0 0 0 25;");
        
        // Custom Migration
        customMigrationRadio = new RadioButton("Custom Migration");
        customMigrationRadio.setToggleGroup(migrationGroup);
        Label customDescription = new Label("Custom migration with manually selected templates and configurations");
        customDescription.setStyle("-fx-text-fill: #666666; -fx-padding: 0 0 0 25;");
        
        // Add change listeners
        migrationGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            updateMigrationTypeSelection();
        });
        
        section.getChildren().addAll(
            sectionLabel,
            pcfToOpenshiftRadio, pcfDescription,
            dockerToKubernetesRadio, dockerDescription,
            frameworkModernizationRadio, frameworkDescription,
            monolithToMicroservicesRadio, monolithDescription,
            cloudNativeMigrationRadio, cloudNativeDescription,
            customMigrationRadio, customDescription
        );
        
        return section;
    }
    
    private VBox createEnvironmentSection() {
        VBox section = new VBox(10);
        
        Label sectionLabel = new Label("🏗️ Environment Configuration");
        sectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        GridPane environmentGrid = new GridPane();
        environmentGrid.setHgap(15);
        environmentGrid.setVgap(10);
        
        // Source environment
        Label sourceLabel = new Label("Source Environment:");
        sourceEnvironmentCombo = new ComboBox<>();
        sourceEnvironmentCombo.getItems().addAll(
            "Pivotal Cloud Foundry (PCF)",
            "Docker Compose",
            "Traditional VM/Bare Metal",
            "Legacy Application Server",
            "Heroku",
            "Other Cloud Platform"
        );
        sourceEnvironmentCombo.setPrefWidth(250);
        sourceEnvironmentCombo.setPromptText("Select source environment...");
        
        // Target environment
        Label targetLabel = new Label("Target Environment:");
        targetEnvironmentCombo = new ComboBox<>();
        targetEnvironmentCombo.getItems().addAll(
            "Red Hat OpenShift",
            "Kubernetes",
            "Docker Compose",
            "AWS EKS",
            "Azure AKS",
            "Google GKE",
            "Rancher"
        );
        targetEnvironmentCombo.setPrefWidth(250);
        targetEnvironmentCombo.setPromptText("Select target environment...");
        
        environmentGrid.add(sourceLabel, 0, 0);
        environmentGrid.add(sourceEnvironmentCombo, 1, 0);
        environmentGrid.add(targetLabel, 0, 1);
        environmentGrid.add(targetEnvironmentCombo, 1, 1);
        
        section.getChildren().addAll(sectionLabel, environmentGrid);
        return section;
    }
    
    private VBox createCustomOptionsSection() {
        VBox section = new VBox(10);
        
        Label sectionLabel = new Label("⚙️ Additional Options");
        sectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        customOptionsBox = new VBox(8);
        
        // CI/CD Pipeline
        enableCIBox = new CheckBox("Generate CI/CD Pipeline Configuration");
        enableCIBox.setSelected(true);
        Label ciDescription = new Label("Generate Jenkins/GitLab CI/GitHub Actions pipeline files");
        ciDescription.setStyle("-fx-text-fill: #666666; -fx-padding: 0 0 0 25;");
        
        // Monitoring
        enableMonitoringBox = new CheckBox("Include Monitoring and Observability");
        enableMonitoringBox.setSelected(true);
        Label monitoringDescription = new Label("Add Prometheus metrics, health checks, and logging configuration");
        monitoringDescription.setStyle("-fx-text-fill: #666666; -fx-padding: 0 0 0 25;");
        
        // Security
        enableSecurityBox = new CheckBox("Enhanced Security Configuration");
        enableSecurityBox.setSelected(true);
        Label securityDescription = new Label("Add security policies, RBAC, and network policies");
        securityDescription.setStyle("-fx-text-fill: #666666; -fx-padding: 0 0 0 25;");
        
        // Database Migration
        enableDatabaseMigrationBox = new CheckBox("Database Migration Assistance");
        enableDatabaseMigrationBox.setSelected(false);
        Label dbDescription = new Label("Generate database migration scripts and connection configurations");
        dbDescription.setStyle("-fx-text-fill: #666666; -fx-padding: 0 0 0 25;");
        
        customOptionsBox.getChildren().addAll(
            enableCIBox, ciDescription,
            enableMonitoringBox, monitoringDescription,
            enableSecurityBox, securityDescription,
            enableDatabaseMigrationBox, dbDescription
        );
        
        section.getChildren().addAll(sectionLabel, customOptionsBox);
        return section;
    }
    
    private VBox createDescriptionSection() {
        VBox section = new VBox(10);
        
        Label sectionLabel = new Label("📝 Migration Summary");
        sectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        migrationDescriptionArea = new TextArea();
        migrationDescriptionArea.setPrefRowCount(4);
        migrationDescriptionArea.setWrapText(true);
        migrationDescriptionArea.setEditable(false);
        migrationDescriptionArea.setStyle("-fx-background-color: #f5f5f5;");
        migrationDescriptionArea.setText("Select a migration type to see detailed information about the migration process.");
        
        section.getChildren().addAll(sectionLabel, migrationDescriptionArea);
        return section;
    }
    
    private void updateMigrationTypeSelection() {
        MigrationWizardModel.MigrationType selectedType = getSelectedMigrationType();
        if (selectedType != null) {
            wizardModel.setMigrationType(selectedType);
            updateMigrationDescription(selectedType);
            updateEnvironmentDefaults(selectedType);
        }
    }
    
    private MigrationWizardModel.MigrationType getSelectedMigrationType() {
        if (pcfToOpenshiftRadio.isSelected()) {
            return MigrationWizardModel.MigrationType.PCF_TO_OPENSHIFT;
        } else if (dockerToKubernetesRadio.isSelected()) {
            return MigrationWizardModel.MigrationType.DOCKER_TO_KUBERNETES;
        } else if (frameworkModernizationRadio.isSelected()) {
            return MigrationWizardModel.MigrationType.FRAMEWORK_MODERNIZATION;
        } else if (monolithToMicroservicesRadio.isSelected()) {
            return MigrationWizardModel.MigrationType.MONOLITH_TO_MICROSERVICES;
        } else if (cloudNativeMigrationRadio.isSelected()) {
            return MigrationWizardModel.MigrationType.CLOUD_NATIVE_MIGRATION;
        } else if (customMigrationRadio.isSelected()) {
            return MigrationWizardModel.MigrationType.CUSTOM;
        }
        return null;
    }
    
    private void updateMigrationDescription(MigrationWizardModel.MigrationType type) {
        String description;
        Set<FrameworkType> detectedFrameworks = getDetectedFrameworks();
        String frameworkList = detectedFrameworks.stream()
            .map(FrameworkType::getDisplayName)
            .collect(Collectors.joining(", "));
        
        switch (type) {
            case PCF_TO_OPENSHIFT:
                description = String.format(
                    "PCF to OpenShift Migration:\n" +
                    "• Detected frameworks: %s\n" +
                    "• Generate OpenShift templates and BuildConfig\n" +
                    "• Create deployment manifests with proper resource limits\n" +
                    "• Configure service discovery and routing\n" +
                    "• Migrate environment variables and secrets\n" +
                    "• Generate CI/CD pipeline for OpenShift builds",
                    frameworkList);
                break;
            case DOCKER_TO_KUBERNETES:
                description = String.format(
                    "Docker to Kubernetes Migration:\n" +
                    "• Detected frameworks: %s\n" +
                    "• Generate multi-stage Dockerfiles with optimization\n" +
                    "• Create Kubernetes deployments, services, and ingress\n" +
                    "• Configure horizontal pod autoscaling\n" +
                    "• Add health checks and readiness probes\n" +
                    "• Generate Helm charts for easy deployment",
                    frameworkList);
                break;
            case FRAMEWORK_MODERNIZATION:
                description = String.format(
                    "Framework Modernization:\n" +
                    "• Detected frameworks: %s\n" +
                    "• Update to latest stable framework versions\n" +
                    "• Migrate deprecated APIs and configurations\n" +
                    "• Add modern security practices\n" +
                    "• Implement cloud-native patterns\n" +
                    "• Generate updated Docker and deployment configurations",
                    frameworkList);
                break;
            case MONOLITH_TO_MICROSERVICES:
                description = String.format(
                    "Monolith to Microservices:\n" +
                    "• Detected frameworks: %s\n" +
                    "• Analyze and decompose monolithic structure\n" +
                    "• Generate microservice templates for each component\n" +
                    "• Create API gateway and service mesh configuration\n" +
                    "• Add distributed tracing and monitoring\n" +
                    "• Generate separate CI/CD pipelines",
                    frameworkList);
                break;
            case CLOUD_NATIVE_MIGRATION:
                description = String.format(
                    "Cloud Native Migration:\n" +
                    "• Detected frameworks: %s\n" +
                    "• Transform to 12-factor app principles\n" +
                    "• Add comprehensive observability stack\n" +
                    "• Implement circuit breakers and resilience patterns\n" +
                    "• Configure auto-scaling and resource management\n" +
                    "• Generate GitOps deployment workflows",
                    frameworkList);
                break;
            case CUSTOM:
                description = String.format(
                    "Custom Migration:\n" +
                    "• Detected frameworks: %s\n" +
                    "• Flexible template selection in next step\n" +
                    "• Choose specific components to migrate\n" +
                    "• Custom environment configurations\n" +
                    "• Selective feature enablement\n" +
                    "• Tailored to your specific requirements",
                    frameworkList);
                break;
            default:
                description = "Select a migration type to see details.";
        }
        
        migrationDescriptionArea.setText(description);
    }
    
    private void updateEnvironmentDefaults(MigrationWizardModel.MigrationType type) {
        switch (type) {
            case PCF_TO_OPENSHIFT:
                sourceEnvironmentCombo.setValue("Pivotal Cloud Foundry (PCF)");
                targetEnvironmentCombo.setValue("Red Hat OpenShift");
                break;
            case DOCKER_TO_KUBERNETES:
                sourceEnvironmentCombo.setValue("Docker Compose");
                targetEnvironmentCombo.setValue("Kubernetes");
                break;
            default:
                // Keep current selections
                break;
        }
    }
    
    private Set<FrameworkType> getDetectedFrameworks() {
        return wizardModel.getDetectionResults().values().stream()
            .map(MigrationWizardModel.FrameworkDetectionResult::getPrimaryFramework)
            .collect(Collectors.toSet());
    }
    
    @Override
    public String getStepTitle() {
        return "Migration Type Selection";
    }
    
    @Override
    public String getStepDescription() {
        return "Choose the migration strategy and target environment based on your requirements.";
    }
    
    @Override
    public Node getStepContent() {
        return stepContent;
    }
    
    @Override
    public void onStepEnter() {
        // Pre-select migration type based on detected frameworks if only one clear option
        Set<FrameworkType> frameworks = getDetectedFrameworks();
        
        if (frameworks.contains(FrameworkType.SPRING_BOOT) && frameworks.size() == 1) {
            // Spring Boot apps commonly migrate to Kubernetes/OpenShift
            dockerToKubernetesRadio.setSelected(true);
            updateMigrationTypeSelection();
        } else if (frameworks.contains(FrameworkType.REACT) || frameworks.contains(FrameworkType.ANGULAR)) {
            // Frontend apps commonly containerized
            dockerToKubernetesRadio.setSelected(true);
            updateMigrationTypeSelection();
        }
        
        logger.info("Migration type selection step entered with {} detected frameworks", frameworks.size());
    }
    
    @Override
    public void onStepExit() {
        // Save selections to wizard model
        wizardModel.setSourceEnvironment(sourceEnvironmentCombo.getValue());
        wizardModel.setTargetEnvironment(targetEnvironmentCombo.getValue());
        
        // Create migration configuration
        MigrationConfiguration config = wizardModel.getMigrationConfiguration();
        config.setEnableCiCdPipeline(enableCIBox.isSelected());
        config.setEnableMonitoring(enableMonitoringBox.isSelected());
        config.setEnableSecurityHardening(enableSecurityBox.isSelected());
        config.setEnableDatabaseMigration(enableDatabaseMigrationBox.isSelected());
        
        logger.info("Migration type selected: {}, Source: {}, Target: {}", 
            wizardModel.getMigrationType(),
            wizardModel.getSourceEnvironment(),
            wizardModel.getTargetEnvironment());
    }
    
    @Override
    public boolean validateStep() {
        if (getSelectedMigrationType() == null) {
            showError("Please select a migration type to proceed.");
            return false;
        }
        
        if (sourceEnvironmentCombo.getValue() == null || sourceEnvironmentCombo.getValue().trim().isEmpty()) {
            showError("Please select a source environment.");
            return false;
        }
        
        if (targetEnvironmentCombo.getValue() == null || targetEnvironmentCombo.getValue().trim().isEmpty()) {
            showError("Please select a target environment.");
            return false;
        }
        
        return true;
    }
    
    @Override
    public void setWizardNavigation(Runnable nextCallback, Runnable previousCallback, Runnable finishCallback) {
        this.nextCallback = nextCallback;
        this.previousCallback = previousCallback;
        this.finishCallback = finishCallback;
    }
    
    @Override
    public double getStepProgress() {
        return getSelectedMigrationType() != null ? 0.75 : 0.0;
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Migration Type Selection Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}