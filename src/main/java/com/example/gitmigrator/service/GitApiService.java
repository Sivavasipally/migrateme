package com.example.gitmigrator.service;

import com.example.gitmigrator.model.RepositoryInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for interacting with Git provider APIs (GitHub, GitLab, Bitbucket).
 * Handles authentication, pagination, and response parsing.
 */
@Service
public class GitApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitApiService.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Fetches repositories from the specified Git API endpoint.
     * Handles pagination automatically to retrieve all repositories.
     * 
     * @param apiUrl The Git API endpoint URL
     * @param token Personal Access Token for authentication
     * @return List of repository information
     * @throws RuntimeException if the API call fails
     */
    public List<RepositoryInfo> fetchRepositories(String apiUrl, String token) {
        logger.info("Fetching repositories from API: {}", apiUrl);
        
        List<RepositoryInfo> allRepositories = new ArrayList<>();
        String currentUrl = apiUrl;
        int page = 1;
        final int maxPages = 50; // Safety limit to prevent infinite loops
        
        try {
            while (currentUrl != null && page <= maxPages) {
                logger.debug("Fetching page {} from: {}", page, currentUrl);
                
                HttpHeaders headers = createAuthHeaders(token);
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                ResponseEntity<String> response = restTemplate.exchange(
                    addPaginationParams(currentUrl, page), 
                    HttpMethod.GET, 
                    entity, 
                    String.class
                );
                
                if (response.getStatusCode() == HttpStatus.OK) {
                    List<RepositoryInfo> pageRepositories = parseRepositories(response.getBody());
                    allRepositories.addAll(pageRepositories);
                    
                    logger.debug("Fetched {} repositories from page {}", pageRepositories.size(), page);
                    
                    // Check if there are more pages
                    currentUrl = getNextPageUrl(response.getHeaders(), currentUrl);
                    page++;
                    
                    // If no repositories returned, assume we've reached the end
                    if (pageRepositories.isEmpty()) {
                        break;
                    }
                } else {
                    logger.warn("Unexpected response status: {}", response.getStatusCode());
                    break;
                }
            }
            
            logger.info("Successfully fetched {} repositories total", allRepositories.size());
            return allRepositories;
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error fetching repositories: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch repositories: " + e.getMessage(), e);
            
        } catch (Exception e) {
            logger.error("Unexpected error fetching repositories", e);
            throw new RuntimeException("Failed to fetch repositories: " + e.getMessage(), e);
        }
    }
    
    /**
     * Creates HTTP headers with authentication token.
     */
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/vnd.github.v3+json");
        
        // Support both GitHub and GitLab token formats
        if (token.startsWith("ghp_") || token.startsWith("github_pat_")) {
            headers.set("Authorization", "token " + token);
        } else if (token.startsWith("glpat-")) {
            headers.set("Authorization", "Bearer " + token);
        } else {
            // Default to Bearer token format
            headers.set("Authorization", "Bearer " + token);
        }
        
        headers.set("User-Agent", "GitMigrator/1.0");
        
        return headers;
    }
    
    /**
     * Adds pagination parameters to the URL.
     */
    private String addPaginationParams(String baseUrl, int page) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "per_page=100&page=" + page;
    }
    
    /**
     * Extracts the next page URL from response headers.
     */
    private String getNextPageUrl(HttpHeaders headers, String currentUrl) {
        List<String> linkHeaders = headers.get("Link");
        if (linkHeaders == null || linkHeaders.isEmpty()) {
            return null;
        }
        
        String linkHeader = linkHeaders.get(0);
        
        // Parse GitHub-style Link header: <https://api.github.com/resource?page=2>; rel="next"
        if (linkHeader.contains("rel=\"next\"")) {
            int startIndex = linkHeader.indexOf('<') + 1;
            int endIndex = linkHeader.indexOf('>', startIndex);
            if (startIndex > 0 && endIndex > startIndex) {
                return linkHeader.substring(startIndex, endIndex);
            }
        }
        
        return null;
    }
    
    /**
     * Parses JSON response into list of RepositoryInfo objects.
     */
    private List<RepositoryInfo> parseRepositories(String jsonResponse) throws JsonProcessingException {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(jsonResponse, new TypeReference<List<RepositoryInfo>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse repository JSON response", e);
            throw new RuntimeException("Failed to parse repository data: " + e.getMessage(), e);
        }
    }
}