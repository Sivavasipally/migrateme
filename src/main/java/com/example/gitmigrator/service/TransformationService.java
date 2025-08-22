package com.example.gitmigrator.service;

import com.example.gitmigrator.model.FrameworkType;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Core transformation service that analyzes repositories and applies framework-specific modifications.
 * Handles Spring Boot to Kubernetes/OpenShift migration with Helm charts.
 */
@Service
public class TransformationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransformationService.class);
    
    @Autowired
    private Configuration freemarkerConfig;
    
    /**
     * Main entry point for processing a repository.
     * Identifies the framework and applies appropriate transformations.
     */
    public FrameworkType processRepository(File repoPath) {
        logger.info("Processing repository: {}", repoPath.getName());
        
        try {
            // Identify the primary framework
            FrameworkType framework = identifyFramework(repoPath);
            logger.info("Identified framework: {} for repository: {}", framework, repoPath.getName());
            
            // Apply framework-specific transformations
            switch (framework) {
                case SPRING_BOOT:
                    applySpringBootHelmTransformation(repoPath);
                    break;
                case REACT:
                    applyReactTransformation(repoPath);
                    break;
                case ANGULAR:
                    applyAngularTransformation(repoPath);
                    break;
                case NODE_JS:
                    applyNodeJsTransformation(repoPath);
                    break;
                default:
                    logger.warn("No specific transformation available for framework: {}", framework);
                    applyGenericTransformation(repoPath);
                    break;
            }
            
            return framework;
            
        } catch (Exception e) {
            logger.error("Error processing repository {}: {}", repoPath.getName(), e.getMessage(), e);
            throw new RuntimeException("Failed to process repository: " + e.getMessage(), e);
        }
    }
    
    /**
     * Identifies the primary framework of a repository by examining key files.
     */
    private FrameworkType identifyFramework(File repoPath) {
        // Check for Spring Boot (pom.xml with spring-boot-starter)
        File pomFile = new File(repoPath, "pom.xml");
        if (pomFile.exists()) {
            try {
                String pomContent = FileUtils.readFileToString(pomFile, StandardCharsets.UTF_8);
                if (pomContent.contains("spring-boot-starter")) {
                    return FrameworkType.SPRING_BOOT;
                } else {
                    return FrameworkType.MAVEN_JAVA;
                }
            } catch (IOException e) {
                logger.warn("Failed to read pom.xml: {}", e.getMessage());
            }
        }
        
        // Check for Gradle
        File gradleFile = new File(repoPath, "build.gradle");
        File gradleKtsFile = new File(repoPath, "build.gradle.kts");
        if (gradleFile.exists() || gradleKtsFile.exists()) {
            return FrameworkType.GRADLE_JAVA;
        }
        
        // Check for Node.js projects
        File packageJsonFile = new File(repoPath, "package.json");
        if (packageJsonFile.exists()) {
            try {
                String packageContent = FileUtils.readFileToString(packageJsonFile, StandardCharsets.UTF_8);
                if (packageContent.contains("\"react\"")) {
                    return FrameworkType.REACT;
                } else if (packageContent.contains("\"@angular/")) {
                    return FrameworkType.ANGULAR;
                } else {
                    return FrameworkType.NODE_JS;
                }
            } catch (IOException e) {
                logger.warn("Failed to read package.json: {}", e.getMessage());
            }
        }
        
        return FrameworkType.UNKNOWN;
    }
    
    /**
     * Applies Spring Boot to Kubernetes/Helm transformation.
     */
    private void applySpringBootHelmTransformation(File repoPath) throws IOException, TemplateException {
        logger.info("Applying Spring Boot Helm transformation to: {}", repoPath.getName());
        
        // Extract project metadata from pom.xml
        Map<String, Object> projectMetadata = extractSpringBootMetadata(repoPath);
        
        // Create Dockerfile
        createDockerfile(repoPath, projectMetadata);
        
        // Create Helm chart structure
        createHelmChartStructure(repoPath, projectMetadata);
        
        // Update pom.xml with container image build plugin
        updatePomWithJibPlugin(repoPath, projectMetadata);
        
        logger.info("Successfully applied Spring Boot Helm transformation");
    }
    
    /**
     * Extracts metadata from Spring Boot project's pom.xml.
     */
    private Map<String, Object> extractSpringBootMetadata(File repoPath) {
        Map<String, Object> metadata = new HashMap<>();
        
        File pomFile = new File(repoPath, "pom.xml");
        if (!pomFile.exists()) {
            throw new RuntimeException("pom.xml not found in repository");
        }
        
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(pomFile);
            
            // Extract basic project information
            String groupId = getTextContent(document, "//project/groupId");
            String artifactId = getTextContent(document, "//project/artifactId");
            String version = getTextContent(document, "//project/version");
            String name = getTextContent(document, "//project/name");
            String description = getTextContent(document, "//project/description");
            
            // Handle parent inheritance
            if (groupId == null || groupId.isEmpty()) {
                groupId = getTextContent(document, "//project/parent/groupId");
            }
            if (version == null || version.isEmpty()) {
                version = getTextContent(document, "//project/parent/version");
            }
            
            metadata.put("groupId", groupId != null ? groupId : "com.example");
            metadata.put("artifactId", artifactId != null ? artifactId : "app");
            metadata.put("version", version != null ? version : "1.0.0");
            metadata.put("name", name != null ? name : artifactId);
            metadata.put("description", description != null ? description : "Spring Boot Application");
            
            // Extract server port from application properties if available
            String serverPort = extractServerPort(repoPath);
            metadata.put("serverPort", serverPort != null ? serverPort : "8080");
            
            logger.debug("Extracted metadata: {}", metadata);
            
        } catch (Exception e) {
            logger.error("Failed to extract metadata from pom.xml", e);
            // Provide default values
            metadata.put("groupId", "com.example");
            metadata.put("artifactId", "app");
            metadata.put("version", "1.0.0");
            metadata.put("name", "Spring Boot App");
            metadata.put("description", "Spring Boot Application");
            metadata.put("serverPort", "8080");
        }
        
        return metadata;
    }
    
    /**
     * Helper method to extract text content from XML using XPath.
     */
    private String getTextContent(Document document, String xpath) {
        Node node = document.selectSingleNode(xpath);
        return node != null ? node.getText() : null;
    }
    
    /**
     * Extracts server port from application.properties or application.yml.
     */
    private String extractServerPort(File repoPath) {
        // Check application.properties
        File propsFile = new File(repoPath, "src/main/resources/application.properties");
        if (propsFile.exists()) {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream(propsFile));
                String port = props.getProperty("server.port");
                if (port != null && !port.isEmpty()) {
                    return port;
                }
            } catch (IOException e) {
                logger.debug("Could not read application.properties: {}", e.getMessage());
            }
        }
        
        // Check application.yml
        File ymlFile = new File(repoPath, "src/main/resources/application.yml");
        if (ymlFile.exists()) {
            try {
                String content = FileUtils.readFileToString(ymlFile, StandardCharsets.UTF_8);
                // Simple regex to find server port in YAML
                if (content.contains("server:") && content.contains("port:")) {
                    // This is a simplified parser - in production, use a proper YAML library
                    String[] lines = content.split("\n");
                    boolean inServerSection = false;
                    for (String line : lines) {
                        if (line.trim().startsWith("server:")) {
                            inServerSection = true;
                        } else if (inServerSection && line.trim().startsWith("port:")) {
                            String port = line.split(":")[1].trim();
                            return port;
                        }
                    }
                }
            } catch (IOException e) {
                logger.debug("Could not read application.yml: {}", e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * Creates a Dockerfile for the Spring Boot application.
     */
    private void createDockerfile(File repoPath, Map<String, Object> metadata) throws IOException, TemplateException {
        logger.debug("Creating Dockerfile for: {}", repoPath.getName());
        
        Template template = freemarkerConfig.getTemplate("Dockerfile.ftl");
        
        File dockerFile = new File(repoPath, "Dockerfile");
        try (FileWriter writer = new FileWriter(dockerFile)) {
            template.process(metadata, writer);
        }
        
        logger.debug("Created Dockerfile at: {}", dockerFile.getAbsolutePath());
    }
    
    /**
     * Creates the complete Helm chart structure with templates.
     */
    private void createHelmChartStructure(File repoPath, Map<String, Object> metadata) throws IOException, TemplateException {
        logger.debug("Creating Helm chart structure for: {}", repoPath.getName());
        
        // Create directories
        File helmDir = new File(repoPath, "helm");
        File templatesDir = new File(helmDir, "templates");
        templatesDir.mkdirs();
        
        // Create Chart.yaml
        Template chartTemplate = freemarkerConfig.getTemplate("Chart.yaml.ftl");
        File chartFile = new File(helmDir, "Chart.yaml");
        try (FileWriter writer = new FileWriter(chartFile)) {
            chartTemplate.process(metadata, writer);
        }
        
        // Create values.yaml
        Template valuesTemplate = freemarkerConfig.getTemplate("values.yaml.ftl");
        File valuesFile = new File(helmDir, "values.yaml");
        try (FileWriter writer = new FileWriter(valuesFile)) {
            valuesTemplate.process(metadata, writer);
        }
        
        // Create template files
        createHelmTemplate(templatesDir, "deployment.yaml.ftl", "deployment.yaml", metadata);
        createHelmTemplate(templatesDir, "service.yaml.ftl", "service.yaml", metadata);
        createHelmTemplate(templatesDir, "ingress.yaml.ftl", "ingress.yaml", metadata);
        
        logger.debug("Created Helm chart structure at: {}", helmDir.getAbsolutePath());
    }
    
    /**
     * Creates individual Helm template files.
     */
    private void createHelmTemplate(File templatesDir, String templateName, String fileName, 
                                   Map<String, Object> metadata) throws IOException, TemplateException {
        Template template = freemarkerConfig.getTemplate(templateName);
        File templateFile = new File(templatesDir, fileName);
        try (FileWriter writer = new FileWriter(templateFile)) {
            template.process(metadata, writer);
        }
    }
    
    /**
     * Updates pom.xml to add Jib plugin for container image building.
     */
    private void updatePomWithJibPlugin(File repoPath, Map<String, Object> metadata) {
        logger.debug("Updating pom.xml with Jib plugin for: {}", repoPath.getName());
        
        File pomFile = new File(repoPath, "pom.xml");
        if (!pomFile.exists()) {
            logger.warn("pom.xml not found, skipping Jib plugin addition");
            return;
        }
        
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(pomFile);
            
            // Find or create build/plugins section
            Element buildElement = (Element) document.selectSingleNode("//project/build");
            if (buildElement == null) {
                buildElement = ((Element) document.selectSingleNode("//project")).addElement("build");
            }
            
            Element pluginsElement = (Element) buildElement.selectSingleNode("plugins");
            if (pluginsElement == null) {
                pluginsElement = buildElement.addElement("plugins");
            }
            
            // Check if Jib plugin already exists
            if (document.selectSingleNode("//plugin[artifactId='jib-maven-plugin']") == null) {
                // Add Jib plugin
                Element pluginElement = pluginsElement.addElement("plugin");
                pluginElement.addElement("groupId").setText("com.google.cloud.tools");
                pluginElement.addElement("artifactId").setText("jib-maven-plugin");
                pluginElement.addElement("version").setText("3.4.0");
                
                Element configElement = pluginElement.addElement("configuration");
                Element fromElement = configElement.addElement("from");
                fromElement.addElement("image").setText("openjdk:17-jre-slim");
                
                Element toElement = configElement.addElement("to");
                toElement.addElement("image").setText("${project.artifactId}:${project.version}");
            }
            
            // Write updated XML
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer = new XMLWriter(new FileWriter(pomFile), format);
            writer.write(document);
            writer.close();
            
            logger.debug("Updated pom.xml with Jib plugin");
            
        } catch (Exception e) {
            logger.error("Failed to update pom.xml with Jib plugin: {}", e.getMessage());
            // Don't throw exception as this is not critical for the migration
        }
    }
    
    // Placeholder methods for other framework transformations
    private void applyReactTransformation(File repoPath) throws IOException, TemplateException {
        logger.info("Applying React transformation (placeholder)");
        // TODO: Implement React-specific transformations
    }
    
    private void applyAngularTransformation(File repoPath) throws IOException, TemplateException {
        logger.info("Applying Angular transformation (placeholder)");
        // TODO: Implement Angular-specific transformations
    }
    
    private void applyNodeJsTransformation(File repoPath) throws IOException, TemplateException {
        logger.info("Applying Node.js transformation (placeholder)");
        // TODO: Implement Node.js-specific transformations
    }
    
    private void applyGenericTransformation(File repoPath) throws IOException, TemplateException {
        logger.info("Applying generic transformation (placeholder)");
        // TODO: Implement generic transformations
    }
}