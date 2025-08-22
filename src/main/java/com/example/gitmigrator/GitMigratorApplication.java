package com.example.gitmigrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Git Repository Migrator.
 * This application automates the modernization and migration of software repositories
 * from legacy deployment models to containerized OpenShift/Kubernetes deployments.
 */
@SpringBootApplication
public class GitMigratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitMigratorApplication.class, args);
    }
}