# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Run
```bash
# Clean build the project
mvn clean compile

# Run the JavaFX application
mvn javafx:run

# Build JAR package
mvn clean package

# Run packaged JAR
java -jar target/git-migrator-1.0.0.jar
```

### Testing
```bash
# Run unit tests only
mvn test

# Run all tests including integration tests
mvn verify

# Run tests with coverage report
mvn clean verify jacoco:report

# Run performance tests specifically
mvn test -Dtest=BatchProcessingPerformanceTest

# Run specific test patterns
mvn test -Dtest="*ComponentTest"
mvn verify -Dtest="*IntegrationTest"
```

### Development Utilities
```bash
# Run with debug logging for development
mvn javafx:run -Dlogging.level.com.example.gitmigrator=DEBUG

# Clean temporary test files
mvn clean

# Check for dependency updates
mvn versions:display-dependency-updates
```

## Architecture Overview

This is a JavaFX desktop application for automated repository modernization and migration. The architecture follows a layered service-oriented design:

### Key Architectural Patterns

**Service Layer Architecture**: All business logic is implemented in services with clear interfaces:
- `GitApiService` - Git operations and repository management
- `MigrationOrchestratorService` - Coordinates complex migration workflows  
- `TemplateManagementService` - Handles migration configuration templates
- `MigrationQueueService` - Manages batch processing of repositories
- `ValidationService` - Validates generated Docker/Kubernetes artifacts
- `ProgressTrackingService` - Real-time progress monitoring

**JavaFX MVVM Pattern**: UI controllers act as view models:
- `EnhancedMainController` - Primary application controller
- Component-based UI with reusable controls in `controller.component` package
- FXML-based view definitions in `src/main/resources/fxml/`

**Manual Dependency Injection**: Services are manually wired in `GitMigratorApplication.initializeServices()` instead of using Spring or other DI frameworks.

**Asynchronous Processing**: Long-running operations use `CompletableFuture` for non-blocking execution:
- Repository cloning and analysis
- Batch migration processing  
- File validation and Docker builds

### Core Service Dependencies

- `MigrationOrchestratorService` requires `GitApiService`, `GitOperationService`, and `TransformationService`
- `EnhancedMainController` coordinates all UI components and services
- Template management uses FreeMarker for generating Dockerfiles, Helm charts, and Kubernetes manifests

### Data Models

Key domain models are in the `model` package:
- `RepositoryInfo` - Repository metadata and migration status
- `MigrationConfiguration` - Migration settings and component selection
- `MigrationQueueItem` - Queue processing items with status tracking
- `ValidationResult` - Results from Docker/Helm validation

## Framework and Technology Stack

- **JavaFX 17**: Desktop UI framework with FXML views and CSS styling
- **JGit**: Pure Java Git implementation for repository operations
- **FreeMarker**: Template engine for generating deployment artifacts
- **Jackson**: JSON/YAML processing for configuration and templates
- **TestFX**: UI testing framework for JavaFX components
- **JUnit 5**: Unit testing with Mockito for mocking
- **JMH**: Performance benchmarking for batch operations

## Testing Strategy

### Test Organization
- Unit tests: `src/test/java/com/example/gitmigrator/` (mirrors source structure)
- Integration tests: `*IntegrationTest.java` classes
- UI tests: TestFX-based component tests
- Performance tests: JMH benchmarks in `performance` package

### Mock Services
Test configuration enables mock services via `application-test.properties`:
- `mock.git.service.enabled=true`
- `mock.validation.service.enabled=true`
- `mock.template.service.enabled=true`

### Test Data
- Sample repositories and configurations in `src/test/resources/test-data/`
- Test data factory classes provide consistent test objects
- Temporary test directories automatically cleaned up

## Configuration Management

### Template System
- Migration templates stored as JSON files in `${user.home}/.git-migrator/templates`
- Built-in templates for microservices, frontends, and monoliths
- FreeMarker templates in `src/main/resources/templates/` for different frameworks:
  - `springboot/` - Spring Boot applications  
  - `react/` - React frontend applications
  - `nodejs/` - Node.js applications
  - `angular/` - Angular applications
  - `generic/` - Generic containerization templates

### Environment Variables
For Git service integration, set these environment variables before running the application:

**GitHub:**
```bash
export GITHUB_TOKEN=your_github_personal_access_token
```
- Create token at: https://github.com/settings/tokens
- Required scopes: `repo` (for private repos), `read:org`, `read:user`

