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

/**
 * Bitbucket implementation of repository discovery service.
 * Integrates with Bitbucket REST API 2.0 for bulk repository discovery.
 * Supports both Bitbucket Cloud and Bitbucket Server instances.
 * Handles Bitbucket-specific concepts (workspaces, projects, repository slugs).
 */
public class BitbucketRepositoryDiscoveryService implements RepositoryDiscoveryService {
    
    private static final Logger logger = LoggerFactory.getLogger(BitbucketRepositoryDiscoveryService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper() {{
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        registerModule(new JavaTimeModule());
    }};
    private static final int DEFAULT_PAGELEN = 50; // Bitbucket uses 'pagelen' instead of 'per_page'
    private static final int MAX_PAGELEN = 100;
    private static final int MAX_PAGES = 10;
    
    private final HttpClient httpClient;
    private final SecureAuthenticationHelper authHelper;
    
    public BitbucketRepositoryDiscoveryService() {
        this.authHelper = new SecureAuthenticationHelper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    public BitbucketRepositoryDiscoveryService(SecureAuthenticationHelper authHelper) {
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
                    logger.info("Bitbucket connection test successful for: {}", connection.getBaseUrl());
                } else {
                    logger.warn("Bitbucket connection test failed. Status: {}, Body: {}", 
                            response.statusCode(), response.body());
                }
                return success;
                
            } catch (Exception e) {
                logger.error("Bitbucket connection test failed", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<List<RepositoryMetadata>> discoverAllRepositories(GitProviderConnection connection) {
        return CompletableFuture.supplyAsync(() -> {
            List<RepositoryMetadata> repositories = new ArrayList<>();
            
            try {
                // First, get the current user to determine their username
                String currentUser = getCurrentUsername(connection);
                if (currentUser != null) {
                    // Fetch user's repositories
                    repositories.addAll(fetchRepositories(connection, "/repositories/" + currentUser));
                }
                
                logger.info("Discovered {} repositories from Bitbucket", repositories.size());
                
            } catch (Exception e) {
                logger.error("Error discovering Bitbucket repositories", e);
                throw new RuntimeException("Failed to discover Bitbucket repositories", e);
            }
            
            return repositories;
        });
    }
    
    @Override
    public CompletableFuture<List<RepositoryMetadata>> discoverOrganizationRepositories(
            GitProviderConnection connection, String organizationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // In Bitbucket, organizations are called "workspaces"
                String endpoint = "/repositories/" + organizationId;
                return fetchRepositories(connection, endpoint);
            } catch (Exception e) {
                logger.error("Error discovering repositories for Bitbucket workspace: {}", organizationId, e);
                throw new RuntimeException("Failed to discover workspace repositories", e);
            }
        });
    }    

