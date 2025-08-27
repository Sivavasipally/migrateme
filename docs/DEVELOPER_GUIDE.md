# Git Repository Migrator - Developer Guide

This guide provides comprehensive information for developers working on the Git Repository Migrator application, including architecture details, development setup, coding standards, and contribution guidelines.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Development Setup](#development-setup)
3. [Project Structure](#project-structure)
4. [Core Components](#core-components)
5. [Service Layer](#service-layer)
6. [UI Components](#ui-components)
7. [Data Models](#data-models)
8. [Testing Strategy](#testing-strategy)
9. [Build and Deployment](#build-and-deployment)
10. [Contributing](#contributing)

## Architecture Overview

The Git Repository Migrator follows a layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                    JavaFX UI Layer                          │
│  - Enhanced Controllers                                     │
│  - UI Components (Drag/Drop, Preview, Progress)            │
│  - FXML Views and CSS Styling                              │
├─────────────────────────────────────────────────────────────┤
│                   Service Layer                             │
│  - Migration Queue Service                                 │
│  - Template Management Service                             │
│  - Git Service Integration                                 │
│  - Validation Service                                      │
│  - Progress Tracking Service                               │
│  - Error Reporting Service                                 │
├─────────────────────────────────────────────────────────────┤
│                   Core Services                             │
│  - Migration Orchestrator                                  │
│  - Transformation Service                                  │
│  - Git Operations Service                                  │
├─────────────────────────────────────────────────────────────┤
│                   Data Layer                                │
│  - Repository Models                                       │
│  - Configuration Models                                    │
│  - Result Models                                           │
└─────────────────────────────────────────────────────────────┘
```

### Design Principles

1. **Separation of Concerns**: Each layer has distinct responsibilities
2. **Dependency Injection**: Services are injected rather than instantiated
3. **Asynchronous Operations**: Long-running operations use CompletableFuture
4. **Event-Driven Architecture**: Components communicate through events
5. **Testability**: All components are designed for easy unit testing

## Development Setup

### Prerequisites

- **Java 17+**: Required for JavaFX and modern language features
- **Maven 3.6+**: Build tool and dependency management
- **Git**: Version control and repository operations
- **IDE**: IntelliJ IDEA or Eclipse with JavaFX support
- **Docker** (optional): For validation features
- **Helm** (optional): For Helm chart validation

### Environment Setup

1. **Clone the Repository**:
```bash
git clone https://github.com/your-org/git-migrator.git
cd git-migrator
```

2. **Configure IDE**:
   - Import as Maven project
   - Set Java 17 as project SDK
   - Enable JavaFX module support
   - Configure code style (see `.editorconfig`)

3. **Install Dependencies**:
```bash
mvn clean install
```

4. **Run Tests**:
```bash
mvn test
```

5. **Start Application**:
```bash
mvn javafx:run
```

### Development Environment Variables

Set these environment variables for full functionality:

```bash
# Git service authentication
export GITHUB_TOKEN=your_github_token
export GITLAB_TOKEN=your_gitlab_token
export BITBUCKET_USERNAME=your_username
export BITBUCKET_APP_PASSWORD=your_app_password

# Development settings
export LOG_LEVEL=DEBUG
export TEMPLATE_STORAGE_DIR=./dev-templates
export TEST_DATA_DIR=./src/test/resources/test-data
```

## Project Structure

```
git-migrator/
├── src/
│   ├── main/
│   │   ├── java/com/example/gitmigrator/
│   │   │   ├── config/              # Application configuration
│   │   │   ├── controller/          # JavaFX controllers
│   │   │   │   └── component/       # Reusable UI components
│   │   │   ├── model/               # Data models and DTOs
│   │   │   ├── service/             # Business logic services
│   │   │   └── GitMigratorApplication.java
│   │   └── resources/
│   │       ├── fxml/                # JavaFX FXML files
│   │       ├── css/                 # Stylesheets
│   │       ├── images/              # Icons and images
│   │       └── templates/           # FreeMarker templates
│   └── test/
│       ├── java/                    # Test classes
│       │   ├── integration/         # Integration tests
│       │   ├── performance/         # Performance tests
│       │   └── ...                  # Unit tests
│       └── resources/
│           ├── test-data/           # Test data files
│           └── application-test.properties
├── docs/                            # Documentation
├── .kiro/specs/                     # Feature specifications
├── pom.xml                          # Maven configuration
└── README.md
```

## Core Components

### GitMigratorApplication

The main application class that bootstraps the JavaFX application:

```java
@Component
public class GitMigratorApplication extends Application {
    
    private ConfigurableApplicationContext applicationContext;
    
    @Override
    public void init() {
        // Initialize Spring context
        applicationContext = SpringApplication.run(GitMigratorApplication.class);
    }
    
    @Override
    public void start(Stage primaryStage) {
        // Load main FXML and show primary stage
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/enhanced-main.fxml"));
        loader.setControllerFactory(applicationContext::getBean);
        // ... stage setup
    }
}
```

### EnhancedMainController

The primary controller that coordinates all UI interactions:

```java
@Controller
public class EnhancedMainController {
    
    @Autowired
    private MigrationQueueService queueService;
    
    @Autowired
    private TemplateManagementService templateService;
    
    @Autowired
    private ProgressTrackingService progressService;
    
    // UI component references
    @FXML private DragDropRepositoryTable repositoryTable;
    @FXML private MigrationConfigurationPanel configPanel;
    @FXML private FilePreviewComponent previewComponent;
    @FXML private ProgressTrackingComponent progressComponent;
    
    // Event handlers and UI logic
}
```

## Service Layer

### Migration Queue Service

Manages batch processing of repositories:

```java
public interface MigrationQueueService {
    void addToQueue(RepositoryInfo repo, MigrationConfiguration config);
    void removeFromQueue(String repoId);
    void reorderQueue(List<String> repoIds);
    CompletableFuture<List<MigrationResult>> processQueue();
    void pauseProcessing();
    void resumeProcessing();
    QueueStatus getQueueStatus();
    void addQueueEventListener(QueueEventListener listener);
}

@Service
public class MigrationQueueServiceImpl implements MigrationQueueService {
    
    private final Queue<MigrationQueueItem> migrationQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService executorService;
    private final MigrationOrchestratorService orchestrator;
    
    // Implementation details...
}
```

### Template Management Service

Handles migration templates:

```java
public interface TemplateManagementService {
    void saveTemplate(String name, MigrationConfiguration config);
    MigrationConfiguration loadTemplate(String name);
    List<String> getAvailableTemplates();
    void deleteTemplate(String name);
    void exportTemplate(String name, File destination);
    void importTemplate(File source);
}
```

### Git Service Integration

Provides unified access to Git hosting services:

```java
public interface GitServiceIntegration {
    CompletableFuture<List<RepositoryInfo>> fetchRepositories(GitServiceConfig config);
    CompletableFuture<List<RepositoryInfo>> searchRepositories(String query, GitServiceConfig config);
    boolean authenticateService(GitServiceConfig config);
    List<String> getSupportedServices();
}

// Implementations for each service
@Service
public class GitHubServiceIntegration implements GitServiceIntegration {
    // GitHub-specific implementation
}

@Service
public class GitLabServiceIntegration implements GitServiceIntegration {
    // GitLab-specific implementation
}
```

### Validation Service

Validates generated artifacts:

```java
public interface ValidationService {
    ValidationResult validateDockerfile(File dockerfile);
    ValidationResult validateHelmChart(File chartDirectory);
    ValidationResult validateKubernetesManifests(List<File> manifests);
    CompletableFuture<ValidationResult> buildDockerImage(File dockerfile, String imageName);
}
```

## UI Components

### Custom JavaFX Components

#### DragDropRepositoryTable

Enhanced TableView with drag-and-drop support:

```java
public class DragDropRepositoryTable extends TableView<RepositoryInfo> {
    
    public DragDropRepositoryTable() {
        setupDragAndDrop();
        setupColumns();
        setupContextMenu();
    }
    
    private void setupDragAndDrop() {
        setOnDragOver(event -> {
            if (event.getDragboard().hasFiles() || event.getDragboard().hasUrl()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        
        setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasFiles()) {
                success = handleFilesDrop(db.getFiles());
            } else if (db.hasUrl()) {
                success = handleUrlDrop(db.getUrl());
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
    }
}
```

#### FilePreviewComponent

Multi-tabbed file preview with syntax highlighting:

```java
public class FilePreviewComponent extends VBox {
    
    private TabPane tabPane;
    private FileTreeComponent fileTree;
    private TabbedFileEditorComponent editor;
    private ValidationIndicatorComponent validator;
    
    public void displayGeneratedFiles(List<GeneratedFile> files) {
        Platform.runLater(() -> {
            fileTree.setFiles(files);
            editor.clearTabs();
            
            files.forEach(file -> {
                Tab tab = createFileTab(file);
                editor.addTab(tab);
            });
        });
    }
}
```

### FXML and CSS

#### FXML Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<BorderPane xmlns="http://javafx.com/javafx/11.0.1" 
            xmlns:fx="http://javafx.com/fxml/1" 
            fx:controller="com.example.gitmigrator.controller.EnhancedMainController">
    
    <top>
        <ToolBar>
            <Button fx:id="connectGitServiceBtn" text="Connect Git Service"/>
            <Button fx:id="loadTemplateBtn" text="Load Template"/>
            <Button fx:id="saveTemplateBtn" text="Save Template"/>
        </ToolBar>
    </top>
    
    <center>
        <SplitPane dividerPositions="0.7">
            <DragDropRepositoryTable fx:id="repositoryTable"/>
            <MigrationConfigurationPanel fx:id="configPanel"/>
        </SplitPane>
    </center>
    
    <bottom>
        <ProgressTrackingComponent fx:id="progressComponent"/>
    </bottom>
</BorderPane>
```

#### CSS Styling

```css
.repository-table {
    -fx-background-color: #f8f9fa;
    -fx-border-color: #dee2e6;
    -fx-border-width: 1px;
}

.drag-over {
    -fx-background-color: #e3f2fd;
    -fx-border-color: #2196f3;
    -fx-border-width: 2px;
    -fx-border-style: dashed;
}

.error-indicator {
    -fx-background-color: #ffebee;
    -fx-text-fill: #c62828;
}

.success-indicator {
    -fx-background-color: #e8f5e8;
    -fx-text-fill: #2e7d32;
}
```

## Data Models

### Core Models

#### RepositoryInfo

```java
public class RepositoryInfo {
    private String id;
    private String name;
    private String url;
    private String localPath;
    private FrameworkType detectedFramework;
    private LocalDateTime lastCommitDate;
    private String lastCommitMessage;
    private int estimatedComplexity;
    private long repositorySize;
    private List<String> languages;
    private MigrationConfiguration migrationConfig;
    private MigrationStatus status;
    
    // Getters, setters, validation methods
}
```

#### MigrationConfiguration

```java
public class MigrationConfiguration implements Cloneable {
    private String targetPlatform;
    private List<String> optionalComponents;
    private Map<String, String> customSettings;
    private String templateName;
    private boolean enableValidation;
    
    public MigrationConfiguration clone() {
        // Deep clone implementation
    }
    
    public boolean isValid() {
        // Validation logic
    }
}
```

### Result Models

#### MigrationResult

```java
public class MigrationResult {
    private String repositoryId;
    private boolean success;
    private List<GeneratedFile> generatedFiles;
    private String errorMessage;
    private LocalDateTime completedAt;
    private Duration processingTime;
    private ValidationResult validationResult;
}
```

## Testing Strategy

### Test Categories

#### Unit Tests
- Test individual components in isolation
- Mock external dependencies
- Focus on business logic and edge cases

```java
@ExtendWith(MockitoExtension.class)
class MigrationQueueServiceImplTest {
    
    @Mock
    private MigrationOrchestratorService orchestrator;
    
    @InjectMocks
    private MigrationQueueServiceImpl queueService;
    
    @Test
    void shouldAddRepositoryToQueue() {
        // Test implementation
    }
}
```

#### Integration Tests
- Test complete workflows
- Use real services with test data
- Verify end-to-end functionality

```java
@SpringBootTest
class MigrationWorkflowIntegrationTest {
    
    @Autowired
    private MigrationQueueService queueService;
    
    @Test
    void shouldCompleteFullMigrationWorkflow() {
        // Integration test implementation
    }
}
```

#### UI Tests
- Test JavaFX components with TestFX
- Verify user interactions
- Test drag-and-drop functionality

```java
@ExtendWith(ApplicationExtension.class)
class DragDropRepositoryTableTest {
    
    @Test
    void shouldAcceptRepositoryDrop(FxRobot robot) {
        // UI test implementation
    }
}
```

#### Performance Tests
- Benchmark batch processing
- Test with large datasets
- Monitor resource usage

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BatchProcessingPerformanceTest {
    
    @Benchmark
    public void benchmarkLargeBatchProcessing() {
        // Performance test implementation
    }
}
```

### Test Data Management

#### Test Data Factory

```java
public class TestDataFactory {
    
    public static RepositoryInfo createSpringBootRepo() {
        RepositoryInfo repo = new RepositoryInfo();
        repo.setName("spring-boot-app");
        repo.setDetectedFramework(FrameworkType.SPRING_BOOT);
        // ... setup test data
        return repo;
    }
    
    public static MigrationConfiguration createKubernetesConfig() {
        MigrationConfiguration config = new MigrationConfiguration();
        config.setTargetPlatform("kubernetes");
        config.setOptionalComponents(Arrays.asList("dockerfile", "helm"));
        return config;
    }
}
```

## Build and Deployment

### Maven Configuration

Key Maven plugins and their purposes:

```xml
<!-- JavaFX Maven Plugin -->
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>com.example.gitmigrator.GitMigratorApplication</mainClass>
    </configuration>
</plugin>

<!-- Surefire for unit tests -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*Test.java</include>
        </includes>
        <excludes>
            <exclude>**/performance/**</exclude>
        </excludes>
    </configuration>
</plugin>

<!-- Failsafe for integration tests -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*IntegrationTest.java</include>
        </includes>
    </configuration>
</plugin>

<!-- JaCoCo for code coverage -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Build Commands

```bash
# Clean build
mvn clean compile

# Run tests
mvn test                    # Unit tests only
mvn verify                  # All tests including integration
mvn test -Dtest=*Performance*  # Performance tests only

# Code coverage
mvn clean verify jacoco:report

# Package application
mvn clean package

# Run application
mvn javafx:run

# Create distribution
mvn clean package -P distribution
```

### Continuous Integration

#### GitHub Actions Workflow

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    
    - name: Run tests
      run: mvn clean verify
    
    - name: Generate code coverage report
      run: mvn jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
```

## Contributing

### Development Workflow

1. **Fork and Clone**: Fork the repository and clone your fork
2. **Create Branch**: Create a feature branch from `develop`
3. **Implement Changes**: Make your changes with appropriate tests
4. **Run Tests**: Ensure all tests pass
5. **Submit PR**: Create a pull request to the `develop` branch

### Code Standards

#### Java Coding Conventions

- Use Java 17 features where appropriate
- Follow Google Java Style Guide
- Use meaningful variable and method names
- Add JavaDoc for public APIs
- Maintain consistent indentation (4 spaces)

#### Testing Requirements

- Maintain minimum 80% code coverage
- Write unit tests for all new functionality
- Add integration tests for new workflows
- Include UI tests for new components
- Performance tests for batch operations

#### Documentation Standards

- Update README for new features
- Add JavaDoc for public methods
- Include inline comments for complex logic
- Update user guide for UI changes
- Document configuration options

### Code Review Process

1. **Automated Checks**: CI pipeline must pass
2. **Code Review**: At least one reviewer approval required
3. **Testing**: Manual testing of new features
4. **Documentation**: Verify documentation updates
5. **Merge**: Squash and merge to maintain clean history

### Release Process

1. **Version Bump**: Update version in pom.xml
2. **Changelog**: Update CHANGELOG.md with new features
3. **Tag Release**: Create Git tag for version
4. **Build Artifacts**: Generate distribution packages
5. **Publish**: Release to GitHub with artifacts

This developer guide provides the foundation for contributing to the Git Repository Migrator project. For specific questions or clarifications, please refer to the project's GitHub discussions or contact the maintainers.