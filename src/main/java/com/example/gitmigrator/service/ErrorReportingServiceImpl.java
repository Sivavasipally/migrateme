package com.example.gitmigrator.service;

import com.example.gitmigrator.model.ErrorCategory;
import com.example.gitmigrator.model.ErrorReport;
import com.example.gitmigrator.model.ErrorSeverity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Implementation of comprehensive error reporting service
 */
public class ErrorReportingServiceImpl implements ErrorReportingService {

    private static final Logger logger = Logger.getLogger(ErrorReportingServiceImpl.class.getName());

    private final Map<ErrorCategory, List<String>> categoryActions;
    private final Map<ErrorCategory, List<String>> categoryDocumentation;
    private final Map<ErrorCategory, String> categoryHelpText;
    private final Map<Pattern, List<String>> commonSolutions;

    public ErrorReportingServiceImpl() {
        this.categoryActions = initializeCategoryActions();
        this.categoryDocumentation = initializeCategoryDocumentation();
        this.categoryHelpText = initializeCategoryHelpText();
        this.commonSolutions = initializeCommonSolutions();
    }

    @Override
    public ErrorReport createErrorReport(Throwable error, String context) {
        ErrorCategory category = ErrorCategory.categorizeError(error);
        ErrorSeverity severity = ErrorSeverity.determineSeverity(category, error);

        String title = generateErrorTitle(error, category);
        String description = generateErrorDescription(error, category);
        String technicalDetails = generateTechnicalDetails(error);

        return ErrorReport.builder()
                .category(category)
                .severity(severity)
                .title(title)
                .description(description)
                .technicalDetails(technicalDetails)
                .context(context)
                .originalException(error)
                .suggestedActions(getSuggestedActions(category, error))
                .relatedDocumentation(getRelatedDocumentation(category))
                .build();
    }

    @Override
    public ErrorReport createErrorReport(String title, String description, ErrorCategory category, String context) {
        ErrorSeverity severity = ErrorSeverity.determineSeverity(category, null);

        return ErrorReport.builder()
                .category(category)
                .severity(severity)
                .title(title)
                .description(description)
                .context(context)
                .suggestedActions(getSuggestedActions(category, null))
                .relatedDocumentation(getRelatedDocumentation(category))
                .build();
    }

    @Override
    public List<String> getSuggestedActions(ErrorCategory category, Throwable error) {
        List<String> baseActions = categoryActions.getOrDefault(category,
                Arrays.asList("Contact support for assistance"));
        List<String> actions = new ArrayList<>(baseActions);

        // Add specific actions based on error details
        if (error != null) {
            String errorMessage = error.getMessage() != null ? error.getMessage().toLowerCase() : "";

            switch (category) {
                case NETWORK:
                    if (errorMessage.contains("timeout")) {
                        actions.add("Increase timeout settings in configuration");
                        actions.add("Check if the remote server is responding slowly");
                    }
                    break;
                case AUTHENTICATION:
                    if (errorMessage.contains("token")) {
                        actions.add("Verify your personal access token is still valid");
                        actions.add("Check token permissions and scopes");
                    }
                    break;
                case DOCKER:
                    if (errorMessage.contains("not found")) {
                        actions.add("Ensure Docker is installed and running");
                        actions.add("Check Docker daemon status");
                    }
                    break;
                case REPOSITORY:
                case FRAMEWORK_DETECTION:
                case FILE_GENERATION:
                case VALIDATION:
                case KUBERNETES:
                case HELM:
                case CONFIGURATION:
                case SYSTEM:
                case UNKNOWN:
                    // Default actions are already included from categoryActions
                    break;
            }
        }

        return actions;
    }

    @Override
    public List<String> getRecoverySuggestions(ErrorCategory category, String context) {
        List<String> suggestions = new ArrayList<>();

        switch (category) {
            case NETWORK:
                suggestions.add("Retry the operation after checking network connectivity");
                suggestions.add("Switch to a different network if available");
                suggestions.add("Use offline mode if supported");
                break;
            case AUTHENTICATION:
                suggestions.add("Re-authenticate with the Git service");
                suggestions.add("Generate a new personal access token");
                suggestions.add("Check repository permissions");
                break;
            case REPOSITORY:
                suggestions.add("Verify repository URL and accessibility");
                suggestions.add("Check if repository exists and is not private");
                suggestions.add("Try cloning manually to test access");
                break;
            case DOCKER:
                suggestions.add("Install or start Docker service");
                suggestions.add("Check Docker configuration");
                suggestions.add("Skip Docker-related operations if not needed");
                break;
            case KUBERNETES:
                suggestions.add("Install kubectl if not present");
                suggestions.add("Check Kubernetes cluster connectivity");
                suggestions.add("Validate Kubernetes configuration");
                break;
            case HELM:
                suggestions.add("Install Helm if not present");
                suggestions.add("Initialize Helm repository");
                suggestions.add("Check Helm chart syntax");
                break;
            default:
                suggestions.add("Review error details and try again");
                suggestions.add("Check application logs for more information");
                break;
        }

        return suggestions;
    }

