package com.example.gitmigrator.service;

import com.example.gitmigrator.model.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * GitLab implementation of GitServiceIntegration.
 * Provides integration with GitLab API v4 for repository management.
 */
public class GitLabServiceIntegration implements GitServiceIntegration {
    
    private static final Logger logger = LoggerFactory.getLogger(GitLabServiceIntegration.class);
    private static final String GITLAB_API_BASE = "https://gitlab.com/api/v4";
    private static final String USER_AGENT = "GitMigrator/1.0";
    
    private final ObjectMapper objectMapper;
    
    public GitLabServiceIntegration() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public CompletableFuture<List<RepositoryInfo>> fetchRepositories(GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = config.getRepositoriesEndpoint();
                if (endpoint == null) {
                    endpoint = config.getApiUrl() + "/projects?membership=true";
                }
                
                List<RepositoryInfo> allRepos = new ArrayList<>();
                int page = 1;
                int maxPages = 10; // Safety limit
                
                while (page <= maxPages) {
                    String url = endpoint + (endpoint.contains("?") ? "&" : "?") + 
                                "page=" + page + "&per_page=100&order_by=last_activity_at&sort=desc";
                    
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
                
                logger.info("Fetched {} repositories from GitLab", allRepos.size());
                return allRepos;
                
            } catch (Exception e) {
                logger.error("Failed to fetch repositories from GitLab", e);
                throw new RuntimeException("Failed to fetch repositories: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<RepositoryInfo>> searchRepositories(String query, GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = config.getApiUrl() + "/projects?search=" + encodedQuery + 
                           "&order_by=last_activity_at&sort=desc&per_page=100";
                
                String response = makeHttpRequest(url, config.getToken());
                List<RepositoryInfo> repositories = parseRepositories(response);
                
                logger.info("Found {} repositories for query: {}", repositories.size(), query);
                return repositories;
                
            } catch (Exception e) {
                logger.error("Failed to search repositories on GitLab", e);
                throw new RuntimeException("Failed to search repositories: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public boolean authenticateService(GitServiceConfig config) {
        try {
            String url = config.getApiUrl() + "/user";
            String response = makeHttpRequest(url, config.getToken());
            
            JsonNode userNode = objectMapper.readTree(response);
            return userNode.has("username");
            
        } catch (Exception e) {
            logger.warn("GitLab authentication failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<String> getSupportedServices() {
        return List.of("gitlab");
    }
    
    @Override
    public boolean isServiceSupported(String serviceName) {
        return "gitlab".equalsIgnoreCase(serviceName);
    }
    
    @Override
    public GitServiceConfig getDefaultConfig(String serviceName) {
        if (!"gitlab".equalsIgnoreCase(serviceName)) {
            return null;
        }
        
        GitServiceConfig config = new GitServiceConfig();
        config.setServiceName("gitlab");
        config.setApiUrl(GITLAB_API_BASE);
        config.setMaxRepositories(100);
        return config;
    }
    
    @Override
    public boolean validateConfig(GitServiceConfig config) {
        if (config == null) return false;
        if (!"gitlab".equalsIgnoreCase(config.getServiceName())) return false;
        if (config.getToken() == null || config.getToken().trim().isEmpty()) return false;
        if (config.getApiUrl() == null || !config.getApiUrl().contains("gitlab")) return false;
        
        return true;
    }
    
    @Override
    public CompletableFuture<GitUserInfo> getUserInfo(GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = config.getApiUrl() + "/user";
                String response = makeHttpRequest(url, config.getToken());
                
                JsonNode userNode = objectMapper.readTree(response);
                GitUserInfo userInfo = new GitUserInfo();
                
                // Map GitLab user fields to GitUserInfo
                if (userNode.has("id")) userInfo.setId(userNode.get("id").asLong());
                if (userNode.has("username")) userInfo.setUsername(userNode.get("username").asText());
                if (userNode.has("name")) userInfo.setDisplayName(userNode.get("name").asText());
                if (userNode.has("email")) userInfo.setEmail(userNode.get("email").asText());
                if (userNode.has("avatar_url")) userInfo.setAvatarUrl(userNode.get("avatar_url").asText());
                if (userNode.has("web_url")) userInfo.setProfileUrl(userNode.get("web_url").asText());
                if (userNode.has("organization")) userInfo.setCompany(userNode.get("organization").asText());
                if (userNode.has("location")) userInfo.setLocation(userNode.get("location").asText());
                
                logger.debug("Retrieved user info for: {}", userInfo.getUsername());
                return userInfo;
                
            } catch (Exception e) {
                logger.error("Failed to get user info from GitLab", e);
                throw new RuntimeException("Failed to get user info: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<GitOrganization>> getUserOrganizations(GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = config.getApiUrl() + "/groups?membership=true";
                String response = makeHttpRequest(url, config.getToken());
                
                JsonNode groupsNode = objectMapper.readTree(response);
                List<GitOrganization> organizations = new ArrayList<>();
                
                if (groupsNode.isArray()) {
                    for (JsonNode groupNode : groupsNode) {
                        GitOrganization org = new GitOrganization();
                        
                        if (groupNode.has("id")) org.setId(groupNode.get("id").asLong());
                        if (groupNode.has("path")) org.setName(groupNode.get("path").asText());
                        if (groupNode.has("name")) org.setDisplayName(groupNode.get("name").asText());
                        if (groupNode.has("description")) org.setDescription(groupNode.get("description").asText());
                        if (groupNode.has("avatar_url")) org.setAvatarUrl(groupNode.get("avatar_url").asText());
                        if (groupNode.has("web_url")) org.setUrl(groupNode.get("web_url").asText());
                        
                        organizations.add(org);
                    }
                }
                
                logger.debug("Retrieved {} organizations", organizations.size());
                return organizations;
                
            } catch (Exception e) {
                logger.error("Failed to get organizations from GitLab", e);
                throw new RuntimeException("Failed to get organizations: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<GitConnectionTestResult> testConnection(GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                String url = config.getApiUrl() + "/version";
                String response = makeHttpRequest(url, config.getToken());
                
                long responseTime = System.currentTimeMillis() - startTime;
                
                JsonNode versionNode = objectMapper.readTree(response);
                if (versionNode.has("version")) {
                    GitConnectionTestResult result = new GitConnectionTestResult(
                        true, "Connection successful", responseTime
                    );
                    result.setServiceName("GitLab");
                    result.setApiVersion("v4");
                    return result;
                } else {
                    return new GitConnectionTestResult(false, "Invalid response from GitLab API");
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
                // GitLab doesn't have a dedicated rate limit endpoint like GitHub
                // We'll make a simple API call and check the response headers
                String url = config.getApiUrl() + "/user";
                
                URL apiUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
                
                try {
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Authorization", "Bearer " + config.getToken());
                    connection.setRequestProperty("User-Agent", USER_AGENT);
                    
                    connection.getResponseCode();
                    
                    // GitLab rate limit headers (if present)
                    String rateLimitHeader = connection.getHeaderField("RateLimit-Limit");
                    String rateLimitRemainingHeader = connection.getHeaderField("RateLimit-Remaining");
                    String rateLimitResetHeader = connection.getHeaderField("RateLimit-Reset");
                    
                    int limit = rateLimitHeader != null ? Integer.parseInt(rateLimitHeader) : 2000; // Default GitLab limit
                    int remaining = rateLimitRemainingHeader != null ? Integer.parseInt(rateLimitRemainingHeader) : limit;
                    
                    LocalDateTime resetTime = LocalDateTime.now().plusHours(1); // Default 1 hour reset
                    if (rateLimitResetHeader != null) {
                        try {
                            resetTime = LocalDateTime.parse(rateLimitResetHeader, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } catch (Exception e) {
                            // Use default if parsing fails
                        }
                    }
                    
                    GitRateLimitInfo rateLimitInfo = new GitRateLimitInfo(limit, remaining, resetTime);
                    rateLimitInfo.setResource("api");
                    
                    return rateLimitInfo;
                    
                } finally {
                    connection.disconnect();
                }
                
            } catch (Exception e) {
                logger.error("Failed to get rate limit info from GitLab", e);
                // Return default rate limit info
                GitRateLimitInfo defaultInfo = new GitRateLimitInfo(2000, 2000, LocalDateTime.now().plusHours(1));
                defaultInfo.setResource("api");
                return defaultInfo;
            }
        });
    }
    
    @Override
    public CompletableFuture<GitRepositoryPage> fetchRepositoriesPage(GitServiceConfig config, int page, int perPage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = config.getRepositoriesEndpoint();
                if (endpoint == null) {
                    endpoint = config.getApiUrl() + "/projects?membership=true";
                }
                
                String url = endpoint + (endpoint.contains("?") ? "&" : "?") + 
                           "page=" + page + "&per_page=" + perPage + "&order_by=last_activity_at&sort=desc";
                
                String response = makeHttpRequest(url, config.getToken());
                List<RepositoryInfo> repositories = parseRepositories(response);
                
                // GitLab doesn't provide total count easily, so we estimate
                int totalCount = repositories.size() < perPage ? 
                    (page - 1) * perPage + repositories.size() : 
                    page * perPage + 1; // Estimate there's at least one more page
                
                int totalPages = (int) Math.ceil((double) totalCount / perPage);
                
                GitRepositoryPage repositoryPage = new GitRepositoryPage(repositories, page, totalPages, totalCount);
                
                logger.debug("Fetched page {} with {} repositories", page, repositories.size());
                return repositoryPage;
                
            } catch (Exception e) {
                logger.error("Failed to fetch repository page from GitLab", e);
                throw new RuntimeException("Failed to fetch repository page: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<RepositoryInfo> getRepositoryDetails(GitServiceConfig config, String repositoryId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = config.getApiUrl() + "/projects/" + URLEncoder.encode(repositoryId, StandardCharsets.UTF_8);
                String response = makeHttpRequest(url, config.getToken());
                
                JsonNode projectNode = objectMapper.readTree(response);
                RepositoryInfo repository = parseRepositoryFromProject(projectNode);
                
                logger.debug("Retrieved details for repository: {}", repository.getName());
                return repository;
                
            } catch (Exception e) {
                logger.error("Failed to get repository details from GitLab", e);
                throw new RuntimeException("Failed to get repository details: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Makes an HTTP request to the GitLab API.
     */
    private String makeHttpRequest(String urlString, String token) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // Set request headers
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readResponse(connection);
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new RuntimeException("Authentication failed. Please check your GitLab token.");
            } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new RuntimeException("Access forbidden. Rate limit exceeded or insufficient permissions.");
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new RuntimeException("Resource not found. Please verify the URL and permissions.");
            } else {
                String errorMessage = readErrorResponse(connection);
                throw new RuntimeException("GitLab API request failed with status " + responseCode + ": " + errorMessage);
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
        JsonNode projectsNode = objectMapper.readTree(response);
        List<RepositoryInfo> repositories = new ArrayList<>();
        
        if (projectsNode.isArray()) {
            for (JsonNode projectNode : projectsNode) {
                repositories.add(parseRepositoryFromProject(projectNode));
            }
        }
        
        return repositories;
    }
    
    /**
     * Parses a single repository from a GitLab project JSON node.
     */
    private RepositoryInfo parseRepositoryFromProject(JsonNode projectNode) {
        RepositoryInfo repo = new RepositoryInfo();
        
        if (projectNode.has("id")) repo.setId(String.valueOf(projectNode.get("id").asLong()));
        if (projectNode.has("name")) repo.setName(projectNode.get("name").asText());
        if (projectNode.has("http_url_to_repo")) repo.setUrl(projectNode.get("http_url_to_repo").asText());
        if (projectNode.has("description")) repo.setDescription(projectNode.get("description").asText());
        if (projectNode.has("default_branch")) repo.setDefaultBranch(projectNode.get("default_branch").asText());
        
        // Set additional GitLab-specific fields
        if (projectNode.has("path_with_namespace")) {
            repo.setFullName(projectNode.get("path_with_namespace").asText());
        }
        
        return repo;
    }
}