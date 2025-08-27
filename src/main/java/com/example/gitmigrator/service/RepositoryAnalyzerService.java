package com.example.gitmigrator.service;

import com.example.gitmigrator.model.FrameworkType;
import com.example.gitmigrator.model.RepositoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for analyzing repositories and determining their characteristics.
 * This service integrates the comprehensive framework detection with repository analysis.
 */
public class RepositoryAnalyzerService {
    
    private static final Logger logger = LoggerFactory.getLogger(RepositoryAnalyzerService.class);
    
    private final FrameworkDetectionService frameworkDetectionService;
    
    public RepositoryAnalyzerService() {
        this.frameworkDetectionService = new FrameworkDetectionService();
    }
    
    /**
     * Analyzes a repository and enriches the RepositoryInfo with detected framework information.
     */
    public void analyzeRepository(RepositoryInfo repositoryInfo) {
        if (repositoryInfo.getLocalPath() == null || repositoryInfo.getLocalPath().isEmpty()) {
            logger.warn("Cannot analyze repository without local path: {}", repositoryInfo.getName());
            return;
        }
        
        File repoDir = new File(repositoryInfo.getLocalPath());
        if (!repoDir.exists() || !repoDir.isDirectory()) {
            logger.warn("Repository directory does not exist: {}", repoDir.getAbsolutePath());
            return;
        }
        
        logger.info("Analyzing repository: {}", repositoryInfo.getName());
        
        // Perform comprehensive framework detection
        FrameworkDetectionService.DetectionResult detectionResult = 
            frameworkDetectionService.detectFramework(repoDir);
        
        // Set the primary detected framework
        repositoryInfo.setDetectedFramework(detectionResult.getPrimaryFramework());
        
        // Calculate estimated complexity based on detection results
        int complexity = calculateComplexity(detectionResult);
        repositoryInfo.setEstimatedComplexity(complexity);
        
        // Store additional metadata about the detection
        Map<String, Object> metadata = createAnalysisMetadata(detectionResult);
        repositoryInfo.setAdditionalMetadata(metadata);
        
        logger.info("Repository analysis completed for '{}': Framework={}, Complexity={}, Components={}", 
            repositoryInfo.getName(), 
            detectionResult.getPrimaryFramework().getDisplayName(),
            complexity,
            detectionResult.getComponents().size());
    }
    
    /**
     * Calculates repository complexity based on framework detection results.
     * Complexity scale: 1 (Simple) to 5 (Very Complex)
     */
    private int calculateComplexity(FrameworkDetectionService.DetectionResult result) {
        int complexity = 1; // Base complexity
        
        // Add complexity based on framework type
        FrameworkType primary = result.getPrimaryFramework();
        switch (primary) {
            case SPRING_BOOT:
                complexity += 2; // Modern, well-structured
                break;
            case SPRING_CLASSIC:
                complexity += 3; // Older, potentially more complex configuration
                break;
            case REACT:
            case ANGULAR:
                complexity += 2; // Modern frontend frameworks
                break;
            case ANGULAR_JS:
                complexity += 3; // Legacy, potentially complex
                break;
            case FASTAPI:
            case FLASK:
                complexity += 1; // Python frameworks are generally simpler
                break;
            case MULTI_STACK:
                complexity += 3; // Multiple technologies
                break;
            case MONOREPO:
                complexity += 2; // Multiple projects but same type
                break;
            case NODE_JS:
            case PYTHON:
            case MAVEN_JAVA:
            case GRADLE_JAVA:
                complexity += 1; // Basic projects
                break;
            default:
                complexity += 0; // Unknown or very simple
        }
        
        // Add complexity for multiple components
        if (result.isMonorepo()) {
            complexity += Math.min(result.getComponents().size() - 1, 2); // Cap additional complexity
        }
        
        // Ensure complexity is within bounds
        return Math.min(Math.max(complexity, 1), 5);
    }
    
