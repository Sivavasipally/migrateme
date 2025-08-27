package com.example.gitmigrator.service;

import com.example.gitmigrator.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub implementation of repository discovery service.
 * Integrates with GitHub REST API v3 for bulk repository discovery.
 */
public class GitHubRepositoryDiscoveryService implements RepositoryDiscoveryService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubRepositoryDiscoveryService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper() {{
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        registerModule(new JavaTimeModule());
    }};
    private static final int DEFAULT_PER_PAGE = 100;
    private static final int MAX_PER_PAGE = 100;
    private static final int MAX_PAGES = 10; // Limit to prevent excessive API calls
    private static final Pattern LINK_HEADER_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"([^\"]+)\"");
    
    private final HttpClient httpClient;
    private final SecureAuthenticationHelper authHelper;
    
    public GitHubRepositoryDiscoveryService() {
        this.authHelper = new SecureAuthenticationHelper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    public GitHubRepositoryDiscoveryService(SecureAuthenticationHelper authHelper) {
        this.authHelper = authHelper != null ? authHelper : new SecureAuthenticationHelper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    @Override
    public CompletableFuture<Boolean> testConnection(GitProviderConnection connection) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = connection.getApiUrl() + "/user";
                HttpRequest request = buildAuthenticatedRequest(connection, url);
                
                HttpResponse<String> response = httpClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                boolean success = response.statusCode() == 200;
                if (success) {
                    logger.info("GitHub connection test successful for: {}", connection.getBaseUrl());
                } else {
                    logger.warn("GitHub connection test failed. Status: {}, Body: {}", 
                            response.statusCode(), response.body());
                }
                return success;
                
            } catch (Exception e) {
                logger.error("GitHub connection test failed", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<List<RepositoryMetadata>> discoverAllRepositories(GitProviderConnection connection) {
        return CompletableFuture.supplyAsync(() -> {
            List<RepositoryMetadata> repositories = new ArrayList<>();
            
            try {
                // Fetch user's repositories (including private ones)
                repositories.addAll(fetchRepositories(connection, "/user/repos"));
                
                logger.info("Discovered {} repositories from GitHub", repositories.size());
                
            } catch (Exception e) {
                logger.error("Error discovering GitHub repositories", e);
                throw new RuntimeException("Failed to discover GitHub repositories", e);
            }
            
            return repositories;
        });
    }
    
    @Override
    public CompletableFuture<List<RepositoryMetadata>> discoverOrganizationRepositories(
            GitProviderConnection connection, String organizationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = "/orgs/" + organizationId + "/repos";
                return fetchRepositories(connection, endpoint);
            } catch (Exception e) {
                logger.error("Error discovering repositories for GitHub organization: {}", organizationId, e);
                throw new RuntimeException("Failed to discover organization repositories", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<RepositoryMetadata>> searchRepositories(
            GitProviderConnection connection, String query) {
        return CompletableFuture.supplyAsync(() -> {
            List<RepositoryMetadata> repositories = new ArrayList<>();
            
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = connection.getApiUrl() + "/search/repositories?q=" + encodedQuery + 
                           "&per_page=" + DEFAULT_PER_PAGE;
                
                HttpRequest request = buildAuthenticatedRequest(connection, url);
                HttpResponse<String> response = httpClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonNode searchResult = objectMapper.readTree(response.body());
                    JsonNode itemsArray = searchResult.get("items");
                    
                    if (itemsArray != null) {
                        for (JsonNode repoNode : itemsArray) {
                            RepositoryMetadata repo = parseRepository(repoNode);
                            if (repo != null) {
                                repositories.add(repo);
                            }
                        }
                    }
                } else {
                    handleApiError(response, "search repositories");
                }
                
            } catch (Exception e) {
                logger.error("Error searching GitHub repositories", e);
                throw new RuntimeException("Failed to search GitHub repositories", e);
            }
            
            return repositories;
        });
    }
    
    @Override
    public CompletableFuture<List<RepositoryMetadata>> discoverRepositoriesWithFilter(
            GitProviderConnection connection, RepositoryFilter filter) {
        return discoverAllRepositories(connection)
                .thenApply(repositories -> {
                    if (filter == null || !filter.hasActiveCriteria()) {
                        return repositories;
                    }
                    
                    return repositories.stream()
                            .filter(filter::matches)
                            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                });
    }
    
    @Override
    public CompletableFuture<RepositoryDiscoveryPage> discoverRepositoriesPage(
            GitProviderConnection connection, int page, int perPage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int actualPerPage = Math.min(perPage, MAX_PER_PAGE);
                String url = connection.getApiUrl() + "/user/repos" +
                           "?per_page=" + actualPerPage + "&page=" + page;
                
                HttpRequest request = buildAuthenticatedRequest(connection, url);
                HttpResponse<String> response = httpClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    handleApiError(response, "fetch repositories page");
                    return RepositoryDiscoveryPage.builder()
                            .repositories(new ArrayList<>())
                            .currentPage(page)
                            .perPage(actualPerPage)
                            .build();
                }
                
                JsonNode reposArray = objectMapper.readTree(response.body());
                List<RepositoryMetadata> repositories = new ArrayList<>();
                
                if (reposArray.isArray()) {
                    for (JsonNode repoNode : reposArray) {
                        RepositoryMetadata repo = parseRepository(repoNode);
                        if (repo != null) {
                            repositories.add(repo);
                        }
                    }
                }
                
                // Parse pagination info from Link header
                PaginationInfo paginationInfo = parseLinkHeader(response.headers().firstValue("Link").orElse(""));
                
                return RepositoryDiscoveryPage.builder()
                        .repositories(repositories)
                        .currentPage(page)
                        .perPage(actualPerPage)
                        .hasNextPage(paginationInfo.hasNext)
                        .hasPreviousPage(page > 1)
                        .nextPageUrl(paginationInfo.nextUrl)
                        .previousPageUrl(paginationInfo.prevUrl)
                        .autoCalculatePagination()
                        .build();
                
            } catch (Exception e) {
                logger.error("Error fetching GitHub repositories page", e);
                throw new RuntimeException("Failed to fetch repositories page", e);
            }
        });
    }
    
    @Override
    public GitProviderType getSupportedProvider() {
        return GitProviderType.GITHUB;
    }
    
    @Override
    public boolean supportsConnection(GitProviderConnection connection) {
        return connection != null && 
               connection.getProviderType() == GitProviderType.GITHUB &&
               connection.isValid();
    }
    
    @Override
    public int getMaxRepositoriesPerCall() {
        return MAX_PER_PAGE;
    }
    
    @Override
    public boolean supportsAdvancedFiltering() {
        return true;
    }
    
    private List<RepositoryMetadata> fetchRepositories(GitProviderConnection connection, String endpoint) 
            throws IOException, InterruptedException {
        List<RepositoryMetadata> repositories = new ArrayList<>();
        
        for (int page = 1; page <= MAX_PAGES; page++) {
            String url = connection.getApiUrl() + endpoint + 
                        "?per_page=" + DEFAULT_PER_PAGE + "&page=" + page;
            
            HttpRequest request = buildAuthenticatedRequest(connection, url);
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                handleApiError(response, "fetch repositories");
                break;
            }
            
            JsonNode reposArray = objectMapper.readTree(response.body());
            if (!reposArray.isArray() || reposArray.size() == 0) {
                break; // No more repositories
            }
            
            for (JsonNode repoNode : reposArray) {
                RepositoryMetadata repo = parseRepository(repoNode);
                if (repo != null) {
                    repositories.add(repo);
                }
            }
            
            // If we got less than the requested amount, we've reached the end
            if (reposArray.size() < DEFAULT_PER_PAGE) {
                break;
            }
        }
        
        return repositories;
    }
    
    private RepositoryMetadata parseRepository(JsonNode repoNode) {
        try {
            JsonNode ownerNode = repoNode.get("owner");
            
            return RepositoryMetadata.builder()
                    .id(repoNode.get("id").asText())
                    .name(repoNode.get("name").asText())
                    .fullName(repoNode.get("full_name").asText())
                    .description(getStringValue(repoNode, "description"))
                    .cloneUrl(repoNode.get("clone_url").asText())
                    .webUrl(repoNode.get("html_url").asText())
                    .defaultBranch(getStringValue(repoNode, "default_branch", "main"))
                    .isPrivate(repoNode.get("private").asBoolean())
                    .isFork(repoNode.get("fork").asBoolean())
                    .isArchived(getBooleanValue(repoNode, "archived", false))
                    .language(getStringValue(repoNode, "language"))
                    .size(getLongValue(repoNode, "size", 0))
                    .starCount(getIntValue(repoNode, "stargazers_count", 0))
                    .forkCount(getIntValue(repoNode, "forks_count", 0))
                    .createdAt(parseDateTime(getStringValue(repoNode, "created_at")))
                    .updatedAt(parseDateTime(getStringValue(repoNode, "updated_at")))
                    .lastActivityAt(parseDateTime(getStringValue(repoNode, "pushed_at")))
                    .ownerId(ownerNode.get("id").asText())
                    .ownerName(ownerNode.get("login").asText())
                    .ownerType(ownerNode.get("type").asText().toLowerCase())
                    .providerType(GitProviderType.GITHUB)
                    .build();
        } catch (Exception e) {
            logger.error("Error parsing GitHub repository", e);
            return null;
        }
    }
    
    private String getStringValue(JsonNode node, String fieldName) {
        return getStringValue(node, fieldName, null);
    }
    
    private String getStringValue(JsonNode node, String fieldName, String defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asText();
    }
    
    private boolean getBooleanValue(JsonNode node, String fieldName, boolean defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asBoolean();
    }
    
    private int getIntValue(JsonNode node, String fieldName, int defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asInt();
    }
    
    private long getLongValue(JsonNode node, String fieldName, long defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asLong();
    }
    
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            logger.debug("Failed to parse datetime: {}", dateTimeStr);
            return null;
        }
    }
    
    private HttpRequest buildAuthenticatedRequest(GitProviderConnection connection, String url) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "GitMigrator/1.0");
        
        // Add authentication
        String authHeader = authHelper.createAuthenticationHeader(connection);
        requestBuilder.header("Authorization", authHeader);
        
        return requestBuilder.GET().build();
    }
    
    private void handleApiError(HttpResponse<String> response, String operation) {
        String errorMessage = "GitHub API error during " + operation + 
                            ". Status: " + response.statusCode();
        
        try {
            JsonNode errorBody = objectMapper.readTree(response.body());
            if (errorBody.has("message")) {
                errorMessage += ", Message: " + errorBody.get("message").asText();
            }
        } catch (Exception e) {
            // Ignore JSON parsing errors
        }
        
        logger.error(errorMessage);
        
        // Handle specific error codes
        switch (response.statusCode()) {
            case 401:
                throw new RuntimeException("Authentication failed - check your credentials");
            case 403:
                if (response.body().contains("rate limit")) {
                    throw new RuntimeException("GitHub API rate limit exceeded - please wait before retrying");
                } else {
                    throw new RuntimeException("Access forbidden - check your permissions");
                }
            case 404:
                throw new RuntimeException("Resource not found - check your URL and permissions");
            default:
                throw new RuntimeException(errorMessage);
        }
    }
    
    private PaginationInfo parseLinkHeader(String linkHeader) {
        PaginationInfo info = new PaginationInfo();
        
        if (linkHeader == null || linkHeader.isEmpty()) {
            return info;
        }
        
        String[] links = linkHeader.split(",");
        for (String link : links) {
            Matcher matcher = LINK_HEADER_PATTERN.matcher(link.trim());
            if (matcher.matches()) {
                String url = matcher.group(1);
                String rel = matcher.group(2);
                
                switch (rel) {
                    case "next":
                        info.nextUrl = url;
                        info.hasNext = true;
                        break;
                    case "prev":
                        info.prevUrl = url;
                        info.hasPrev = true;
                        break;
                    case "last":
                        info.lastUrl = url;
                        break;
                    case "first":
                        info.firstUrl = url;
                        break;
                }
            }
        }
        
        return info;
    }
    
    private static class PaginationInfo {
        boolean hasNext = false;
        boolean hasPrev = false;
        String nextUrl;
        String prevUrl;
        String firstUrl;
        String lastUrl;
    }
}