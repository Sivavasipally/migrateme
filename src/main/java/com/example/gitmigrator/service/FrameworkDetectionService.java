package com.example.gitmigrator.service;

import com.example.gitmigrator.model.FrameworkType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive framework detection service that follows a systematic approach
 * to identify repository types and frameworks used in projects.
 */
public class FrameworkDetectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(FrameworkDetectionService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Component detection result containing framework type, location, and evidence.
     */
    public static class ComponentResult {
        private final FrameworkType type;
        private final String location;
        private final Set<String> evidence;
        
        public ComponentResult(FrameworkType type, String location, Set<String> evidence) {
            this.type = type;
            this.location = location;
            this.evidence = evidence;
        }
        
        public FrameworkType getType() { return type; }
        public String getLocation() { return location; }
        public Set<String> getEvidence() { return evidence; }
        
        @Override
        public String toString() {
            return String.format("%s at %s (evidence: %s)", 
                type.getDisplayName(), location, String.join(", ", evidence));
        }
    }
    
    /**
     * Detection result for an entire repository.
     */
    public static class DetectionResult {
        private final FrameworkType primaryFramework;
        private final List<ComponentResult> components;
        private final boolean isMonorepo;
        
        public DetectionResult(FrameworkType primaryFramework, List<ComponentResult> components) {
            this.primaryFramework = primaryFramework;
            this.components = components;
            this.isMonorepo = components.size() > 1;
        }
        
        public FrameworkType getPrimaryFramework() { return primaryFramework; }
        public List<ComponentResult> getComponents() { return components; }
        public boolean isMonorepo() { return isMonorepo; }
    }
    
    /**
     * Step 1: Detect framework type following the systematic approach.
     * This is the main entry point for framework detection.
     */
    public DetectionResult detectFramework(File repoDir) {
        logger.debug("Starting framework detection for repository: {}", repoDir.getAbsolutePath());
        
        List<ComponentResult> components = new ArrayList<>();
        
        // Step 1: Look for manifest files at root and major subfolders
        components.addAll(scanForComponents(repoDir, ""));
        
        // Scan major subfolders for additional components (monorepo detection)
        scanSubfoldersForComponents(repoDir, components);
        
        if (components.isEmpty()) {
            components.add(new ComponentResult(FrameworkType.UNKNOWN, "", 
                Set.of("No recognizable manifest files found")));
        }
        
        // Determine primary framework
        FrameworkType primaryFramework = determinePrimaryFramework(components);
        
        DetectionResult result = new DetectionResult(primaryFramework, components);
        logger.info("Framework detection completed. Primary: {}, Components: {}, Monorepo: {}", 
            primaryFramework, components.size(), result.isMonorepo());
        
        return result;
    }
    
    /**
     * Scan a directory for framework components based on manifest files.
     */
    private List<ComponentResult> scanForComponents(File dir, String relativePath) {
        List<ComponentResult> components = new ArrayList<>();
        
        // Java builds: pom.xml (Maven) or build.gradle (Gradle)
        ComponentResult javaComponent = detectJavaFramework(dir, relativePath);
        if (javaComponent != null) {
            components.add(javaComponent);
        }
        
        // JavaScript/TypeScript apps: package.json
        ComponentResult jsComponent = detectJavaScriptFramework(dir, relativePath);
        if (jsComponent != null) {
            components.add(jsComponent);
        }
        
        // Python apps: pyproject.toml or requirements.txt
        ComponentResult pythonComponent = detectPythonFramework(dir, relativePath);
        if (pythonComponent != null) {
            components.add(pythonComponent);
        }
        
        return components;
    }
    
    /**
     * Step 1 continued: Scan subfolders for monorepo detection.
     */
    private void scanSubfoldersForComponents(File repoDir, List<ComponentResult> components) {
        File[] subdirs = repoDir.listFiles(File::isDirectory);
        if (subdirs == null) return;
        
        for (File subdir : subdirs) {
            // Skip common non-project directories
            String name = subdir.getName();
            if (name.startsWith(".") || name.equals("node_modules") || 
                name.equals("target") || name.equals("build") || name.equals("dist")) {
                continue;
            }
            
            // Check if this subfolder has its own manifest
            String relativePath = subdir.getName();
            List<ComponentResult> subComponents = scanForComponents(subdir, relativePath);
            components.addAll(subComponents);
        }
    }
    
    /**
     * Step 2: Detect Java frameworks (Spring vs Spring Boot).
     */
    private ComponentResult detectJavaFramework(File dir, String location) {
        Set<String> evidence = new HashSet<>();
        
        // Check Maven
        File pomFile = new File(dir, "pom.xml");
        if (pomFile.exists()) {
            evidence.add("pom.xml");
            try {
                String pomContent = FileUtils.readFileToString(pomFile, StandardCharsets.UTF_8);
                
                // Step 2: Decide between Spring vs Spring Boot
                if (pomContent.contains("spring-boot-starter") || 
                    pomContent.contains("org.springframework.boot")) {
                    evidence.add("spring-boot dependencies");
                    
                    // Look for Spring Boot application class
                    if (hasSpringBootMainClass(dir)) {
                        evidence.add("@SpringBootApplication main class");
                    }
                    
                    // Look for application.properties/yml
                    if (new File(dir, "src/main/resources/application.properties").exists()) {
                        evidence.add("application.properties");
                    }
                    if (new File(dir, "src/main/resources/application.yml").exists()) {
                        evidence.add("application.yml");
                    }
                    
                    return new ComponentResult(FrameworkType.SPRING_BOOT, location, evidence);
                } else if (pomContent.contains("spring-core") || pomContent.contains("spring-context") ||
                          pomContent.contains("spring-web")) {
                    evidence.add("classic Spring dependencies");
                    
                    // Look for XML configuration
                    if (hasSpringXmlConfig(dir)) {
                        evidence.add("Spring XML configuration");
                    }
                    
                    // Look for web.xml
                    if (new File(dir, "src/main/webapp/WEB-INF/web.xml").exists()) {
                        evidence.add("web.xml");
                    }
                    
                    return new ComponentResult(FrameworkType.SPRING_CLASSIC, location, evidence);
                } else {
                    return new ComponentResult(FrameworkType.MAVEN_JAVA, location, evidence);
                }
            } catch (IOException e) {
                logger.debug("Failed to read pom.xml in {}: {}", location, e.getMessage());
            }
        }
        
        // Check Gradle
        File gradleBuild = new File(dir, "build.gradle");
        File gradleBuildKts = new File(dir, "build.gradle.kts");
        if (gradleBuild.exists() || gradleBuildKts.exists()) {
            evidence.add(gradleBuild.exists() ? "build.gradle" : "build.gradle.kts");
            
            try {
                File buildFile = gradleBuild.exists() ? gradleBuild : gradleBuildKts;
                String buildContent = FileUtils.readFileToString(buildFile, StandardCharsets.UTF_8);
                
                if (buildContent.contains("org.springframework.boot") || 
                    buildContent.contains("spring-boot")) {
                    evidence.add("Spring Boot Gradle plugin");
                    return new ComponentResult(FrameworkType.SPRING_BOOT, location, evidence);
                } else if (buildContent.contains("org.springframework") || 
                          buildContent.contains("spring-")) {
                    evidence.add("Spring Gradle dependencies");
                    return new ComponentResult(FrameworkType.SPRING_CLASSIC, location, evidence);
                } else {
                    return new ComponentResult(FrameworkType.GRADLE_JAVA, location, evidence);
                }
            } catch (IOException e) {
                logger.debug("Failed to read build.gradle in {}: {}", location, e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * Step 3: Detect JavaScript/TypeScript frameworks (React vs Angular).
     */
    private ComponentResult detectJavaScriptFramework(File dir, String location) {
        File packageJson = new File(dir, "package.json");
        if (!packageJson.exists()) {
            return null;
        }
        
        Set<String> evidence = new HashSet<>();
        evidence.add("package.json");
        
        try {
            String packageContent = FileUtils.readFileToString(packageJson, StandardCharsets.UTF_8);
            JsonNode packageNode = objectMapper.readTree(packageContent);
            
            // Check for Angular workspace
            File angularJson = new File(dir, "angular.json");
            if (angularJson.exists()) {
                evidence.add("angular.json");
                evidence.add("Angular workspace");
                
                JsonNode dependencies = packageNode.get("dependencies");
                if (dependencies != null && dependencies.has("@angular/core")) {
                    evidence.add("@angular/core dependency");
                }
                
                // Look for typical Angular structure
                if (new File(dir, "src/app/app.module.ts").exists()) {
                    evidence.add("src/app/app.module.ts");
                }
                if (new File(dir, "src/main.ts").exists()) {
                    evidence.add("src/main.ts");
                }
                
                return new ComponentResult(FrameworkType.ANGULAR, location, evidence);
            }
            
            // Check for React
            JsonNode dependencies = packageNode.get("dependencies");
            JsonNode devDependencies = packageNode.get("devDependencies");
            
            if (hasDependency(dependencies, "react") || hasDependency(devDependencies, "react")) {
                evidence.add("React dependency");
                
                // Look for typical React structure
                if (new File(dir, "src").exists()) {
                    evidence.add("src folder");
                }
                if (new File(dir, "public").exists()) {
                    evidence.add("public folder");
                }
                
                // Check for build tools
                if (hasDependency(dependencies, "vite") || hasDependency(devDependencies, "vite")) {
                    evidence.add("Vite build tool");
                }
                if (hasDependency(dependencies, "webpack") || hasDependency(devDependencies, "webpack")) {
                    evidence.add("Webpack build tool");
                }
                
                return new ComponentResult(FrameworkType.REACT, location, evidence);
            }
            
            // Check for AngularJS (1.x)
            if (hasDependency(dependencies, "angular") && !hasDependency(dependencies, "@angular/core")) {
                evidence.add("AngularJS dependency");
                
                // Look for bower.json (legacy)
                if (new File(dir, "bower.json").exists()) {
                    evidence.add("bower.json");
                }
                
                return new ComponentResult(FrameworkType.ANGULAR_JS, location, evidence);
            }
            
            // Generic Node.js project
            evidence.add("Node.js project");
            return new ComponentResult(FrameworkType.NODE_JS, location, evidence);
            
        } catch (IOException e) {
            logger.debug("Failed to read package.json in {}: {}", location, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Step 4: Detect Python frameworks (Flask vs FastAPI).
     */
    private ComponentResult detectPythonFramework(File dir, String location) {
        Set<String> evidence = new HashSet<>();
        
        // Check for pyproject.toml
        File pyprojectToml = new File(dir, "pyproject.toml");
        if (pyprojectToml.exists()) {
            evidence.add("pyproject.toml");
            try {
                String content = FileUtils.readFileToString(pyprojectToml, StandardCharsets.UTF_8);
                return analyzePythonDependencies(content, location, evidence);
            } catch (IOException e) {
                logger.debug("Failed to read pyproject.toml in {}: {}", location, e.getMessage());
            }
        }
        
        // Check for requirements.txt
        File requirementsTxt = new File(dir, "requirements.txt");
        if (requirementsTxt.exists()) {
            evidence.add("requirements.txt");
            try {
                String content = FileUtils.readFileToString(requirementsTxt, StandardCharsets.UTF_8);
                return analyzePythonDependencies(content, location, evidence);
            } catch (IOException e) {
                logger.debug("Failed to read requirements.txt in {}: {}", location, e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * Step 4 continued: Analyze Python dependencies.
     */
    private ComponentResult analyzePythonDependencies(String content, String location, Set<String> evidence) {
        if (content.contains("fastapi")) {
            evidence.add("FastAPI dependency");
            if (content.contains("uvicorn")) {
                evidence.add("Uvicorn server");
            }
            
            // Look for FastAPI application pattern
            if (hasFileContaining(new File(location.isEmpty() ? "." : location), "FastAPI()")) {
                evidence.add("FastAPI() app creation");
            }
            
            return new ComponentResult(FrameworkType.FASTAPI, location, evidence);
        } else if (content.contains("flask")) {
            evidence.add("Flask dependency");
            
            // Look for Flask application pattern
            if (hasFileContaining(new File(location.isEmpty() ? "." : location), "Flask(__name__)")) {
                evidence.add("Flask(__name__) app creation");
            }
            
            return new ComponentResult(FrameworkType.FLASK, location, evidence);
        } else {
            return new ComponentResult(FrameworkType.PYTHON, location, evidence);
        }
    }
    
    /**
     * Helper method to check if a dependency exists in package.json dependencies.
     */
    private boolean hasDependency(JsonNode dependencies, String depName) {
        return dependencies != null && dependencies.has(depName);
    }
    
    /**
     * Helper method to check for Spring Boot main class.
     */
    private boolean hasSpringBootMainClass(File dir) {
        File srcMain = new File(dir, "src/main/java");
        if (!srcMain.exists()) return false;
        
        try {
            return Files.walk(srcMain.toPath())
                .filter(path -> path.toString().endsWith(".java"))
                .anyMatch(path -> {
                    try {
                        String content = Files.readString(path);
                        return content.contains("@SpringBootApplication");
                    } catch (IOException e) {
                        return false;
                    }
                });
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Helper method to check for Spring XML configuration.
     */
    private boolean hasSpringXmlConfig(File dir) {
        try {
            return Files.walk(dir.toPath())
                .filter(path -> path.toString().endsWith("-context.xml") || 
                              path.toString().endsWith("applicationContext.xml"))
                .findAny()
                .isPresent();
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Helper method to check if any file in directory contains a specific string.
     */
    private boolean hasFileContaining(File dir, String searchText) {
        if (!dir.exists()) return false;
        
        try {
            return Files.walk(dir.toPath())
                .filter(path -> path.toString().endsWith(".py"))
                .anyMatch(path -> {
                    try {
                        String content = Files.readString(path);
                        return content.contains(searchText);
                    } catch (IOException e) {
                        return false;
                    }
                });
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Step 5: Determine primary framework for mixed/multi-stack repos.
     */
    private FrameworkType determinePrimaryFramework(List<ComponentResult> components) {
        if (components.isEmpty()) {
            return FrameworkType.UNKNOWN;
        }
        
        if (components.size() == 1) {
            return components.get(0).getType();
        }
        
        // For multiple components, determine if it's a monorepo or multi-stack
        Map<FrameworkType, Long> typeCount = components.stream()
            .collect(Collectors.groupingBy(ComponentResult::getType, Collectors.counting()));
        
        // If we have multiple different framework types, it's multi-stack
        if (typeCount.size() > 1) {
            // Check for common patterns
            boolean hasJava = typeCount.containsKey(FrameworkType.SPRING_BOOT) || 
                             typeCount.containsKey(FrameworkType.SPRING_CLASSIC) ||
                             typeCount.containsKey(FrameworkType.MAVEN_JAVA) ||
                             typeCount.containsKey(FrameworkType.GRADLE_JAVA);
                             
            boolean hasJS = typeCount.containsKey(FrameworkType.REACT) || 
                           typeCount.containsKey(FrameworkType.ANGULAR) ||
                           typeCount.containsKey(FrameworkType.NODE_JS);
            
            if (hasJava && hasJS) {
                return FrameworkType.MULTI_STACK; // Backend + Frontend
            } else {
                return FrameworkType.MONOREPO; // Multiple projects of different types
            }
        }
        
        // Same framework type in multiple locations = monorepo
        return typeCount.keySet().iterator().next();
    }
}