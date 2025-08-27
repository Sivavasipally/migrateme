# Git Repository Migrator

A powerful JavaFX application for automated repository modernization and migration to containerized deployments. This tool helps developers migrate their existing projects to modern deployment platforms like Kubernetes, OpenShift, and Docker Compose with intelligent analysis and artifact generation.

## Features

### 🚀 Enhanced Repository Management
- **Drag-and-Drop Interface**: Easily add repositories by dragging folders or URLs directly into the application
- **Intelligent Detection**: Automatic framework detection (Spring Boot, React, Angular, Node.js, etc.)
- **Rich Metadata Display**: View repository size, complexity, last commit info, and detected languages
- **Bulk Operations**: Configure and process multiple repositories simultaneously

### ⚙️ Advanced Migration Configuration
- **Multi-Platform Support**: Target Kubernetes, OpenShift, or Docker Compose deployments
- **Flexible Components**: Choose from Dockerfiles, Helm charts, CI/CD pipelines, and monitoring configurations
- **Custom Settings**: Configure base images, resource limits, environment variables, and more
- **Template System**: Save and reuse migration configurations across projects

### 📊 Real-Time Progress Tracking
- **Detailed Progress Views**: Step-by-step status for each repository in the migration queue
- **Live Operation Tracking**: See current operations (cloning, analyzing, generating files) with progress indicators
- **Comprehensive Error Reporting**: Actionable error messages with suggested fixes and recovery options
- **Migration Summaries**: Complete reports with generated files and next steps

### 🔍 Preview and Validation
- **File Preview**: Review all generated artifacts before applying changes
- **Syntax Highlighting**: Built-in editor with syntax highlighting for Dockerfiles, YAML, and more
- **In-Place Editing**: Modify generated content with real-time validation
- **Docker Build Validation**: Test generated Dockerfiles by building images
- **Helm Chart Validation**: Validate Helm charts using `helm lint`

### 🔄 Batch Processing and Queue Management
- **Migration Queue**: Process multiple repositories with configurable order and priority
- **Template Application**: Apply different configurations per repository or use saved templates
- **Graceful Error Handling**: Continue processing remaining repositories when failures occur
- **Comprehensive Reporting**: Detailed success/failure statistics for batch operations

### 🌐 Git Service Integration
- **Multi-Service Support**: Connect to GitHub, GitLab, and Bitbucket
- **Repository Browsing**: Search and select repositories directly from your Git hosting service
- **Authentication**: Secure authentication using personal access tokens
- **Advanced Filtering**: Filter repositories by language, framework, activity, and organization

## Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- Git (for repository operations)
- Docker (optional, for validation features)
- Helm (optional, for Helm chart validation)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/your-org/git-migrator.git
cd git-migrator
```

2. Build the application:
```bash
mvn clean package
```

3. Run the application:
```bash
mvn javafx:run
```

Or run the packaged JAR:
```bash
java -jar target/git-migrator-1.0.0.jar
```

### Basic Usage

1. **Add Repositories**: 
   - Drag and drop local repository folders onto the application
   - Drag Git URLs from your browser
   - Use the Git service integration to browse and select repositories

2. **Configure Migration**:
   - Select target platform (Kubernetes, OpenShift, Docker Compose)
   - Choose optional components (Helm charts, CI/CD pipelines, monitoring)
   - Set custom configuration options
   - Apply or save as template for reuse

3. **Preview and Validate**:
   - Review generated files in the preview panel
   - Edit content if needed with syntax highlighting
   - Run validation tests on Dockerfiles and Helm charts

4. **Execute Migration**:
   - Add configured repositories to the migration queue
   - Process queue with real-time progress tracking
   - Review results and handle any errors

## Architecture

The application follows a service-oriented architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                    JavaFX UI Layer                          │
├─────────────────────────────────────────────────────────────┤
│  Enhanced Controllers  │  Preview Components  │  Validation │
├─────────────────────────────────────────────────────────────┤
│                   Service Layer                             │
├─────────────────────────────────────────────────────────────┤
│ Migration Queue │ Template Mgmt │ Git Service │ Validation  │
├─────────────────────────────────────────────────────────────┤
│                   Core Services                             │
├─────────────────────────────────────────────────────────────┤
│   Orchestrator   │  Transformation  │  Git Operations      │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

- **Enhanced Main Controller**: Manages the primary UI and coordinates all operations
- **Migration Queue Service**: Handles batch processing with configurable concurrency
- **Template Management Service**: Manages migration templates and presets
- **Git Service Integration**: Connects to GitHub, GitLab, and Bitbucket APIs
- **Validation Service**: Validates generated artifacts using Docker and Helm tools
- **Progress Tracking Service**: Provides real-time progress updates and error reporting

## Configuration

### Application Settings

Create an `application.properties` file in the application directory:

```properties
# Git service configurations
git.github.api.url=https://api.github.com
git.gitlab.api.url=https://gitlab.com/api/v4
git.bitbucket.api.url=https://api.bitbucket.org/2.0

