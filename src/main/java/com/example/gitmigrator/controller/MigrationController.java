package com.example.gitmigrator.controller;

import com.example.gitmigrator.model.MigrationRequest;
import com.example.gitmigrator.model.MigrationResult;
import com.example.gitmigrator.model.RepositoryInfo;
import com.example.gitmigrator.service.GitApiService;
import com.example.gitmigrator.service.MigrationOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main controller for the Git Repository Migrator web application.
 * Handles both web UI requests and REST API endpoints.
 */
@Controller
public class MigrationController {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationController.class);
    
    @Autowired
    private GitApiService gitApiService;
    
    @Autowired
    private MigrationOrchestratorService migrationService;
    
    /**
     * Renders the main application page.
     */
    @GetMapping("/")
    public String index(Model model) {
        logger.info("Rendering main application page");
        model.addAttribute("title", "Git Repository Migrator");
        return "index";
    }
    
    /**
     * Fetches repositories from the specified Git API endpoint.
     */
    @PostMapping("/api/fetch-repos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> fetchRepositories(
            @RequestParam String apiUrl, 
            @RequestParam String token) {
        
        logger.info("Fetching repositories from: {}", apiUrl);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate inputs
            if (apiUrl == null || apiUrl.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "API URL is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (token == null || token.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Authentication token is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            List<RepositoryInfo> repositories = gitApiService.fetchRepositories(apiUrl.trim(), token);
            
            response.put("success", true);
            response.put("repositories", repositories);
            response.put("count", repositories.size());
            
            logger.info("Successfully fetched {} repositories", repositories.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching repositories: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Failed to fetch repositories: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Initiates the migration process for selected repositories.
     */
    @PostMapping("/api/migrate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> migrateRepositories(
            @RequestBody MigrationRequest request) {
        
        logger.info("Starting migration process for {} repositories", 
                   request.getRepositoryUrls().size());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate request
            if (request.getRepositoryUrls() == null || request.getRepositoryUrls().isEmpty()) {
                response.put("success", false);
                response.put("message", "No repositories selected for migration");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Start migration process
            List<MigrationResult> results = migrationService.migrateRepositories(request);
            
            // Calculate success rate
            long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
            
            response.put("success", true);
            response.put("results", results);
            response.put("totalRepositories", results.size());
            response.put("successfulMigrations", successCount);
            response.put("failedMigrations", results.size() - successCount);
            
            logger.info("Migration process completed. Success: {}/{}", 
                       successCount, results.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during migration process: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Migration process failed: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Git Repository Migrator");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}