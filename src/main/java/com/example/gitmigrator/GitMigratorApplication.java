package com.example.gitmigrator;

import com.example.gitmigrator.config.AppConfiguration;
import com.example.gitmigrator.controller.EnhancedMainController;
import com.example.gitmigrator.service.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main JavaFX application class for the Git Repository Migrator.
 * This application automates the modernization and migration of software repositories
 * from legacy deployment models to containerized OpenShift/Kubernetes deployments.
 */
public class GitMigratorApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(GitMigratorApplication.class);
    
    // Service instances
    private AppConfiguration appConfiguration;
    private GitApiService gitApiService;
    private GitOperationService gitOperationService;
    private TransformationService transformationService;
    private MigrationOrchestratorService migrationOrchestratorService;
    
    // Enhanced services
    private TemplateManagementService templateManagementService;
    private MigrationQueueService migrationQueueService;
    private GitServiceIntegration gitServiceIntegration;
    private ValidationService validationService;
    private ProgressTrackingService progressTrackingService;
    private ErrorReportingService errorReportingService;

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting Git Repository Migrator JavaFX Application");
            
            // Initialize services
            initializeServices();
            
            // Load FXML for enhanced UI
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/enhanced-main.fxml"));
            
            // Create enhanced controller and inject dependencies
            EnhancedMainController controller = new EnhancedMainController();
            controller.setServices(gitApiService, migrationOrchestratorService, 
                                 templateManagementService, transformationService);
            controller.setEnhancedServices(migrationQueueService, gitServiceIntegration,
                                         validationService, progressTrackingService, errorReportingService);
            loader.setController(controller);
            
            // Load the scene
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            // Configure stage for enhanced UI
            primaryStage.setTitle("Enhanced Git Repository Migrator");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);
            primaryStage.setMaximized(false);
            
            // Show the application
            primaryStage.show();
            
            logger.info("JavaFX Application started successfully");
            
        } catch (IOException e) {
            logger.error("Failed to start JavaFX Application", e);
            throw new RuntimeException("Failed to load FXML", e);
        }
    }
    
    /**
     * Initialize all service dependencies manually (replacing Spring DI).
     */
    private void initializeServices() {
        try {
            logger.info("Initializing services");
            
            // Initialize configuration
            appConfiguration = new AppConfiguration();
            
            // Initialize core services with dependencies
            gitOperationService = new GitOperationService();
            gitApiService = new GitApiService(gitOperationService);
            transformationService = new TransformationService();
            
            migrationOrchestratorService = new MigrationOrchestratorService();
            migrationOrchestratorService.setServices(gitApiService, gitOperationService, transformationService);
            
            // Initialize enhanced services
            initializeEnhancedServices();
            
            logger.info("All services initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize services", e);
            throw new RuntimeException("Failed to initialize application services", e);
        }
    }
    
    /**
     * Initialize enhanced services for the new functionality.
     */
    private void initializeEnhancedServices() {
        logger.info("Initializing enhanced services");
        
        // Template Management Service
        templateManagementService = new TemplateManagementServiceImpl();
        
        // Clean up any corrupted templates and reinitialize
        templateManagementService.cleanupAndReinitializeTemplates();
        
        // Migration Queue Service
        migrationQueueService = new MigrationQueueServiceImpl();
        
        // Git Service Integration (using GitHub as default)
        gitServiceIntegration = new GitHubServiceIntegration();
        
        // Validation Service
        validationService = new ValidationServiceImpl();
        
        // Progress Tracking Service
        progressTrackingService = new ProgressTrackingServiceImpl();
        
        // Error Reporting Service
        errorReportingService = new ErrorReportingServiceImpl();
        
        logger.info("Enhanced services initialized successfully");
    }

    @Override
    public void stop() {
        logger.info("Shutting down Enhanced Git Repository Migrator Application");
        
        // Cleanup enhanced services
        shutdownEnhancedServices();
    }
    
    /**
     * Shutdown enhanced services and cleanup resources.
     */
    private void shutdownEnhancedServices() {
        try {
            // Stop migration queue processing
            if (migrationQueueService != null) {
                migrationQueueService.pauseProcessing();
            }
            
            // Cleanup progress tracking
            if (progressTrackingService != null) {
                // Any cleanup needed for progress tracking
            }
            
            logger.info("Enhanced services shutdown completed");
            
        } catch (Exception e) {
            logger.error("Error during enhanced services shutdown", e);
        }
    }
    
    /**
     * Get the template management service instance.
     */
    public TemplateManagementService getTemplateManagementService() {
        return templateManagementService;
    }
    
    /**
     * Get the migration queue service instance.
     */
    public MigrationQueueService getMigrationQueueService() {
        return migrationQueueService;
    }
    
    /**
     * Get the git service integration instance.
     */
    public GitServiceIntegration getGitServiceIntegration() {
        return gitServiceIntegration;
    }
    
    /**
     * Get the validation service instance.
     */
    public ValidationService getValidationService() {
        return validationService;
    }
    
    /**
     * Get the progress tracking service instance.
     */
    public ProgressTrackingService getProgressTrackingService() {
        return progressTrackingService;
    }
    
    /**
     * Get the error reporting service instance.
     */
    public ErrorReportingService getErrorReportingService() {
        return errorReportingService;
    }

    public static void main(String[] args) {
        logger.info("Launching Git Repository Migrator JavaFX Application");
        launch(args);
    }
}