    @Override
    public List<String> getRelatedDocumentation(ErrorCategory category) {
        return categoryDocumentation.getOrDefault(category, Arrays.asList("General troubleshooting guide"));
    }

    @Override
    public String formatUserFriendlyMessage(Throwable error, String context) {
        ErrorCategory category = ErrorCategory.categorizeError(error);
        String baseMessage = generateErrorDescription(error, category);

        StringBuilder message = new StringBuilder();
        message.append(baseMessage);

        if (context != null && !context.isEmpty()) {
            message.append("\n\nContext: ").append(context);
        }

        List<String> actions = getSuggestedActions(category, error);
        if (!actions.isEmpty()) {
            message.append("\n\nSuggested actions:");
            for (int i = 0; i < Math.min(3, actions.size()); i++) {
                message.append("\n• ").append(actions.get(i));
            }
        }

        return message.toString();
    }

    @Override
    public boolean isRecoverable(ErrorCategory category, Throwable error) {
        switch (category) {
            case NETWORK:
            case AUTHENTICATION:
            case CONFIGURATION:
            case VALIDATION:
                return true;
            case SYSTEM:
                return false;
            case DOCKER:
            case KUBERNETES:
            case HELM:
                return error == null || !error.getMessage().contains("not installed");
            default:
                return true;
        }
    }

    @Override
    public String getHelpText(ErrorCategory category) {
        return categoryHelpText.getOrDefault(category, "General error occurred. Check logs for details.");
    }

    @Override
    public void logErrorReport(ErrorReport report) {
        Level logLevel = mapSeverityToLogLevel(report.getSeverity());
        logger.log(logLevel, "Error Report: {0} - {1}", new Object[] { report.getTitle(), report.getDescription() });

        if (report.getOriginalException() != null) {
            logger.log(logLevel, "Original exception", report.getOriginalException());
        }
    }

    @Override
    public List<String> getCommonSolutions(String errorPattern) {
        for (Map.Entry<Pattern, List<String>> entry : commonSolutions.entrySet()) {
            if (entry.getKey().matcher(errorPattern.toLowerCase()).find()) {
                return entry.getValue();
            }
        }
        return Arrays.asList("No specific solutions found for this error pattern");
    }

    private String generateErrorTitle(Throwable error, ErrorCategory category) {
        if (error == null) {
            return category.getDisplayName();
        }

        String className = error.getClass().getSimpleName();
        String message = error.getMessage();

        if (message != null && message.length() > 50) {
            message = message.substring(0, 47) + "...";
        }

        return String.format("%s: %s", className, message != null ? message : "Unknown error");
    }

    private String generateErrorDescription(Throwable error, ErrorCategory category) {
        if (error == null) {
            return category.getDescription();
        }

        String message = error.getMessage();
        if (message == null || message.isEmpty()) {
            return String.format("A %s occurred during migration. %s",
                    error.getClass().getSimpleName(), category.getDescription());
        }

        return message;
    }

    private String generateTechnicalDetails(Throwable error) {
        if (error == null) {
            return null;
        }

        StringBuilder details = new StringBuilder();
        details.append("Exception: ").append(error.getClass().getName()).append("\n");
        details.append("Message: ").append(error.getMessage()).append("\n");

        if (error.getCause() != null) {
            details.append("Caused by: ").append(error.getCause().getClass().getName());
            if (error.getCause().getMessage() != null) {
                details.append(" - ").append(error.getCause().getMessage());
            }
            details.append("\n");
        }

        // Add stack trace summary (first few lines)
        StackTraceElement[] stackTrace = error.getStackTrace();
        if (stackTrace.length > 0) {
            details.append("Stack trace (top 3 frames):\n");
            for (int i = 0; i < Math.min(3, stackTrace.length); i++) {
                details.append("  at ").append(stackTrace[i].toString()).append("\n");
            }
        }

        return details.toString();
    }

    private Level mapSeverityToLogLevel(ErrorSeverity severity) {
        switch (severity) {
            case LOW:
                return Level.INFO;
            case MEDIUM:
                return Level.WARNING;
            case HIGH:
                return Level.SEVERE;
            case CRITICAL:
                return Level.SEVERE;
            default:
                return Level.WARNING;
        }
    }

