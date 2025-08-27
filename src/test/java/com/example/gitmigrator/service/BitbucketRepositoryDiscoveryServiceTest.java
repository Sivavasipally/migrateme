package com.example.gitmigrator.service;

import com.example.gitmigrator.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BitbucketRepositoryDiscoveryServiceTest {
    
    @Mock
    private SecureAuthenticationHelper authHelper;
    
    @Mock
    private HttpClient httpClient;
    
    @Mock
    private HttpResponse<String> httpResponse;
    
    private BitbucketRepositoryDiscoveryService service;
    private GitProviderConnection connection;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        service = new BitbucketRepositoryDiscoveryService(authHelper);
        objectMapper = new ObjectMapper();
        
        connection = GitProviderConnection.builder()
                .providerType(GitProviderType.BITBUCKET)
                .baseUrl("https://bitbucket.org")
                .apiUrl("https://api.bitbucket.org/2.0")
                .username("testuser")
                .password("testpass".toCharArray())
                .build();
    }
    
    @Test
    void testGetSupportedProvider() {
        assertEquals(GitProviderType.BITBUCKET, service.getSupportedProvider());
    }
    
    @Test
    void testSupportsConnection() {
        assertTrue(service.supportsConnection(connection));
        
        GitProviderConnection githubConnection = GitProviderConnection.builder()
                .providerType(GitProviderType.GITHUB)
                .baseUrl("https://github.com")
                .build();
        
        assertFalse(service.supportsConnection(githubConnection));
        assertFalse(service.supportsConnection(null));
    }
    
    @Test
    void testGetMaxRepositoriesPerCall() {
        assertEquals(100, service.getMaxRepositoriesPerCall());
    }
    
    @Test
    void testSupportsAdvancedFiltering() {
        assertTrue(service.supportsAdvancedFiltering());
    }
    
    @Test
    void testTestConnectionSuccess() throws Exception {
        // Mock successful connection test
        when(authHelper.createAuthenticationHeader(connection)).thenReturn("Basic dGVzdHVzZXI6dGVzdHBhc3M=");
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        
        // Use reflection to set the httpClient field
        java.lang.reflect.Field httpClientField = BitbucketRepositoryDiscoveryService.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(service, httpClient);
        
        CompletableFuture<Boolean> result = service.testConnection(connection);
        assertTrue(result.get());
    }
    
    @Test
    void testTestConnectionFailure() throws Exception {
        // Mock failed connection test
        when(authHelper.createAuthenticationHeader(connection)).thenReturn("Basic dGVzdHVzZXI6dGVzdHBhc3M=");
        when(httpResponse.statusCode()).thenReturn(401);
        when(httpResponse.body()).thenReturn("{\"error\":{\"message\":\"Authentication failed\"}}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        
        // Use reflection to set the httpClient field
        java.lang.reflect.Field httpClientField = BitbucketRepositoryDiscoveryService.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(service, httpClient);
        
        CompletableFuture<Boolean> result = service.testConnection(connection);
        assertFalse(result.get());
    }
    
    @Test
    void testDiscoverAllRepositoriesSuccess() throws Exception {
        // Mock user endpoint response
        String userResponse = "{\"username\":\"testuser\"}";
        
        // Mock repositories response
        String reposResponse = createMockRepositoriesResponse();
        
        when(authHelper.createAuthenticationHeader(connection)).thenReturn("Basic dGVzdHVzZXI6dGVzdHBhc3M=");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        
        // First call for user info, second call for repositories
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(userResponse, reposResponse);
        
        // Use reflection to set the httpClient field
        java.lang.reflect.Field httpClientField = BitbucketRepositoryDiscoveryService.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(service, httpClient);
        
        CompletableFuture<List<RepositoryMetadata>> result = service.discoverAllRepositories(connection);
        List<RepositoryMetadata> repositories = result.get();
        
        assertNotNull(repositories);
        assertEquals(2, repositories.size());
        
        RepositoryMetadata repo1 = repositories.get(0);
        assertEquals("test-repo-1", repo1.getName());
        assertEquals("testuser/test-repo-1", repo1.getFullName());
        assertEquals("Test repository 1", repo1.getDescription());
        assertTrue(repo1.isPrivate());
        assertEquals(GitProviderType.BITBUCKET, repo1.getProviderType());
    }
    
    @Test
    void testSearchRepositories() throws Exception {
        String searchResponse = createMockRepositoriesResponse();
        
        when(authHelper.createAuthenticationHeader(connection)).thenReturn("Basic dGVzdHVzZXI6dGVzdHBhc3M=");
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(searchResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        
        // Use reflection to set the httpClient field
        java.lang.reflect.Field httpClientField = BitbucketRepositoryDiscoveryService.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(service, httpClient);
        
        CompletableFuture<List<RepositoryMetadata>> result = service.searchRepositories(connection, "test");
        List<RepositoryMetadata> repositories = result.get();
        
        assertNotNull(repositories);
        assertEquals(2, repositories.size());
    }
    
    @Test
    void testDiscoverOrganizationRepositories() throws Exception {
        String orgReposResponse = createMockRepositoriesResponse();
        
        when(authHelper.createAuthenticationHeader(connection)).thenReturn("Basic dGVzdHVzZXI6dGVzdHBhc3M=");
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(orgReposResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        
        // Use reflection to set the httpClient field
        java.lang.reflect.Field httpClientField = BitbucketRepositoryDiscoveryService.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(service, httpClient);
        
        CompletableFuture<List<RepositoryMetadata>> result = service.discoverOrganizationRepositories(connection, "myworkspace");
        List<RepositoryMetadata> repositories = result.get();
        
        assertNotNull(repositories);
        assertEquals(2, repositories.size());
    }
    
    @Test
    void testDiscoverRepositoriesPage() throws Exception {
        // Mock user endpoint response
        String userResponse = "{\"username\":\"testuser\"}";
        
        // Mock paginated repositories response
        String pageResponse = createMockPaginatedResponse();
        
        when(authHelper.createAuthenticationHeader(connection)).thenReturn("Basic dGVzdHVzZXI6dGVzdHBhc3M=");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(userResponse, pageResponse);
        
        // Use reflection to set the httpClient field
        java.lang.reflect.Field httpClientField = BitbucketRepositoryDiscoveryService.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(service, httpClient);
        
        CompletableFuture<RepositoryDiscoveryPage> result = service.discoverRepositoriesPage(connection, 1, 50);
        RepositoryDiscoveryPage page = result.get();
        
        assertNotNull(page);
        assertEquals(1, page.getCurrentPage());
        assertEquals(50, page.getPerPage());
        assertEquals(2, page.getRepositories().size());
        assertTrue(page.hasNextPage());
        assertFalse(page.hasPreviousPage());
    }
    
    private String createMockRepositoriesResponse() {
        return "{\n" +
                "  \"values\": [\n" +
                "    {\n" +
                "      \"uuid\": \"{12345678-1234-1234-1234-123456789012}\",\n" +
                "      \"name\": \"test-repo-1\",\n" +
                "      \"full_name\": \"testuser/test-repo-1\",\n" +
                "      \"description\": \"Test repository 1\",\n" +
                "      \"is_private\": true,\n" +
                "      \"language\": \"Java\",\n" +
                "      \"size\": 1024,\n" +
                "      \"created_on\": \"2023-01-01T10:00:00.000000+00:00\",\n" +
                "      \"updated_on\": \"2023-01-15T15:30:00.000000+00:00\",\n" +
                "      \"mainbranch\": {\n" +
                "        \"name\": \"main\"\n" +
                "      },\n" +
                "      \"owner\": {\n" +
                "        \"uuid\": \"{user-uuid}\",\n" +
                "        \"username\": \"testuser\",\n" +
                "        \"type\": \"user\"\n" +
                "      },\n" +
                "      \"links\": {\n" +
                "        \"clone\": [\n" +
                "          {\n" +
                "            \"name\": \"https\",\n" +
                "            \"href\": \"https://bitbucket.org/testuser/test-repo-1.git\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"html\": {\n" +
                "          \"href\": \"https://bitbucket.org/testuser/test-repo-1\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"uuid\": \"{87654321-4321-4321-4321-210987654321}\",\n" +
                "      \"name\": \"test-repo-2\",\n" +
                "      \"full_name\": \"testuser/test-repo-2\",\n" +
                "      \"description\": \"Test repository 2\",\n" +
                "      \"is_private\": false,\n" +
                "      \"language\": \"Python\",\n" +
                "      \"size\": 2048,\n" +
                "      \"created_on\": \"2023-02-01T10:00:00.000000+00:00\",\n" +
                "      \"updated_on\": \"2023-02-15T15:30:00.000000+00:00\",\n" +
                "      \"mainbranch\": {\n" +
                "        \"name\": \"develop\"\n" +
                "      },\n" +
                "      \"owner\": {\n" +
                "        \"uuid\": \"{user-uuid}\",\n" +
                "        \"username\": \"testuser\",\n" +
                "        \"type\": \"user\"\n" +
                "      },\n" +
                "      \"links\": {\n" +
                "        \"clone\": [\n" +
                "          {\n" +
                "            \"name\": \"https\",\n" +
                "            \"href\": \"https://bitbucket.org/testuser/test-repo-2.git\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"html\": {\n" +
                "          \"href\": \"https://bitbucket.org/testuser/test-repo-2\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }
    
    private String createMockPaginatedResponse() {
        return "{\n" +
                "  \"size\": 100,\n" +
                "  \"pagelen\": 50,\n" +
                "  \"next\": \"https://api.bitbucket.org/2.0/repositories/testuser?pagelen=50&page=2\",\n" +
                "  \"values\": [\n" +
                "    {\n" +
                "      \"uuid\": \"{12345678-1234-1234-1234-123456789012}\",\n" +
                "      \"name\": \"test-repo-1\",\n" +
                "      \"full_name\": \"testuser/test-repo-1\",\n" +
                "      \"description\": \"Test repository 1\",\n" +
                "      \"is_private\": true,\n" +
                "      \"language\": \"Java\",\n" +
                "      \"size\": 1024,\n" +
                "      \"created_on\": \"2023-01-01T10:00:00.000000+00:00\",\n" +
                "      \"updated_on\": \"2023-01-15T15:30:00.000000+00:00\",\n" +
                "      \"mainbranch\": {\n" +
                "        \"name\": \"main\"\n" +
                "      },\n" +
                "      \"owner\": {\n" +
                "        \"uuid\": \"{user-uuid}\",\n" +
                "        \"username\": \"testuser\",\n" +
                "        \"type\": \"user\"\n" +
                "      },\n" +
                "      \"links\": {\n" +
                "        \"clone\": [\n" +
                "          {\n" +
                "            \"name\": \"https\",\n" +
                "            \"href\": \"https://bitbucket.org/testuser/test-repo-1.git\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"html\": {\n" +
                "          \"href\": \"https://bitbucket.org/testuser/test-repo-1\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"uuid\": \"{87654321-4321-4321-4321-210987654321}\",\n" +
                "      \"name\": \"test-repo-2\",\n" +
                "      \"full_name\": \"testuser/test-repo-2\",\n" +
                "      \"description\": \"Test repository 2\",\n" +
                "      \"is_private\": false,\n" +
                "      \"language\": \"Python\",\n" +
                "      \"size\": 2048,\n" +
                "      \"created_on\": \"2023-02-01T10:00:00.000000+00:00\",\n" +
                "      \"updated_on\": \"2023-02-15T15:30:00.000000+00:00\",\n" +
                "      \"mainbranch\": {\n" +
                "        \"name\": \"develop\"\n" +
                "      },\n" +
                "      \"owner\": {\n" +
                "        \"uuid\": \"{user-uuid}\",\n" +
                "        \"username\": \"testuser\",\n" +
                "        \"type\": \"user\"\n" +
                "      },\n" +
                "      \"links\": {\n" +
                "        \"clone\": [\n" +
                "          {\n" +
                "            \"name\": \"https\",\n" +
                "            \"href\": \"https://bitbucket.org/testuser/test-repo-2.git\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"html\": {\n" +
                "          \"href\": \"https://bitbucket.org/testuser/test-repo-2\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }
}