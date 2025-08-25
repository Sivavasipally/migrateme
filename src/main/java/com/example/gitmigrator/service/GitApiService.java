package com.example.gitmigrator.service;

import com.example.gitmigrator.model.RepositoryInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Service for interacting with Git provider APIs (GitHub, GitLab, Bitbucket).
 * Handles authentication, pagination, and response parsing.
 * Rewritten to remove Spring dependencies.
 */
public class GitApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitApiService.class);
    
    private final ObjectMapper objectMapper;
    private GitOperationService gitOperationService;
    
    // Constructor for dependency injection
    public GitApiService(GitOperationService gitOperationService) {
        this.gitOperationService = gitOperationService;
        this.objectMapper = new ObjectMapper();
    }
    
    // Default constructor for testing
    public GitApiService() {
        this.gitOperationService = new GitOperationService();
        this.objectMapper = new ObjectMapper();
    }

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
        String currentUrl = buildPaginatedUrl(apiUrl, 1);
        int page = 1;
        final int maxPages = 50; // Safety limit to prevent infinite loops
        
        try {
            while (currentUrl != null && page <= maxPages) {
                logger.debug("Fetching page {} from: {}", page, currentUrl);
                
                String responseBody = makeHttpRequest(currentUrl, token);
                
                if (responseBody == null || responseBody.trim().isEmpty()) {
                    logger.warn("Received empty response from API");
                    break;
                }
                
                // Parse the JSON response
                List<RepositoryInfo> repositories = parseRepositoriesResponse(responseBody);
                
                if (repositories.isEmpty()) {
                    logger.debug("No more repositories found, stopping pagination");
                    break;
                }
                
                allRepositories.addAll(repositories);
                logger.debug("Fetched {} repositories from page {}", repositories.size(), page);
                
                // Prepare next page URL
                page++;
                if (page <= maxPages) {
                    currentUrl = buildPaginatedUrl(apiUrl, page);
                } else {
                    currentUrl = null;
                }
                
                // Rate limiting - add small delay between requests
                Thread.sleep(100);
            }
            
            logger.info("Successfully fetched {} repositories from {} pages", allRepositories.size(), page - 1);
            return allRepositories;
            
        } catch (Exception e) {
            logger.error("Failed to fetch repositories from {}: {}", apiUrl, e.getMessage(), e);
            throw new RuntimeException("API request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Makes HTTP request to the API endpoint.
     */
    private String makeHttpRequest(String url, String token) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        
        try {
            // Set request method and headers
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "token " + token);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "GitMigrator/1.0");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            
            int responseCode = connection.getResponseCode();
            logger.debug("API response code: {}", responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readResponse(connection);
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new RuntimeException("Authentication failed. Please check your access token.");
            } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new RuntimeException("Access forbidden. Rate limit exceeded or insufficient permissions.");
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new RuntimeException("API endpoint not found. Please verify the URL.");
            } else {
                String errorMessage = readErrorResponse(connection);
                throw new RuntimeException("API request failed with status " + responseCode + ": " + errorMessage);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Reads the response from the connection.
     */
    private String readResponse(HttpURLConnection connection) throws IOException {
        try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
            return scanner.useDelimiter("\\A").next();
        }
    }
    
    /**
     * Reads error response from the connection.
     */
    private String readErrorResponse(HttpURLConnection connection) {
        try (Scanner scanner = new Scanner(connection.getErrorStream(), StandardCharsets.UTF_8)) {
            return scanner.useDelimiter("\\A").next();
        } catch (Exception e) {
            return "Unknown error";
        }
    }
    
    /**
     * Builds paginated URL for API requests.
     */
    private String buildPaginatedUrl(String baseUrl, int page) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "page=" + page + "&per_page=100";
    }
    
    /**
     * Parses the JSON response to extract repository information.
     */
    private List<RepositoryInfo> parseRepositoriesResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, new TypeReference<List<RepositoryInfo>>() {});
        } catch (Exception e) {
            logger.error("Failed to parse repositories response: {}", e.getMessage());
            
            // Try to parse as a single repository (some APIs return single objects)
            try {
                RepositoryInfo singleRepo = objectMapper.readValue(responseBody, RepositoryInfo.class);
                List<RepositoryInfo> result = new ArrayList<>();
                result.add(singleRepo);
                return result;
            } catch (Exception e2) {
                logger.error("Failed to parse as single repository: {}", e2.getMessage());
                throw new RuntimeException("Failed to parse API response", e);
            }
        }
    }
    
    /**
     * Validates the API URL format.
     */
    public boolean isValidApiUrl(String apiUrl) {
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            return false;
        }
        
        try {
            URL url = new URL(apiUrl.trim());
            String protocol = url.getProtocol();
            return "http".equals(protocol) || "https".equals(protocol);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Detects the Git provider based on the API URL.
     */
    public String detectProvider(String apiUrl) {
        if (apiUrl == null) {
            return "unknown";
        }
        
        String lowerUrl = apiUrl.toLowerCase();
        if (lowerUrl.contains("github.com") || lowerUrl.contains("api.github.com")) {
            return "github";
        } else if (lowerUrl.contains("gitlab.com") || lowerUrl.contains("gitlab")) {
            return "gitlab";
        } else if (lowerUrl.contains("bitbucket.org") || lowerUrl.contains("bitbucket")) {
            return "bitbucket";
        } else {
            return "unknown";
        }
    }
}