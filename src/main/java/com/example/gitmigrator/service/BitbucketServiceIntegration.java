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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * Bitbucket implementation of GitServiceIntegration.
 * Provides integration with Bitbucket API 2.0 for repository management.
 */
public class BitbucketServiceIntegration implements GitServiceIntegration {
    
    private static final Logger logger = LoggerFactory.getLogger(BitbucketServiceIntegration.class);
    private static final String BITBUCKET_API_BASE = "https://api.bitbucket.org/2.0";
    private static final String USER_AGENT = "GitMigrator/1.0";
    
    private final ObjectMapper objectMapper;
    
    public BitbucketServiceIntegration() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public CompletableFuture<List<RepositoryInfo>> fetchRepositories(GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = config.getRepositoriesEndpoint();
                if (endpoint == null) {
                    endpoint = BITBUCKET_API_BASE + "/repositories";
                    if (config.getUsername() != null && !config.getUsername().isEmpty()) {
                        endpoint += "/" + config.getUsername();
                    }
                }
                
                List<RepositoryInfo> allRepos = new ArrayList<>();
                String nextUrl = endpoint + "?pagelen=100&sort=-updated_on";
                int maxPages = 10; // Safety limit
                int pageCount = 0;
                
                while (nextUrl != null && pageCount < maxPages) {
                    String response = makeHttpRequest(nextUrl, config.getToken(), config.getUsername());
                    JsonNode responseNode = objectMapper.readTree(response);
                    
                    JsonNode valuesNode = responseNode.get("values");
                    if (valuesNode != null && valuesNode.isArray()) {
                        List<RepositoryInfo> pageRepos = parseRepositories(valuesNode);
                        allRepos.addAll(pageRepos);
                        
                        if (allRepos.size() >= config.getMaxRepositories()) {
                            allRepos = allRepos.subList(0, config.getMaxRepositories());
                            break;
                        }
                    }
                    
                    // Get next page URL
                    JsonNode nextNode = responseNode.get("next");
                    nextUrl = nextNode != null ? nextNode.asText() : null;
                    pageCount++;
                }
                
                logger.info("Fetched {} repositories from Bitbucket", allRepos.size());
                return allRepos;
                
            } catch (Exception e) {
                logger.error("Failed to fetch repositories from Bitbucket", e);
                throw new RuntimeException("Failed to fetch repositories: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<RepositoryInfo>> searchRepositories(String query, GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = BITBUCKET_API_BASE + "/repositories?q=name~\"" + encodedQuery + "\"" + 
                           "&sort=-updated_on&pagelen=100";
                
                String response = makeHttpRequest(url, config.getToken(), config.getUsername());
                JsonNode responseNode = objectMapper.readTree(response);
                
                JsonNode valuesNode = responseNode.get("values");
                List<RepositoryInfo> repositories = new ArrayList<>();
                
                if (valuesNode != null && valuesNode.isArray()) {
                    repositories = parseRepositories(valuesNode);
                }
                
                logger.info("Found {} repositories for query: {}", repositories.size(), query);
                return repositories;
                
            } catch (Exception e) {
                logger.error("Failed to search repositories on Bitbucket", e);
                throw new RuntimeException("Failed to search repositories: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public boolean authenticateService(GitServiceConfig config) {
        try {
            String url = BITBUCKET_API_BASE + "/user";
            String response = makeHttpRequest(url, config.getToken(), config.getUsername());
            
            JsonNode userNode = objectMapper.readTree(response);
            return userNode.has("username");
            
        } catch (Exception e) {
            logger.warn("Bitbucket authentication failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<String> getSupportedServices() {
        return List.of("bitbucket");
    }
    
    @Override
    public boolean isServiceSupported(String serviceName) {
        return "bitbucket".equalsIgnoreCase(serviceName);
    }
    
    @Override
    public GitServiceConfig getDefaultConfig(String serviceName) {
        if (!"bitbucket".equalsIgnoreCase(serviceName)) {
            return null;
        }
        
        GitServiceConfig config = new GitServiceConfig();
        config.setServiceName("bitbucket");
        config.setApiUrl(BITBUCKET_API_BASE);
        config.setMaxRepositories(100);
        return config;
    }
    
    @Override
    public boolean validateConfig(GitServiceConfig config) {
        if (config == null) return false;
        if (!"bitbucket".equalsIgnoreCase(config.getServiceName())) return false;
        if (config.getToken() == null || config.getToken().trim().isEmpty()) return false;
        if (config.getApiUrl() == null || !config.getApiUrl().contains("bitbucket")) return false;
        
        return true;
    }
    
    @Override
    public CompletableFuture<GitUserInfo> getUserInfo(GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = BITBUCKET_API_BASE + "/user";
                String response = makeHttpRequest(url, config.getToken(), config.getUsername());
                
                JsonNode userNode = objectMapper.readTree(response);
                GitUserInfo userInfo = new GitUserInfo();
                
                // Map Bitbucket user fields to GitUserInfo
                if (userNode.has("uuid")) userInfo.setId(Long.valueOf(userNode.get("uuid").asText().hashCode()));
                if (userNode.has("username")) userInfo.setUsername(userNode.get("username").asText());
                if (userNode.has("display_name")) userInfo.setDisplayName(userNode.get("display_name").asText());
                
                JsonNode linksNode = userNode.get("links");
                if (linksNode != null) {
                    JsonNode avatarNode = linksNode.get("avatar");
                    if (avatarNode != null && avatarNode.has("href")) {
                        userInfo.setAvatarUrl(avatarNode.get("href").asText());
                    }
                    
                    JsonNode htmlNode = linksNode.get("html");
                    if (htmlNode != null && htmlNode.has("href")) {
                        userInfo.setProfileUrl(htmlNode.get("href").asText());
                    }
                }
                
                if (userNode.has("location")) userInfo.setLocation(userNode.get("location").asText());
                
                logger.debug("Retrieved user info for: {}", userInfo.getUsername());
                return userInfo;
                
            } catch (Exception e) {
                logger.error("Failed to get user info from Bitbucket", e);
                throw new RuntimeException("Failed to get user info: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<GitOrganization>> getUserOrganizations(GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = BITBUCKET_API_BASE + "/teams?role=member";
                String response = makeHttpRequest(url, config.getToken(), config.getUsername());
                
                JsonNode responseNode = objectMapper.readTree(response);
                JsonNode valuesNode = responseNode.get("values");
                List<GitOrganization> organizations = new ArrayList<>();
                
                if (valuesNode != null && valuesNode.isArray()) {
                    for (JsonNode teamNode : valuesNode) {
                        GitOrganization org = new GitOrganization();
                        
                        if (teamNode.has("uuid")) org.setId(Long.valueOf(teamNode.get("uuid").asText().hashCode()));
                        if (teamNode.has("username")) org.setName(teamNode.get("username").asText());
                        if (teamNode.has("display_name")) org.setDisplayName(teamNode.get("display_name").asText());
                        
                        JsonNode linksNode = teamNode.get("links");
                        if (linksNode != null) {
                            JsonNode avatarNode = linksNode.get("avatar");
                            if (avatarNode != null && avatarNode.has("href")) {
                                org.setAvatarUrl(avatarNode.get("href").asText());
                            }
                            
                            JsonNode htmlNode = linksNode.get("html");
                            if (htmlNode != null && htmlNode.has("href")) {
                                org.setUrl(htmlNode.get("href").asText());
                            }
                        }
                        
                        organizations.add(org);
                    }
                }
                
                logger.debug("Retrieved {} organizations", organizations.size());
                return organizations;
                
            } catch (Exception e) {
                logger.error("Failed to get organizations from Bitbucket", e);
                throw new RuntimeException("Failed to get organizations: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<GitConnectionTestResult> testConnection(GitServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                String url = BITBUCKET_API_BASE + "/user";
                String response = makeHttpRequest(url, config.getToken(), config.getUsername());
                
                long responseTime = System.currentTimeMillis() - startTime;
                
                JsonNode userNode = objectMapper.readTree(response);
                if (userNode.has("username")) {
                    GitConnectionTestResult result = new GitConnectionTestResult(
                        true, "Connection successful", responseTime
                    );
                    result.setServiceName("Bitbucket");
                    result.setApiVersion("2.0");
                    return result;
                } else {
                    return new GitConnectionTestResult(false, "Invalid response from Bitbucket API");
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
                // Bitbucket doesn't have a dedicated rate limit endpoint
                // We'll return default rate limit info based on Bitbucket's documented limits
                int limit = 1000; // Bitbucket's default hourly limit
                int remaining = limit; // We can't determine actual usage without making requests
                LocalDateTime resetTime = LocalDateTime.now().plusHours(1);
                
                GitRateLimitInfo rateLimitInfo = new GitRateLimitInfo(limit, remaining, resetTime);
                rateLimitInfo.setResource("api");
                
                return rateLimitInfo;
                
            } catch (Exception e) {
                logger.error("Failed to get rate limit info from Bitbucket", e);
                // Return default rate limit info
                GitRateLimitInfo defaultInfo = new GitRateLimitInfo(1000, 1000, LocalDateTime.now().plusHours(1));
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
                    endpoint = BITBUCKET_API_BASE + "/repositories";
                    if (config.getUsername() != null && !config.getUsername().isEmpty()) {
                        endpoint += "/" + config.getUsername();
                    }
                }
                
                String url = endpoint + "?pagelen=" + perPage + "&page=" + page + "&sort=-updated_on";
                
                String response = makeHttpRequest(url, config.getToken(), config.getUsername());
                JsonNode responseNode = objectMapper.readTree(response);
                
                List<RepositoryInfo> repositories = new ArrayList<>();
                JsonNode valuesNode = responseNode.get("values");
                if (valuesNode != null && valuesNode.isArray()) {
                    repositories = parseRepositories(valuesNode);
                }
                
                // Bitbucket provides pagination info
                int totalCount = responseNode.has("size") ? responseNode.get("size").asInt() : repositories.size();
                int totalPages = (int) Math.ceil((double) totalCount / perPage);
                
                GitRepositoryPage repositoryPage = new GitRepositoryPage(repositories, page, totalPages, totalCount);
                
                logger.debug("Fetched page {} with {} repositories", page, repositories.size());
                return repositoryPage;
                
            } catch (Exception e) {
                logger.error("Failed to fetch repository page from Bitbucket", e);
                throw new RuntimeException("Failed to fetch repository page: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<RepositoryInfo> getRepositoryDetails(GitServiceConfig config, String repositoryId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = BITBUCKET_API_BASE + "/repositories/" + repositoryId;
                String response = makeHttpRequest(url, config.getToken(), config.getUsername());
                
                JsonNode repoNode = objectMapper.readTree(response);
                RepositoryInfo repository = parseRepositoryFromNode(repoNode);
                
                logger.debug("Retrieved details for repository: {}", repository.getName());
                return repository;
                
            } catch (Exception e) {
                logger.error("Failed to get repository details from Bitbucket", e);
                throw new RuntimeException("Failed to get repository details: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Makes an HTTP request to the Bitbucket API.
     */
    private String makeHttpRequest(String urlString, String token, String username) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // Set request headers
            connection.setRequestMethod("GET");
            
            // Bitbucket uses Basic Auth with username:app_password
            if (username != null && !username.isEmpty()) {
                String auth = username + ":" + token;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
            } else {
                // Fallback to Bearer token if no username provided
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }
            
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readResponse(connection);
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new RuntimeException("Authentication failed. Please check your Bitbucket credentials.");
            } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new RuntimeException("Access forbidden. Rate limit exceeded or insufficient permissions.");
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new RuntimeException("Resource not found. Please verify the URL and permissions.");
            } else {
                String errorMessage = readErrorResponse(connection);
                throw new RuntimeException("Bitbucket API request failed with status " + responseCode + ": " + errorMessage);
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
    private List<RepositoryInfo> parseRepositories(JsonNode valuesNode) {
        List<RepositoryInfo> repositories = new ArrayList<>();
        
        for (JsonNode repoNode : valuesNode) {
            repositories.add(parseRepositoryFromNode(repoNode));
        }
        
        return repositories;
    }
    
    /**
     * Parses a single repository from a Bitbucket repository JSON node.
     */
    private RepositoryInfo parseRepositoryFromNode(JsonNode repoNode) {
        RepositoryInfo repo = new RepositoryInfo();
        
        if (repoNode.has("uuid")) repo.setId(repoNode.get("uuid").asText());
        if (repoNode.has("name")) repo.setName(repoNode.get("name").asText());
        if (repoNode.has("full_name")) repo.setFullName(repoNode.get("full_name").asText());
        if (repoNode.has("description")) repo.setDescription(repoNode.get("description").asText());
        
        // Get clone URLs
        JsonNode linksNode = repoNode.get("links");
        if (linksNode != null) {
            JsonNode cloneNode = linksNode.get("clone");
            if (cloneNode != null && cloneNode.isArray()) {
                for (JsonNode cloneUrlNode : cloneNode) {
                    if ("https".equals(cloneUrlNode.get("name").asText())) {
                        repo.setUrl(cloneUrlNode.get("href").asText());
                        break;
                    }
                }
            }
        }
        
        // Get main branch
        JsonNode mainBranchNode = repoNode.get("mainbranch");
        if (mainBranchNode != null && mainBranchNode.has("name")) {
            repo.setDefaultBranch(mainBranchNode.get("name").asText());
        }
        
        return repo;
    }
}