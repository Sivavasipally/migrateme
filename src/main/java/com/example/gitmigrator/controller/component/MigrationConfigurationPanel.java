package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.MigrationConfiguration;
import com.example.gitmigrator.service.TemplateManagementService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * Enhanced configuration panel for migration settings with tabbed interface,
 * template management, and custom settings editor.
 */
public class MigrationConfigurationPanel extends VBox {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationConfigurationPanel.class);
    
    // Services
    private TemplateManagementService templateService;
    
    // Configuration properties
    private final StringProperty targetPlatform = new SimpleStringProperty("kubernetes");
    private final BooleanProperty includeHelm = new SimpleBooleanProperty(true);
    private final BooleanProperty includeDockerfile = new SimpleBooleanProperty(true);
    private final BooleanProperty includeCiCd = new SimpleBooleanProperty(false);
    private final BooleanProperty includeMonitoring = new SimpleBooleanProperty(false);
    private final ObservableList<CustomSetting> customSettings = FXCollections.observableArrayList();
    
    // UI Components
    private TabPane tabPane;
    private ComboBox<String> platformComboBox;
    private CheckBox helmCheckBox;
    private CheckBox dockerfileCheckBox;
    private CheckBox cicdCheckBox;
    private CheckBox monitoringCheckBox;
    private ComboBox<String> templateSelector;
    private TextArea templatePreview;
    private TableView<CustomSetting> customSettingsTable;
    private Label validationLabel;
    
    // Callbacks
    private Consumer<MigrationConfiguration> onConfigurationChanged;
    private Consumer<String> onValidationError;
    
    public MigrationConfigurationPanel(TemplateManagementService templateService) {
        this.templateService = templateService;
        initializePanel();
        setupEventHandlers();
        setupValidation();
    }
    
    /**
     * Initialize the configuration panel with tabbed interface.
     */
    private void initializePanel() {
        setSpacing(10);
        setPadding(new Insets(10));
        getStyleClass().add("migration-config-panel");
        
        // Title
        Text title = new Text("Migration Configuration");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.getStyleClass().add("config-title");
        
        // Create tabbed interface
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Platform tab
        Tab platformTab = new Tab("Platform", createPlatformTab());
        platformTab.getStyleClass().add("config-tab");
        
        // Components tab
        Tab componentsTab = new Tab("Components", createComponentsTab());
        componentsTab.getStyleClass().add("config-tab");
        
        // Templates tab
        Tab templatesTab = new Tab("Templates", createTemplatesTab());
        templatesTab.getStyleClass().add("config-tab");
        
        // Advanced tab
        Tab advancedTab = new Tab("Advanced", createAdvancedTab());
        advancedTab.getStyleClass().add("config-tab");
        
        tabPane.getTabs().addAll(platformTab, componentsTab, templatesTab, advancedTab);
        
        // Validation feedback
        validationLabel = new Label();
        validationLabel.getStyleClass().add("validation-label");
        validationLabel.setVisible(false);
        
        getChildren().addAll(title, tabPane, validationLabel);
    }
    
    /**
     * Create the platform selection tab.
     */
    private VBox createPlatformTab() {
        VBox platformTab = new VBox(15);
        platformTab.setPadding(new Insets(15));
        
        // Platform selection
        Label platformLabel = new Label("Target Platform:");
        platformLabel.getStyleClass().add("config-label");
        
        platformComboBox = new ComboBox<>();
        platformComboBox.getItems().addAll("kubernetes", "openshift", "docker-compose");
        platformComboBox.setValue("kubernetes");
        platformComboBox.getStyleClass().add("config-combo");
        
        // Platform descriptions
        TextArea platformDescription = new TextArea();
        platformDescription.setEditable(false);
        platformDescription.setPrefRowCount(4);
        platformDescription.getStyleClass().add("platform-description");
        updatePlatformDescription(platformDescription, "kubernetes");
        
        // Update description when platform changes
        platformComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            targetPlatform.set(newVal);
            updatePlatformDescription(platformDescription, newVal);
        });
        
        // Resource configuration
        Label resourceLabel = new Label("Resource Configuration:");
        resourceLabel.getStyleClass().add("config-label");
        
        GridPane resourceGrid = new GridPane();
        resourceGrid.setHgap(10);
        resourceGrid.setVgap(10);
        
        // CPU and Memory settings
        resourceGrid.add(new Label("CPU Limit:"), 0, 0);
        TextField cpuField = new TextField("500m");
        cpuField.getStyleClass().add("config-field");
        resourceGrid.add(cpuField, 1, 0);
        
        resourceGrid.add(new Label("Memory Limit:"), 0, 1);
        TextField memoryField = new TextField("512Mi");
        memoryField.getStyleClass().add("config-field");
        resourceGrid.add(memoryField, 1, 1);
        
        resourceGrid.add(new Label("Replicas:"), 0, 2);
        Spinner<Integer> replicasSpinner = new Spinner<>(1, 10, 1);
        replicasSpinner.getStyleClass().add("config-spinner");
        resourceGrid.add(replicasSpinner, 1, 2);
        
        platformTab.getChildren().addAll(
            platformLabel, platformComboBox,
            new Separator(),
            new Label("Platform Description:"),
            platformDescription,
            new Separator(),
            resourceLabel, resourceGrid
        );
        
        return platformTab;
    }
    
    /**
     * Create the components selection tab with scrolling support.
     */
    private ScrollPane createComponentsTab() {
        VBox componentsTab = new VBox(15);
        componentsTab.setPadding(new Insets(15));
        
        Label componentsLabel = new Label("Optional Components:");
        componentsLabel.getStyleClass().add("config-label");
        
        // Component checkboxes with descriptions
        VBox checkboxContainer = new VBox(10);
        
        // Dockerfile
        dockerfileCheckBox = new CheckBox("Generate Dockerfile");
        dockerfileCheckBox.setSelected(true);
        dockerfileCheckBox.getStyleClass().add("config-checkbox");
        Label dockerfileDesc = new Label("Creates optimized Docker container configuration");
        dockerfileDesc.getStyleClass().add("checkbox-description");
        
        // Helm Charts
        helmCheckBox = new CheckBox("Generate Helm Charts");
        helmCheckBox.setSelected(true);
        helmCheckBox.getStyleClass().add("config-checkbox");
        Label helmDesc = new Label("Kubernetes package manager templates for easy deployment");
        helmDesc.getStyleClass().add("checkbox-description");
        
        // CI/CD Pipeline
        cicdCheckBox = new CheckBox("Generate CI/CD Pipeline");
        cicdCheckBox.setSelected(false);
        cicdCheckBox.getStyleClass().add("config-checkbox");
        Label cicdDesc = new Label("GitHub Actions, GitLab CI, or Jenkins pipeline configuration");
        cicdDesc.getStyleClass().add("checkbox-description");
        
        // Monitoring
        monitoringCheckBox = new CheckBox("Generate Monitoring Configuration");
        monitoringCheckBox.setSelected(false);
        monitoringCheckBox.getStyleClass().add("config-checkbox");
        Label monitoringDesc = new Label("Prometheus metrics and health check endpoints");
        monitoringDesc.getStyleClass().add("checkbox-description");
        
        checkboxContainer.getChildren().addAll(
            dockerfileCheckBox, dockerfileDesc,
            new Separator(),
            helmCheckBox, helmDesc,
            new Separator(),
            cicdCheckBox, cicdDesc,
            new Separator(),
            monitoringCheckBox, monitoringDesc
        );
        
        // Base image configuration
        Label baseImageLabel = new Label("Base Image Configuration:");
        baseImageLabel.getStyleClass().add("config-label");
        
        GridPane baseImageGrid = new GridPane();
        baseImageGrid.setHgap(10);
        baseImageGrid.setVgap(10);
        
        baseImageGrid.add(new Label("Base Image:"), 0, 0);
        ComboBox<String> baseImageCombo = new ComboBox<>();
        baseImageCombo.getItems().addAll(
            "openjdk:17-jre-slim",
            "node:18-alpine",
            "python:3.11-slim",
            "nginx:alpine",
            "custom"
        );
        baseImageCombo.setValue("openjdk:17-jre-slim");
        baseImageCombo.getStyleClass().add("config-combo");
        baseImageGrid.add(baseImageCombo, 1, 0);
        
        componentsTab.getChildren().addAll(
            componentsLabel, checkboxContainer,
            new Separator(),
            baseImageLabel, baseImageGrid
        );
        
        // Wrap in ScrollPane for proper scrolling
        ScrollPane scrollPane = new ScrollPane(componentsTab);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("config-scroll-pane");
        
        return scrollPane;
    }
    
    /**
     * Create the templates management tab with scrolling support.
     */
    private ScrollPane createTemplatesTab() {
        VBox templatesTab = new VBox(15);
        templatesTab.setPadding(new Insets(15));
        
        // Template selector
        Label templateLabel = new Label("Migration Templates:");
        templateLabel.getStyleClass().add("config-label");
        
        HBox templateControls = new HBox(10);
        templateControls.setAlignment(Pos.CENTER_LEFT);
        
        templateSelector = new ComboBox<>();
        templateSelector.getStyleClass().add("config-combo");
        templateSelector.setPrefWidth(200);
        refreshTemplateList();
        
        Button loadTemplateBtn = new Button("Load");
        loadTemplateBtn.getStyleClass().add("config-button");
        loadTemplateBtn.setOnAction(e -> loadSelectedTemplate());
        
        Button saveTemplateBtn = new Button("Save As...");
        saveTemplateBtn.getStyleClass().add("config-button");
        saveTemplateBtn.setOnAction(e -> saveCurrentTemplate());
        
        Button deleteTemplateBtn = new Button("Delete");
        deleteTemplateBtn.getStyleClass().add("config-button-danger");
        deleteTemplateBtn.setOnAction(e -> deleteSelectedTemplate());
        
        // Add new template management buttons
        Button newTemplateBtn = new Button("New");
        newTemplateBtn.getStyleClass().add("config-button");
        newTemplateBtn.setOnAction(e -> createNewTemplate());
        
        Button editTemplateBtn = new Button("Edit");
        editTemplateBtn.getStyleClass().add("config-button");
        editTemplateBtn.setOnAction(e -> editSelectedTemplate());
        
        Button copyTemplateBtn = new Button("Copy");
        copyTemplateBtn.getStyleClass().add("config-button");
        copyTemplateBtn.setOnAction(e -> copySelectedTemplate());
        
        Button exportTemplateBtn = new Button("Export");
        exportTemplateBtn.getStyleClass().add("config-button");
        exportTemplateBtn.setOnAction(e -> exportSelectedTemplate());
        
        templateControls.getChildren().addAll(
            templateSelector, loadTemplateBtn, newTemplateBtn, editTemplateBtn, 
            copyTemplateBtn, saveTemplateBtn, exportTemplateBtn, deleteTemplateBtn
        );
        
        // Template file type selector
        Label fileTypeLabel = new Label("Template File Type:");
        fileTypeLabel.getStyleClass().add("config-label");
        
        ComboBox<String> templateFileTypeCombo = new ComboBox<>();
        templateFileTypeCombo.getItems().addAll(
            "Dockerfile.ftl", "Chart.yaml.ftl", "values.yaml.ftl", 
            "deployment.yaml.ftl", "service.yaml.ftl", "ingress.yaml.ftl",
            "configmap.yaml.ftl", "secret.yaml.ftl", "custom.ftl"
        );
        templateFileTypeCombo.setValue("Dockerfile.ftl");
        templateFileTypeCombo.getStyleClass().add("config-combo");
        templateFileTypeCombo.setPrefWidth(150);
        
        HBox fileTypeControls = new HBox(10);
        fileTypeControls.setAlignment(Pos.CENTER_LEFT);
        fileTypeControls.getChildren().addAll(fileTypeLabel, templateFileTypeCombo);
        
        // Template editor with syntax highlighting
        Label editorLabel = new Label("FreeMarker Template Editor:");
        editorLabel.getStyleClass().add("config-label");
        
        templatePreview = new TextArea();
        templatePreview.setEditable(true);
        templatePreview.setPrefRowCount(15);
        templatePreview.setWrapText(false);
        templatePreview.getStyleClass().addAll("template-preview", "ftl-editor");
        templatePreview.setPromptText("Select a template to edit or create a new one...\n\n" +
            "FreeMarker Syntax Examples:\n" +
            "${variable} - Variable substitution\n" +
            "<#if condition>...</#if> - Conditional\n" +
            "<#list items as item>...</#list> - Loop\n" +
            "<#-- Comment --> - Comments");
        
        // Add template file type change listener
        templateFileTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTemplateEditorForFileType(newVal);
        });
        
        // Update preview when template selection changes
        templateSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTemplatePreview(newVal);
        });
        
        // Built-in templates section
        Label builtInLabel = new Label("Built-in Templates:");
        builtInLabel.getStyleClass().add("config-label");
        
        VBox builtInTemplates = new VBox(5);
        
        Button microserviceBtn = new Button("Microservice Template");
        microserviceBtn.getStyleClass().add("template-button");
        microserviceBtn.setOnAction(e -> applyBuiltInTemplate("microservice"));
        
        Button monolithBtn = new Button("Monolith Template");
        monolithBtn.getStyleClass().add("template-button");
        monolithBtn.setOnAction(e -> applyBuiltInTemplate("monolith"));
        
        Button frontendBtn = new Button("Frontend App Template");
        frontendBtn.getStyleClass().add("template-button");
        frontendBtn.setOnAction(e -> applyBuiltInTemplate("frontend"));
        
        builtInTemplates.getChildren().addAll(microserviceBtn, monolithBtn, frontendBtn);
        
        // Template validation and help section
        Label validationLabel = new Label("Template Validation:");
        validationLabel.getStyleClass().add("config-label");
        
        Button validateTemplateBtn = new Button("Validate FTL Syntax");
        validateTemplateBtn.getStyleClass().add("config-button");
        validateTemplateBtn.setOnAction(e -> validateCurrentTemplate());
        
        Button previewOutputBtn = new Button("Preview Output");
        previewOutputBtn.getStyleClass().add("config-button");
        previewOutputBtn.setOnAction(e -> previewTemplateOutput());
        
        HBox validationControls = new HBox(10);
        validationControls.setAlignment(Pos.CENTER_LEFT);
        validationControls.getChildren().addAll(validateTemplateBtn, previewOutputBtn);
        
        templatesTab.getChildren().addAll(
            templateLabel, templateControls,
            new Separator(),
            fileTypeControls,
            new Separator(),
            editorLabel, templatePreview,
            new Separator(),
            validationLabel, validationControls,
            new Separator(),
            builtInLabel, builtInTemplates
        );
        
        // Wrap in ScrollPane for proper scrolling
        ScrollPane scrollPane = new ScrollPane(templatesTab);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("config-scroll-pane");
        
        return scrollPane;
    }
    
    /**
     * Create the advanced settings tab with scrolling support.
     */
    private ScrollPane createAdvancedTab() {
        VBox advancedTab = new VBox(15);
        advancedTab.setPadding(new Insets(15));
        
        // Custom settings table
        Label customLabel = new Label("Custom Settings:");
        customLabel.getStyleClass().add("config-label");
        
        customSettingsTable = new TableView<>();
        customSettingsTable.setEditable(true);
        customSettingsTable.setPrefHeight(200);
        customSettingsTable.getStyleClass().add("custom-settings-table");
        
        // Key column
        TableColumn<CustomSetting, String> keyColumn = new TableColumn<>("Key");
        keyColumn.setPrefWidth(150);
        keyColumn.setCellValueFactory(cellData -> cellData.getValue().keyProperty());
        keyColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        keyColumn.setOnEditCommit(event -> {
            event.getRowValue().setKey(event.getNewValue());
            notifyConfigurationChanged();
        });
        
        // Value column
        TableColumn<CustomSetting, String> valueColumn = new TableColumn<>("Value");
        valueColumn.setPrefWidth(200);
        valueColumn.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setOnEditCommit(event -> {
            event.getRowValue().setValue(event.getNewValue());
            notifyConfigurationChanged();
        });
        
        // Description column
        TableColumn<CustomSetting, String> descColumn = new TableColumn<>("Description");
        descColumn.setPrefWidth(250);
        descColumn.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        descColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        descColumn.setOnEditCommit(event -> {
            event.getRowValue().setDescription(event.getNewValue());
        });
        
        customSettingsTable.getColumns().addAll(keyColumn, valueColumn, descColumn);
        customSettingsTable.setItems(customSettings);
        
        // Custom settings controls
        HBox customControls = new HBox(10);
        customControls.setAlignment(Pos.CENTER_LEFT);
        
        Button addSettingBtn = new Button("Add Setting");
        addSettingBtn.getStyleClass().add("config-button");
        addSettingBtn.setOnAction(e -> addCustomSetting());
        
        Button removeSettingBtn = new Button("Remove Selected");
        removeSettingBtn.getStyleClass().add("config-button-danger");
        removeSettingBtn.setOnAction(e -> removeSelectedSetting());
        
        customControls.getChildren().addAll(addSettingBtn, removeSettingBtn);
        
        // Environment variables section
        Label envLabel = new Label("Environment Variables:");
        envLabel.getStyleClass().add("config-label");
        
        TextArea envTextArea = new TextArea();
        envTextArea.setPrefRowCount(5);
        envTextArea.getStyleClass().add("config-textarea");
        envTextArea.setPromptText("KEY1=value1\\nKEY2=value2\\n...");
        
        advancedTab.getChildren().addAll(
            customLabel, customSettingsTable, customControls,
            new Separator(),
            envLabel, envTextArea
        );
        
        // Wrap in ScrollPane for proper scrolling
        ScrollPane scrollPane = new ScrollPane(advancedTab);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("config-scroll-pane");
        
        return scrollPane;
    }
    
    /**
     * Setup event handlers for configuration changes.
     */
    private void setupEventHandlers() {
        // Bind checkbox properties
        includeHelm.bind(helmCheckBox.selectedProperty());
        includeDockerfile.bind(dockerfileCheckBox.selectedProperty());
        includeCiCd.bind(cicdCheckBox.selectedProperty());
        includeMonitoring.bind(monitoringCheckBox.selectedProperty());
        
        // Listen for changes and notify
        targetPlatform.addListener((obs, oldVal, newVal) -> notifyConfigurationChanged());
        includeHelm.addListener((obs, oldVal, newVal) -> notifyConfigurationChanged());
        includeDockerfile.addListener((obs, oldVal, newVal) -> notifyConfigurationChanged());
        includeCiCd.addListener((obs, oldVal, newVal) -> notifyConfigurationChanged());
        includeMonitoring.addListener((obs, oldVal, newVal) -> notifyConfigurationChanged());
    }
    
    /**
     * Setup validation for configuration options.
     */
    private void setupValidation() {
        // Validate platform-specific requirements
        targetPlatform.addListener((obs, oldVal, newVal) -> validateConfiguration());
        includeHelm.addListener((obs, oldVal, newVal) -> validateConfiguration());
    }
    
    /**
     * Update platform description based on selection.
     */
    private void updatePlatformDescription(TextArea descArea, String platform) {
        String description;
        switch (platform) {
            case "kubernetes":
                description = "Kubernetes: Container orchestration platform for automated deployment, scaling, and management. " +
                             "Generates Deployment, Service, and ConfigMap resources.";
                break;
            case "openshift":
                description = "OpenShift: Enterprise Kubernetes platform with additional developer and operational tools. " +
                             "Generates DeploymentConfig, Route, and BuildConfig resources.";
                break;
            case "docker-compose":
                description = "Docker Compose: Tool for defining and running multi-container Docker applications. " +
                             "Generates docker-compose.yml with service definitions and networking.";
                break;
            default:
                description = "Select a platform to see its description.";
        }
        descArea.setText(description);
    }
    
    /**
     * Refresh the template list from the template service.
     */
    private void refreshTemplateList() {
        if (templateService != null) {
            try {
                List<String> templates = templateService.getAvailableTemplates();
                templateSelector.getItems().clear();
                templateSelector.getItems().addAll(templates);
            } catch (Exception e) {
                logger.error("Error refreshing template list", e);
                showValidationError("Error loading templates: " + e.getMessage());
            }
        }
    }
    
    /**
     * Load the selected template.
     */
    private void loadSelectedTemplate() {
        String selectedTemplate = templateSelector.getValue();
        if (selectedTemplate != null && templateService != null) {
            try {
                MigrationConfiguration config = templateService.loadTemplate(selectedTemplate);
                applyConfiguration(config);
                showValidationMessage("Template loaded successfully", false);
            } catch (Exception e) {
                logger.error("Error loading template: " + selectedTemplate, e);
                showValidationError("Error loading template: " + e.getMessage());
            }
        }
    }
    
    /**
     * Save current configuration as a template.
     */
    private void saveCurrentTemplate() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Save Template");
        dialog.setHeaderText("Save Migration Template");
        dialog.setContentText("Template name:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (templateService != null) {
                try {
                    MigrationConfiguration config = getCurrentConfiguration();
                    templateService.saveTemplate(name, config);
                    refreshTemplateList();
                    templateSelector.setValue(name);
                    showValidationMessage("Template saved successfully", false);
                } catch (Exception e) {
                    logger.error("Error saving template: " + name, e);
                    showValidationError("Error saving template: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Delete the selected template.
     */
    private void deleteSelectedTemplate() {
        String selectedTemplate = templateSelector.getValue();
        if (selectedTemplate != null && templateService != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Template");
            alert.setHeaderText("Delete Migration Template");
            alert.setContentText("Are you sure you want to delete template: " + selectedTemplate + "?");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    templateService.deleteTemplate(selectedTemplate);
                    refreshTemplateList();
                    templateSelector.setValue(null);
                    templatePreview.clear();
                    showValidationMessage("Template deleted successfully", false);
                } catch (Exception e) {
                    logger.error("Error deleting template: " + selectedTemplate, e);
                    showValidationError("Error deleting template: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Update template preview.
     */
    private void updateTemplatePreview(String templateName) {
        if (templateName != null && templateService != null) {
            try {
                MigrationConfiguration config = templateService.loadTemplate(templateName);
                templatePreview.setText(formatConfigurationPreview(config));
            } catch (Exception e) {
                templatePreview.setText("Error loading template preview: " + e.getMessage());
            }
        } else {
            templatePreview.clear();
        }
    }
    
    /**
     * Apply a built-in template.
     */
    private void applyBuiltInTemplate(String templateType) {
        MigrationConfiguration config = createBuiltInTemplate(templateType);
        applyConfiguration(config);
        showValidationMessage("Built-in template applied", false);
    }
    
    /**
     * Create a built-in template configuration.
     */
    private MigrationConfiguration createBuiltInTemplate(String templateType) {
        MigrationConfiguration config = new MigrationConfiguration();
        
        switch (templateType) {
            case "microservice":
                config.setTargetPlatform("kubernetes");
                config.getOptionalComponents().addAll(Arrays.asList("helm", "dockerfile", "cicd", "monitoring"));
                config.getCustomSettings().put("replicas", "3");
                config.getCustomSettings().put("cpu-limit", "500m");
                config.getCustomSettings().put("memory-limit", "512Mi");
                break;
            case "monolith":
                config.setTargetPlatform("kubernetes");
                config.getOptionalComponents().addAll(Arrays.asList("helm", "dockerfile"));
                config.getCustomSettings().put("replicas", "2");
                config.getCustomSettings().put("cpu-limit", "1000m");
                config.getCustomSettings().put("memory-limit", "1Gi");
                break;
            case "frontend":
                config.setTargetPlatform("kubernetes");
                config.getOptionalComponents().addAll(Arrays.asList("dockerfile", "cicd"));
                config.getCustomSettings().put("replicas", "2");
                config.getCustomSettings().put("cpu-limit", "100m");
                config.getCustomSettings().put("memory-limit", "128Mi");
                break;
        }
        
        return config;
    }
    
    /**
     * Apply configuration to UI components.
     */
    private void applyConfiguration(MigrationConfiguration config) {
        platformComboBox.setValue(config.getTargetPlatform());
        
        Set<String> components = config.getOptionalComponents();
        helmCheckBox.setSelected(components.contains("helm"));
        dockerfileCheckBox.setSelected(components.contains("dockerfile"));
        cicdCheckBox.setSelected(components.contains("cicd"));
        monitoringCheckBox.setSelected(components.contains("monitoring"));
        
        // Apply custom settings
        customSettings.clear();
        config.getCustomSettings().forEach((key, value) -> {
            customSettings.add(new CustomSetting(key, value, ""));
        });
    }
    
    /**
     * Get current configuration from UI.
     */
    public MigrationConfiguration getCurrentConfiguration() {
        MigrationConfiguration config = new MigrationConfiguration();
        config.setTargetPlatform(targetPlatform.get());
        
        Set<String> components = new HashSet<>();
        if (includeHelm.get()) components.add("helm");
        if (includeDockerfile.get()) components.add("dockerfile");
        if (includeCiCd.get()) components.add("cicd");
        if (includeMonitoring.get()) components.add("monitoring");
        config.setOptionalComponents(components);
        
        Map<String, String> customSettingsMap = new HashMap<>();
        customSettings.forEach(setting -> {
            if (setting.getKey() != null && !setting.getKey().trim().isEmpty()) {
                customSettingsMap.put(setting.getKey(), setting.getValue());
            }
        });
        config.setCustomSettings(customSettingsMap);
        
        return config;
    }
    
    /**
     * Format configuration for preview display.
     */
    private String formatConfigurationPreview(MigrationConfiguration config) {
        StringBuilder sb = new StringBuilder();
        sb.append("Target Platform: ").append(config.getTargetPlatform()).append("\n\n");
        
        sb.append("Optional Components:\n");
        config.getOptionalComponents().forEach(component -> 
            sb.append("  - ").append(component).append("\n"));
        
        if (!config.getCustomSettings().isEmpty()) {
            sb.append("\nCustom Settings:\n");
            config.getCustomSettings().forEach((key, value) -> 
                sb.append("  ").append(key).append(": ").append(value).append("\n"));
        }
        
        return sb.toString();
    }
    
    /**
     * Add a new custom setting.
     */
    private void addCustomSetting() {
        customSettings.add(new CustomSetting("", "", ""));
        customSettingsTable.getSelectionModel().selectLast();
        customSettingsTable.edit(customSettings.size() - 1, customSettingsTable.getColumns().get(0));
    }
    
    /**
     * Remove selected custom setting.
     */
    private void removeSelectedSetting() {
        CustomSetting selected = customSettingsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            customSettings.remove(selected);
            notifyConfigurationChanged();
        }
    }
    
    /**
     * Validate current configuration.
     */
    private void validateConfiguration() {
        List<String> errors = new ArrayList<>();
        
        // Platform-specific validation
        if ("kubernetes".equals(targetPlatform.get()) && !includeDockerfile.get()) {
            errors.add("Kubernetes deployment requires Dockerfile generation");
        }
        
        if (errors.isEmpty()) {
            hideValidationMessage();
        } else {
            showValidationError(String.join("; ", errors));
        }
    }
    
    /**
     * Show validation error message.
     */
    private void showValidationError(String message) {
        showValidationMessage(message, true);
        if (onValidationError != null) {
            onValidationError.accept(message);
        }
    }
    
    /**
     * Show validation message.
     */
    private void showValidationMessage(String message, boolean isError) {
        validationLabel.setText(message);
        validationLabel.getStyleClass().removeAll("validation-error", "validation-success");
        validationLabel.getStyleClass().add(isError ? "validation-error" : "validation-success");
        validationLabel.setVisible(true);
    }
    
    /**
     * Hide validation message.
     */
    private void hideValidationMessage() {
        validationLabel.setVisible(false);
    }
    
    /**
     * Notify configuration changed.
     */
    private void notifyConfigurationChanged() {
        if (onConfigurationChanged != null) {
            onConfigurationChanged.accept(getCurrentConfiguration());
        }
        validateConfiguration();
    }
    
    // Setter methods for callbacks
    public void setOnConfigurationChanged(Consumer<MigrationConfiguration> callback) {
        this.onConfigurationChanged = callback;
    }
    
    public void setOnValidationError(Consumer<String> callback) {
        this.onValidationError = callback;
    }
    
    // === Enhanced Template Management Methods ===
    
    /**
     * Create a new template from scratch.
     */
    private void createNewTemplate() {
        templatePreview.clear();
        templatePreview.setPromptText("Enter your new FreeMarker template content here...\n\n" +
            "Example Dockerfile template:\n" +
            "FROM ${baseImage!'openjdk:17-jre-slim'}\n" +
            "WORKDIR /app\n" +
            "COPY target/${artifactId}-${version}.jar app.jar\n" +
            "EXPOSE ${serverPort!8080}\n" +
            "CMD [\"java\", \"-jar\", \"app.jar\"]\n\n" +
            "Available variables: ${artifactId}, ${version}, ${baseImage}, ${serverPort}");
        templatePreview.requestFocus();
        showValidationMessage("New template created. Enter content and save when ready.", false);
    }
    
    /**
     * Edit the currently selected template.
     */
    private void editSelectedTemplate() {
        String selectedTemplate = templateSelector.getValue();
        if (selectedTemplate == null || selectedTemplate.isEmpty()) {
            showValidationError("Please select a template to edit");
            return;
        }
        
        templatePreview.setEditable(true);
        templatePreview.requestFocus();
        showValidationMessage("Template editing enabled. Make changes and save when done.", false);
    }
    
    /**
     * Copy the selected template to create a new one.
     */
    private void copySelectedTemplate() {
        String selectedTemplate = templateSelector.getValue();
        if (selectedTemplate == null || selectedTemplate.isEmpty()) {
            showValidationError("Please select a template to copy");
            return;
        }
        
        // Create a copy with modified name
        String currentContent = templatePreview.getText();
        templatePreview.setText(currentContent);
        templateSelector.getSelectionModel().clearSelection();
        showValidationMessage("Template copied. Modify as needed and save with a new name.", false);
    }
    
    /**
     * Export the selected template to a file.
     */
    private void exportSelectedTemplate() {
        String selectedTemplate = templateSelector.getValue();
        if (selectedTemplate == null || selectedTemplate.isEmpty()) {
            showValidationError("Please select a template to export");
            return;
        }
        
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Template");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("FreeMarker Templates", "*.ftl")
            );
            fileChooser.setInitialFileName(selectedTemplate + ".ftl");
            
            java.io.File file = fileChooser.showSaveDialog(templatePreview.getScene().getWindow());
            if (file != null) {
                java.nio.file.Files.write(file.toPath(), templatePreview.getText().getBytes());
                showValidationMessage("Template exported to: " + file.getAbsolutePath(), false);
            }
        } catch (Exception e) {
            showValidationError("Failed to export template: " + e.getMessage());
        }
    }
    
    /**
     * Update template editor content based on file type selection.
     */
    private void updateTemplateEditorForFileType(String fileType) {
        if (fileType == null) return;
        
        String templateContent = getTemplateExample(fileType);
        if (templatePreview.getText().isEmpty() || templatePreview.getText().equals(templatePreview.getPromptText())) {
            templatePreview.setText(templateContent);
        }
        
        // Update prompt text with file-type specific help
        templatePreview.setPromptText("Enter " + fileType + " template content...\n\n" + templateContent);
    }
    
    /**
     * Get example template content for different file types.
     */
    private String getTemplateExample(String fileType) {
        switch (fileType) {
            case "Dockerfile.ftl":
                return "FROM ${baseImage!'openjdk:17-jre-slim'}\n" +
                       "WORKDIR /app\n" +
                       "COPY target/${artifactId}-${version}.jar app.jar\n" +
                       "EXPOSE ${serverPort!8080}\n" +
                       "CMD [\"java\", \"-jar\", \"app.jar\"]";
                       
            case "Chart.yaml.ftl":
                return "apiVersion: v2\n" +
                       "name: ${artifactId}\n" +
                       "description: A Helm chart for ${name}\n" +
                       "type: application\n" +
                       "version: 0.1.0\n" +
                       "appVersion: \"${version}\"";
                       
            case "deployment.yaml.ftl":
                return "apiVersion: apps/v1\n" +
                       "kind: Deployment\n" +
                       "metadata:\n" +
                       "  name: ${artifactId}\n" +
                       "spec:\n" +
                       "  replicas: ${replicas!3}\n" +
                       "  selector:\n" +
                       "    matchLabels:\n" +
                       "      app: ${artifactId}\n" +
                       "  template:\n" +
                       "    metadata:\n" +
                       "      labels:\n" +
                       "        app: ${artifactId}\n" +
                       "    spec:\n" +
                       "      containers:\n" +
                       "      - name: ${artifactId}\n" +
                       "        image: ${artifactId}:${version}\n" +
                       "        ports:\n" +
                       "        - containerPort: ${serverPort!8080}";
                       
            case "service.yaml.ftl":
                return "apiVersion: v1\n" +
                       "kind: Service\n" +
                       "metadata:\n" +
                       "  name: ${artifactId}-service\n" +
                       "spec:\n" +
                       "  selector:\n" +
                       "    app: ${artifactId}\n" +
                       "  ports:\n" +
                       "  - port: ${servicePort!80}\n" +
                       "    targetPort: ${serverPort!8080}\n" +
                       "  type: ClusterIP";
                       
            default:
                return "# Enter your " + fileType + " template content here\n" +
                       "# Use ${variableName} for variable substitution\n" +
                       "# Use <#if condition>...</#if> for conditionals\n" +
                       "# Use <#list items as item>...</#list> for loops";
        }
    }
    
    /**
     * Validate the current template syntax.
     */
    private void validateCurrentTemplate() {
        String templateContent = templatePreview.getText();
        if (templateContent.isEmpty()) {
            showValidationError("Template is empty");
            return;
        }
        
        try {
            // Basic FreeMarker syntax validation
            freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_32);
            cfg.setTemplateLoader(new freemarker.cache.StringTemplateLoader());
            
            freemarker.template.Template template = new freemarker.template.Template("validation", templateContent, cfg);
            showValidationMessage("Template syntax is valid!", false);
            
        } catch (Exception e) {
            showValidationError("Template syntax error: " + e.getMessage());
        }
    }
    
    /**
     * Preview the template output with sample data.
     */
    private void previewTemplateOutput() {
        String templateContent = templatePreview.getText();
        if (templateContent.isEmpty()) {
            showValidationError("Template is empty");
            return;
        }
        
        try {
            // Create sample data for preview
            java.util.Map<String, Object> sampleData = new java.util.HashMap<>();
            sampleData.put("artifactId", "sample-app");
            sampleData.put("version", "1.0.0");
            sampleData.put("name", "Sample Application");
            sampleData.put("baseImage", "openjdk:17-jre-slim");
            sampleData.put("serverPort", "8080");
            sampleData.put("replicas", "3");
            sampleData.put("servicePort", "80");
            
            // Process template with sample data
            freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_32);
            cfg.setTemplateLoader(new freemarker.cache.StringTemplateLoader());
            freemarker.template.Template template = new freemarker.template.Template("preview", templateContent, cfg);
            
            java.io.StringWriter writer = new java.io.StringWriter();
            template.process(sampleData, writer);
            String output = writer.toString();
            
            // Show preview in a dialog
            showTemplatePreviewDialog(output);
            
        } catch (Exception e) {
            showValidationError("Template processing error: " + e.getMessage());
        }
    }
    
    /**
     * Show template preview output in a dialog.
     */
    private void showTemplatePreviewDialog(String output) {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Template Output Preview");
        dialog.setHeaderText("Generated output with sample data:");
        
        javafx.scene.control.TextArea outputArea = new javafx.scene.control.TextArea(output);
        outputArea.setEditable(false);
        outputArea.setPrefRowCount(20);
        outputArea.setPrefColumnCount(80);
        outputArea.setWrapText(false);
        outputArea.getStyleClass().add("template-preview");
        
        dialog.getDialogPane().setContent(outputArea);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }

    /**
     * Custom setting data model for the advanced settings table.
     */
    public static class CustomSetting {
        private final StringProperty key = new SimpleStringProperty();
        private final StringProperty value = new SimpleStringProperty();
        private final StringProperty description = new SimpleStringProperty();
        
        public CustomSetting(String key, String value, String description) {
            setKey(key);
            setValue(value);
            setDescription(description);
        }
        
        public StringProperty keyProperty() { return key; }
        public String getKey() { return key.get(); }
        public void setKey(String key) { this.key.set(key); }
        
        public StringProperty valueProperty() { return value; }
        public String getValue() { return value.get(); }
        public void setValue(String value) { this.value.set(value); }
        
        public StringProperty descriptionProperty() { return description; }
        public String getDescription() { return description.get(); }
        public void setDescription(String description) { this.description.set(description); }
    }
}