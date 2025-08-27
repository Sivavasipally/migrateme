package com.example.gitmigrator.service;

import com.example.gitmigrator.model.MigrationConfiguration;

import java.io.File;
import java.util.List;

/**
 * Service interface for managing migration templates.
 * Provides functionality to save, load, and manage migration configuration templates.
 */
public interface TemplateManagementService {
    
    /**
     * Saves a migration configuration as a template.
     * 
     * @param name The name of the template
     * @param config The migration configuration to save
     * @throws RuntimeException if the template cannot be saved
     */
    void saveTemplate(String name, MigrationConfiguration config);
    
    /**
     * Loads a migration configuration template by name.
     * 
     * @param name The name of the template to load
     * @return The migration configuration, or null if not found
     * @throws RuntimeException if the template cannot be loaded
     */
    MigrationConfiguration loadTemplate(String name);
    
    /**
     * Gets a list of all available template names.
     * 
     * @return List of template names
     */
    List<String> getAvailableTemplates();
    
    /**
     * Deletes a template by name.
     * 
     * @param name The name of the template to delete
     * @return true if the template was deleted, false if it didn't exist
     */
    boolean deleteTemplate(String name);
    
    /**
     * Exports a template to a file.
     * 
     * @param name The name of the template to export
     * @param destination The destination file
     * @throws RuntimeException if the template cannot be exported
     */
    void exportTemplate(String name, File destination);
    
    /**
     * Imports a template from a file.
     * 
     * @param source The source file containing the template
     * @return The name of the imported template
     * @throws RuntimeException if the template cannot be imported
     */
    String importTemplate(File source);
    
    /**
     * Checks if a template exists.
     * 
     * @param name The name of the template
     * @return true if the template exists, false otherwise
     */
    boolean templateExists(String name);
    
    /**
     * Validates a template configuration.
     * 
     * @param config The configuration to validate
     * @return true if the configuration is valid, false otherwise
     */
    boolean validateTemplate(MigrationConfiguration config);
    
    /**
     * Gets the template directory path.
     * 
     * @return The path to the template directory
     */
    String getTemplateDirectory();
    
    /**
     * Initializes default templates if they don't exist.
     */
    void initializeDefaultTemplates();
    
    /**
     * Cleans up corrupted template files and recreates default templates.
     */
    void cleanupAndReinitializeTemplates();
}