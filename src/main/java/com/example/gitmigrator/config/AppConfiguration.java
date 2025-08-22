package com.example.gitmigrator.config;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Application configuration for beans and services.
 */
@org.springframework.context.annotation.Configuration
public class AppConfiguration {
    
    /**
     * RestTemplate configuration for making HTTP API calls.
     */
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(30000);    // 30 seconds
        
        return new RestTemplate(factory);
    }
    
    /**
     * FreeMarker configuration for template processing.
     */
    @Bean
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