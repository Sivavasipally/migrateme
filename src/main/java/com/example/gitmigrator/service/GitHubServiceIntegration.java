package com.example.gitmigrator.service;

import com.example.gitmigrator.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * GitHub implementation of GitServiceIntegration.
 * Provides integration with GitHub API v3 for repository management.
 */
public class GitHubServiceIntegration implements GitServiceIntegration {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubServiceIntegration.class);
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String USER_AGENT = "GitMigrator/1.0";
    
    private final ObjectMapper objectMapper;
    
    public GitHubServiceIntegration() {
        this.objectMapper = new ObjectMapper();
        // Configure ObjectMapper to ignore unknown properties from GitHub API
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Register JSR310 module for Java 8 time support
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Override
    public CompletableFuture<List<RepositoryInfo>> fetchRepositories(GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = config.getRepositoriesEndpoint();
                if (endpoint == null) {
                    endpoint = GITHUB_API_BASE + "/user/repos";
                }
                
                List<RepositoryInfo> allRepos = new ArrayList<>();
                int page = 1;
                int maxPages = 10; // Safety limit
                
                while (page <= maxPages) {
                    String url = endpoint + (endpoint.contains("?") ? "&" : "?") + 
                                "page=" + page + "&per_page=100&sort=updated";
                    
                    String response = makeHttpRequest(url, config.getToken());
                    List<RepositoryInfo> pageRepos = parseRepositories(response);
                    
                    if (pageRepos.isEmpty()) {
                        break;
                    }
                    
                    allRepos.addAll(pageRepos);
                    
                    if (pageRepos.size() < 100) {
                        break; // Last page
                    }
                    
                    page++;
                    
                    if (allRepos.size() >= config.getMaxRepositories()) {
                        allRepos = allRepos.subList(0, config.getMaxRepositories());
                        break;
                    }
                }
                
                logger.info("Fetched {} repositories from GitHub", allRepos.size());
                return allRepos;
                
            } catch (Exception e) {
                logger.error("Failed to fetch repositories from GitHub", e);
                throw new RuntimeException("Failed to fetch repositories: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<RepositoryInfo>> searchRepositories(String query, GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = GITHUB_API_BASE + "/search/repositories?q=" + encodedQuery + 
                           "&sort=updated&order=desc&per_page=100";
                
                String response = makeHttpRequest(url, config.getToken());
                JsonNode rootNode = objectMapper.readTree(response);
                JsonNode itemsNode = rootNode.get("items");
                
                if (itemsNode == null || !itemsNode.isArray()) {
                    return new ArrayList<>();
                }
                
                List<RepositoryInfo> repositories = objectMapper.convertValue(
                    itemsNode, new TypeReference<List<RepositoryInfo>>() {}
                );
                
                logger.info("Found {} repositories for query: {}", repositories.size(), query);
                return repositories;
                
            } catch (Exception e) {
                logger.error("Failed to search repositories on GitHub", e);
                throw new RuntimeException("Failed to search repositories: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public boolean authenticateService(GitServiceConfig config) {
        try {
            String url = GITHUB_API_BASE + "/user";
            String response = makeHttpRequest(url, config.getToken());
            
            JsonNode userNode = objectMapper.readTree(response);
            return userNode.has("login");
            
        } catch (Exception e) {
            logger.warn("GitHub authentication failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<String> getSupportedServices() {
        return List.of("github");
    }
    
    @Override
    public boolean isServiceSupported(String serviceName) {
        return "github".equalsIgnoreCase(serviceName);
    }
    
    @Override
    public GitServiceConfig getDefaultConfig(String serviceName) {
        if (!"github".equalsIgnoreCase(serviceName)) {
            return null;
        }
        
        GitServiceConfig config = new GitServiceConfig();
        config.setServiceName("github");
        config.setApiUrl(GITHUB_API_BASE);
        config.setMaxRepositories(100);
        return config;
    }
    
    @Override
    public boolean validateConfig(GitServiceConfig config) {
        if (config == null) return false;
        if (!"github".equalsIgnoreCase(config.getServiceName())) return false;
        if (config.getToken() == null || config.getToken().trim().isEmpty()) return false;
        if (config.getApiUrl() == null || !config.getApiUrl().startsWith("https://api.github.com")) return false;
        
        return true;
    }
    
    @Override
    public CompletableFuture<GitUserInfo> getUserInfo(GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = GITHUB_API_BASE + "/user";
                String response = makeHttpRequest(url, config.getToken());
                
                GitUserInfo userInfo = objectMapper.readValue(response, GitUserInfo.class);
                logger.debug("Retrieved user info for: {}", userInfo.getUsername());
                return userInfo;
                
            } catch (Exception e) {
                logger.error("Failed to get user info from GitHub", e);
                throw new RuntimeException("Failed to get user info: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<GitOrganization>> getUserOrganizations(GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = GITHUB_API_BASE + "/user/orgs";
                String response = makeHttpRequest(url, config.getToken());
                
                List<GitOrganization> organizations = objectMapper.readValue(
                    response, new TypeReference<List<GitOrganization>>() {}
                );
                
                logger.debug("Retrieved {} organizations", organizations.size());
                return organizations;
                
            } catch (Exception e) {
                logger.error("Failed to get organizations from GitHub", e);
                throw new RuntimeException("Failed to get organizations: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<GitConnectionTestResult> testConnection(GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                String url = GITHUB_API_BASE + "/rate_limit";
                String response = makeHttpRequest(url, config.getToken());
                
                long responseTime = System.currentTimeMillis() - startTime;
                
                JsonNode rootNode = objectMapper.readTree(response);
                if (rootNode.has("rate")) {
                    GitConnectionTestResult result = new GitConnectionTestResult(
                        true, "Connection successful", responseTime
                    );
                    result.setServiceName("GitHub");
                    result.setApiVersion("v3");
                    return result;
                } else {
                    return new GitConnectionTestResult(false, "Invalid response from GitHub API");
                }
                
            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - startTime;
                return new GitConnectionTestResult(false, "Connection failed: " + e.getMessage(), responseTime);
            }
        });
    }
    
    @Override
    public CompletableFuture<GitRateLimitInfo> getRateLimitInfo(GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = GITHUB_API_BASE + "/rate_limit";
                String response = makeHttpRequest(url, config.getToken());
                
                JsonNode rootNode = objectMapper.readTree(response);
                JsonNode rateNode = rootNode.get("rate");
                
                if (rateNode != null) {
                    int limit = rateNode.get("limit").asInt();
                    int remaining = rateNode.get("remaining").asInt();
                    long resetTimestamp = rateNode.get("reset").asLong();
                    
                    LocalDateTime resetTime = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(resetTimestamp), ZoneId.systemDefault()
                    );
                    
                    GitRateLimitInfo rateLimitInfo = new GitRateLimitInfo(limit, remaining, resetTime);
                    rateLimitInfo.setResource("core");
                    
                    return rateLimitInfo;
                } else {
                    throw new RuntimeException("Invalid rate limit response");
                }
                
            } catch (Exception e) {
                logger.error("Failed to get rate limit info from GitHub", e);
                throw new RuntimeException("Failed to get rate limit info: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<GitRepositoryPage> fetchRepositoriesPage(GitServiceConfig config, int page, int perPage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = config.getRepositoriesEndpoint();
                if (endpoint == null) {
                    endpoint = GITHUB_API_BASE + "/user/repos";
                }
                
                String url = endpoint + (endpoint.contains("?") ? "&" : "?") + 
                           "page=" + page + "&per_page=" + perPage + "&sort=updated";
                
                String response = makeHttpRequest(url, config.getToken());
                List<RepositoryInfo> repositories = parseRepositories(response);
                
                // GitHub doesn't provide total count in the response, so we estimate
                int totalCount = repositories.size() < perPage ? 
                    (page - 1) * perPage + repositories.size() : 
                    page * perPage + 1; // Estimate there's at least one more page
                
                int totalPages = (int) Math.ceil((double) totalCount / perPage);
                
                GitRepositoryPage repositoryPage = new GitRepositoryPage(repositories, page, totalPages, totalCount);
                
                logger.debug("Fetched page {} with {} repositories", page, repositories.size());
                return repositoryPage;
                
            } catch (Exception e) {
                logger.error("Failed to fetch repository page from GitHub", e);
                throw new RuntimeException("Failed to fetch repository page: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<RepositoryInfo> getRepositoryDetails(GitServiceConfig config, String repositoryId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = GITHUB_API_BASE + "/repos/" + repositoryId;
                String response = makeHttpRequest(url, config.getToken());
                
                RepositoryInfo repository = objectMapper.readValue(response, RepositoryInfo.class);
                logger.debug("Retrieved details for repository: {}", repository.getName());
                return repository;
                
            } catch (Exception e) {
                logger.error("Failed to get repository details from GitHub", e);
                throw new RuntimeException("Failed to get repository details: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Makes an HTTP request to the GitHub API.
     */
    private String makeHttpRequest(String urlString, String token) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // Set request headers
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "token " + token);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readResponse(connection);
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new RuntimeException("Authentication failed. Please check your GitHub token.");
            } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new RuntimeException("Access forbidden. Rate limit exceeded or insufficient permissions.");
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new RuntimeException("Resource not found. Please verify the URL and permissions.");
            } else {
                String errorMessage = readErrorResponse(connection);
                throw new RuntimeException("GitHub API request failed with status " + responseCode + ": " + errorMessage);
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
     * Parses repository information from JSON response.
     */
    private List<RepositoryInfo> parseRepositories(String response) throws IOException {
        return objectMapper.readValue(response, new TypeReference<List<RepositoryInfo>>() {});
    }
}