    /**
     * Creates metadata object with detailed analysis information.
     */
    private Map<String, Object> createAnalysisMetadata(FrameworkDetectionService.DetectionResult result) {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("isMonorepo", result.isMonorepo());
        metadata.put("componentCount", result.getComponents().size());
        
        // Store component details
        Map<String, Object> components = new HashMap<>();
        for (int i = 0; i < result.getComponents().size(); i++) {
            FrameworkDetectionService.ComponentResult component = result.getComponents().get(i);
            Map<String, Object> componentInfo = new HashMap<>();
            componentInfo.put("type", component.getType().name());
            componentInfo.put("displayName", component.getType().getDisplayName());
            componentInfo.put("location", component.getLocation());
            componentInfo.put("evidence", component.getEvidence());
            
            components.put("component" + i, componentInfo);
        }
        metadata.put("components", components);
        
        // Add framework-specific recommendations
        metadata.put("migrationRecommendations", getMigrationRecommendations(result));
        
        return metadata;
    }
    
    /**
     * Provides migration recommendations based on detected frameworks.
     */
    private Map<String, String> getMigrationRecommendations(FrameworkDetectionService.DetectionResult result) {
        Map<String, String> recommendations = new HashMap<>();
        
        FrameworkType primary = result.getPrimaryFramework();
        
        switch (primary) {
            case SPRING_BOOT:
                recommendations.put("containerStrategy", "Multi-stage Docker build with Maven/Gradle");
                recommendations.put("deploymentTarget", "Kubernetes with Helm charts");
                recommendations.put("baseImage", "openjdk:17-jre-slim or eclipse-temurin:17-jre");
                break;
                
            case SPRING_CLASSIC:
                recommendations.put("containerStrategy", "Tomcat-based deployment or migration to Spring Boot");
                recommendations.put("deploymentTarget", "Consider modernization to Spring Boot first");
                recommendations.put("baseImage", "tomcat:9-jre17 or migration path assessment needed");
                break;
                
            case REACT:
                recommendations.put("containerStrategy", "Multi-stage build with Nginx serving static files");
                recommendations.put("deploymentTarget", "Kubernetes with CDN integration");
                recommendations.put("baseImage", "nginx:alpine");
                break;
                
            case ANGULAR:
                recommendations.put("containerStrategy", "Multi-stage build with Nginx");
                recommendations.put("deploymentTarget", "Kubernetes with ingress");
                recommendations.put("baseImage", "nginx:alpine");
                break;
                
            case NODE_JS:
                recommendations.put("containerStrategy", "Node.js runtime container");
                recommendations.put("deploymentTarget", "Kubernetes with horizontal pod autoscaling");
                recommendations.put("baseImage", "node:18-alpine");
                break;
                
            case FASTAPI:
            case FLASK:
                recommendations.put("containerStrategy", "Python runtime with WSGI server");
                recommendations.put("deploymentTarget", "Kubernetes with gunicorn/uvicorn");
                recommendations.put("baseImage", "python:3.11-slim");
                break;
                
            case MULTI_STACK:
                recommendations.put("containerStrategy", "Separate containers for each stack component");
                recommendations.put("deploymentTarget", "Kubernetes with service mesh consideration");
                recommendations.put("baseImage", "Multiple base images - one per component");
                break;
                
            case MONOREPO:
                recommendations.put("containerStrategy", "Shared build stages, separate deployment containers");
                recommendations.put("deploymentTarget", "Kubernetes namespace per project or unified deployment");
                recommendations.put("baseImage", "Depends on project types - may need multiple");
                break;
                
            default:
                recommendations.put("containerStrategy", "Generic containerization approach needed");
                recommendations.put("deploymentTarget", "Assessment required");
                recommendations.put("baseImage", "To be determined based on runtime requirements");
        }
        
        return recommendations;
    }
    
    /**
     * Get a quick framework detection for a local directory path.
     */
    public FrameworkType detectFramework(String localPath) {
        File repoDir = new File(localPath);
        FrameworkDetectionService.DetectionResult result = frameworkDetectionService.detectFramework(repoDir);
        return result.getPrimaryFramework();
    }
    
    /**
     * Get detailed detection results for a local directory path.
     */
    public FrameworkDetectionService.DetectionResult getDetailedDetection(String localPath) {
        File repoDir = new File(localPath);
        return frameworkDetectionService.detectFramework(repoDir);
    }
}