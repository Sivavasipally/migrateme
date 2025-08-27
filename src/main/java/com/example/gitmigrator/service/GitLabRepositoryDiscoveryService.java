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
 * GitLab implementation of repository discovery service.
 * Integrates with GitLab REST API v4 for bulk repository discovery.
 * Supports both GitLab.com and self-hosted GitLab instances.
 */
public class GitLabRepositoryDiscoveryService implements RepositoryDiscoveryService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitLabRepositoryDiscoveryService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper() {{
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        registerModule(new JavaTimeModule());
    }};
    private static final int DEFAULT_PER_PAGE = 100;
    private static final int MAX_PER_PAGE = 100;
    private static final int MAX_PAGES = 10;
    
    private final HttpClient httpClient;
    private final SecureAuthenticationHelper authHelper;
    
    public GitLabRepositoryDiscoveryService() {
        this.authHelper = new SecureAuthenticationHelper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    public GitLabRepositoryDiscoveryService(SecureAuthenticationHelper authHelper) {
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
                    logger.info("GitLab connection test successful for: {}", connection.getBaseUrl());
                } else {
                    logger.warn("GitLab connection test failed. Status: {}, Body: {}", 
                            response.statusCode(), response.body());
                }
                return success;
                
            } catch (Exception e) {
                logger.error("GitLab connection test failed", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<List<RepositoryMetadata>> discoverAllRepositories(GitProviderConnection connection) {
        return CompletableFuture.supplyAsync(() -> {
            List<RepositoryMetadata> repositories = new ArrayList<>();
            
            try {
                // Fetch user's projects (GitLab calls repositories "projects")
                repositories.addAll(fetchProjects(connection, "/projects?membership=true"));
                
                logger.info("Discovered {} projects from GitLab", repositories.size());
                
            } catch (Exception e) {
                logger.error("Error discovering GitLab projects", e);
                throw new RuntimeException("Failed to discover GitLab projects", e);
            }
            
            return repositories;
        });
    }
    
    @Override
    public CompletableFuture<List<RepositoryMetadata>> discoverOrganizationRepositories(
            GitProviderConnection connection, String organizationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // In GitLab, organizations are called "groups"
                String endpoint = "/groups/" + organizationId + "/projects";
                return fetchProjects(connection, endpoint);
            } catch (Exception e) {
                logger.error("Error discovering projects for GitLab group: {}", organizationId, e);
                throw new RuntimeException("Failed to discover group projects", e);
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
                String url = connection.getApiUrl() + "/projects?search=" + encodedQuery + 
                           "&per_page=" + DEFAULT_PER_PAGE;
                
                HttpRequest request = buildAuthenticatedRequest(connection, url);
                HttpResponse<String> response = httpClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonNode projectsArray = objectMapper.readTree(response.body());
                    
                    if (projectsArray.isArray()) {
                        for (JsonNode projectNode : projectsArray) {
                            RepositoryMetadata repo = parseProject(projectNode);
                            if (repo != null) {
                                repositories.add(repo);
                            }
                        }
                    }
                } else {
                    handleApiError(response, "search projects");
                }
                
            } catch (Exception e) {
                logger.error("Error searching GitLab projects", e);
                throw new RuntimeException("Failed to search GitLab projects", e);
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
                String url = connection.getApiUrl() + "/projects?membership=true" +
                           "&per_page=" + actualPerPage + "&page=" + page;
                
                HttpRequest request = buildAuthenticatedRequest(connection, url);
                HttpResponse<String> response = httpClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    handleApiError(response, "fetch projects page");
                    return RepositoryDiscoveryPage.builder()
                            .repositories(new ArrayList<>())
                            .currentPage(page)
                            .perPage(actualPerPage)
                            .build();
                }
                
                JsonNode projectsArray = objectMapper.readTree(response.body());
                List<RepositoryMetadata> repositories = new ArrayList<>();
                
                if (projectsArray.isArray()) {
                    for (JsonNode projectNode : projectsArray) {
                        RepositoryMetadata repo = parseProject(projectNode);
                        if (repo != null) {
                            repositories.add(repo);
                        }
                    }
                }
                
                // Parse pagination info from headers
                PaginationInfo paginationInfo = parseGitLabPaginationHeaders(response);
                
                return RepositoryDiscoveryPage.builder()
                        .repositories(repositories)
                        .currentPage(page)
                        .perPage(actualPerPage)
                        .totalPages(paginationInfo.totalPages)
                        .totalRepositories(paginationInfo.totalItems)
                        .hasNextPage(paginationInfo.hasNext)
                        .hasPreviousPage(page > 1)
                        .autoCalculatePagination()
                        .build();
                
            } catch (Exception e) {
                logger.error("Error fetching GitLab projects page", e);
                throw new RuntimeException("Failed to fetch projects page", e);
            }
        });
    }  
  
    @Override
    public GitProviderType getSupportedProvider() {
        return GitProviderType.GITLAB;
    }
    
    @Override
    public boolean supportsConnection(GitProviderConnection connection) {
        return connection != null && 
               connection.getProviderType() == GitProviderType.GITLAB &&
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
    
    private List<RepositoryMetadata> fetchProjects(GitProviderConnection connection, String endpoint) 
            throws IOException, InterruptedException {
        List<RepositoryMetadata> repositories = new ArrayList<>();
        
        for (int page = 1; page <= MAX_PAGES; page++) {
            String url = connection.getApiUrl() + endpoint + 
                        (endpoint.contains("?") ? "&" : "?") +
                        "per_page=" + DEFAULT_PER_PAGE + "&page=" + page;
            
            HttpRequest request = buildAuthenticatedRequest(connection, url);
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                handleApiError(response, "fetch projects");
                break;
            }
            
            JsonNode projectsArray = objectMapper.readTree(response.body());
            if (!projectsArray.isArray() || projectsArray.size() == 0) {
                break; // No more projects
            }
            
            for (JsonNode projectNode : projectsArray) {
                RepositoryMetadata repo = parseProject(projectNode);
                if (repo != null) {
                    repositories.add(repo);
                }
            }
            
            // If we got less than the requested amount, we've reached the end
            if (projectsArray.size() < DEFAULT_PER_PAGE) {
                break;
            }
        }
        
        return repositories;
    }    
    