    private Map<ErrorCategory, List<String>> initializeCategoryActions() {
        Map<ErrorCategory, List<String>> actions = new HashMap<>();

        actions.put(ErrorCategory.NETWORK, Arrays.asList(
                "Check your internet connection",
                "Verify the repository URL is accessible",
                "Try again in a few minutes",
                "Check firewall settings"));

        actions.put(ErrorCategory.AUTHENTICATION, Arrays.asList(
                "Verify your credentials are correct",
                "Check if your access token is valid",
                "Ensure you have permission to access the repository",
                "Re-authenticate with the Git service"));

        actions.put(ErrorCategory.REPOSITORY, Arrays.asList(
                "Verify the repository exists and is accessible",
                "Check if the repository URL is correct",
                "Ensure the repository is not empty",
                "Try cloning the repository manually"));

        actions.put(ErrorCategory.FRAMEWORK_DETECTION, Arrays.asList(
                "Ensure the project has recognizable framework files",
                "Check if the project structure is standard",
                "Manually specify the framework type",
                "Review project dependencies"));

        actions.put(ErrorCategory.FILE_GENERATION, Arrays.asList(
                "Check available disk space",
                "Verify write permissions to the target directory",
                "Ensure the output path is valid",
                "Try generating files to a different location"));

        actions.put(ErrorCategory.VALIDATION, Arrays.asList(
                "Review the generated files for syntax errors",
                "Check if required tools are installed",
                "Validate configuration settings",
                "Skip validation if not critical"));

        actions.put(ErrorCategory.DOCKER, Arrays.asList(
                "Ensure Docker is installed and running",
                "Check Docker daemon status",
                "Verify Docker permissions",
                "Try restarting Docker service"));

        actions.put(ErrorCategory.KUBERNETES, Arrays.asList(
                "Install kubectl if not present",
                "Check Kubernetes cluster connectivity",
                "Verify kubeconfig settings",
                "Test cluster access manually"));

        actions.put(ErrorCategory.HELM, Arrays.asList(
                "Install Helm if not present",
                "Initialize Helm repository",
                "Check Helm version compatibility",
                "Verify chart syntax"));

        actions.put(ErrorCategory.CONFIGURATION, Arrays.asList(
                "Review configuration settings",
                "Check for required parameters",
                "Validate configuration format",
                "Reset to default configuration"));

        actions.put(ErrorCategory.SYSTEM, Arrays.asList(
                "Restart the application",
                "Check system resources",
                "Review application logs",
                "Contact system administrator"));

        actions.put(ErrorCategory.UNKNOWN, Arrays.asList(
                "Review error details carefully",
                "Check application logs",
                "Try the operation again",
                "Contact support if the issue persists"));

        return actions;
    }

    private Map<ErrorCategory, List<String>> initializeCategoryDocumentation() {
        Map<ErrorCategory, List<String>> docs = new HashMap<>();

        docs.put(ErrorCategory.NETWORK, Arrays.asList(
                "Network troubleshooting guide",
                "Proxy configuration documentation",
                "Firewall setup guide"));

        docs.put(ErrorCategory.AUTHENTICATION, Arrays.asList(
                "Git authentication setup guide",
                "Personal access token creation",
                "SSH key configuration"));

        docs.put(ErrorCategory.DOCKER, Arrays.asList(
                "Docker installation guide",
                "Docker troubleshooting",
                "Dockerfile best practices"));

        docs.put(ErrorCategory.KUBERNETES, Arrays.asList(
                "Kubernetes setup guide",
                "kubectl installation",
                "Kubernetes manifest validation"));

        docs.put(ErrorCategory.HELM, Arrays.asList(
                "Helm installation guide",
                "Helm chart development",
                "Helm troubleshooting"));

        return docs;
    }

    private Map<ErrorCategory, String> initializeCategoryHelpText() {
        Map<ErrorCategory, String> helpText = new HashMap<>();

        helpText.put(ErrorCategory.NETWORK,
                "Network errors occur when the application cannot connect to remote repositories or services. " +
                        "This is often due to connectivity issues, firewall restrictions, or server problems.");

        helpText.put(ErrorCategory.AUTHENTICATION,
                "Authentication errors happen when credentials are invalid, expired, or insufficient. " +
                        "Check your access tokens, passwords, and repository permissions.");

        helpText.put(ErrorCategory.REPOSITORY,
                "Repository errors occur when there are issues accessing or processing Git repositories. " +
                        "Verify the repository exists, is accessible, and has the expected structure.");

        helpText.put(ErrorCategory.DOCKER,
                "Docker errors happen when Docker operations fail. This could be due to Docker not being " +
                        "installed, not running, or configuration issues.");

        helpText.put(ErrorCategory.KUBERNETES,
                "Kubernetes errors occur during manifest generation or validation. Ensure kubectl is " +
                        "installed and properly configured.");

        helpText.put(ErrorCategory.HELM,
                "Helm errors happen during chart generation or validation. Make sure Helm is installed " +
                        "and the chart syntax is correct.");

        return helpText;
    }

    private Map<Pattern, List<String>> initializeCommonSolutions() {
        Map<Pattern, List<String>> solutions = new HashMap<>();

        solutions.put(Pattern.compile("connection.*refused"), Arrays.asList(
                "Check if the service is running",
                "Verify the correct port is being used",
                "Check firewall settings"));

        solutions.put(Pattern.compile("timeout"), Arrays.asList(
                "Increase timeout settings",
                "Check network connectivity",
                "Try again later"));

        solutions.put(Pattern.compile("permission.*denied"), Arrays.asList(
                "Check file/directory permissions",
                "Run with appropriate privileges",
                "Verify access rights"));

        solutions.put(Pattern.compile("not.*found"), Arrays.asList(
                "Check if the file/resource exists",
                "Verify the path is correct",
                "Install missing dependencies"));

        solutions.put(Pattern.compile("unauthorized"), Arrays.asList(
                "Check authentication credentials",
                "Verify access permissions",
                "Re-authenticate if needed"));

        return solutions;
    }
}