    @Override
    public CompletableFuture<List<RepositoryMetadata>> searchRepositories(
            GitProviderConnection connection, String query) {
        return CompletableFuture.supplyAsync(() -> {
            List<RepositoryMetadata> repositories = new ArrayList<>();
            
            try {
                String encodedQuery = URLEncoder.encode("name~\"" + query + "\"", StandardCharsets.UTF_8);
                String url = connection.getApiUrl() + "/repositories?q=" + encodedQuery + 
                           "&pagelen=" + DEFAULT_PAGELEN;
                
                HttpRequest request = buildAuthenticatedRequest(connection, url);
                HttpResponse<String> response = httpClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonNode searchResult = objectMapper.readTree(response.body());
                    JsonNode valuesArray = searchResult.get("values");
                    
                    if (valuesArray != null && valuesArray.isArray()) {
                        for (JsonNode repoNode : valuesArray) {
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
                logger.error("Error searching Bitbucket repositories", e);
                throw new RuntimeException("Failed to search Bitbucket repositories", e);
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
                int actualPagelen = Math.min(perPage, MAX_PAGELEN);
                
                // Get current user for the endpoint
                String currentUser = getCurrentUsername(connection);
                if (currentUser == null) {
                    return RepositoryDiscoveryPage.builder()
                            .repositories(new ArrayList<>())
                            .currentPage(page)
                            .perPage(actualPagelen)
                            .build();
                }
                
                String url = connection.getApiUrl() + "/repositories/" + currentUser +
                           "?pagelen=" + actualPagelen + "&page=" + page;
                
                HttpRequest request = buildAuthenticatedRequest(connection, url);
                HttpResponse<String> response = httpClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    handleApiError(response, "fetch repositories page");
                    return RepositoryDiscoveryPage.builder()
                            .repositories(new ArrayList<>())
                            .currentPage(page)
                            .perPage(actualPagelen)
                            .build();
                }
                
                JsonNode responseBody = objectMapper.readTree(response.body());
                JsonNode valuesArray = responseBody.get("values");
                List<RepositoryMetadata> repositories = new ArrayList<>();
                
                if (valuesArray != null && valuesArray.isArray()) {
                    for (JsonNode repoNode : valuesArray) {
                        RepositoryMetadata repo = parseRepository(repoNode);
                        if (repo != null) {
                            repositories.add(repo);
                        }
                    }
                }
                
                // Parse pagination info from response body
                PaginationInfo paginationInfo = parseBitbucketPagination(responseBody, page);
                
                return RepositoryDiscoveryPage.builder()
                        .repositories(repositories)
                        .currentPage(page)
                        .perPage(actualPagelen)
                        .totalRepositories(paginationInfo.totalSize)
                        .hasNextPage(paginationInfo.hasNext)
                        .hasPreviousPage(page > 1)
                        .nextPageUrl(paginationInfo.nextUrl)
                        .previousPageUrl(paginationInfo.prevUrl)
                        .autoCalculatePagination()
                        .build();
                
            } catch (Exception e) {
                logger.error("Error fetching Bitbucket repositories page", e);
                throw new RuntimeException("Failed to fetch repositories page", e);
            }
        });
    }
    
    @Override
    public GitProviderType getSupportedProvider() {
        return GitProviderType.BITBUCKET;
    }
    
    @Override
    public boolean supportsConnection(GitProviderConnection connection) {
        return connection != null && 
               connection.getProviderType() == GitProviderType.BITBUCKET &&
               connection.isValid();
    }
    
    @Override
    public int getMaxRepositoriesPerCall() {
        return MAX_PAGELEN;
    }
    
    @Override
    public boolean supportsAdvancedFiltering() {
        return true;
    } 
   
    /**
     * Gets the current authenticated user's username.
     * This is needed for constructing repository endpoints in Bitbucket.
     */
    private String getCurrentUsername(GitProviderConnection connection) {
        try {
            String url = connection.getApiUrl() + "/user";
            HttpRequest request = buildAuthenticatedRequest(connection, url);
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode userNode = objectMapper.readTree(response.body());
                return userNode.get("username").asText();
            } else {
                logger.warn("Failed to get current user. Status: {}", response.statusCode());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error getting current user", e);
            return null;
        }
    }
    
    private List<RepositoryMetadata> fetchRepositories(GitProviderConnection connection, String endpoint) 
            throws IOException, InterruptedException {
        List<RepositoryMetadata> repositories = new ArrayList<>();
        String nextUrl = connection.getApiUrl() + endpoint + "?pagelen=" + DEFAULT_PAGELEN;
        
        for (int page = 1; page <= MAX_PAGES && nextUrl != null; page++) {
            HttpRequest request = buildAuthenticatedRequest(connection, nextUrl);
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                handleApiError(response, "fetch repositories");
                break;
            }
            
            JsonNode responseBody = objectMapper.readTree(response.body());
            JsonNode valuesArray = responseBody.get("values");
            
            if (valuesArray == null || !valuesArray.isArray() || valuesArray.size() == 0) {
                break; // No more repositories
            }
            
            for (JsonNode repoNode : valuesArray) {
                RepositoryMetadata repo = parseRepository(repoNode);
                if (repo != null) {
                    repositories.add(repo);
                }
            }
            
            // Get next page URL from response
            nextUrl = getStringValue(responseBody, "next");
        }
        
        return repositories;
    }
    
    private RepositoryMetadata parseRepository(JsonNode repoNode) {
        try {
            JsonNode ownerNode = repoNode.get("owner");
            JsonNode projectNode = repoNode.get("project");
            JsonNode linksNode = repoNode.get("links");
            
            // Extract clone URLs
            String cloneUrl = null;
            String webUrl = null;
            
            if (linksNode != null) {
                JsonNode cloneLinks = linksNode.get("clone");
                if (cloneLinks != null && cloneLinks.isArray()) {
                    for (JsonNode cloneLink : cloneLinks) {
                        String name = getStringValue(cloneLink, "name");
                        if ("https".equals(name)) {
                            cloneUrl = getStringValue(cloneLink, "href");
                            break;
                        }
                    }
                }
                
                JsonNode htmlLink = linksNode.get("html");
                if (htmlLink != null) {
                    webUrl = getStringValue(htmlLink, "href");
                }
            }
            
            // Handle Bitbucket-specific repository structure
            String fullName = repoNode.get("full_name").asText();
            String[] nameParts = fullName.split("/");
            String workspaceName = nameParts.length > 0 ? nameParts[0] : "";
            String repoName = nameParts.length > 1 ? nameParts[1] : repoNode.get("name").asText();
            
            return RepositoryMetadata.builder()
                    .id(repoNode.get("uuid").asText())
                    .name(repoName)
                    .fullName(fullName)
                    .description(getStringValue(repoNode, "description"))
                    .cloneUrl(cloneUrl != null ? cloneUrl : "")
                    .webUrl(webUrl != null ? webUrl : "")
                    .defaultBranch(getMainBranch(repoNode))
                    .isPrivate(getBooleanValue(repoNode, "is_private", true))
                    .isFork(isForkRepository(repoNode))
                    .isArchived(false) // Bitbucket doesn't have archived status in API 2.0
                    .language(getStringValue(repoNode, "language"))
                    .size(getLongValue(repoNode, "size", 0))
                    .starCount(0) // Not available in Bitbucket API 2.0
                    .forkCount(0) // Not directly available
                    .createdAt(parseDateTime(getStringValue(repoNode, "created_on")))
                    .updatedAt(parseDateTime(getStringValue(repoNode, "updated_on")))
                    .lastActivityAt(parseDateTime(getStringValue(repoNode, "updated_on")))
                    .ownerId(ownerNode != null ? ownerNode.get("uuid").asText() : "")
                    .ownerName(workspaceName)
                    .ownerType(getOwnerType(ownerNode))
                    .providerType(GitProviderType.BITBUCKET)
                    .build();
        } catch (Exception e) {
            logger.error("Error parsing Bitbucket repository", e);
            return null;
        }
    }
    
