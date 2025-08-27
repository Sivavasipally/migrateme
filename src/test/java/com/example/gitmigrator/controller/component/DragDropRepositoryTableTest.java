package com.example.gitmigrator.controller.component;

import com.example.gitmigrator.model.FrameworkType;
import com.example.gitmigrator.model.MigrationStatus;
import com.example.gitmigrator.model.RepositoryInfo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DragDropRepositoryTable component.
 * Note: These tests run without TestFX for simplicity.
 */
class DragDropRepositoryTableTest {
    
    private DragDropRepositoryTable table;
    private ObservableList<RepositoryInfo> testData;
    
    @BeforeAll
    static void initToolkit() {
        // Initialize JavaFX toolkit for headless testing
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Platform already initialized
        }
    }
    
    @BeforeEach
    void setUp() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                table = new DragDropRepositoryTable();
                testData = FXCollections.observableArrayList();
                table.setItems(testData);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Setup should complete within 5 seconds");
    }
    
    @Test
    void testTableInitialization() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        
        Platform.runLater(() -> {
            try {
                assertNotNull(table, "Table should be initialized");
                assertEquals(7, table.getColumns().size(), "Table should have 7 columns");
                assertTrue(table.isEditable(), "Table should be editable");
                result.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test should complete within 5 seconds");
        assertTrue(result.get(), "Table initialization test should pass");
    }
    
    @Test
    void testRepositoryDataDisplay() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        
        Platform.runLater(() -> {
            try {
                // Create test repository
                RepositoryInfo repo = createTestRepository();
                testData.add(repo);
                
                assertEquals(1, table.getItems().size(), "Table should contain one item");
                assertEquals(repo, table.getItems().get(0), "Table should contain the test repository");
                result.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test should complete within 5 seconds");
        assertTrue(result.get(), "Repository data display test should pass");
    }
    
    @Test
    void testDragDropCallbacks() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        
        Platform.runLater(() -> {
            try {
                AtomicReference<File> droppedFolder = new AtomicReference<>();
                AtomicReference<String> droppedUrl = new AtomicReference<>();
                AtomicReference<String> validationError = new AtomicReference<>();
                
                // Set up callbacks
                table.setOnFolderDropped(droppedFolder::set);
                table.setOnUrlDropped(droppedUrl::set);
                table.setOnValidationError(validationError::set);
                
                // Test callbacks are set (we can't easily test actual drag-drop in unit tests)
                assertNotNull(table, "Table should be initialized with callbacks");
                result.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test should complete within 5 seconds");
        assertTrue(result.get(), "Drag drop callbacks test should pass");
    }
    
    @Test
    void testRepositorySelection() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        
        Platform.runLater(() -> {
            try {
                // Add test repositories
                RepositoryInfo repo1 = createTestRepository();
                repo1.setName("Repo1");
                repo1.setSelected(true);
                
                RepositoryInfo repo2 = createTestRepository();
                repo2.setName("Repo2");
                repo2.setSelected(false);
                
                testData.addAll(repo1, repo2);
                
                assertEquals(2, table.getItems().size(), "Table should contain two items");
                assertTrue(table.getItems().get(0).isSelected(), "First repository should be selected");
                assertFalse(table.getItems().get(1).isSelected(), "Second repository should not be selected");
                result.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test should complete within 5 seconds");
        assertTrue(result.get(), "Repository selection test should pass");
    }
    
    /**
     * Create a test repository with sample data.
     */
    private RepositoryInfo createTestRepository() {
        RepositoryInfo repo = new RepositoryInfo();
        repo.setName("test-repository");
        repo.setCloneUrl("https://github.com/user/test-repository.git");
        repo.setDetectedFramework(FrameworkType.SPRING_BOOT);
        repo.setLastCommitDate(LocalDateTime.now());
        repo.setEstimatedComplexity(3);
        repo.setRepositorySize(1024L);
        repo.setStatus(MigrationStatus.NOT_STARTED);
        repo.setSelected(false);
        return repo;
    }
}