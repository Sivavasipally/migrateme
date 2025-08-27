package com.example.gitmigrator.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationConfigurationTest {

    @Test
    void shouldCreateDefaultConfiguration() {
        // When
        MigrationConfiguration config = new MigrationConfiguration();

        // Then
        assertThat(config.getTargetPlatform()).isEqualTo("kubernetes");
        assertThat(config.getOptionalComponents()).containsExactlyInAnyOrder("helm", "dockerfile");
        assertThat(config.getCustomSettings()).isEmpty();
        assertThat(config.isEnableValidation()).isTrue();
    }

    @Test
    void shouldSetAndGetTargetPlatform() {
        // Given
        MigrationConfiguration config = new MigrationConfiguration();

        // When
        config.setTargetPlatform("kubernetes");

        // Then
        assertThat(config.getTargetPlatform()).isEqualTo("kubernetes");
    }

    @Test
    void shouldManageOptionalComponents() {
        // Given
        MigrationConfiguration config = new MigrationConfiguration();
        Set<String> components = new HashSet<>();
        components.add("dockerfile");
        components.add("helm");
        components.add("cicd");

        // When
        config.setOptionalComponents(components);

        // Then
        assertThat(config.getOptionalComponents()).containsExactlyInAnyOrder("dockerfile", "helm", "cicd");
    }

    @Test
    void shouldManageCustomSettings() {
        // Given
        MigrationConfiguration config = new MigrationConfiguration();
        Map<String, String> customSettings = new HashMap<>();
        customSettings.put("base-image", "openjdk:17-jre-slim");
        customSettings.put("memory-limit", "512Mi");

        // When
        config.setCustomSettings(customSettings);

        // Then
        assertThat(config.getCustomSettings()).hasSize(2);
        assertThat(config.getCustomSettings().get("base-image")).isEqualTo("openjdk:17-jre-slim");
        assertThat(config.getCustomSettings().get("memory-limit")).isEqualTo("512Mi");
    }

    @Test
    void shouldManageComponentsWithConvenienceMethods() {
        // Given
        MigrationConfiguration config = new MigrationConfiguration();

        // When & Then - test individual component methods
        config.setIncludeHelm(true);
        assertThat(config.isIncludeHelm()).isTrue();
        assertThat(config.hasComponent("helm")).isTrue();

        config.setIncludeDockerfile(false);
        assertThat(config.isIncludeDockerfile()).isFalse();
        assertThat(config.hasComponent("dockerfile")).isFalse();

        config.setIncludeCiCd(true);
        assertThat(config.isIncludeCiCd()).isTrue();
        assertThat(config.hasComponent("cicd")).isTrue();

        config.setIncludeMonitoring(true);
        assertThat(config.isIncludeMonitoring()).isTrue();
        assertThat(config.hasComponent("monitoring")).isTrue();
    }

    @Test
    void shouldManageCustomSettings() {
        // Given
        MigrationConfiguration config = new MigrationConfiguration();

        // When
        config.setCustomSetting("base-image", "openjdk:17-jre-slim");
        config.setCustomSetting("memory-limit", "512Mi");
        config.setBaseImage("custom-image:latest");
        config.setResourceLimits("cpu=500m,memory=1Gi");

        // Then
        assertThat(config.getCustomSetting("base-image")).isEqualTo("custom-image:latest");
        assertThat(config.getCustomSetting("memory-limit")).isEqualTo("512Mi");
        assertThat(config.getBaseImage()).isEqualTo("custom-image:latest");
        assertThat(config.getResourceLimits()).isEqualTo("cpu=500m,memory=1Gi");
    }

    @Test
    void shouldConvertToString() {
        // Given
        MigrationConfiguration config = new MigrationConfiguration();
        config.setTargetPlatform("kubernetes");
        config.setTemplateName("web-app-template");

        // When
        String configString = config.toString();

        // Then
        assertThat(configString).contains("kubernetes");
        assertThat(configString).contains("web-app-template");
        assertThat(configString).contains("enableValidation=true");
    }

    @Test
    void shouldHandleNullAndEmptyCustomSettings() {
        // Given
        MigrationConfiguration config = new MigrationConfiguration();

        // When - set null and empty values
        config.setCustomSetting("null-value", null);
        config.setCustomSetting("empty-value", "");
        config.setCustomSetting("whitespace-value", "   ");
        config.setCustomSetting("valid-value", "test");

        // Then
        assertThat(config.getCustomSetting("null-value")).isNull();
        assertThat(config.getCustomSetting("empty-value")).isNull();
        assertThat(config.getCustomSetting("whitespace-value")).isNull();
        assertThat(config.getCustomSetting("valid-value")).isEqualTo("test");
    }
}