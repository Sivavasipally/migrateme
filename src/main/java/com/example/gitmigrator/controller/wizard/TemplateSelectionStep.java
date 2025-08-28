package com.example.gitmigrator.controller.wizard;

import com.example.gitmigrator.model.*;
import com.example.gitmigrator.service.TemplateManagementService;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Fourth wizard step: Template Selection and Preview.
 * Users select templates and preview generated files before execution.
 */
public class TemplateSelectionStep implements WizardStep {
    
    private static final Logger logger = LoggerFactory.getLogger(TemplateSelectionStep.class);
    
    private final MigrationWizardModel wizardModel;
    private final TemplateManagementService templateService;
    
    private VBox stepContent;
    private TableView<TemplateSelectionInfo> templateTable;
    private TextArea previewArea;
    private ComboBox<String> previewFileCombo;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Button refreshTemplatesButton;
    private Button previewGeneratedButton;
    private CheckBox selectAllTemplatesBox;
    
    private Runnable nextCallback;
    private Runnable previousCallback;
    private Runnable finishCallback;
    
    public TemplateSelectionStep(MigrationWizardModel wizardModel, TemplateManagementService templateService) {
        this.wizardModel = wizardModel;
        this.templateService = templateService;
        initializeUI();
    }
    
    private void initializeUI() {
        stepContent = new VBox(20);
        stepContent.setPadding(new Insets(20));
        
        // Title and description
        Label titleLabel = new Label("Select Templates and Preview");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label descriptionLabel = new Label(
            "Select the templates to apply based on your migration type and detected frameworks. " +
            "You can preview the generated files before proceeding with the migration.");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-text-fill: #666666;");
        
        // Template selection section
        VBox templateSection = createTemplateSelectionSection();
        
        // Preview section
        VBox previewSection = createPreviewSection();
        
        // Status section
        HBox statusSection = createStatusSection();
        
        stepContent.getChildren().addAll(
            titleLabel,
            descriptionLabel,
            new Separator(),
            templateSection,
            new Separator(),
            previewSection,
            statusSection
        );
    }
    
    private VBox createTemplateSelectionSection() {
        VBox section = new VBox(15);
        
        Label sectionLabel = new Label("📋 Available Templates");
        sectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Control buttons
        HBox controlsBox = new HBox(10);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        
        refreshTemplatesButton = new Button("🔄 Refresh Templates");
        refreshTemplatesButton.getStyleClass().add("config-button");
        refreshTemplatesButton.setOnAction(e -> refreshTemplates());
        
        selectAllTemplatesBox = new CheckBox("Select All Recommended");
        selectAllTemplatesBox.setOnAction(e -> selectAllRecommended());
        
        controlsBox.getChildren().addAll(refreshTemplatesButton, selectAllTemplatesBox);
        
        // Template table
        templateTable = createTemplateTable();
        
        section.getChildren().addAll(sectionLabel, controlsBox, templateTable);
        return section;
    }
    
    private TableView<TemplateSelectionInfo> createTemplateTable() {
        TableView<TemplateSelectionInfo> table = new TableView<>();
        table.setPrefHeight(200);
        table.setEditable(true);
        
        // Selection column
        TableColumn<TemplateSelectionInfo, Boolean> selectColumn = new TableColumn<>("Select");
        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setPrefWidth(60);
        selectColumn.setEditable(true);
        
        // Template name column
        TableColumn<TemplateSelectionInfo, String> nameColumn = new TableColumn<>("Template Name");
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameColumn.setPrefWidth(200);
        
        // Template type column
        TableColumn<TemplateSelectionInfo, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        typeColumn.setPrefWidth(150);
        
        // Framework column
        TableColumn<TemplateSelectionInfo, String> frameworkColumn = new TableColumn<>("Framework");
        frameworkColumn.setCellValueFactory(cellData -> cellData.getValue().frameworkProperty());
        frameworkColumn.setPrefWidth(120);
        
        // Description column
        TableColumn<TemplateSelectionInfo, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        descriptionColumn.setPrefWidth(300);
        
        // Recommended column
        TableColumn<TemplateSelectionInfo, String> recommendedColumn = new TableColumn<>("Recommended");
        recommendedColumn.setCellValueFactory(cellData -> {
            boolean recommended = cellData.getValue().isRecommended();
            return new SimpleStringProperty(recommended ? "✓ Yes" : "");
        });
        recommendedColumn.setPrefWidth(100);
        
        table.getColumns().addAll(selectColumn, nameColumn, typeColumn, frameworkColumn, descriptionColumn, recommendedColumn);
        return table;
    }
    
