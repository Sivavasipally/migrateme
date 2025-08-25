# Git Repository Migrator - JavaFX Desktop Application

This application has been converted from a Spring Boot web application to a JavaFX standalone desktop application.

## Key Changes Made

### Architecture Transformation
- **Removed Spring Boot framework** - No more web server, replaced with JavaFX desktop UI
- **Replaced Spring MVC with JavaFX** - Controllers now handle UI events instead of HTTP requests
- **Manual Dependency Injection** - Services are manually wired instead of using Spring's IoC container
- **Native HTTP Client** - Replaced Spring's RestTemplate with Java's HttpURLConnection

### New Components
1. **JavaFX Main Application** (`GitMigratorApplication.java`)
2. **JavaFX Controller** (`MainController.java`) 
3. **FXML UI Layout** (`/resources/fxml/main.fxml`)
4. **CSS Styling** (`/resources/css/styles.css`)
5. **Logback Configuration** (`/resources/logback.xml`)

### Updated Dependencies
- **Removed**: Spring Boot, Spring Web, Spring MVC, Thymeleaf
- **Added**: JavaFX Controls, JavaFX FXML, JavaFX Web
- **Kept**: JGit, FreeMarker, Jackson, Apache Commons, SLF4J/Logback

## Building and Running

### Requirements
- Java 17 or higher
- Maven 3.8+

### Build the Application
```bash
mvn clean compile
```

### Run the Application
```bash
mvn javafx:run
```

### Create Executable JAR
```bash
mvn clean package
java -jar target/git-migrator-1.0.0.jar
```

## User Interface

The JavaFX application provides a desktop interface with the following features:

1. **Git API Configuration**
   - Enter API URL (e.g., `https://api.github.com/user/repos`)
   - Enter authentication token
   - Fetch repositories button

2. **Repository Selection**
   - Table displaying fetched repositories
   - Checkboxes for selecting repositories to migrate
   - Select All/Select None buttons

3. **Migration Process**
   - Start Migration button
   - Progress bar showing migration status
   - Real-time log output in text area

4. **Status and Logging**
   - Status label showing current operation
   - Scrollable log area with migration details
   - Success/failure notifications

## Features Retained

All core migration functionality has been preserved:
- ✅ Git repository fetching and cloning
- ✅ Framework detection (Spring Boot, React, Angular, Node.js)
- ✅ Template-based file generation
- ✅ Dockerfile creation
- ✅ Helm chart generation
- ✅ Multi-framework support
- ✅ Comprehensive logging

## Deployment

The JavaFX application can be:
- **Distributed as executable JAR** - Single file deployment
- **Packaged with native installer** - Using tools like jpackage
- **Run from IDE** - Direct execution during development

## Benefits of JavaFX Version

1. **Standalone Application** - No need for web server or browser
2. **Better User Experience** - Native desktop UI with rich controls
3. **Offline Capable** - Works without internet connection for local repos
4. **Resource Efficient** - Lower memory footprint than web application
5. **Platform Native** - Integrates better with desktop environments
6. **Single Executable** - Easy distribution and deployment

## Migration Notes

- All service logic remains unchanged
- Framework detection and transformation capabilities preserved
- Template system fully functional with all framework types
- Error handling and logging maintained
- Configuration flexibility retained

The application now provides the same powerful repository migration capabilities in a user-friendly desktop interface.