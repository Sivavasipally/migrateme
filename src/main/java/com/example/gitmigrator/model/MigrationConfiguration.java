package com.example.gitmigrator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Configuration model for migration operations.
 * Contains all settings needed to customize the migration process.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MigrationConfiguration {
    
    private String targetPlatform = "kubernetes"; // kubernetes, openshift, docker-compose
    private Set<String> optionalComponents = new HashSet<>(); // helm, dockerfile, cicd, monitoring
    private Map<String, String> customSettings = new HashMap<>(); // base-image, resource-limits, etc.
    private String templateName;
    private boolean enableValidation = true;
    
    // Default constructor
    public MigrationConfiguration() {
        // Set default optional components
        optionalComponents.add("helm");
        optionalComponents.add("dockerfile");
    }
    
    // Constructor with target platform
    public MigrationConfiguration(String targetPlatform) {
        this();
        this.targetPlatform = targetPlatform;
    }
    
    // Getters and Setters
    public String getTargetPlatform() { return targetPlatform; }
    public void setTargetPlatform(String targetPlatform) { this.targetPlatform = targetPlatform; }
    
    public Set<String> getOptionalComponents() { return optionalComponents; }
    public void setOptionalComponents(Set<String> optionalComponents) { this.optionalComponents = optionalComponents; }
    
    public Map<String, String> getCustomSettings() { return customSettings; }
    public void setCustomSettings(Map<String, String> customSettings) { this.customSettings = customSettings; }
    
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    
    public boolean isEnableValidation() { return enableValidation; }
    public void setEnableValidation(boolean enableValidation) { this.enableValidation = enableValidation; }
    
    // Convenience methods for optional components
    public boolean hasComponent(String component) {
        return optionalComponents.contains(component);
    }
    
    public void addComponent(String component) {
        optionalComponents.add(component);
    }
    
    public void removeComponent(String component) {
        optionalComponents.remove(component);
    }
    
    public boolean isIncludeHelm() {
        return hasComponent("helm");
    }
    
    public void setIncludeHelm(boolean includeHelm) {
        if (includeHelm) {
            addComponent("helm");
        } else {
            removeComponent("helm");
        }
    }
    
    public boolean isIncludeDockerfile() {
        return hasComponent("dockerfile");
    }
    
    public void setIncludeDockerfile(boolean includeDockerfile) {
        if (includeDockerfile) {
            addComponent("dockerfile");
        } else {
            removeComponent("dockerfile");
        }
    }
    
    public boolean isIncludeCiCd() {
        return hasComponent("cicd");
    }
    
    public void setIncludeCiCd(boolean includeCiCd) {
        if (includeCiCd) {
            addComponent("cicd");
        } else {
            removeComponent("cicd");
        }
    }
    
    public boolean isIncludeMonitoring() {
        return hasComponent("monitoring");
    }
    
    public void setIncludeMonitoring(boolean includeMonitoring) {
        if (includeMonitoring) {
            addComponent("monitoring");
        } else {
            removeComponent("monitoring");
        }
    }
    
    // Convenience methods for custom settings
    public String getCustomSetting(String key) {
        return customSettings.get(key);
    }
    
    public void setCustomSetting(String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            customSettings.put(key, value);
        } else {
            customSettings.remove(key);
        }
    }
    
    public String getBaseImage() {
        return getCustomSetting("base-image");
    }
    
    public void setBaseImage(String baseImage) {
        setCustomSetting("base-image", baseImage);
    }
    
    public String getResourceLimits() {
        return getCustomSetting("resource-limits");
    }
    
    public void setResourceLimits(String resourceLimits) {
        setCustomSetting("resource-limits", resourceLimits);
    }
    
    @Override
    public String toString() {
        return "MigrationConfiguration{" +
                "targetPlatform='" + targetPlatform + '\'' +
                ", optionalComponents=" + optionalComponents +
                ", templateName='" + templateName + '\'' +
                ", enableValidation=" + enableValidation +
                '}';
    }
}