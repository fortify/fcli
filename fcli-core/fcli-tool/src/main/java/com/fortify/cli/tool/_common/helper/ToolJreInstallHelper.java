/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.tool._common.helper;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.JreHelper;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor.JreSource;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;

import lombok.Builder;
import lombok.Data;

/**
 * Helper class for managing JRE detection and configuration during tool installation.
 * Handles auto-detection from environment variables when explicit JRE options are not provided.
 */
public class ToolJreInstallHelper {
    
    /**
     * Determines JRE configuration for tool installation based on user options and environment.
     * 
     * @param config Configuration parameters including explicit JRE path, env var prefixes, and version info
     * @return JRE configuration result indicating source and path
     */
    public static JreInstallResult determineJreConfig(JreInstallConfig config) {
        // 1. Explicit JRE path provided by user (--jre option)
        if (config.explicitJrePath != null) {
            validateJreCompatibility(config.explicitJrePath, config.versionDescriptor, config.toolName);
            return JreInstallResult.builder()
                .jreSource(JreSource.EXPLICIT)
                .jrePath(config.explicitJrePath.toAbsolutePath().normalize().toString())
                .build();
        }
        
        // 2. Embedded JRE requested (--with-jre option)
        if (config.requestEmbeddedJre) {
            return JreInstallResult.builder()
                .jreSource(JreSource.EMBEDDED)
                .jrePath(config.embeddedJrePath.toAbsolutePath().normalize().toString())
                .build();
        }
        
        // 3. Auto-detect from environment variables
        var detectionResult = JreHelper.findJavaHomeFromEnv(
            config.envVarPrefixes,
            getCompatibleJavaVersions(config.versionDescriptor)
        );
        
        if (detectionResult != null) {
            // Validate that Java actually exists at the detected path
            Path detectedJrePath = Path.of(detectionResult.getJavaHome());
            if (isJavaExecutablePresent(detectedJrePath)) {
                return JreInstallResult.builder()
                    .jreSource(JreSource.ENV_VAR)
                    .jrePath(detectionResult.getJavaHome())
                    .jreEnvVarName(detectionResult.getEnvVarName())
                    .build();
            }
            // If detected path is invalid, fall through to install embedded JRE
        }
        
        // 4. No JRE found - use embedded JRE unless explicitly skipped with --no-with-jre
        if (config.skipEmbeddedJre) {
            // User explicitly disabled embedded JRE, don't store any JRE path
            // Runtime will use ENV_VAR logic (including JAVA_HOME and PATH fallback)
            return JreInstallResult.builder()
                .jreSource(JreSource.ENV_VAR)
                .jrePath(null)
                .build();
        }
        
        // Default to embedded JRE
        return JreInstallResult.builder()
            .jreSource(JreSource.EMBEDDED)
            .jrePath(config.embeddedJrePath.toAbsolutePath().normalize().toString())
            .requiresEmbeddedJreInstall(true)
            .build();
    }
    
    /**
     * Validates that provided JRE is compatible with tool's requirements.
     */
    private static void validateJreCompatibility(Path jrePath, ToolDefinitionVersionDescriptor versionDescriptor, String toolName) {
        String requiredJreVersion = getRequiredJreVersion(versionDescriptor, toolName);
        String detectedVersion = ToolJavaVersionHelper.detectJavaVersion(jrePath);
        
        if (!ToolJavaVersionHelper.isCompatibleVersion(detectedVersion, requiredJreVersion)) {
            throw new FcliSimpleException(String.format(
                "Incompatible JRE version. %s requires Java %s, but provided JRE at '%s' is version %s",
                toolName,
                requiredJreVersion,
                jrePath,
                detectedVersion != null ? detectedVersion : "unknown"
            ));
        }
    }
    
    /**
     * Checks if a Java executable exists at the given JRE path.
     */
    private static boolean isJavaExecutablePresent(Path jrePath) {
        if (jrePath == null || !Files.exists(jrePath)) {
            return false;
        }
        var javaExecutable = jrePath.resolve("bin").resolve(
            com.fortify.cli.common.util.PlatformHelper.isWindows() ? "java.exe" : "java"
        );
        return Files.exists(javaExecutable) && Files.isRegularFile(javaExecutable);
    }
    
    /**
     * Extracts required JRE version from tool version descriptor.
     */
    private static String getRequiredJreVersion(ToolDefinitionVersionDescriptor versionDescriptor, String toolName) {
        var extraProperties = versionDescriptor.getExtraProperties();
        var jreVersion = extraProperties == null ? null : extraProperties.get("jre");
        if (StringUtils.isBlank(jreVersion)) {
            throw new FcliSimpleException(String.format(
                "Tool definitions don't list JRE version for %s; cannot validate JRE compatibility",
                toolName
            ));
        }
        return jreVersion;
    }
    
    /**
     * Determines compatible Java versions based on minimum required version.
     * Returns versions in descending order (newest first).
     */
    private static String[] getCompatibleJavaVersions(ToolDefinitionVersionDescriptor versionDescriptor) {
        var extraProperties = versionDescriptor.getExtraProperties();
        var jreVersion = extraProperties == null ? null : extraProperties.get("jre");
        
        if (StringUtils.isBlank(jreVersion)) {
            // Fallback for versions without JRE spec
            return new String[]{"21", "17", "11", "8"};
        }
        
        int required = Integer.parseInt(jreVersion);
        if (required >= 17) return new String[]{"21", "17"};
        if (required >= 11) return new String[]{"21", "17", "11"};
        return new String[]{"21", "17", "11", "8"};
    }
    
    /**
     * Configuration for JRE installation detection.
     */
    @Data @Builder
    public static class JreInstallConfig {
        private final Path explicitJrePath;
        private final boolean requestEmbeddedJre;
        private final boolean skipEmbeddedJre;
        private final Path embeddedJrePath;
        private final String[] envVarPrefixes;
        private final ToolDefinitionVersionDescriptor versionDescriptor;
        private final String toolName;
    }
    
    /**
     * Result of JRE installation configuration.
     */
    @Data @Builder
    public static class JreInstallResult {
        private final JreSource jreSource;
        private final String jrePath;
        private final String jreEnvVarName;
        @Builder.Default
        private final boolean requiresEmbeddedJreInstall = false;
    }
}
