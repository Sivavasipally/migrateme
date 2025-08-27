package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.GitProviderType;
import com.example.gitmigrator.model.RepositoryMetadata;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class RepositoryDiscoveryPanelIntegrationTest {
    
    private RepositoryDiscoveryPanel panel;
    
    @Start
    void start(Stage stage) {
        panel = new RepositoryDiscoveryPanel();
        Scene scene = new Scene(panel, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    
    @Test
    void testBasicFunctionality(FxRobot robot) throws InterruptedException {
        // Create test repositories
        List<RepositoryMetadata> testRepositories = Arrays.asList(
            RepositoryMetadata.builder()
                .id("1")
                .name("test-repo")
                .fullName("org/test-repo")
                .description("A test repository")
                .cloneUrl("https://github.com/org/test-repo.git")
                .language("Java")
                .size(1024)
                .starCount(5)
                .isPrivate(false)
                .isFork(false)
                .isArchived(false)
                .updatedAt(LocalDateTime.now())
                .providerType(GitProviderType.GITHUB)
                .build(),
            
            RepositoryMetadata.builder()
                .id("2")
                .name("another-repo")
                .fullName("org/another-repo")
                .description("Another test repository")
                .cloneUrl("https://github.com/org/another-repo.git")
                .language("Python")
                .size(512)
                .starCount(2)
                .isPrivate(true)
                .isFork(false)
                .isArchived(false)
                .updatedAt(LocalDateTime.now().minusDays(1))
                .providerType(GitProviderType.GITHUB)
                .build()
        );
        
        // Set repositories on JavaFX thread
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            panel.setRepositories(testRepositories);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Failed to set repositories");
        
        // Verify basic state
        assertEquals(2, panel.getTotalCount());
        assertEquals(2, panel.getFilteredCount());
        assertEquals(0, panel.getSelectedCount());
        
        // Verify table has correct number of rows
        TableView<?> table = robot.lookup(".repository-table").queryAs(TableView.class);
        assertEquals(2, table.getItems().size());
        
        // Test that the component is properly initialized
        assertNotNull(panel.getCurrentFilter());
        assertFalse(panel.isLoading());
    }
    
    @Test
    void testEmptyState(FxRobot robot) throws InterruptedException {
        // Test with empty repository list
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            panel.setRepositories(Arrays.asList());
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Failed to set empty repositories");
        
        assertEquals(0, panel.getTotalCount());
        assertEquals(0, panel.getFilteredCount());
        assertEquals(0, panel.getSelectedCount());
        assertEquals("No repositories loaded", panel.getStatusMessage());
    }
    
    @Test
    void testLoadingState(FxRobot robot) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            panel.setLoading(true);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Failed to set loading state");
        
        assertTrue(panel.isLoading());
        assertEquals("Loading repositories...", panel.getStatusMessage());
        
        CountDownLatch latch2 = new CountDownLatch(1);
        Platform.runLater(() -> {
            panel.setLoading(false);
            latch2.countDown();
        });
        assertTrue(latch2.await(5, TimeUnit.SECONDS), "Failed to clear loading state");
        
        assertFalse(panel.isLoading());
    }
}