    private VBox createPreviewSection() {
        VBox section = new VBox(15);
        
        Label sectionLabel = new Label("👁️ File Preview");
        sectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Preview controls
        HBox previewControls = new HBox(10);
        previewControls.setAlignment(Pos.CENTER_LEFT);
        
        Label previewLabel = new Label("Preview file:");
        previewFileCombo = new ComboBox<>();
        previewFileCombo.setPrefWidth(250);
        previewFileCombo.setPromptText("Select a file to preview...");
        previewFileCombo.setOnAction(e -> previewSelectedFile());
        
        previewGeneratedButton = new Button("🔍 Generate Preview");
        previewGeneratedButton.getStyleClass().add("primary-button");
        previewGeneratedButton.setOnAction(e -> generatePreview());
        
        previewControls.getChildren().addAll(previewLabel, previewFileCombo, previewGeneratedButton);
        
        // Preview text area
        previewArea = new TextArea();
        previewArea.setPrefRowCount(15);
        previewArea.setWrapText(false);
        previewArea.setEditable(false);
        previewArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");
        previewArea.setText("Select templates and click 'Generate Preview' to see the generated files.");
        
        section.getChildren().addAll(sectionLabel, previewControls, previewArea);
        return section;
    }
    
    private HBox createStatusSection() {
        HBox statusSection = new HBox(10);
        statusSection.setAlignment(Pos.CENTER_LEFT);
        
        statusLabel = new Label("Ready to select templates");
        statusLabel.setStyle("-fx-text-fill: #666666;");
        
        progressBar = new ProgressBar(0);
        progressBar.setVisible(false);
        progressBar.setPrefWidth(200);
        
        statusSection.getChildren().addAll(statusLabel, progressBar);
        return statusSection;
    }
    