private RepositoryMetadata parseProject(JsonNode projectNode) {
        try {
            JsonNode namespaceNode = projectNode.get("namespace");
            
            return RepositoryMetadata.builder()
                    .id(projectNode.get("id").asText())
                    .name(projectNode.get("name").asText())
                    .fullName(projectNode.get("path_with_namespace").asText())
                    .description(getStringValue(projectNode, "description"))
                    .cloneUrl(projectNode.get("http_url_to_repo").asText())
                    .webUrl(projectNode.get("web_url").asText())
                    .defaultBranch(getStringValue(projectNode, "default_branch", "main"))
                    .isPrivate(getVisibilityAsPrivate(projectNode))
                    .isFork(getBooleanValue(projectNode, "forked_from_project", false))
                    .isArchived(getBooleanValue(projectNode, "archived", false))
                    .language(getStringValue(projectNode, "language"))
                    .size(getLongValue(projectNode, "repository_size", 0))
                    .starCount(getIntValue(projectNode, "star_count", 0))
                    .forkCount(getIntValue(projectNode, "forks_count", 0))
                    .createdAt(parseDateTime(getStringValue(projectNode, "created_at")))
                    .updatedAt(parseDateTime(getStringValue(projectNode, "last_activity_at")))
                    .lastActivityAt(parseDateTime(getStringValue(projectNode, "last_activity_at")))
                    .ownerId(namespaceNode != null ? namespaceNode.get("id").asText() : "")
                    .ownerName(namespaceNode != null ? namespaceNode.get("path").asText() : "")
                    .ownerType(namespaceNode != null ? namespaceNode.get("kind").asText().toLowerCase() : "user")
                    .providerType(GitProviderType.GITLAB)
                    .build();
        } catch (Exception e) {
            logger.error("Error parsing GitLab project", e);
            return null;
        }
    }
    
    private boolean getVisibilityAsPrivate(JsonNode projectNode) {
        String visibility = getStringValue(projectNode, "visibility", "private");
        // GitLab visibility levels: private, internal, public
        return !"public".equals(visibility);
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
        if (fieldName.equals("forked_from_project")) {
            // GitLab returns an object if it's a fork, null if not
            return !fieldNode.isNull();
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
                .header("Accept", "application/json")
                .header("User-Agent", "GitMigrator/1.0");
        
        // Add authentication - GitLab supports both Basic Auth and Private Token
        String authHeader = authHelper.createAuthenticationHeader(connection);
        if (authHeader.startsWith("Basic")) {
            requestBuilder.header("Authorization", authHeader);
        } else {
            // For GitLab, we can also use Private-Token header
            requestBuilder.header("Private-Token", connection.getPasswordAsString());
        }
        
        return requestBuilder.GET().build();
    }    
    
    private void handleApiError(HttpResponse<String> response, String operation) {
        String errorMessage = "GitLab API error during " + operation + 
                            ". Status: " + response.statusCode();
        
        try {
            JsonNode errorBody = objectMapper.readTree(response.body());
            if (errorBody.has("message")) {
                errorMessage += ", Message: " + errorBody.get("message").asText();
            } else if (errorBody.has("error")) {
                errorMessage += ", Error: " + errorBody.get("error").asText();
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
                throw new RuntimeException("Access forbidden - check your permissions or API rate limits");
            case 404:
                throw new RuntimeException("Resource not found - check your URL and permissions");
            case 429:
                throw new RuntimeException("GitLab API rate limit exceeded - please wait before retrying");
            default:
                throw new RuntimeException(errorMessage);
        }
    }
    
    private PaginationInfo parseGitLabPaginationHeaders(HttpResponse<String> response) {
        PaginationInfo info = new PaginationInfo();
        
        // GitLab uses custom headers for pagination
        response.headers().firstValue("X-Total").ifPresent(total -> {
            try {
                info.totalItems = Long.parseLong(total);
            } catch (NumberFormatException e) {
                logger.debug("Failed to parse X-Total header: {}", total);
            }
        });
        
        response.headers().firstValue("X-Total-Pages").ifPresent(totalPages -> {
            try {
                info.totalPages = Integer.parseInt(totalPages);
            } catch (NumberFormatException e) {
                logger.debug("Failed to parse X-Total-Pages header: {}", totalPages);
            }
        });
        
        response.headers().firstValue("X-Page").ifPresent(currentPage -> {
            try {
                info.currentPage = Integer.parseInt(currentPage);
            } catch (NumberFormatException e) {
                logger.debug("Failed to parse X-Page header: {}", currentPage);
            }
        });
        
        response.headers().firstValue("X-Per-Page").ifPresent(perPage -> {
            try {
                info.perPage = Integer.parseInt(perPage);
            } catch (NumberFormatException e) {
                logger.debug("Failed to parse X-Per-Page header: {}", perPage);
            }
        });
        
        response.headers().firstValue("X-Next-Page").ifPresent(nextPage -> {
            info.hasNext = !nextPage.isEmpty();
        });
        
        response.headers().firstValue("X-Prev-Page").ifPresent(prevPage -> {
            info.hasPrev = !prevPage.isEmpty();
        });
        
        return info;
    }    

    private static class PaginationInfo {
        int currentPage = 1;
        int totalPages = 1;
        int perPage = DEFAULT_PER_PAGE;
        long totalItems = 0;
        boolean hasNext = false;
        boolean hasPrev = false;
    }
}