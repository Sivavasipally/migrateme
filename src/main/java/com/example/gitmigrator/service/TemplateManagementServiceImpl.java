package com.example.gitmigrator.service;

import com.example.gitmigrator.model.MigrationConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of TemplateManagementService for managing migration templates.
 * Templates are stored as JSON files in the user's home directory.
 */
public class TemplateManagementServiceImpl implements TemplateManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(TemplateManagementServiceImpl.class);
    private static final String TEMPLATE_DIR_NAME = ".git-migrator/templates";
    private static final String TEMPLATE_FILE_EXTENSION = ".json";
    
    private final ObjectMapper objectMapper;
    private final Path templateDirectory;
    private final TemplateVersionManager versionManager;
    
    public TemplateManagementServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.versionManager = new TemplateVersionManager();
        
        // Initialize template directory in user home
        String userHome = System.getProperty("user.home");
        this.templateDirectory = Paths.get(userHome, TEMPLATE_DIR_NAME);
        
        // Create template directory if it doesn't exist
        try {
            Files.createDirectories(templateDirectory);
            logger.info("Template directory initialized: {}", templateDirectory);
        } catch (IOException e) {
            logger.error("Failed to create template directory: {}", templateDirectory, e);
            throw new RuntimeException("Failed to initialize template directory", e);
        }
        
        // Initialize default templates
        initializeDefaultTemplates();
    }
    
    @Override
    public void saveTemplate(String name, MigrationConfiguration config) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Template name cannot be null or empty");
        }
        
        if (config == null) {
            throw new IllegalArgumentException("Migration configuration cannot be null");
        }
        
        if (!validateTemplate(config)) {
            throw new IllegalArgumentException("Invalid migration configuration");
        }
        
        String sanitizedName = sanitizeTemplateName(name);
        Path templateFile = templateDirectory.resolve(sanitizedName + TEMPLATE_FILE_EXTENSION);
        
        try {
            versionManager.writeTemplateWithVersion(config, templateFile.toFile());
            logger.info("Template saved: {} -> {}", name, templateFile);
        } catch (IOException e) {
            logger.error("Failed to save template: {}", name, e);
            throw new RuntimeException("Failed to save template: " + name, e);
        }
    }
    
    @Override
    public MigrationConfiguration loadTemplate(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        String sanitizedName = sanitizeTemplateName(name);
        Path templateFile = templateDirectory.resolve(sanitizedName + TEMPLATE_FILE_EXTENSION);
        
        if (!Files.exists(templateFile)) {
            logger.warn("Template not found: {}", name);
            return null;
        }
        
        try {
            MigrationConfiguration config = versionManager.readAndMigrateTemplate(templateFile.toFile());
            logger.debug("Template loaded: {}", name);
            return config;
        } catch (IOException e) {
            logger.error("Failed to load template: {}", name, e);
            throw new RuntimeException("Failed to load template: " + name, e);
        }
    }
    
    @Override
    public List<String> getAvailableTemplates() {
        try {
            return Files.list(templateDirectory)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(TEMPLATE_FILE_EXTENSION))
                    .map(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.substring(0, fileName.length() - TEMPLATE_FILE_EXTENSION.length());
                    })
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to list templates", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean deleteTemplate(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        String sanitizedName = sanitizeTemplateName(name);
        Path templateFile = templateDirectory.resolve(sanitizedName + TEMPLATE_FILE_EXTENSION);
        
        if (!Files.exists(templateFile)) {
            return false;
        }
        
        try {
            Files.delete(templateFile);
            logger.info("Template deleted: {}", name);
            return true;
        } catch (IOException e) {
            logger.error("Failed to delete template: {}", name, e);
            return false;
        }
    }
    
    @Override
    public void exportTemplate(String name, File destination) {
        MigrationConfiguration config = loadTemplate(name);
        if (config == null) {
            throw new RuntimeException("Template not found: " + name);
        }
        
        try {
            versionManager.writeTemplateWithVersion(config, destination);
            logger.info("Template exported: {} -> {}", name, destination.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to export template: {}", name, e);
            throw new RuntimeException("Failed to export template: " + name, e);
        }
    }
    
    @Override
    public String importTemplate(File source) {
        if (!source.exists() || !source.isFile()) {
            throw new IllegalArgumentException("Source file does not exist or is not a file");
        }
        
        try {
            MigrationConfiguration config = versionManager.readAndMigrateTemplate(source);
            
            if (!validateTemplate(config)) {
                throw new RuntimeException("Invalid template configuration in source file");
            }
            
            // Generate template name from file name
            String fileName = source.getName();
            String templateName = fileName.endsWith(TEMPLATE_FILE_EXTENSION) 
                ? fileName.substring(0, fileName.length() - TEMPLATE_FILE_EXTENSION.length())
                : fileName;
            
            // Ensure unique name
            templateName = ensureUniqueTemplateName(templateName);
            
            saveTemplate(templateName, config);
            logger.info("Template imported: {} from {}", templateName, source.getAbsolutePath());
            
            return templateName;
        } catch (IOException e) {
            logger.error("Failed to import template from: {}", source.getAbsolutePath(), e);
            throw new RuntimeException("Failed to import template", e);
        }
    }
    
    @Override
    public boolean templateExists(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        String sanitizedName = sanitizeTemplateName(name);
        Path templateFile = templateDirectory.resolve(sanitizedName + TEMPLATE_FILE_EXTENSION);
        return Files.exists(templateFile);
    }
    
    @Override
    public boolean validateTemplate(MigrationConfiguration config) {
        if (config == null) {
            return false;
        }
        
        // Check required fields
        if (config.getTargetPlatform() == null || config.getTargetPlatform().trim().isEmpty()) {
            return false;
        }
        
        // Validate target platform
        Set<String> validPlatforms = Set.of("kubernetes", "openshift", "docker-compose");
        if (!validPlatforms.contains(config.getTargetPlatform().toLowerCase())) {
            return false;
        }
        
        // Validate optional components
        if (config.getOptionalComponents() != null) {
            Set<String> validComponents = Set.of("helm", "dockerfile", "cicd", "monitoring");
            for (String component : config.getOptionalComponents()) {
                if (!validComponents.contains(component.toLowerCase())) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    @Override
    public String getTemplateDirectory() {
        return templateDirectory.toString();
    }
    
    @Override
    public void initializeDefaultTemplates() {
        // Create default templates if they don't exist
        createDefaultTemplateIfNotExists("microservices-k8s", createMicroservicesTemplate());
        createDefaultTemplateIfNotExists("frontend-app", createFrontendTemplate());
        createDefaultTemplateIfNotExists("monolith-migration", createMonolithTemplate());
        createDefaultTemplateIfNotExists("openshift-basic", createOpenShiftTemplate());
    }
    
    @Override
    public void cleanupAndReinitializeTemplates() {
        logger.info("Cleaning up corrupted templates and reinitializing defaults");
        
        try {
            // Delete all existing template files
            if (Files.exists(templateDirectory)) {
                Files.list(templateDirectory)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(TEMPLATE_FILE_EXTENSION))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            logger.debug("Deleted corrupted template: {}", path.getFileName());
                        } catch (IOException e) {
                            logger.warn("Failed to delete template file: {}", path, e);
                        }
                    });
            }
            
            // Recreate default templates
            initializeDefaultTemplates();
            logger.info("Templates cleanup and reinitialization completed");
            
        } catch (IOException e) {
            logger.error("Failed to cleanup templates", e);
            throw new RuntimeException("Failed to cleanup templates", e);
        }
    }
    
    /**
     * Creates a default template if it doesn't already exist.
     */
    private void createDefaultTemplateIfNotExists(String name, MigrationConfiguration config) {
        if (!templateExists(name)) {
            try {
                saveTemplate(name, config);
                logger.info("Created default template: {}", name);
            } catch (Exception e) {
                logger.warn("Failed to create default template: {}", name, e);
            }
        }
    }
    
    /**
     * Creates a microservices template configuration.
     */
    private MigrationConfiguration createMicroservicesTemplate() {
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        config.setIncludeHelm(true);
        config.setIncludeDockerfile(true);
        config.setIncludeCiCd(true);
        config.setIncludeMonitoring(true);
        config.setEnableValidation(true);
        
        // Add custom settings for microservices
        config.setCustomSetting("base-image", "openjdk:17-jre-slim");
        config.setCustomSetting("resource-limits", "memory=512Mi,cpu=500m");
        config.setCustomSetting("replicas", "3");
        config.setCustomSetting("service-type", "ClusterIP");
        
        return config;
    }
    
    /**
     * Creates a frontend application template configuration.
     */
    private MigrationConfiguration createFrontendTemplate() {
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        config.setIncludeHelm(true);
        config.setIncludeDockerfile(true);
        config.setIncludeCiCd(false);
        config.setIncludeMonitoring(false);
        config.setEnableValidation(true);
        
        // Add custom settings for frontend apps
        config.setCustomSetting("base-image", "nginx:alpine");
        config.setCustomSetting("resource-limits", "memory=128Mi,cpu=100m");
        config.setCustomSetting("replicas", "2");
        config.setCustomSetting("service-type", "LoadBalancer");
        
        return config;
    }
    
    /**
     * Creates a monolith migration template configuration.
     */
    private MigrationConfiguration createMonolithTemplate() {
        MigrationConfiguration config = new MigrationConfiguration("kubernetes");
        config.setIncludeHelm(true);
        config.setIncludeDockerfile(true);
        config.setIncludeCiCd(false);
        config.setIncludeMonitoring(true);
        config.setEnableValidation(true);
        
        // Add custom settings for monoliths
        config.setCustomSetting("base-image", "openjdk:17-jre-slim");
        config.setCustomSetting("resource-limits", "memory=2Gi,cpu=1000m");
        config.setCustomSetting("replicas", "1");
        config.setCustomSetting("service-type", "LoadBalancer");
        
        return config;
    }
    
    /**
     * Creates an OpenShift template configuration.
     */
    private MigrationConfiguration createOpenShiftTemplate() {
        MigrationConfiguration config = new MigrationConfiguration("openshift");
        config.setIncludeHelm(false); // OpenShift uses templates instead
        config.setIncludeDockerfile(true);
        config.setIncludeCiCd(true);
        config.setIncludeMonitoring(true);
        config.setEnableValidation(true);
        
        // Add custom settings for OpenShift
        config.setCustomSetting("base-image", "registry.redhat.io/ubi8/openjdk-17");
        config.setCustomSetting("resource-limits", "memory=1Gi,cpu=500m");
        config.setCustomSetting("replicas", "2");
        config.setCustomSetting("route-enabled", "true");
        
        return config;
    }
    
    /**
     * Sanitizes template name to be safe for file system.
     */
    private String sanitizeTemplateName(String name) {
        return name.trim()
                .replaceAll("[^a-zA-Z0-9\\-_]", "-")
                .replaceAll("-+", "-")
                .toLowerCase();
    }
    
    /**
     * Ensures template name is unique by appending a number if necessary.
     */
    private String ensureUniqueTemplateName(String baseName) {
        String candidateName = baseName;
        int counter = 1;
        
        while (templateExists(candidateName)) {
            candidateName = baseName + "-" + counter;
            counter++;
        }
        
        return candidateName;
    }
}