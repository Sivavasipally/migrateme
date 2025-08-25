package com.example.gitmigrator.config;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

/**
 * Application configuration for services.
 * Converted from Spring Configuration to plain Java configuration.
 */
public class AppConfiguration {
    
    /**
     * FreeMarker configuration for template processing.
     */
    public Configuration freemarkerConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        
        // Set template loading location
        cfg.setClassForTemplateLoading(this.getClass(), "/templates");
        
        // Set default encoding
        cfg.setDefaultEncoding("UTF-8");
        
        // Set template exception handler
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        
        // Don't log exceptions inside FreeMarker
        cfg.setLogTemplateExceptions(false);
        
        // Wrap unchecked exceptions thrown during template processing
        cfg.setWrapUncheckedExceptions(true);
        
        return cfg;
    }
}