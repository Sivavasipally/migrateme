package com.example.gitmigrator.service;

import com.example.gitmigrator.model.ValidationResult;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for validating generated migration artifacts.
 * Provides validation capabilities for Dockerfiles, Helm charts, and Kubernetes
 * manifests.
 */
public interface ValidationService {

    /**
     * Validates a Dockerfile for syntax and best practices.
     * 
     * @param dockerfile The Dockerfile to validate
     * @return ValidationResult containing validation status and any issues found
     */
    ValidationResult validateDockerfile(File dockerfile);

    /**
     * Validates a Helm chart directory for syntax and best practices.
     * 
     * @param chartDirectory The directory containing the Helm chart
     * @return ValidationResult containing validation status and any issues found
     */
    ValidationResult validateHelmChart(File chartDirectory);

    /**
     * Validates Kubernetes manifest files for syntax and best practices.
     * 
     * @param manifests List of Kubernetes manifest files to validate
     * @return ValidationResult containing validation status and any issues found
     */
    ValidationResult validateKubernetesManifests(List<File> manifests);

    /**
     * Validates a single Kubernetes manifest file.
     * 
     * @param manifest The Kubernetes manifest file to validate
     * @return ValidationResult containing validation status and any issues found
     */
    ValidationResult validateKubernetesManifest(File manifest);

    /**
     * Builds a Docker image from a Dockerfile to validate it can be built
     * successfully.
     * This is an asynchronous operation as Docker builds can take time.
     * 
     * @param dockerfile The Dockerfile to build
     * @param imageName  The name to give the built image
     * @return CompletableFuture with ValidationResult containing build status and
     *         any issues
     */
    CompletableFuture<ValidationResult> buildDockerImage(File dockerfile, String imageName);

    /**
     * Validates Docker Compose files for syntax and best practices.
     * 
     * @param composeFile The Docker Compose file to validate
     * @return ValidationResult containing validation status and any issues found
     */
    ValidationResult validateDockerCompose(File composeFile);

    /**
     * Validates multiple files of different types in a batch operation.
     * 
     * @param files List of files to validate (mixed types)
     * @return ValidationResult containing aggregated validation status and issues
     */
    ValidationResult validateFiles(List<File> files);

    /**
     * Checks if Docker is available and accessible for validation operations.
     * 
     * @return true if Docker is available, false otherwise
     */
    boolean isDockerAvailable();

    /**
     * Checks if Helm is available and accessible for validation operations.
     * 
     * @return true if Helm is available, false otherwise
     */
    boolean isHelmAvailable();

    /**
     * Checks if kubectl is available and accessible for validation operations.
     * 
     * @return true if kubectl is available, false otherwise
     */
    boolean isKubectlAvailable();

    /**
     * Gets the version of Docker if available.
     * 
     * @return Docker version string, or null if not available
     */
    String getDockerVersion();

    /**
     * Gets the version of Helm if available.
     * 
     * @return Helm version string, or null if not available
     */
    String getHelmVersion();

    /**
     * Gets the version of kubectl if available.
     * 
     * @return kubectl version string, or null if not available
     */
    String getKubectlVersion();

    /**
     * Validates the environment and tools required for validation operations.
     * 
     * @return ValidationResult containing environment validation status
     */
    ValidationResult validateEnvironment();
}