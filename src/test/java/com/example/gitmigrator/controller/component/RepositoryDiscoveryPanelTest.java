package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.GitProviderType;
import com.example.gitmigrator.model.RepositoryFilter;
import com.example.gitmigrator.model.RepositoryMetadata;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class RepositoryDiscoveryPanelTest {
    
    private RepositoryDiscoveryPanel panel;
    private List<RepositoryMetadata> testRepositories;
    
    @Start
    void start(Stage stage) {
        panel = new RepositoryDiscoveryPanel();
        Scene scene = new Scene(panel, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    
    @BeforeEach
    void setUp() throws InterruptedException {
        // Create test repositories
        testRepositories = Arrays.asList(
            RepositoryMetadata.builder()
                .id("1")
                .name("spring-boot-app")
                .fullName("myorg/spring-boot-app")
                .description("A Spring Boot application")
                .cloneUrl("https://github.com/myorg/spring-boot-app.git")
                .webUrl("https://github.com/myorg/spring-boot-app")
                .language("Java")
                .size(1024)
                .starCount(15)
                .isPrivate(false)
                .isFork(false)
                .isArchived(false)
                .updatedAt(LocalDateTime.now().minusDays(5))
                .providerType(GitProviderType.GITHUB)
                .build(),
            
            RepositoryMetadata.builder()
                .id("2")
                .name("react-frontend")
                .fullName("myorg/react-frontend")
                .description("React frontend application")
                .cloneUrl("https://github.com/myorg/react-frontend.git")
                .webUrl("https://github.com/myorg/react-frontend")
                .language("JavaScript")
                .size(512)
                .starCount(8)
                .isPrivate(true)
                .isFork(false)
                .isArchived(false)
                .updatedAt(LocalDateTime.now().minusDays(2))
                .providerType(GitProviderType.GITHUB)
                .build(),
            
            RepositoryMetadata.builder()
                .id("3")
                .name("python-utils")
                .fullName("myorg/python-utils")
                .description("Python utility functions")
                .cloneUrl("https://github.com/myorg/python-utils.git")
                .webUrl("https://github.com/myorg/python-utils")
                .language("Python")
                .size(256)
                .starCount(3)
                .isPrivate(false)
                .isFork(true)
                .isArchived(false)
                .updatedAt(LocalDateTime.now().minusDays(10))
                .providerType(GitProviderType.GITHUB)
                .build(),
            
            RepositoryMetadata.builder()
                .id("4")
                .name("old-project")
                .fullName("myorg/old-project")
                .description("An archived project")
                .cloneUrl("https://github.com/myorg/old-project.git")
                .webUrl("https://github.com/myorg/old-project")
                .language("Java")
                .size(2048)
                .starCount(0)
                .isPrivate(false)
                .isFork(false)
                .isArchived(true)
                .updatedAt(LocalDateTime.now().minusDays(365))
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
    }
    
    @Test
    void testInitialState(FxRobot robot) {
        // Verify initial state
        assertEquals(4, panel.getTotalCount());
        assertEquals(4, panel.getFilteredCount());
        assertEquals(0, panel.getSelectedCount());
        assertFalse(panel.isLoading());
        
        // Verify table has correct number of rows
        TableView<?> table = robot.lookup(".repository-table").queryAs(TableView.class);
        assertEquals(4, table.getItems().size());
        
        // Verify selection summary
        Label summary = robot.lookup(".selection-summary").queryAs(Label.class);
        assertTrue(summary.getText().contains("0 selected"));
        assertTrue(summary.getText().contains("of 4"));
    }
    
    @Test
    void testSearchFiltering(FxRobot robot) throws InterruptedException {
        TextField searchField = robot.lookup(".text-field").queryAs(TextField.class);
        
        // Test search by name
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            searchField.setText("spring");
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // Wait for filtering to complete
        Thread.sleep(100);
        
        assertEquals(1, panel.getFilteredCount());
        
        // Test search by description
        CountDownLatch latch2 = new CountDownLatch(1);
        Platform.runLater(() -> {
            searchField.setText("React");
            latch2.countDown();
        });
        assertTrue(latch2.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertEquals(1, panel.getFilteredCount());
        
        // Clear search
        CountDownLatch latch3 = new CountDownLatch(1);
        Platform.runLater(() -> {
            searchField.clear();
            latch3.countDown();
        });
        assertTrue(latch3.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertEquals(4, panel.getFilteredCount());
    }
    
    @Test
    void testLanguageFiltering(FxRobot robot) throws InterruptedException {
        ComboBox<String> languageFilter = robot.lookup(".combo-box").queryAs(ComboBox.class);
        
        // Test Java filter
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            languageFilter.setValue("Java");
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertEquals(2, panel.getFilteredCount()); // spring-boot-app and old-project
        
        // Test Python filter
        CountDownLatch latch2 = new CountDownLatch(1);
        Platform.runLater(() -> {
            languageFilter.setValue("Python");
            latch2.countDown();
        });
        assertTrue(latch2.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertEquals(1, panel.getFilteredCount()); // python-utils
        
        // Reset to all languages
        CountDownLatch latch3 = new CountDownLatch(1);
        Platform.runLater(() -> {
            languageFilter.setValue("All Languages");
            latch3.countDown();
        });
        assertTrue(latch3.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertEquals(4, panel.getFilteredCount());
    }
    
    @Test
    void testVisibilityFiltering(FxRobot robot) throws InterruptedException {
        // Find the visibility filter (second combo box)
        List<ComboBox> comboBoxes = new ArrayList<>(robot.lookup(".combo-box").queryAllAs(ComboBox.class));
        ComboBox<String> visibilityFilter = comboBoxes.get(1); // Second combo box is visibility
        
        // Test Public filter
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            visibilityFilter.setValue("Public");
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertEquals(3, panel.getFilteredCount()); // All except react-frontend (private)
        
        // Test Non-Forks filter
        CountDownLatch latch2 = new CountDownLatch(1);
        Platform.runLater(() -> {
            visibilityFilter.setValue("Non-Forks");
            latch2.countDown();
        });
        assertTrue(latch2.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertEquals(3, panel.getFilteredCount()); // All except python-utils (fork)
        
        // Test Non-Archived filter
        CountDownLatch latch3 = new CountDownLatch(1);
        Platform.runLater(() -> {
            visibilityFilter.setValue("Non-Archived");
            latch3.countDown();
        });
        assertTrue(latch3.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertEquals(3, panel.getFilteredCount()); // All except old-project (archived)
        
        // Reset to all
        CountDownLatch latch4 = new CountDownLatch(1);
        Platform.runLater(() -> {
            visibilityFilter.setValue("All");
            latch4.countDown();
        });
        assertTrue(latch4.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertEquals(4, panel.getFilteredCount());
    }
    
    @Test
    void testBulkSelection(FxRobot robot) throws InterruptedException {
        Button selectAllButton = robot.lookup("Select All").queryAs(Button.class);
        Button selectNoneButton = robot.lookup("Select None").queryAs(Button.class);
        
        // Test select all
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            selectAllButton.fire();
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertEquals(4, panel.getSelectedCount());
        
        List<RepositoryMetadata> selected = panel.getSelectedRepositories();
        assertEquals(4, selected.size());
        
        // Test select none
        CountDownLatch latch2 = new CountDownLatch(1);
        Platform.runLater(() -> {
            selectNoneButton.fire();
            latch2.countDown();
        });
        assertTrue(latch2.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertEquals(0, panel.getSelectedCount());
        
        selected = panel.getSelectedRepositories();
        assertEquals(0, selected.size());
    }
    
    @Test
    void testClearFilters(FxRobot robot) throws InterruptedException {
        TextField searchField = robot.lookup(".text-field").queryAs(TextField.class);
        Button clearButton = robot.lookup("Clear Filters").queryAs(Button.class);
        
        // Apply some filters
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            searchField.setText("spring");
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertEquals(1, panel.getFilteredCount());
        
        // Clear filters
        CountDownLatch latch2 = new CountDownLatch(1);
        Platform.runLater(() -> {
            clearButton.fire();
            latch2.countDown();
        });
        assertTrue(latch2.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertEquals(4, panel.getFilteredCount());
        assertTrue(searchField.getText().isEmpty());
    }
    
    @Test
    void testLoadingState(FxRobot robot) throws InterruptedException {
        ProgressIndicator loadingIndicator = robot.lookup(".progress-indicator").queryAs(ProgressIndicator.class);
        
        // Test loading state
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            panel.setLoading(true);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        assertTrue(panel.isLoading());
        assertTrue(loadingIndicator.isVisible());
        
        // Test not loading state
        CountDownLatch latch2 = new CountDownLatch(1);
        Platform.runLater(() -> {
            panel.setLoading(false);
            latch2.countDown();
        });
        assertTrue(latch2.await(5, TimeUnit.SECONDS));
        
        assertFalse(panel.isLoading());
        assertFalse(loadingIndicator.isVisible());
    }
    
    @Test
    void testFilterApplication() throws InterruptedException {
        RepositoryFilter filter = new RepositoryFilter();
        filter.setSearchQuery("spring");
        filter.setLanguages(Set.of("Java"));
        filter.setIncludePrivate(false);
        
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            panel.applyFilter(filter);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertEquals(1, panel.getFilteredCount()); // Only spring-boot-app matches
        
        // Verify UI controls are updated
        TextField searchField = robot.lookup(".text-field").queryAs(TextField.class);
        assertEquals("spring", searchField.getText());
    }
    
    @Test
    void testEmptyRepositoryList(FxRobot robot) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            panel.setRepositories(Arrays.asList());
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        assertEquals(0, panel.getTotalCount());
        assertEquals(0, panel.getFilteredCount());
        assertEquals(0, panel.getSelectedCount());
        
        Label statusLabel = robot.lookup(".status-label").queryAs(Label.class);
        assertEquals("No repositories loaded", statusLabel.getText());
    }
    
    @Test
    void testSelectionWithFiltering(FxRobot robot) throws InterruptedException {
        TextField searchField = robot.lookup(".text-field").queryAs(TextField.class);
        Button selectAllButton = robot.lookup("Select All").queryAs(Button.class);
        
        // Apply filter to show only Java repositories
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            searchField.setText("java"); // This should match description or language
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        
        // Select all filtered repositories
        CountDownLatch latch2 = new CountDownLatch(1);
        Platform.runLater(() -> {
            selectAllButton.fire();
            latch2.countDown();
        });
        assertTrue(latch2.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        
        // Should only select the filtered repositories
        List<RepositoryMetadata> selected = panel.getSelectedRepositories();
        assertTrue(selected.size() <= panel.getFilteredCount());
        
        // Clear filter and check total selection
        CountDownLatch latch3 = new CountDownLatch(1);
        Platform.runLater(() -> {
            searchField.clear();
            latch3.countDown();
        });
        assertTrue(latch3.await(5, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        
        // Selection should be preserved
        selected = panel.getSelectedRepositories();
        assertEquals(panel.getSelectedCount(), selected.size());
    }
}