    private String getMainBranch(JsonNode repoNode) {
        JsonNode mainBranch = repoNode.get("mainbranch");
        if (mainBranch != null && !mainBranch.isNull()) {
            return getStringValue(mainBranch, "name", "main");
        }
        return "main"; // Default fallback
    }
    
    private boolean isForkRepository(JsonNode repoNode) {
        JsonNode parent = repoNode.get("parent");
        return parent != null && !parent.isNull();
    }
    
    private String getOwnerType(JsonNode ownerNode) {
        if (ownerNode == null) {
            return "user";
        }
        
        String type = getStringValue(ownerNode, "type", "user");
        // Bitbucket uses "user" and "team" types
        return type.toLowerCase();
    } 
   
    private String getStringValue(JsonNode node, String fieldName) {
        return getStringValue(node, fieldName, null);
    }
    
    private String getStringValue(JsonNode node, String fieldName, String defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asText();
    }
    
    private boolean getBooleanValue(JsonNode node, String fieldName, boolean defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asBoolean();
    }
    
    private int getIntValue(JsonNode node, String fieldName, int defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asInt();
    }
    
    private long getLongValue(JsonNode node, String fieldName, long defaultValue) {
        if (node == null) {
            return defaultValue;
        }
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
            // Bitbucket uses ISO 8601 format with timezone
            return LocalDateTime.parse(dateTimeStr.substring(0, 19), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            logger.debug("Failed to parse datetime: {}", dateTimeStr);
            return null;
        }
    }
    
    private HttpRequest buildAuthenticatedRequest(GitProviderConnection connection, String url) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("User-Agent", "GitMigrator/1.0");
        
        // Add authentication - Bitbucket supports Basic Auth with App Passwords
        String authHeader = authHelper.createAuthenticationHeader(connection);
        requestBuilder.header("Authorization", authHeader);
        
        return requestBuilder.GET().build();
    }
    
    private void handleApiError(HttpResponse<String> response, String operation) {
        String errorMessage = "Bitbucket API error during " + operation + 
                            ". Status: " + response.statusCode();
        
        try {
            JsonNode errorBody = objectMapper.readTree(response.body());
            if (errorBody.has("error")) {
                JsonNode error = errorBody.get("error");
                if (error.has("message")) {
                    errorMessage += ", Message: " + error.get("message").asText();
                }
            }
        } catch (Exception e) {
            // Ignore JSON parsing errors
        }
        
        logger.error(errorMessage);
        
        // Handle specific error codes
        switch (response.statusCode()) {
            case 401:
                throw new RuntimeException("Authentication failed - check your credentials or App Password");
            case 403:
                throw new RuntimeException("Access forbidden - check your permissions or API rate limits");
            case 404:
                throw new RuntimeException("Resource not found - check your workspace/repository name and permissions");
            case 429:
                throw new RuntimeException("Bitbucket API rate limit exceeded - please wait before retrying");
            default:
                throw new RuntimeException(errorMessage);
        }
    }
    
    private PaginationInfo parseBitbucketPagination(JsonNode responseBody, int currentPage) {
        PaginationInfo info = new PaginationInfo();
        info.currentPage = currentPage;
        
        // Bitbucket pagination structure
        if (responseBody.has("size")) {
            info.totalSize = responseBody.get("size").asLong();
        }
        
        if (responseBody.has("pagelen")) {
            info.pageLen = responseBody.get("pagelen").asInt();
        }
        
        if (responseBody.has("next")) {
            info.nextUrl = getStringValue(responseBody, "next");
            info.hasNext = info.nextUrl != null && !info.nextUrl.isEmpty();
        }
        
        if (responseBody.has("previous")) {
            info.prevUrl = getStringValue(responseBody, "previous");
            info.hasPrev = info.prevUrl != null && !info.prevUrl.isEmpty();
        }
        
        return info;
    }
    
    private static class PaginationInfo {
        int currentPage = 1;
        int pageLen = DEFAULT_PAGELEN;
        long totalSize = 0;
        boolean hasNext = false;
        boolean hasPrev = false;
        String nextUrl;
        String prevUrl;
    }
}