    private void refreshTemplates() {
        setOperationInProgress("Loading available templates...");
        
        Task<List<TemplateSelectionInfo>> loadTask = new Task<List<TemplateSelectionInfo>>() {
            @Override
            protected List<TemplateSelectionInfo> call() throws Exception {
                return loadAvailableTemplates();
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                templateTable.getItems().clear();
                templateTable.getItems().addAll(loadTask.getValue());
                setOperationComplete("Templates loaded successfully");
                updatePreviewFileList();
            });
        });
        
        loadTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                setOperationComplete("Failed to load templates");
                showError("Failed to load templates: " + loadTask.getException().getMessage());
            });
        });
        
        new Thread(loadTask).start();
    }
    
    private List<TemplateSelectionInfo> loadAvailableTemplates() {
        List<TemplateSelectionInfo> templates = new ArrayList<>();
        Set<FrameworkType> detectedFrameworks = getDetectedFrameworks();
        MigrationWizardModel.MigrationType migrationType = wizardModel.getMigrationType();
        
        // Docker templates
        templates.add(new TemplateSelectionInfo(
            "Dockerfile", "Container", "Generic", 
            "Multi-stage Dockerfile for containerization", true));
        templates.add(new TemplateSelectionInfo(
            "docker-compose.yml", "Container", "Generic",
            "Docker Compose configuration for local development", false));
        
        // Kubernetes templates
        templates.add(new TemplateSelectionInfo(
            "deployment.yaml", "Kubernetes", "Generic",
            "Kubernetes Deployment manifest", true));
        templates.add(new TemplateSelectionInfo(
            "service.yaml", "Kubernetes", "Generic",
            "Kubernetes Service configuration", true));
        templates.add(new TemplateSelectionInfo(
            "ingress.yaml", "Kubernetes", "Generic",
            "Ingress configuration for external access", false));
        
        // Helm templates
        templates.add(new TemplateSelectionInfo(
            "Chart.yaml", "Helm", "Generic",
            "Helm chart metadata", true));
        templates.add(new TemplateSelectionInfo(
            "values.yaml", "Helm", "Generic",
            "Helm chart default values", true));
        
        // Framework-specific templates
        if (detectedFrameworks.contains(FrameworkType.SPRING_BOOT)) {
            templates.add(new TemplateSelectionInfo(
                "application.yaml", "Config", "Spring Boot",
                "Spring Boot application configuration", true));
            templates.add(new TemplateSelectionInfo(
                "actuator-config.yaml", "Config", "Spring Boot",
                "Spring Boot Actuator health check configuration", false));
        }
        
        if (detectedFrameworks.contains(FrameworkType.REACT) || detectedFrameworks.contains(FrameworkType.ANGULAR)) {
            templates.add(new TemplateSelectionInfo(
                "nginx.conf", "Config", "Frontend",
                "Nginx configuration for frontend applications", true));
            templates.add(new TemplateSelectionInfo(
                "frontend-dockerfile", "Container", "Frontend",
                "Optimized Dockerfile for frontend applications", true));
        }
        
        // CI/CD templates
        if (wizardModel.getMigrationConfiguration().isEnableCiCdPipeline()) {
            templates.add(new TemplateSelectionInfo(
                "Jenkinsfile", "CI/CD", "Generic",
                "Jenkins pipeline configuration", false));
            templates.add(new TemplateSelectionInfo(
                ".gitlab-ci.yml", "CI/CD", "Generic",
                "GitLab CI/CD pipeline configuration", false));
            templates.add(new TemplateSelectionInfo(
                ".github/workflows/deploy.yml", "CI/CD", "Generic",
                "GitHub Actions workflow", false));
        }
        
        // Monitoring templates
        if (wizardModel.getMigrationConfiguration().isEnableMonitoring()) {
            templates.add(new TemplateSelectionInfo(
                "prometheus-config.yml", "Monitoring", "Generic",
                "Prometheus monitoring configuration", false));
            templates.add(new TemplateSelectionInfo(
                "grafana-dashboard.json", "Monitoring", "Generic",
                "Grafana dashboard configuration", false));
        }
        
        // Migration type specific adjustments
        if (migrationType == MigrationWizardModel.MigrationType.PCF_TO_OPENSHIFT) {
            templates.add(new TemplateSelectionInfo(
                "buildconfig.yaml", "OpenShift", "Generic",
                "OpenShift BuildConfig for S2I builds", true));
            templates.add(new TemplateSelectionInfo(
                "route.yaml", "OpenShift", "Generic",
                "OpenShift Route configuration", true));
        }
        
        return templates;
    }
    
    private void selectAllRecommended() {
        boolean selectRecommended = selectAllTemplatesBox.isSelected();
        
        for (TemplateSelectionInfo template : templateTable.getItems()) {
            if (template.isRecommended()) {
                template.setSelected(selectRecommended);
            }
        }
        
        templateTable.refresh();
    }
    
    private void generatePreview() {
        List<TemplateSelectionInfo> selectedTemplates = templateTable.getItems().stream()
            .filter(TemplateSelectionInfo::isSelected)
            .collect(Collectors.toList());
        
        if (selectedTemplates.isEmpty()) {
            showError("Please select at least one template to preview.");
            return;
        }
        
        setOperationInProgress("Generating file preview...");
        
        Task<Map<String, String>> previewTask = new Task<Map<String, String>>() {
            @Override
            protected Map<String, String> call() throws Exception {
                return generatePreviewFiles(selectedTemplates);
            }
        };
        
        previewTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                Map<String, String> previewFiles = previewTask.getValue();
                updatePreviewFiles(previewFiles);
                setOperationComplete("Preview generated successfully");
            });
        });
        
        previewTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                setOperationComplete("Failed to generate preview");
                showError("Failed to generate preview: " + previewTask.getException().getMessage());
            });
        });
        
        new Thread(previewTask).start();
    }
    
    private Map<String, String> generatePreviewFiles(List<TemplateSelectionInfo> selectedTemplates) {
        Map<String, String> previewFiles = new HashMap<>();
        
        // Get first repository for preview (in real scenario, would generate for all)
        if (wizardModel.getSelectedRepositories().isEmpty()) {
            return previewFiles;
        }
        
        RepositoryInfo sampleRepo = wizardModel.getSelectedRepositories().get(0);
        MigrationConfiguration config = wizardModel.getMigrationConfiguration();
        
        // Generate sample content for each selected template
        for (TemplateSelectionInfo template : selectedTemplates) {
            String fileName = template.getName();
            String content = generateSampleContent(template, sampleRepo, config);
            previewFiles.put(fileName, content);
        }
        
        return previewFiles;
    }
    
    private String generateSampleContent(TemplateSelectionInfo template, RepositoryInfo repo, MigrationConfiguration config) {
        switch (template.getName()) {
            case "Dockerfile":
                return generateDockerfileContent(repo);
            case "deployment.yaml":
                return generateDeploymentContent(repo);
            case "service.yaml":
                return generateServiceContent(repo);
            case "Chart.yaml":
                return generateChartContent(repo);
            case "values.yaml":
                return generateValuesContent(repo);
            default:
                return "# " + template.getName() + "\n# Generated template content for " + repo.getName() + 
                       "\n# This is a preview - actual files will be generated during migration\n\n" +
                       "# Template: " + template.getDescription();
        }
    }
    
    private String generateDockerfileContent(RepositoryInfo repo) {
        FrameworkType framework = repo.getDetectedFramework();
        
        if (framework == FrameworkType.SPRING_BOOT) {
            return "# Multi-stage Dockerfile for Spring Boot\n" +
                   "FROM openjdk:17-jdk-alpine AS build\n" +
                   "WORKDIR /app\n" +
                   "COPY pom.xml .\n" +
                   "COPY src ./src\n" +
                   "RUN ./mvnw package -DskipTests\n\n" +
                   "FROM openjdk:17-jre-alpine\n" +
                   "WORKDIR /app\n" +
                   "COPY --from=build /app/target/*.jar app.jar\n" +
                   "EXPOSE 8080\n" +
                   "CMD [\"java\", \"-jar\", \"app.jar\"]";
        } else if (framework == FrameworkType.REACT || framework == FrameworkType.ANGULAR) {
            return "# Multi-stage Dockerfile for Frontend\n" +
                   "FROM node:18-alpine AS build\n" +
                   "WORKDIR /app\n" +
                   "COPY package*.json ./\n" +
                   "RUN npm ci --only=production\n" +
                   "COPY . .\n" +
                   "RUN npm run build\n\n" +
                   "FROM nginx:alpine\n" +
                   "COPY --from=build /app/build /usr/share/nginx/html\n" +
                   "COPY nginx.conf /etc/nginx/nginx.conf\n" +
                   "EXPOSE 80\n" +
                   "CMD [\"nginx\", \"-g\", \"daemon off;\"]";
        } else {
            return "# Generic Dockerfile\n" +
                   "FROM alpine:latest\n" +
                   "WORKDIR /app\n" +
                   "COPY . .\n" +
                   "# Add your application setup here\n" +
                   "CMD [\"./start.sh\"]";
        }
    }
    
    private String generateDeploymentContent(RepositoryInfo repo) {
        return "apiVersion: apps/v1\n" +
               "kind: Deployment\n" +
               "metadata:\n" +
               "  name: " + repo.getName().toLowerCase() + "\n" +
               "  labels:\n" +
               "    app: " + repo.getName().toLowerCase() + "\n" +
               "spec:\n" +
               "  replicas: 3\n" +
               "  selector:\n" +
               "    matchLabels:\n" +
               "      app: " + repo.getName().toLowerCase() + "\n" +
               "  template:\n" +
               "    metadata:\n" +
               "      labels:\n" +
               "        app: " + repo.getName().toLowerCase() + "\n" +
               "    spec:\n" +
               "      containers:\n" +
               "      - name: " + repo.getName().toLowerCase() + "\n" +
               "        image: " + repo.getName().toLowerCase() + ":latest\n" +
               "        ports:\n" +
               "        - containerPort: 8080\n" +
               "        resources:\n" +
               "          requests:\n" +
               "            memory: \"256Mi\"\n" +
               "            cpu: \"250m\"\n" +
               "          limits:\n" +
               "            memory: \"512Mi\"\n" +
               "            cpu: \"500m\"";
    }
    
    private String generateServiceContent(RepositoryInfo repo) {
        return "apiVersion: v1\n" +
               "kind: Service\n" +
               "metadata:\n" +
               "  name: " + repo.getName().toLowerCase() + "-service\n" +
               "spec:\n" +
               "  selector:\n" +
               "    app: " + repo.getName().toLowerCase() + "\n" +
               "  ports:\n" +
               "  - protocol: TCP\n" +
               "    port: 80\n" +
               "    targetPort: 8080\n" +
               "  type: ClusterIP";
    }
    
    private String generateChartContent(RepositoryInfo repo) {
        return "apiVersion: v2\n" +
               "name: " + repo.getName().toLowerCase() + "\n" +
               "description: A Helm chart for " + repo.getName() + "\n" +
               "type: application\n" +
               "version: 0.1.0\n" +
               "appVersion: \"1.0.0\"";
    }
    
    private String generateValuesContent(RepositoryInfo repo) {
        return "# Default values for " + repo.getName().toLowerCase() + "\n" +
               "replicaCount: 3\n\n" +
               "image:\n" +
               "  repository: " + repo.getName().toLowerCase() + "\n" +
               "  pullPolicy: IfNotPresent\n" +
               "  tag: \"latest\"\n\n" +
               "service:\n" +
               "  type: ClusterIP\n" +
               "  port: 80\n\n" +
               "resources:\n" +
               "  limits:\n" +
               "    cpu: 500m\n" +
               "    memory: 512Mi\n" +
               "  requests:\n" +
               "    cpu: 250m\n" +
               "    memory: 256Mi\n\n" +
               "autoscaling:\n" +
               "  enabled: false\n" +
               "  minReplicas: 1\n" +
               "  maxReplicas: 100\n" +
               "  targetCPUUtilizationPercentage: 80";
    }
    
    private void updatePreviewFiles(Map<String, String> previewFiles) {
        // Store preview files in wizard model
        wizardModel.getPreviewFiles().clear();
        for (Map.Entry<String, String> entry : previewFiles.entrySet()) {
            GeneratedFile file = new GeneratedFile();
            file.setFileName(entry.getKey());
            file.setContent(entry.getValue());
            file.setFileType(GeneratedFile.FileType.fromFileName(entry.getKey()));
            wizardModel.getPreviewFiles().add(file);
        }
        
        updatePreviewFileList();
    }
    
    private void updatePreviewFileList() {
        previewFileCombo.getItems().clear();
        for (GeneratedFile file : wizardModel.getPreviewFiles()) {
            previewFileCombo.getItems().add(file.getFileName());
        }
        
        if (!previewFileCombo.getItems().isEmpty()) {
            previewFileCombo.setValue(previewFileCombo.getItems().get(0));
            previewSelectedFile();
        }
    }
    
    private void previewSelectedFile() {
        String selectedFile = previewFileCombo.getValue();
        if (selectedFile != null) {
            GeneratedFile file = wizardModel.getPreviewFiles().stream()
                .filter(f -> f.getFileName().equals(selectedFile))
                .findFirst()
                .orElse(null);
            
            if (file != null) {
                previewArea.setText(file.getContent());
            }
        }
    }
    
    private String determineFileType(String fileName) {
        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            return "YAML";
        } else if (fileName.equals("Dockerfile")) {
            return "Docker";
        } else if (fileName.endsWith(".json")) {
            return "JSON";
        } else if (fileName.endsWith(".conf")) {
            return "Config";
        } else {
            return "Text";
        }
    }
    
    private Set<FrameworkType> getDetectedFrameworks() {
        return wizardModel.getDetectionResults().values().stream()
            .map(MigrationWizardModel.FrameworkDetectionResult::getPrimaryFramework)
            .collect(Collectors.toSet());
    }
    
    private void setOperationInProgress(String message) {
        statusLabel.setText(message);
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        refreshTemplatesButton.setDisable(true);
        previewGeneratedButton.setDisable(true);
    }
    
    private void setOperationComplete(String message) {
        statusLabel.setText(message);
        progressBar.setVisible(false);
        refreshTemplatesButton.setDisable(false);
        previewGeneratedButton.setDisable(false);
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Template Selection Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @Override
    public String getStepTitle() {
        return "Template Selection";
    }
    
    @Override
    public String getStepDescription() {
        return "Select templates to generate and preview the files that will be created.";
    }
    
    @Override
    public Node getStepContent() {
        return stepContent;
    }
    
    @Override
    public void onStepEnter() {
        refreshTemplates();
        logger.info("Template selection step entered");
    }
    
    @Override
    public void onStepExit() {
        // Save selected templates to wizard model
        List<TemplateSelectionInfo> selectedTemplates = templateTable.getItems().stream()
            .filter(TemplateSelectionInfo::isSelected)
            .collect(Collectors.toList());
        
        for (TemplateSelectionInfo template : selectedTemplates) {
            wizardModel.setSelectedTemplate(template.getType(), template.getName());
        }
        
        logger.info("Template selection completed. Selected {} templates", selectedTemplates.size());
    }
    
    @Override
    public boolean validateStep() {
        boolean hasSelectedTemplates = templateTable.getItems().stream()
            .anyMatch(TemplateSelectionInfo::isSelected);
        
        if (!hasSelectedTemplates) {
            showError("Please select at least one template to proceed.");
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
        boolean hasSelected = templateTable.getItems().stream().anyMatch(TemplateSelectionInfo::isSelected);
        boolean hasPreview = !wizardModel.getPreviewFiles().isEmpty();
        
        if (hasSelected && hasPreview) {
            return 1.0;
        } else if (hasSelected) {
            return 0.5;
        } else {
            return 0.0;
        }
    }
    
    /**
     * Data class for template selection information.
     */
    public static class TemplateSelectionInfo {
        private final SimpleStringProperty name;
        private final SimpleStringProperty type;
        private final SimpleStringProperty framework;
        private final SimpleStringProperty description;
        private final SimpleBooleanProperty selected;
        private final boolean recommended;
        
        public TemplateSelectionInfo(String name, String type, String framework, 
                                   String description, boolean recommended) {
            this.name = new SimpleStringProperty(name);
            this.type = new SimpleStringProperty(type);
            this.framework = new SimpleStringProperty(framework);
            this.description = new SimpleStringProperty(description);
            this.selected = new SimpleBooleanProperty(recommended);
            this.recommended = recommended;
        }
        
        // Property getters for JavaFX binding
        public SimpleStringProperty nameProperty() { return name; }
        public SimpleStringProperty typeProperty() { return type; }
        public SimpleStringProperty frameworkProperty() { return framework; }
        public SimpleStringProperty descriptionProperty() { return description; }
        public SimpleBooleanProperty selectedProperty() { return selected; }
        
        // Value getters
        public String getName() { return name.get(); }
        public String getType() { return type.get(); }
        public String getFramework() { return framework.get(); }
        public String getDescription() { return description.get(); }
        public boolean isSelected() { return selected.get(); }
        public boolean isRecommended() { return recommended; }
        
        // Setters
        public void setSelected(boolean selected) { this.selected.set(selected); }
    }
}