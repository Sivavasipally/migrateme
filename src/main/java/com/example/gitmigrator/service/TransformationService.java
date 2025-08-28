package com.example.gitmigrator.service;

import com.example.gitmigrator.model.FrameworkType;
import com.example.gitmigrator.model.RepositoryInfo;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core transformation service that analyzes repositories and applies framework-specific modifications.
 * Handles Spring Boot to Kubernetes/OpenShift migration with Helm charts.
 */
public class TransformationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransformationService.class);
    
    private Configuration freemarkerConfig;
    
    // Cache for expensive analysis operations
    private final Map<String, RepositoryAnalysis> analysisCache = new ConcurrentHashMap<>();
    
    // Language detection patterns
    private static final Map<String, Pattern> LANGUAGE_PATTERNS = new HashMap<>();
    static {
        LANGUAGE_PATTERNS.put("Java", Pattern.compile(".*\\.java$", Pattern.CASE_INSENSITIVE));
        LANGUAGE_PATTERNS.put("JavaScript", Pattern.compile(".*\\.(js|jsx)$", Pattern.CASE_INSENSITIVE));
        LANGUAGE_PATTERNS.put("TypeScript", Pattern.compile(".*\\.(ts|tsx)$", Pattern.CASE_INSENSITIVE));
        LANGUAGE_PATTERNS.put("Python", Pattern.compile(".*\\.py$", Pattern.CASE_INSENSITIVE));
        LANGUAGE_PATTERNS.put("C#", Pattern.compile(".*\\.cs$", Pattern.CASE_INSENSITIVE));
    }
    
    /**
     * Initialize the transformation service with FreeMarker configuration.
     */
    public TransformationService() {
        initializeFreeMarker();
    }
    
    /**
     * Initialize FreeMarker configuration for template processing.
     */
    private void initializeFreeMarker() {
        freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        freemarkerConfig.setClassForTemplateLoading(this.getClass(), "/templates");
        freemarkerConfig.setDefaultEncoding("UTF-8");
    }
    
    /**
     * Analyzes a repository and returns enhanced metadata.
     */
    public RepositoryInfo analyzeRepository(String repositoryPath) {
        logger.info("Analyzing repository: {}", repositoryPath);
        
        File repoDir = new File(repositoryPath);
        if (!repoDir.exists() || !repoDir.isDirectory()) {
            throw new IllegalArgumentException("Repository path does not exist or is not a directory: " + repositoryPath);
        }
        
        RepositoryInfo repoInfo = new RepositoryInfo();
        repoInfo.setName(repoDir.getName());
        repoInfo.setLocalPath(repositoryPath);
        repoInfo.setCloneUrl("file://" + repositoryPath);
        
        // Detect framework type with enhanced debugging
        logger.debug("Starting framework detection for: {}", repositoryPath);
        FrameworkType framework = detectFrameworkType(repoDir);
        logger.info("Framework detection completed - detected: {}", 
            framework != null ? framework.getDisplayName() : "null");
        repoInfo.setDetectedFramework(framework);
        
        // Extract metadata based on framework
        Map<String, Object> metadata = extractFrameworkMetadata(repoDir, framework);
        
        // Calculate complexity and size with debugging
        int complexity = calculateComplexity(repoDir, framework);
        logger.debug("Calculated complexity: {}", complexity);
        repoInfo.setEstimatedComplexity(complexity);
        repoInfo.setRepositorySize(calculateDirectorySize(repoDir));
        
        // Detect languages
        List<String> languages = detectLanguages(repoDir);
        if (!languages.isEmpty()) {
            repoInfo.setLanguage(languages.get(0)); // Primary language
            logger.debug("Detected primary language: {}", languages.get(0));
        }
        
        // Set last commit date (simplified - would use Git API in real implementation)
        repoInfo.setLastCommitDate(LocalDateTime.now());
        
        logger.info("Repository analysis completed for '{}': Framework={}, Complexity={}, Languages={}", 
            repoInfo.getName(), 
            framework != null ? framework.getDisplayName() : "null",
            complexity,
            languages.size());
        return repoInfo;
    }
    
    /**
     * Transforms a repository by applying framework-specific changes.
     */
    public void transformRepository(String repositoryPath, String targetFramework, boolean includeHelm, boolean includeDockerfile) throws IOException, TemplateException {
        logger.info("Transforming repository: {} to target: {}", repositoryPath, targetFramework);
        
        File repoDir = new File(repositoryPath);
        FrameworkType framework = detectFrameworkType(repoDir);
        Map<String, Object> metadata = extractFrameworkMetadata(repoDir, framework);
        
        // Add target framework to metadata
        metadata.put("targetFramework", targetFramework);
        
        if (includeDockerfile) {
            createDockerfile(repoDir, metadata, framework);
        }
        
        if (includeHelm) {
            createHelmChartStructure(repoDir, metadata, framework);
        }
        
        logger.info("Repository transformation completed for: {}", repositoryPath);
    }
    
    /**
     * Detects the framework type of a repository using the comprehensive detection service.
     */
    private FrameworkType detectFrameworkType(File repoDir) {
        FrameworkDetectionService detectionService = new FrameworkDetectionService();
        FrameworkDetectionService.DetectionResult result = detectionService.detectFramework(repoDir);
        
        // Log detailed detection results
        if (result.isMonorepo()) {
            logger.info("Detected monorepo with {} components:", result.getComponents().size());
            for (FrameworkDetectionService.ComponentResult component : result.getComponents()) {
                logger.info("  - {}", component);
            }
        } else {
            logger.info("Detected single framework: {}", result.getPrimaryFramework().getDisplayName());
            if (!result.getComponents().isEmpty()) {
                FrameworkDetectionService.ComponentResult component = result.getComponents().get(0);
                logger.debug("Evidence: {}", String.join(", ", component.getEvidence()));
            }
        }
        
        return result.getPrimaryFramework();
    }
    
    /**
     * Extracts framework-specific metadata.
     */
    private Map<String, Object> extractFrameworkMetadata(File repoDir, FrameworkType framework) {
        Map<String, Object> metadata = new HashMap<>();
        
        switch (framework) {
            case SPRING_BOOT:
                metadata = extractSpringBootMetadata(repoDir);
                break;
            case REACT:
            case ANGULAR:
            case NODE_JS:
                metadata = extractNodeBasedMetadata(repoDir, framework.getDisplayName() + " Application");
                break;
            default:
                metadata = extractGenericMetadata(repoDir);
                break;
        }
        
        return metadata;
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
            metadata.put("serverPort", "8080");
            
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
     * Calculates the complexity of a repository based on various factors.
     */
    private int calculateComplexity(File repoDir, FrameworkType framework) {
        int complexity = 1; // Base complexity
        
        // Add complexity based on file count
        int fileCount = countFiles(repoDir);
        if (fileCount > 100) complexity++;
        if (fileCount > 500) complexity++;
        if (fileCount > 1000) complexity++;
        
        // Add complexity based on framework
        switch (framework) {
            case SPRING_BOOT:
                complexity++; // Spring Boot projects are generally more complex
                break;
            case REACT:
            case ANGULAR:
                complexity++; // Frontend frameworks add complexity
                break;
        }
        
        return Math.min(complexity, 5); // Cap at 5
    }
    
    /**
     * Calculates the total size of a directory in bytes.
     */
    private long calculateDirectorySize(File directory) {
        long size = 0;
        try {
            size = Files.walk(Paths.get(directory.getAbsolutePath()))
                    .filter(p -> p.toFile().isFile())
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        } catch (IOException e) {
            logger.debug("Failed to calculate directory size: {}", e.getMessage());
        }
        return size;
    }
    
    /**
     * Detects programming languages used in the repository.
     */
    private List<String> detectLanguages(File repoDir) {
        Map<String, Integer> languageCounts = new HashMap<>();
        
        try {
            Files.walk(Paths.get(repoDir.getAbsolutePath()))
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        for (Map.Entry<String, Pattern> entry : LANGUAGE_PATTERNS.entrySet()) {
                            if (entry.getValue().matcher(fileName).matches()) {
                                languageCounts.merge(entry.getKey(), 1, Integer::sum);
                            }
                        }
                    });
        } catch (IOException e) {
            logger.debug("Failed to detect languages: {}", e.getMessage());
        }
        
        return languageCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }
    
    /**
     * Counts the total number of files in a directory.
     */
    private int countFiles(File directory) {
        try {
            return (int) Files.walk(Paths.get(directory.getAbsolutePath()))
                    .filter(Files::isRegularFile)
                    .count();
        } catch (IOException e) {
            logger.debug("Failed to count files: {}", e.getMessage());
            return 0;
        }
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
     * Helper method to extract text content from XML using XPath.
     */
    private String getTextContent(Document document, String xpath) {
        Node node = document.selectSingleNode(xpath);
        return node != null ? node.getText() : null;
    }
    
    /**
     * Simple helper to extract JSON values (simplified approach).
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
    
    /**
     * Internal class to cache repository analysis results.
     */
    private static class RepositoryAnalysis {
        private final FrameworkType frameworkType;
        private final Map<String, Object> metadata;
        private final long timestamp;
        
        public RepositoryAnalysis(FrameworkType frameworkType, Map<String, Object> metadata) {
            this.frameworkType = frameworkType;
            this.metadata = metadata;
            this.timestamp = System.currentTimeMillis();
        }
        
        public FrameworkType getFrameworkType() { return frameworkType; }
        public Map<String, Object> getMetadata() { return metadata; }
        public long getTimestamp() { return timestamp; }
    }
}