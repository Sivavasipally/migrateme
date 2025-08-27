package com.example.gitmigrator;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Comprehensive test suite for the Git Migrator application.
 * This suite runs all unit tests, integration tests, and component tests.
 */
@Suite
@SuiteDisplayName("Git Migrator Comprehensive Test Suite")
@SelectPackages({
    "com.example.gitmigrator.service",
    "com.example.gitmigrator.controller",
    "com.example.gitmigrator.integration",
    "com.example.gitmigrator.model"
})
@IncludeClassNamePatterns({
    ".*Test.*",
    ".*Tests.*"
})
public class TestSuite {
    // Test suite configuration class
    // All tests are discovered and run automatically based on annotations
}