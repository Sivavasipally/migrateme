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
public class TransformationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransformationService.class);
    
    private Configuration freemarkerConfig;
    
    public void setFreemarkerConfig(Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
    }
    
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
        createDockerfile(repoPath, projectMetadata, FrameworkType.SPRING_BOOT);
        
        // Create Helm chart structure
        createHelmChartStructure(repoPath, projectMetadata, FrameworkType.SPRING_BOOT);
        
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
     * Creates a Dockerfile for the application based on framework type.
     */
    private void createDockerfile(File repoPath, Map<String, Object> metadata, FrameworkType framework) throws IOException, TemplateException {
        logger.debug("Creating Dockerfile for: {} with framework: {}", repoPath.getName(), framework);
        
        String templatePath = getFrameworkTemplatePath(framework, "Dockerfile.ftl");
        Template template = freemarkerConfig.getTemplate(templatePath);
        
        File dockerFile = new File(repoPath, "Dockerfile");
        try (FileWriter writer = new FileWriter(dockerFile)) {
            template.process(metadata, writer);
        }
        
        logger.debug("Created Dockerfile at: {}", dockerFile.getAbsolutePath());
    }
    
    /**
     * Creates the complete Helm chart structure with templates based on framework type.
     */
    private void createHelmChartStructure(File repoPath, Map<String, Object> metadata, FrameworkType framework) throws IOException, TemplateException {
        logger.debug("Creating Helm chart structure for: {} with framework: {}", repoPath.getName(), framework);
        
        // Create directories
        File helmDir = new File(repoPath, "helm");
        File templatesDir = new File(helmDir, "templates");
        templatesDir.mkdirs();
        
        // Create Chart.yaml
        String chartTemplatePath = getFrameworkTemplatePath(framework, "Chart.yaml.ftl");
        Template chartTemplate = freemarkerConfig.getTemplate(chartTemplatePath);
        File chartFile = new File(helmDir, "Chart.yaml");
        try (FileWriter writer = new FileWriter(chartFile)) {
            chartTemplate.process(metadata, writer);
        }
        
        // Create values.yaml
        String valuesTemplatePath = getFrameworkTemplatePath(framework, "values.yaml.ftl");
        Template valuesTemplate = freemarkerConfig.getTemplate(valuesTemplatePath);
        File valuesFile = new File(helmDir, "values.yaml");
        try (FileWriter writer = new FileWriter(valuesFile)) {
            valuesTemplate.process(metadata, writer);
        }
        
        // Create template files
        createHelmTemplate(templatesDir, framework, "deployment.yaml.ftl", "deployment.yaml", metadata);
        createHelmTemplate(templatesDir, framework, "service.yaml.ftl", "service.yaml", metadata);
        createHelmTemplate(templatesDir, framework, "ingress.yaml.ftl", "ingress.yaml", metadata);
        
        logger.debug("Created Helm chart structure at: {}", helmDir.getAbsolutePath());
    }
    
    /**
     * Creates individual Helm template files based on framework type.
     */
    private void createHelmTemplate(File templatesDir, FrameworkType framework, String templateName, String fileName, 
                                   Map<String, Object> metadata) throws IOException, TemplateException {
        String templatePath = getFrameworkTemplatePath(framework, templateName);
        Template template = freemarkerConfig.getTemplate(templatePath);
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
    
    /**
     * Gets the framework-specific template path.
     */
    private String getFrameworkTemplatePath(FrameworkType framework, String templateName) {
        String frameworkDir;
        switch (framework) {
            case SPRING_BOOT:
            case MAVEN_JAVA:
            case GRADLE_JAVA:
                frameworkDir = "springboot";
                break;
            case REACT:
                frameworkDir = "react";
                break;
            case ANGULAR:
                frameworkDir = "angular";
                break;
            case NODE_JS:
                frameworkDir = "nodejs";
                break;
            default:
                frameworkDir = "generic";
                break;
        }
        
        // Try framework-specific template first, fall back to Spring Boot if not found
        String frameworkTemplate = frameworkDir + "/" + templateName;
        try {
            freemarkerConfig.getTemplate(frameworkTemplate);
            return frameworkTemplate;
        } catch (IOException e) {
            logger.debug("Framework-specific template not found: {}, falling back to springboot template", frameworkTemplate);
            return "springboot/" + templateName;
        }
    }
    
    /**
     * Applies React-specific transformations.
     */
    private void applyReactTransformation(File repoPath) throws IOException, TemplateException {
        logger.info("Applying React transformation to: {}", repoPath.getName());
        
        Map<String, Object> projectMetadata = extractReactMetadata(repoPath);
        
        // Create Dockerfile
        createDockerfile(repoPath, projectMetadata, FrameworkType.REACT);
        
        // Create Helm chart structure
        createHelmChartStructure(repoPath, projectMetadata, FrameworkType.REACT);
        
        // Create nginx configuration
        createNginxConfig(repoPath);
        
        logger.info("Successfully applied React transformation");
    }
    
    /**
     * Applies Angular-specific transformations.
     */
    private void applyAngularTransformation(File repoPath) throws IOException, TemplateException {
        logger.info("Applying Angular transformation to: {}", repoPath.getName());
        
        Map<String, Object> projectMetadata = extractAngularMetadata(repoPath);
        
        // Create Dockerfile
        createDockerfile(repoPath, projectMetadata, FrameworkType.ANGULAR);
        
        // Create Helm chart structure
        createHelmChartStructure(repoPath, projectMetadata, FrameworkType.ANGULAR);
        
        // Create nginx configuration
        createNginxConfig(repoPath);
        
        logger.info("Successfully applied Angular transformation");
    }
    
    /**
     * Applies Node.js-specific transformations.
     */
    private void applyNodeJsTransformation(File repoPath) throws IOException, TemplateException {
        logger.info("Applying Node.js transformation to: {}", repoPath.getName());
        
        Map<String, Object> projectMetadata = extractNodeJsMetadata(repoPath);
        
        // Create Dockerfile
        createDockerfile(repoPath, projectMetadata, FrameworkType.NODE_JS);
        
        // Create Helm chart structure
        createHelmChartStructure(repoPath, projectMetadata, FrameworkType.NODE_JS);
        
        logger.info("Successfully applied Node.js transformation");
    }
    
    /**
     * Applies generic transformations.
     */
    private void applyGenericTransformation(File repoPath) throws IOException, TemplateException {
        logger.info("Applying generic transformation to: {}", repoPath.getName());
        
        Map<String, Object> projectMetadata = extractGenericMetadata(repoPath);
        
        // Create Dockerfile
        createDockerfile(repoPath, projectMetadata, FrameworkType.UNKNOWN);
        
        // Create Helm chart structure
        createHelmChartStructure(repoPath, projectMetadata, FrameworkType.UNKNOWN);
        
        logger.info("Successfully applied generic transformation");
    }
    
    /**
     * Extracts metadata from React project's package.json.
     */
    private Map<String, Object> extractReactMetadata(File repoPath) {
        return extractNodeBasedMetadata(repoPath, "React Application");
    }
    
    /**
     * Extracts metadata from Angular project's package.json.
     */
    private Map<String, Object> extractAngularMetadata(File repoPath) {
        return extractNodeBasedMetadata(repoPath, "Angular Application");
    }
    
    /**
     * Extracts metadata from Node.js project's package.json.
     */
    private Map<String, Object> extractNodeJsMetadata(File repoPath) {
        return extractNodeBasedMetadata(repoPath, "Node.js Application");
    }
    
    /**
     * Extracts metadata from Node-based projects (React, Angular, Node.js).
     */
    private Map<String, Object> extractNodeBasedMetadata(File repoPath, String defaultDescription) {
        Map<String, Object> metadata = new HashMap<>();
        
        File packageJsonFile = new File(repoPath, "package.json");
        if (!packageJsonFile.exists()) {
            logger.warn("package.json not found, using default values");
            metadata.put("artifactId", repoPath.getName());
            metadata.put("name", repoPath.getName());
            metadata.put("version", "1.0.0");
            metadata.put("description", defaultDescription);
            metadata.put("serverPort", "3000");
            return metadata;
        }
        
        try {
            String packageContent = FileUtils.readFileToString(packageJsonFile, StandardCharsets.UTF_8);
            // Simple JSON parsing - in production, use a proper JSON library
            String name = extractJsonValue(packageContent, "name");
            String version = extractJsonValue(packageContent, "version");
            String description = extractJsonValue(packageContent, "description");
            
            metadata.put("artifactId", name != null ? name.replaceAll("[@/]", "") : repoPath.getName());
            metadata.put("name", name != null ? name : repoPath.getName());
            metadata.put("version", version != null ? version : "1.0.0");
            metadata.put("description", description != null ? description : defaultDescription);
            metadata.put("serverPort", "3000");
            
            logger.debug("Extracted Node.js metadata: {}", metadata);
            
        } catch (IOException e) {
            logger.error("Failed to extract metadata from package.json", e);
            metadata.put("artifactId", repoPath.getName());
            metadata.put("name", repoPath.getName());
            metadata.put("version", "1.0.0");
            metadata.put("description", defaultDescription);
            metadata.put("serverPort", "3000");
        }
        
        return metadata;
    }
    
    /**
     * Extracts metadata for generic projects.
     */
    private Map<String, Object> extractGenericMetadata(File repoPath) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("artifactId", repoPath.getName());
        metadata.put("name", repoPath.getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "Generic Application");
        metadata.put("serverPort", "8080");
        return metadata;
    }
    
    /**
     * Simple helper to extract JSON values (simplified approach).
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
    
    /**
     * Creates nginx configuration for frontend applications.
     */
    private void createNginxConfig(File repoPath) throws IOException {
        logger.debug("Creating nginx configuration for: {}", repoPath.getName());
        
        String nginxConfig = "events {\n" +
                "    worker_connections 1024;\n" +
                "}\n\n" +
                "http {\n" +
                "    include /etc/nginx/mime.types;\n" +
                "    default_type application/octet-stream;\n\n" +
                "    sendfile on;\n" +
                "    keepalive_timeout 65;\n\n" +
                "    server {\n" +
                "        listen 80;\n" +
                "        server_name localhost;\n" +
                "        root /usr/share/nginx/html;\n" +
                "        index index.html;\n\n" +
                "        # Handle client-side routing\n" +
                "        location / {\n" +
                "            try_files $uri $uri/ /index.html;\n" +
                "        }\n\n" +
                "        # Security headers\n" +
                "        add_header X-Frame-Options DENY;\n" +
                "        add_header X-Content-Type-Options nosniff;\n" +
                "        add_header X-XSS-Protection \"1; mode=block\";\n" +
                "    }\n" +
                "}";
        
        File nginxFile = new File(repoPath, "nginx.conf");
        try (FileWriter writer = new FileWriter(nginxFile)) {
            writer.write(nginxConfig);
        }
        
        logger.debug("Created nginx configuration at: {}", nginxFile.getAbsolutePath());
    }
}