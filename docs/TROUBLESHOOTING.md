# Git Repository Migrator - Troubleshooting Guide

This guide helps you diagnose and resolve common issues when using the Git Repository Migrator application.

## Table of Contents

1. [General Troubleshooting](#general-troubleshooting)
2. [Git Authentication Issues](#git-authentication-issues)
3. [Repository Detection Problems](#repository-detection-problems)
4. [Migration Configuration Errors](#migration-configuration-errors)
5. [Template Management Issues](#template-management-issues)
6. [Validation Failures](#validation-failures)
7. [Performance Problems](#performance-problems)
8. [UI and Display Issues](#ui-and-display-issues)
9. [Batch Processing Errors](#batch-processing-errors)
10. [Log Analysis](#log-analysis)

## General Troubleshooting

### Application Won't Start

**Symptoms:**
- Application fails to launch
- JavaFX runtime errors
- "Module not found" errors

**Solutions:**

1. **Check Java Version:**
```bash
java -version
# Should show Java 17 or higher
```

2. **Verify JavaFX Installation:**
```bash
# If using system JavaFX
java --list-modules | grep javafx

# If using Maven
mvn javafx:run
```

3. **Check System Requirements:**
- Java 17+ installed
- Sufficient memory (minimum 2GB RAM)
- Display resolution 1024x768 or higher

4. **Clear Application Cache:**
```bash
# Remove application cache directory
rm -rf ~/.git-migrator/cache
```

### Application Crashes or Freezes

**Symptoms:**
- Application becomes unresponsive
- Sudden crashes during operation
- Out of memory errors

**Solutions:**

1. **Increase Memory Allocation:**
```bash
# Run with more memory
java -Xmx4g -jar git-migrator.jar

# Or with Maven
export MAVEN_OPTS="-Xmx4g"
mvn javafx:run
```

2. **Check System Resources:**
- Close unnecessary applications
- Ensure sufficient disk space (minimum 1GB free)
- Monitor CPU and memory usage

3. **Review Application Logs:**
```bash
# Check logs for error details
tail -f logs/git-migrator.log
```

## Git Authentication Issues

### GitHub Authentication Failures

**Symptoms:**
- "Authentication failed" errors
- Cannot access private repositories
- API rate limit exceeded

**Solutions:**

1. **Verify Personal Access Token:**
```bash
# Test token manually
curl -H "Authorization: token YOUR_TOKEN" https://api.github.com/user
```

2. **Check Token Permissions:**
- Ensure token has `repo` scope for private repositories
- Add `read:org` scope for organization repositories
- Verify token hasn't expired

3. **Set Environment Variable:**
```bash
# Linux/Mac
export GITHUB_TOKEN=your_personal_access_token

# Windows
set GITHUB_TOKEN=your_personal_access_token
```

4. **Handle Rate Limiting:**
- Wait for rate limit reset (check headers)
- Use authenticated requests (higher limits)
- Implement request throttling

### GitLab Authentication Problems

**Symptoms:**
- Cannot connect to GitLab instance
- "Unauthorized" errors
- Self-hosted GitLab issues

**Solutions:**

1. **Configure GitLab URL:**
```bash
# For GitLab.com (default)
export GITLAB_URL=https://gitlab.com

# For self-hosted instance
export GITLAB_URL=https://your-gitlab-instance.com
```

2. **Create Personal Access Token:**
- Go to GitLab → User Settings → Access Tokens
- Create token with `read_repository` and `read_user` scopes
- Set token in environment:
```bash
export GITLAB_TOKEN=your_gitlab_token
```

3. **Self-Hosted GitLab Issues:**
- Verify SSL certificate validity
- Check network connectivity
- Ensure API is enabled on the instance

### Bitbucket Authentication Issues

**Symptoms:**
- App password authentication fails
- Cannot access team repositories
- OAuth errors

**Solutions:**

1. **Create App Password:**
- Go to Bitbucket → Personal Settings → App passwords
- Create password with `Repositories: Read` permission
- Set credentials:
```bash
export BITBUCKET_USERNAME=your_username
export BITBUCKET_APP_PASSWORD=your_app_password
```

2. **Team Repository Access:**
- Ensure you're a member of the team/workspace
- Check repository permissions
- Use team/workspace name in repository URLs

## Repository Detection Problems

### Framework Detection Failures

**Symptoms:**
- Framework shows as "Unknown"
- Incorrect framework detection
- Missing project metadata

**Solutions:**

1. **Verify Project Structure:**
- Ensure standard project files exist (pom.xml, package.json, etc.)
- Check file permissions and readability
- Verify project is in repository root

2. **Manual Framework Override:**
- Right-click repository in table
- Select "Override Framework Detection"
- Choose correct framework from list

3. **Update Detection Rules:**
- Check for new framework versions
- Update application to latest version
- Report missing frameworks as feature requests

### Repository Size Calculation Issues

**Symptoms:**
- Incorrect repository size display
- Size shows as 0 or very large
- Performance issues during size calculation

**Solutions:**

1. **Check Repository Access:**
- Ensure read permissions on all files
- Verify no symbolic links to external locations
- Check for large binary files

2. **Exclude Large Files:**
- Add `.gitignore` entries for large files
- Use Git LFS for binary assets
- Clean up unnecessary files

3. **Performance Optimization:**
- Process smaller repositories first
- Use local repositories when possible
- Enable size calculation caching

## Migration Configuration Errors

### Invalid Configuration Settings

**Symptoms:**
- "Invalid configuration" errors
- Missing required settings
- Validation failures

**Solutions:**

1. **Check Required Fields:**
- Target platform must be selected
- At least one component must be chosen
- Custom settings must be valid key-value pairs

2. **Validate Custom Settings:**
```json
{
  "base-image": "openjdk:17-jre-slim",
  "memory-limit": "512Mi",
  "cpu-limit": "500m"
}
```

3. **Reset to Defaults:**
- Click "Reset Configuration" button
- Apply a working template
- Start with minimal configuration

### Template Application Failures

**Symptoms:**
- Template doesn't apply correctly
- Settings not populated
- Template corruption errors

**Solutions:**

1. **Verify Template Format:**
```bash
# Validate JSON syntax
cat template.json | python -m json.tool
```

2. **Check Template Compatibility:**
- Ensure template version matches application
- Verify all referenced settings exist
- Update deprecated configuration options

3. **Recreate Template:**
- Configure settings manually
- Save as new template
- Delete corrupted template

## Template Management Issues

### Template Loading Failures

**Symptoms:**
- Templates don't appear in list
- "Template not found" errors
- Corrupted template files

**Solutions:**

1. **Check Template Directory:**
```bash
# Default template location
ls -la ~/.git-migrator/templates/

# Check permissions
chmod 755 ~/.git-migrator/templates/
chmod 644 ~/.git-migrator/templates/*.json
```

2. **Validate Template Files:**
```bash
# Check JSON syntax
for file in ~/.git-migrator/templates/*.json; do
    echo "Checking $file"
    python -m json.tool < "$file" > /dev/null
done
```

3. **Restore Default Templates:**
- Delete corrupted templates
- Restart application to regenerate defaults
- Import working templates from backup

### Template Export/Import Issues

**Symptoms:**
- Export fails with permission errors
- Import doesn't recognize template files
- Template sharing problems

**Solutions:**

1. **Check File Permissions:**
```bash
# Ensure write permissions for export
chmod 755 /path/to/export/directory

# Ensure read permissions for import
chmod 644 /path/to/template/file.json
```

2. **Verify Template Format:**
- Exported templates should be valid JSON
- Include all required fields
- Check for special characters in names

3. **Template Sharing:**
- Use version control for team templates
- Document template dependencies
- Test templates before sharing

## Validation Failures

### Docker Validation Issues

**Symptoms:**
- Docker build failures
- "Docker not found" errors
- Permission denied errors

**Solutions:**

1. **Verify Docker Installation:**
```bash
# Check Docker is installed and running
docker --version
docker info
```

2. **Check Docker Permissions:**
```bash
# Add user to docker group (Linux)
sudo usermod -aG docker $USER
# Logout and login again

# Or run with sudo (not recommended for production)
sudo docker build -t test .
```

3. **Docker Build Issues:**
- Check Dockerfile syntax
- Verify base image availability
- Ensure sufficient disk space
- Review build context size

4. **Network Issues:**
```bash
# Test Docker Hub connectivity
docker pull hello-world

# Configure proxy if needed
docker build --build-arg HTTP_PROXY=http://proxy:port .
```

### Helm Validation Problems

**Symptoms:**
- "Helm not found" errors
- Chart validation failures
- Template rendering errors

**Solutions:**

1. **Install Helm:**
```bash
# Install Helm CLI
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Verify installation
helm version
```

2. **Chart Validation:**
```bash
# Manual validation
helm lint /path/to/chart

# Check template rendering
helm template test-release /path/to/chart
```

3. **Common Chart Issues:**
- Invalid YAML syntax
- Missing required values
- Incorrect template references
- Invalid Kubernetes resource definitions

### Kubernetes Manifest Validation

**Symptoms:**
- Invalid Kubernetes resources
- API version errors
- Resource validation failures

**Solutions:**

1. **Check Kubernetes API Versions:**
```bash
# List available API versions
kubectl api-versions

# Validate manifest
kubectl apply --dry-run=client -f manifest.yaml
```

2. **Common Manifest Issues:**
- Outdated API versions
- Invalid resource specifications
- Missing required fields
- Incorrect label selectors

## Performance Problems

### Slow Repository Processing

**Symptoms:**
- Long processing times
- Application becomes unresponsive
- High CPU/memory usage

**Solutions:**

1. **Optimize Processing Settings:**
- Reduce concurrent processing threads
- Process smaller batches
- Use local repositories when possible

2. **System Optimization:**
```bash
# Increase available memory
export JAVA_OPTS="-Xmx4g -XX:+UseG1GC"

# Monitor resource usage
top -p $(pgrep java)
```

3. **Repository Optimization:**
- Clean up large files before processing
- Use shallow clones for analysis
- Cache repository metadata

### Memory Issues

**Symptoms:**
- OutOfMemoryError exceptions
- Application becomes slow
- Frequent garbage collection

**Solutions:**

1. **Increase Heap Size:**
```bash
# Set maximum heap size
java -Xmx8g -jar git-migrator.jar

# Monitor memory usage
jstat -gc $(pgrep java) 5s
```

2. **Optimize Memory Usage:**
- Process repositories sequentially
- Clear caches periodically
- Avoid keeping large objects in memory

3. **Garbage Collection Tuning:**
```bash
# Use G1 garbage collector
java -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar git-migrator.jar
```

## UI and Display Issues

### JavaFX Display Problems

**Symptoms:**
- Blank or corrupted display
- UI elements not responding
- Scaling issues on high-DPI displays

**Solutions:**

1. **Graphics Driver Issues:**
- Update graphics drivers
- Try software rendering:
```bash
java -Dprism.order=sw -jar git-migrator.jar
```

2. **High-DPI Display Issues:**
```bash
# Force DPI scaling
java -Dglass.gtk.uiScale=1.5 -jar git-migrator.jar

# Disable DPI scaling
java -Dglass.gtk.uiScale=1.0 -jar git-migrator.jar
```

3. **Theme and Styling Issues:**
- Reset to default theme
- Clear CSS cache
- Check for custom CSS conflicts

### Drag and Drop Not Working

**Symptoms:**
- Cannot drag files to application
- Drop operations fail
- No visual feedback during drag

**Solutions:**

1. **Check File Permissions:**
- Ensure files are readable
- Verify directory permissions
- Check for file locks

2. **Operating System Issues:**
- Try running as administrator (Windows)
- Check security settings (macOS)
- Verify X11 forwarding (Linux remote)

3. **Application Settings:**
- Restart application
- Check drag-and-drop is enabled
- Verify supported file types

## Batch Processing Errors

### Queue Processing Failures

**Symptoms:**
- Queue stops processing
- Some repositories skipped
- Inconsistent results

**Solutions:**

1. **Check Queue Status:**
- View queue status in progress panel
- Look for error indicators
- Check individual repository status

2. **Error Handling:**
- Enable "Continue on Error" option
- Review error logs for failed repositories
- Retry failed repositories individually

3. **Resource Constraints:**
- Reduce concurrent processing
- Increase timeout values
- Monitor system resources

### Partial Processing Results

**Symptoms:**
- Some repositories processed successfully
- Others fail with errors
- Inconsistent artifact generation

**Solutions:**

1. **Analyze Failure Patterns:**
- Group failures by error type
- Check for common characteristics
- Identify problematic repositories

2. **Incremental Processing:**
- Process successful repositories first
- Handle failures separately
- Use different configurations for different types

## Log Analysis

### Understanding Log Levels

```
ERROR - Critical errors that stop processing
WARN  - Warnings that don't stop processing
INFO  - General information about operations
DEBUG - Detailed technical information
TRACE - Very detailed execution traces
```

### Common Log Patterns

#### Git Operation Errors
```
ERROR GitOperationService - Failed to clone repository: Authentication failed
WARN  GitOperationService - Repository already exists, updating instead
INFO  GitOperationService - Successfully cloned repository to /tmp/repo
```

#### Template Processing
```
ERROR TemplateService - Template not found: microservice-template
WARN  TemplateService - Template version mismatch, using defaults
INFO  TemplateService - Applied template: web-app-template
```

#### Validation Issues
```
ERROR ValidationService - Docker build failed: base image not found
WARN  ValidationService - Helm chart has warnings but is valid
INFO  ValidationService - All validations passed successfully
```

### Log File Locations

```bash
# Application logs
logs/git-migrator.log          # Main application log
logs/git-migrator-error.log    # Error-only log
logs/performance.log           # Performance metrics

# System logs (Linux)
/var/log/syslog               # System messages
~/.xsession-errors            # X11/desktop errors

# System logs (Windows)
# Check Windows Event Viewer for application errors
```

### Log Analysis Tools

```bash
# Search for errors
grep "ERROR" logs/git-migrator.log

# Filter by time range
awk '/2024-01-01 10:00:00/,/2024-01-01 11:00:00/' logs/git-migrator.log

# Count error types
grep "ERROR" logs/git-migrator.log | cut -d' ' -f4 | sort | uniq -c

# Monitor logs in real-time
tail -f logs/git-migrator.log | grep --color=always "ERROR\|WARN"
```

## Getting Additional Help

### Before Seeking Help

1. **Check Application Version:**
```bash
java -jar git-migrator.jar --version
```

2. **Gather System Information:**
```bash
# Java version
java -version

# Operating system
uname -a  # Linux/Mac
systeminfo  # Windows

# Available memory
free -h  # Linux
vm_stat  # Mac
```

3. **Collect Relevant Logs:**
- Application logs from the issue timeframe
- Error messages and stack traces
- Configuration files used

### Support Channels

1. **Documentation:**
   - Check README.md for basic setup
   - Review User Guide for feature help
   - Consult Developer Guide for technical details

2. **Community Support:**
   - GitHub Discussions for general questions
   - Stack Overflow with `git-migrator` tag
   - Community forums and chat rooms

3. **Issue Reporting:**
   - GitHub Issues for bugs and feature requests
   - Include system information and logs
   - Provide steps to reproduce the issue

### Creating Effective Bug Reports

Include the following information:

1. **Environment Details:**
   - Operating system and version
   - Java version
   - Application version
   - Hardware specifications

2. **Problem Description:**
   - What you were trying to do
   - What you expected to happen
   - What actually happened
   - Error messages received

3. **Reproduction Steps:**
   - Step-by-step instructions
   - Sample data or repositories
   - Configuration settings used

4. **Logs and Screenshots:**
   - Relevant log entries
   - Screenshots of error dialogs
   - Configuration files (remove sensitive data)

This troubleshooting guide covers the most common issues encountered with the Git Repository Migrator. For issues not covered here, please refer to the support channels listed above.