**GitLab:**
```bash
export GITLAB_TOKEN=your_gitlab_personal_access_token
```
- Create token at: https://gitlab.com/-/profile/personal_access_tokens
- Required scopes: `read_repository`, `read_user`

**Bitbucket:**
```bash
export BITBUCKET_USERNAME=your_username
export BITBUCKET_APP_PASSWORD=your_app_password
```
- Create app password at: https://bitbucket.org/account/settings/app-passwords/
- Required permissions: `Repositories: Read`

## Development Patterns

### Service Implementation
All services follow interface-implementation pattern:
- Interface defines contract (e.g., `MigrationQueueService`)
- Implementation class provides logic (e.g., `MigrationQueueServiceImpl`)
- Services injected via constructor or setter methods

### Error Handling
- Comprehensive error reporting via `ErrorReportingService`
- Recovery suggestions provided for common failure scenarios
- Progress tracking includes error state management
- Validation failures include specific remediation steps

### JavaFX Component Structure
- Custom components extend JavaFX base classes
- FXML controllers handle UI logic and event binding
- CSS styling in `src/main/resources/css/styles.css`
- Drag-and-drop support for repository management

### Async Operations
- Use `CompletableFuture` for long-running operations
- JavaFX `Platform.runLater()` for UI updates from background threads
- Progress callbacks for real-time status updates
- Cancellation support for user-initiated stops

## Git Repository Migration Workflow

1. **Repository Discovery**: Add repositories via drag-drop, manual entry, or Git service integration
2. **Framework Detection**: Automatic detection of Spring Boot, React, Angular, Node.js, etc.  
3. **Configuration**: Select target platform (Kubernetes/OpenShift/Docker Compose) and optional components
4. **Template Processing**: Generate Dockerfiles, Helm charts, CI/CD pipelines using FreeMarker templates
5. **Validation**: Validate generated artifacts using Docker build and Helm lint
6. **Queue Processing**: Batch process multiple repositories with progress tracking
7. **Result Review**: Preview generated files and handle any validation errors

This workflow is orchestrated by `MigrationOrchestratorService` and coordinated through the `EnhancedMainController`.

## Comprehensive Framework Detection System

The application includes a sophisticated framework detection system that follows a systematic 5-step approach to identify repository types and frameworks:

### Framework Detection Process

**Step 1 - Manifest File Detection:**
- Scans root directory and major subfolders for manifest files
- Java: `pom.xml` (Maven), `build.gradle` (Gradle)
- JavaScript/TypeScript: `package.json`, `angular.json`
- Python: `pyproject.toml`, `requirements.txt`

**Step 2 - Java Framework Classification:**
- **Spring Boot**: Dependencies mention "spring-boot", has `@SpringBootApplication` class, `application.properties/yml`
- **Spring Classic**: Uses "spring-core", "spring-context", XML configuration files, `web.xml`
- **Maven/Gradle Java**: Generic Java projects with build tools

**Step 3 - JavaScript/TypeScript Framework Detection:**
- **React**: `package.json` with React dependencies, `src` and `public` folders, Vite/Webpack
- **Angular**: `angular.json` workspace, `@angular/core` dependencies, `app.module.ts`
- **AngularJS**: Legacy Angular 1.x dependencies, `bower.json`

**Step 4 - Python Framework Detection:**
- **FastAPI**: FastAPI dependencies, Uvicorn server, `FastAPI()` app creation
- **Flask**: Flask dependencies, `Flask(__name__)` app pattern

**Step 5 - Multi-Stack and Monorepo Detection:**
- **Multi-Stack**: Different framework types (e.g., Java backend + React frontend)
- **Monorepo**: Multiple projects of same or different types in subfolders

### Framework Detection Services

**Core Classes:**
- `FrameworkDetectionService` - Implements the 5-step detection algorithm
- `RepositoryAnalyzerService` - Integrates detection with repository analysis
- `FrameworkType` enum - Comprehensive list of supported frameworks

**Supported Framework Types:**
- Java: `SPRING_BOOT`, `SPRING_CLASSIC`, `MAVEN_JAVA`, `GRADLE_JAVA`
- JavaScript/TypeScript: `REACT`, `ANGULAR`, `ANGULAR_JS`, `NODE_JS`
- Python: `FLASK`, `FASTAPI`, `PYTHON`
- Multi-component: `MULTI_STACK`, `MONOREPO`

**Detection Features:**
- Evidence-based detection with detailed logging
- Complexity scoring (1-5 scale)
- Migration recommendations per framework type
- Monorepo and multi-stack repository support
- Automatic subfolder scanning for components