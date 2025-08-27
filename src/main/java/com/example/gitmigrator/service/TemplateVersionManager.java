package com.example.gitmigrator.service;

import com.example.gitmigrator.model.MigrationConfiguration;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for managing template versions and migrations.
 * Handles backward compatibility when template format changes.
 */
public class TemplateVersionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TemplateVersionManager.class);
    private static final String CURRENT_VERSION = "1.0";
    private static final String VERSION_FIELD = "_templateVersion";
    
    private final ObjectMapper objectMapper;
    
    public TemplateVersionManager() {
        this.objectMapper = new ObjectMapper();
        // Configure ObjectMapper to ignore unknown properties
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    /**
     * Reads and migrates a template file to the current version if needed.
     * 
     * @param templateFile The template file to read
     * @return The migrated configuration
     * @throws IOException if the file cannot be read
     */
    public MigrationConfiguration readAndMigrateTemplate(File templateFile) throws IOException {
        JsonNode rootNode = objectMapper.readTree(templateFile);
        
        String version = getTemplateVersion(rootNode);
        JsonNode configNode;
        
        if (CURRENT_VERSION.equals(version)) {
            // Current version, remove version field before deserializing
            configNode = removeVersionField(rootNode);
        } else {
            // Migrate from older version (version field already removed in migrateTemplate)
            configNode = migrateTemplate(rootNode, version);
        }
        
        return objectMapper.treeToValue(configNode, MigrationConfiguration.class);
    }
    
    /**
     * Writes a template with version information.
     * 
     * @param config The configuration to write
     * @param templateFile The file to write to
     * @throws IOException if the file cannot be written
     */
    public void writeTemplateWithVersion(MigrationConfiguration config, File templateFile) throws IOException {
        // Convert to JSON node
        JsonNode configNode = objectMapper.valueToTree(config);
        
        // Add version information
        Map<String, Object> versionedConfig = objectMapper.convertValue(configNode, Map.class);
        versionedConfig.put(VERSION_FIELD, CURRENT_VERSION);
        
        // Write to file
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(templateFile, versionedConfig);
    }
    
    /**
     * Gets the template version from a JSON node.
     */
    private String getTemplateVersion(JsonNode rootNode) {
        JsonNode versionNode = rootNode.get(VERSION_FIELD);
        if (versionNode != null && versionNode.isTextual()) {
            return versionNode.asText();
        }
        
        // No version field means it's a legacy template (version 0.9)
        return "0.9";
    }
    
    /**
     * Migrates a template from an older version to the current version.
     */
    private JsonNode migrateTemplate(JsonNode rootNode, String fromVersion) {
        logger.info("Migrating template from version {} to {}", fromVersion, CURRENT_VERSION);
        
        Map<String, Object> migratedConfig = objectMapper.convertValue(rootNode, Map.class);
        
        // Remove version field from source
        migratedConfig.remove(VERSION_FIELD);
        
        switch (fromVersion) {
            case "0.9":
                migratedConfig = migrateFrom09(migratedConfig);
                break;
            default:
                logger.warn("Unknown template version: {}, attempting to use as-is", fromVersion);
                break;
        }
        
        // Don't add version field here - it will be removed anyway for deserialization
        return objectMapper.valueToTree(migratedConfig);
    }
    
    /**
     * Migrates from version 0.9 to current version.
     * Version 0.9 had different field names and structure.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> migrateFrom09(Map<String, Object> oldConfig) {
        Map<String, Object> newConfig = new HashMap<>(oldConfig);
        
        // Migrate old field names to new ones
        if (oldConfig.containsKey("platform")) {
            newConfig.put("targetPlatform", oldConfig.get("platform"));
            newConfig.remove("platform");
        }
        
        // Migrate old boolean flags to optionalComponents set
        if (oldConfig.containsKey("includeHelm") || oldConfig.containsKey("includeDockerfile") ||
            oldConfig.containsKey("includeCiCd") || oldConfig.containsKey("includeMonitoring")) {
            
            Map<String, Boolean> components = new HashMap<>();
            components.put("helm", (Boolean) oldConfig.getOrDefault("includeHelm", false));
            components.put("dockerfile", (Boolean) oldConfig.getOrDefault("includeDockerfile", false));
            components.put("cicd", (Boolean) oldConfig.getOrDefault("includeCiCd", false));
            components.put("monitoring", (Boolean) oldConfig.getOrDefault("includeMonitoring", false));
            
            // Convert to set of enabled components
            java.util.Set<String> optionalComponents = new java.util.HashSet<>();
            components.forEach((key, value) -> {
                if (value) {
                    optionalComponents.add(key);
                }
            });
            
            newConfig.put("optionalComponents", optionalComponents);
            
            // Remove old fields
            newConfig.remove("includeHelm");
            newConfig.remove("includeDockerfile");
            newConfig.remove("includeCiCd");
            newConfig.remove("includeMonitoring");
        }
        
        // Migrate old settings structure
        if (oldConfig.containsKey("settings") && oldConfig.get("settings") instanceof Map) {
            Map<String, Object> oldSettings = (Map<String, Object>) oldConfig.get("settings");
            newConfig.put("customSettings", oldSettings);
            newConfig.remove("settings");
        }
        
        logger.debug("Migrated template from 0.9 to current version");
        return newConfig;
    }
    
    /**
     * Checks if a template file needs migration.
     * 
     * @param templateFile The template file to check
     * @return true if migration is needed, false otherwise
     */
    public boolean needsMigration(File templateFile) {
        try {
            JsonNode rootNode = objectMapper.readTree(templateFile);
            String version = getTemplateVersion(rootNode);
            return !CURRENT_VERSION.equals(version);
        } catch (IOException e) {
            logger.warn("Failed to check template version: {}", templateFile.getName(), e);
            return false;
        }
    }
    
    /**
     * Gets the current template version.
     */
    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }
    
    /**
     * Removes the version field from a JSON node.
     */
    private JsonNode removeVersionField(JsonNode node) {
        if (node.isObject()) {
            Map<String, Object> nodeMap = objectMapper.convertValue(node, Map.class);
            nodeMap.remove(VERSION_FIELD);
            return objectMapper.valueToTree(nodeMap);
        }
        return node;
    }
}