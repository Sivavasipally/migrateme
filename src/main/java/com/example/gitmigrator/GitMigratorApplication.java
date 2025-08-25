package com.example.gitmigrator;

import com.example.gitmigrator.config.AppConfiguration;
import com.example.gitmigrator.controller.MainController;
import com.example.gitmigrator.service.GitApiService;
import com.example.gitmigrator.service.GitOperationService;
import com.example.gitmigrator.service.MigrationOrchestratorService;
import com.example.gitmigrator.service.TransformationService;
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

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting Git Repository Migrator JavaFX Application");
            
            // Initialize services
            initializeServices();
            
            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            
            // Create controller and inject dependencies
            MainController controller = new MainController();
            controller.setServices(gitApiService, migrationOrchestratorService);
            loader.setController(controller);
            
            // Load the scene
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            // Configure stage
            primaryStage.setTitle("Git Repository Migrator");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
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
            
            // Initialize services with dependencies
            gitOperationService = new GitOperationService();
            gitApiService = new GitApiService(gitOperationService);
            transformationService = new TransformationService();
            transformationService.setFreemarkerConfig(appConfiguration.freemarkerConfiguration());
            
            migrationOrchestratorService = new MigrationOrchestratorService();
            migrationOrchestratorService.setServices(gitApiService, gitOperationService, transformationService);
            
            logger.info("All services initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize services", e);
            throw new RuntimeException("Failed to initialize application services", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Shutting down Git Repository Migrator Application");
    }

    public static void main(String[] args) {
        logger.info("Launching Git Repository Migrator JavaFX Application");
        launch(args);
    }
}