# Template storage
template.storage.directory=${user.home}/.git-migrator/templates

# Validation settings
validation.docker.enabled=true
validation.helm.enabled=true
validation.timeout.seconds=300

# Queue processing
queue.max.concurrent.jobs=3
queue.retry.attempts=3
queue.retry.delay.seconds=30
```

### Git Service Authentication

Configure authentication for Git services by setting environment variables:

```bash
# GitHub
export GITHUB_TOKEN=your_github_personal_access_token

# GitLab
export GITLAB_TOKEN=your_gitlab_personal_access_token

# Bitbucket
export BITBUCKET_USERNAME=your_username
export BITBUCKET_APP_PASSWORD=your_app_password
```

## Templates

### Built-in Templates

The application includes several built-in templates:

- **Microservice Template**: Kubernetes deployment with Helm charts, monitoring, and CI/CD
- **Frontend Template**: Static site deployment with Nginx, CDN configuration
- **Monolith Template**: Traditional application deployment with database integration
- **Serverless Template**: Function-based deployment configuration

### Custom Templates

Create custom templates by saving migration configurations:

1. Configure a migration with your desired settings
2. Click "Save as Template" and provide a name
3. Reuse the template for similar projects

Templates are stored as JSON files and can be shared between team members.

## Testing

### Running Tests

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run all tests with coverage
mvn clean verify jacoco:report

# Run performance tests
mvn test -Dtest=BatchProcessingPerformanceTest
```

### Test Categories

- **Unit Tests**: Test individual components and services
- **Integration Tests**: Test complete workflows and service interactions
- **UI Tests**: Test JavaFX components using TestFX
- **Performance Tests**: Benchmark batch processing and queue operations

## Troubleshooting

### Common Issues

**Git Authentication Failures**
- Verify your personal access tokens are valid and have required permissions
- Check that tokens are properly set in environment variables
- Ensure your Git service URLs are accessible

**Docker Validation Errors**
- Verify Docker is installed and running
- Check that the Docker daemon is accessible
- Ensure sufficient disk space for image builds

**Helm Validation Failures**
- Install Helm CLI tools
- Verify Helm is in your system PATH
- Check Helm chart syntax and structure

**Template Loading Issues**
- Verify template files are not corrupted
- Check template directory permissions
- Ensure template format matches expected schema

### Getting Help

- Check the application logs in `logs/git-migrator.log`
- Review error messages in the application's error reporting panel
- Use the built-in recovery suggestions for common issues
- Submit issues on the project's GitHub repository

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Development Setup

1. Fork the repository
2. Create a feature branch
3. Make your changes with appropriate tests
4. Ensure all tests pass
5. Submit a pull request

### Code Style

- Follow Java coding conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Maintain test coverage above 80%

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built with JavaFX for cross-platform desktop support
- Uses JGit for Git operations
- Integrates with Docker and Helm for validation
- Powered by FreeMarker for template processing
