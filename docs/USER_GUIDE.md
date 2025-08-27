# Git Repository Migrator - User Guide

This comprehensive guide will help you get the most out of the Git Repository Migrator application, covering all features from basic repository migration to advanced batch processing and template management.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Repository Management](#repository-management)
3. [Migration Configuration](#migration-configuration)
4. [Template System](#template-system)
5. [Preview and Editing](#preview-and-editing)
6. [Batch Processing](#batch-processing)
7. [Git Service Integration](#git-service-integration)
8. [Validation and Testing](#validation-and-testing)
9. [Progress Tracking](#progress-tracking)
10. [Troubleshooting](#troubleshooting)

## Getting Started

### First Launch

When you first launch the Git Repository Migrator, you'll see the main interface with several key areas:

- **Repository Table**: Central area for managing repositories
- **Configuration Panel**: Right sidebar for migration settings
- **Progress Panel**: Bottom area for tracking operations
- **Menu Bar**: Access to templates, settings, and help

### Basic Workflow

1. **Add repositories** to the migration list
2. **Configure migration settings** for each repository
3. **Preview generated artifacts** before applying
4. **Execute migration** and monitor progress
5. **Review results** and handle any issues

## Repository Management

### Adding Repositories

#### Drag and Drop
The easiest way to add repositories is through drag and drop:

- **Local Repositories**: Drag a folder containing a Git repository directly onto the repository table
- **Remote URLs**: Drag a Git URL from your browser onto the application
- **Multiple Selection**: Select multiple folders in your file manager and drag them all at once

#### Manual Entry
You can also add repositories manually:

1. Click the "Add Repository" button
2. Choose "Local Folder" or "Remote URL"
3. Browse to select a local folder or enter a Git URL
4. Click "Add" to include it in the migration list

#### Git Service Integration
Connect to your Git hosting service to browse repositories:

1. Click "Connect Git Service" in the toolbar
2. Select your service (GitHub, GitLab, or Bitbucket)
3. Enter your authentication credentials
4. Browse and select repositories from the service

### Repository Information

The repository table displays comprehensive information about each repository:

- **Name**: Repository name with framework icon
- **Framework**: Detected framework (Spring Boot, React, etc.)
- **Size**: Repository size in MB
- **Complexity**: Estimated migration complexity (1-5 scale)
- **Last Commit**: Date and message of the last commit
- **Status**: Current migration status
- **Actions**: Quick access buttons for configuration and removal

### Repository Actions

Right-click on any repository to access additional actions:

- **Configure Migration**: Open detailed configuration panel
- **View Details**: See comprehensive repository metadata
- **Clone/Update**: Refresh local copy of remote repositories
- **Remove**: Remove from migration list
- **Duplicate Configuration**: Copy settings to other repositories

## Migration Configuration

### Target Platforms

Choose your deployment target:

- **Kubernetes**: Generate Kubernetes manifests, services, and ingress
- **OpenShift**: Create OpenShift-specific resources and routes
- **Docker Compose**: Generate docker-compose.yml for local development

### Optional Components

Select additional components to generate:

#### Dockerfile
- **Automatic Base Image Selection**: Chooses appropriate base image based on detected framework
- **Multi-stage Builds**: Optimized builds for production deployments
- **Security Best Practices**: Non-root users, minimal attack surface

#### Helm Charts
- **Complete Chart Structure**: Templates, values, and Chart.yaml
- **Configurable Values**: Customizable deployment parameters
- **Best Practices**: Following Helm chart conventions

#### CI/CD Pipelines
- **GitHub Actions**: Workflows for build, test, and deploy
- **GitLab CI**: Pipeline configurations for GitLab
- **Jenkins**: Jenkinsfile for Jenkins pipelines

#### Monitoring
- **Prometheus Metrics**: Service monitors and alerting rules
- **Grafana Dashboards**: Pre-configured monitoring dashboards
- **Health Checks**: Liveness and readiness probes

### Custom Settings

Fine-tune your migration with custom settings:

#### Container Configuration
- **Base Image**: Override default base image selection
- **Resource Limits**: Set CPU and memory limits
- **Environment Variables**: Define runtime environment
- **Port Configuration**: Expose specific ports

#### Kubernetes Settings
- **Namespace**: Target namespace for deployment
- **Service Type**: ClusterIP, NodePort, or LoadBalancer
- **Ingress**: Enable and configure ingress rules
- **Replica Count**: Number of pod replicas

#### Advanced Options
- **Build Arguments**: Docker build-time variables
- **Volume Mounts**: Persistent storage configuration
- **Security Context**: Pod and container security settings
- **Network Policies**: Traffic control rules

## Template System

### Using Built-in Templates

The application includes several pre-configured templates:

#### Microservice Template
- Target: Kubernetes
- Components: Dockerfile, Helm chart, CI/CD, monitoring
- Settings: Optimized for microservice architecture
- Best for: Spring Boot, Node.js APIs

#### Frontend Template
- Target: Kubernetes with Nginx
- Components: Dockerfile, Helm chart, CDN configuration
- Settings: Static file serving optimization
- Best for: React, Angular, Vue.js applications

#### Monolith Template
- Target: Docker Compose
- Components: Dockerfile, database integration
- Settings: Traditional application deployment
- Best for: Legacy applications, development environments

### Creating Custom Templates

1. Configure a migration with your desired settings
2. Click "Save as Template" in the configuration panel
3. Provide a template name and description
4. Add tags for easy categorization
5. Save the template for future use

### Managing Templates

Access template management through the Templates menu:

- **View All Templates**: Browse available templates
- **Edit Template**: Modify existing template settings
- **Delete Template**: Remove unused templates
- **Export Template**: Share templates with team members
- **Import Template**: Load templates from files

### Template Sharing

Templates can be shared between team members:

1. **Export**: Save template to a JSON file
2. **Share**: Distribute the file to team members
3. **Import**: Load the template in other installations
4. **Version Control**: Store templates in your project repository

## Preview and Editing

### File Preview

Before applying migrations, preview all generated files:

#### File Tree View
- **Hierarchical Display**: See all generated files in a tree structure
- **File Status**: New files, modified files, and unchanged files
- **Quick Navigation**: Click to jump to specific files

#### Tabbed Editor
- **Syntax Highlighting**: Automatic highlighting for Dockerfile, YAML, JSON
- **Line Numbers**: Easy reference for editing
- **Search and Replace**: Find and modify content quickly
- **Undo/Redo**: Safe editing with change history

#### Diff Viewer
- **Side-by-Side Comparison**: See original vs. generated content
- **Highlighted Changes**: Added, removed, and modified lines
- **Merge Options**: Accept or reject specific changes

### Editing Generated Files

Make modifications to generated content:

1. **Select File**: Click on a file in the preview panel
2. **Edit Content**: Modify the content in the editor
3. **Validate Changes**: Real-time validation shows errors
4. **Save Changes**: Apply modifications to the generated files

### Validation Indicators

The editor provides real-time feedback:

- **Syntax Errors**: Red underlines for syntax issues
- **Warnings**: Yellow highlights for potential problems
- **Suggestions**: Blue hints for improvements
- **Error Panel**: Detailed error descriptions and fixes

## Batch Processing

### Migration Queue

Process multiple repositories efficiently:

#### Adding to Queue
- **Individual Addition**: Add repositories one by one with specific configurations
- **Bulk Addition**: Select multiple repositories and apply the same configuration
- **Template Application**: Apply saved templates to multiple repositories

#### Queue Management
- **Reorder Items**: Drag and drop to change processing order
- **Priority Settings**: Set high, normal, or low priority
- **Pause/Resume**: Control queue processing
- **Remove Items**: Remove repositories from the queue

### Processing Configuration

Configure how the queue processes repositories:

#### Concurrency Settings
- **Parallel Processing**: Set number of concurrent migrations
- **Resource Management**: Balance speed vs. system resources
- **Throttling**: Prevent system overload

#### Error Handling
- **Continue on Error**: Keep processing remaining repositories
- **Retry Logic**: Automatic retry for transient failures
- **Error Reporting**: Detailed error logs and recovery suggestions

### Batch Results

Review comprehensive results after processing:

#### Summary Report
- **Success Rate**: Percentage of successful migrations
- **Processing Time**: Total and average processing time
- **Resource Usage**: CPU and memory consumption

#### Detailed Results
- **Per-Repository Status**: Success/failure for each repository
- **Generated Files**: List of created artifacts
- **Error Details**: Specific error messages and suggested fixes

## Git Service Integration

### Supported Services

Connect to popular Git hosting services:

- **GitHub**: Public and private repositories, organizations
- **GitLab**: GitLab.com and self-hosted instances
- **Bitbucket**: Atlassian Bitbucket Cloud and Server

### Authentication Setup

#### GitHub
1. Generate a Personal Access Token in GitHub settings
2. Grant required permissions: `repo`, `read:org`
3. Set the `GITHUB_TOKEN` environment variable
4. Connect through the application interface

#### GitLab
1. Create a Personal Access Token in GitLab
2. Grant permissions: `read_repository`, `read_user`
3. Set the `GITLAB_TOKEN` environment variable
4. Configure GitLab instance URL if using self-hosted

#### Bitbucket
1. Create an App Password in Bitbucket settings
2. Grant permissions: `Repositories: Read`
3. Set `BITBUCKET_USERNAME` and `BITBUCKET_APP_PASSWORD`
4. Connect through the application

### Repository Browsing

Once connected, browse repositories efficiently:

#### Search and Filter
- **Text Search**: Find repositories by name or description
- **Language Filter**: Filter by programming language
- **Framework Filter**: Show only specific framework types
- **Organization Filter**: Browse by organization or user

#### Repository Selection
- **Bulk Selection**: Select multiple repositories at once
- **Metadata Preview**: See repository details before adding
- **Clone Options**: Choose local clone location

## Validation and Testing

### Docker Validation

Validate generated Dockerfiles:

#### Build Testing
- **Image Building**: Attempt to build Docker images
- **Build Logs**: Detailed output from Docker build process
- **Error Analysis**: Specific error identification and suggestions
- **Cleanup**: Automatic removal of test images

#### Best Practices Check
- **Security Scanning**: Check for security vulnerabilities
- **Size Optimization**: Identify opportunities to reduce image size
- **Layer Analysis**: Review Docker layer structure

### Helm Validation

Validate Helm charts:

#### Lint Testing
- **Syntax Validation**: Check YAML syntax and structure
- **Template Rendering**: Verify templates render correctly
- **Value Validation**: Check values.yaml completeness
- **Best Practices**: Follow Helm chart conventions

#### Dry Run Testing
- **Kubernetes Validation**: Test against Kubernetes API
- **Resource Conflicts**: Check for naming conflicts
- **Dependency Validation**: Verify chart dependencies

### Custom Validation

Add custom validation rules:

1. **Define Rules**: Create validation criteria
2. **Configure Checks**: Set up automated validation
3. **Review Results**: See validation outcomes
4. **Fix Issues**: Apply suggested corrections

## Progress Tracking

### Real-Time Updates

Monitor migration progress in real-time:

#### Overall Progress
- **Completion Percentage**: Overall migration progress
- **Estimated Time**: Remaining time calculation
- **Current Operation**: What the system is currently doing

#### Repository-Level Progress
- **Individual Status**: Progress for each repository
- **Current Step**: Specific operation being performed
- **Step Progress**: Completion within current step

### Detailed Logging

Access comprehensive logging information:

#### Log Levels
- **INFO**: General information about operations
- **WARN**: Warnings that don't stop processing
- **ERROR**: Errors that require attention
- **DEBUG**: Detailed technical information

#### Log Filtering
- **By Level**: Show only specific log levels
- **By Repository**: Filter logs for specific repositories
- **By Time**: Show logs from specific time ranges
- **Text Search**: Find specific log entries

### Progress Persistence

Progress is automatically saved:

- **Session Recovery**: Resume interrupted migrations
- **History Tracking**: Review past migration attempts
- **Performance Metrics**: Track improvement over time

## Troubleshooting

### Common Issues and Solutions

#### Git Authentication Problems

**Symptoms**: Cannot clone repositories, authentication errors

**Solutions**:
1. Verify personal access tokens are valid
2. Check token permissions include repository access
3. Ensure environment variables are set correctly
4. Test Git access from command line

#### Docker Build Failures

**Symptoms**: Dockerfile validation fails, build errors

**Solutions**:
1. Check Docker daemon is running
2. Verify base image availability
3. Review Dockerfile syntax
4. Check available disk space

#### Template Loading Issues

**Symptoms**: Templates don't load, corruption errors

**Solutions**:
1. Verify template file format
2. Check file permissions
3. Validate JSON syntax
4. Restore from backup if available

#### Performance Issues

**Symptoms**: Slow processing, high resource usage

**Solutions**:
1. Reduce concurrent processing
2. Close unnecessary applications
3. Check available system resources
4. Process smaller batches

### Getting Additional Help

If you encounter issues not covered in this guide:

1. **Check Application Logs**: Review detailed logs in the logs directory
2. **Error Recovery**: Use built-in recovery suggestions
3. **Community Support**: Visit the project's GitHub discussions
4. **Issue Reporting**: Submit detailed bug reports with logs

### Best Practices

#### For Optimal Performance
- Process repositories in smaller batches
- Close unnecessary applications during migration
- Use SSD storage for better I/O performance
- Monitor system resources during processing

#### For Reliable Results
- Always preview generated files before applying
- Test migrations on non-critical repositories first
- Keep backups of original repositories
- Validate generated artifacts before deployment

#### For Team Collaboration
- Share templates through version control
- Document custom configurations
- Use consistent naming conventions
- Regular template updates and maintenance

## Advanced Features

### Scripting and Automation

For advanced users, the application supports:

- **Command Line Interface**: Batch processing from scripts
- **Configuration Files**: Automated setup and configuration
- **API Integration**: Programmatic access to migration features
- **Custom Plugins**: Extend functionality with custom code

### Integration with CI/CD

Integrate with your development workflow:

- **Pipeline Integration**: Use as part of CI/CD pipelines
- **Automated Triggers**: Trigger migrations on code changes
- **Result Reporting**: Send results to external systems
- **Quality Gates**: Block deployments on validation failures

This user guide covers the comprehensive functionality of the Git Repository Migrator. For the latest updates and additional resources, visit the project documentation at [project-url].