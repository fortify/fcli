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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.EnvHelper;
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
    private static final Logger LOG = LoggerFactory.getLogger(ToolJreInstallHelper.class);
    
    /**
     * Determines JRE configuration for tool installation based on user options and environment.
     * 
     * @param config Configuration parameters including explicit JRE path, env var prefixes, and version info
     * @return JRE configuration result indicating source and path
     */
    public static JreInstallResult determineJreConfig(JreInstallConfig config) {
        LOG.debug("Determining JRE configuration for {} installation", config.toolName);
        
        // 1. Explicit JRE path provided by user (--jre option)
        if (config.explicitJrePath != null) {
            LOG.debug("Using explicit JRE path: {}", config.explicitJrePath);
            validateJreCompatibility(config.explicitJrePath, config.versionDescriptor, config.toolName);
            return JreInstallResult.builder()
                .jreSource(JreSource.EXPLICIT)
                .jrePath(config.explicitJrePath.toAbsolutePath().normalize().toString())
                .build();
        }
        
        // 2. Embedded JRE requested (--with-jre option)
        if (config.requestEmbeddedJre) {
            LOG.debug("Embedded JRE explicitly requested");
            return JreInstallResult.builder()
                .jreSource(JreSource.EMBEDDED)
                .jrePath(config.embeddedJrePath.toAbsolutePath().normalize().toString())
                .build();
        }
        
        // 3. Check tool-specific environment variables (fail immediately if set but invalid)
        if (config.envVarPrefixes != null) {
            for (String prefix : config.envVarPrefixes) {
                String envVarName = prefix + "_JAVA_HOME";
                String javaHome = EnvHelper.env(envVarName);
                if (StringUtils.isNotBlank(javaHome)) {
                    LOG.debug("Checking tool-specific env var {}={}", envVarName, javaHome);
                    Path jrePath = Path.of(javaHome);
                    Path javaExecutable = JreHelper.findJavaExecutable(jrePath);
                    
                    if (javaExecutable == null) {
                        throw new FcliSimpleException(String.format(
                            "Environment variable %s is set to '%s', but no java executable found. " +
                            "Please ensure the path points to a valid JRE/JDK with java in bin/ or jre/bin/ subdirectory.",
                            envVarName, javaHome
                        ));
                    }
                    
                    String validationError = validateJavaExecutable(javaExecutable);
                    if (validationError != null) {
                        throw new FcliSimpleException(String.format(
                            "Environment variable %s points to '%s', but java executable is not usable: %s",
                            envVarName, javaHome, validationError
                        ));
                    }
                    
                    LOG.debug("Successfully validated Java at {} from {}", javaExecutable, envVarName);
                    return JreInstallResult.builder()
                        .jreSource(JreSource.ENV_VAR)
                        .jrePath(jrePath.toAbsolutePath().normalize().toString())
                        .jreEnvVarName(envVarName)
                        .build();
                }
            }
        }
        
        // 4. Check version-specific environment variables (with fallback chain)
        String[] compatibleVersions = getCompatibleJavaVersions(config.versionDescriptor);
        if (compatibleVersions != null) {
            LOG.debug("Searching for compatible Java versions: {}", String.join(", ", compatibleVersions));
            String osArch = System.getProperty("os.arch", "").toUpperCase();
            
            for (String version : compatibleVersions) {
                // Try JAVA_HOME_<version>_<arch> with raw os.arch value
                if (StringUtils.isNotBlank(osArch)) {
                    String envVarName = "JAVA_HOME_" + version + "_" + osArch;
                    JreInstallResult result = tryVersionEnvVar(envVarName, version);
                    if (result != null) return result;
                }
                
                // Try GitHub Actions-style patterns (e.g., JAVA_HOME_17_X64)
                {
                    String githubActionsArch = com.fortify.cli.common.util.PlatformHelper.getGitHubActionsArchSuffix();
                    String envVarName = "JAVA_HOME_" + version + "_" + githubActionsArch;
                    JreInstallResult result = tryVersionEnvVar(envVarName, version);
                    if (result != null) return result;
                }
                
                // Try JAVA_HOME_<version>
                String envVarName = "JAVA_HOME_" + version;
                JreInstallResult result = tryVersionEnvVar(envVarName, version);
                if (result != null) return result;
            }
        }
        
        // 5. No valid JRE found - use embedded JRE unless explicitly skipped with --no-with-jre
        if (config.skipEmbeddedJre) {
            LOG.debug("No JRE found in environment variables and embedded JRE disabled. Runtime will use JAVA_HOME/PATH fallback.");
            // User explicitly disabled embedded JRE, don't store any JRE path
            // Runtime will use ENV_VAR logic (including JAVA_HOME and PATH fallback)
            return JreInstallResult.builder()
                .jreSource(JreSource.ENV_VAR)
                .jrePath(null)
                .build();
        }
        
        // Default to embedded JRE
        LOG.debug("No valid JRE found in environment. Will install embedded JRE at {}", config.embeddedJrePath);
        return JreInstallResult.builder()
            .jreSource(JreSource.EMBEDDED)
            .jrePath(config.embeddedJrePath.toAbsolutePath().normalize().toString())
            .requiresEmbeddedJreInstall(true)
            .build();
    }
    
    /**
     * Tries to use a version-specific environment variable. Returns null if not set or invalid.
     * Logs warnings for invalid configurations but continues (allows fallback to next version).
     */
    private static JreInstallResult tryVersionEnvVar(String envVarName, String version) {
        String javaHome = EnvHelper.env(envVarName);
        if (StringUtils.isBlank(javaHome)) {
            return null; // Not set, try next
        }
        
        LOG.debug("Checking version-specific env var {}={}", envVarName, javaHome);
        Path jrePath = Path.of(javaHome);
        Path javaExecutable = JreHelper.findJavaExecutable(jrePath);
        
        if (javaExecutable == null) {
            LOG.warn("Environment variable {} is set to '{}', but no java executable found. Trying next version...", 
                envVarName, javaHome);
            return null;
        }
        
        String validationError = validateJavaExecutable(javaExecutable);
        if (validationError != null) {
            LOG.warn("Environment variable {} points to '{}', but java executable is not usable: {}. Trying next version...",
                envVarName, javaHome, validationError);
            return null;
        }
        
        LOG.debug("Successfully validated Java at {} from {}", javaExecutable, envVarName);
        return JreInstallResult.builder()
            .jreSource(JreSource.ENV_VAR)
            .jrePath(jrePath.toAbsolutePath().normalize().toString())
            .jreEnvVarName(envVarName)
            .build();
    }
    
    /**
     * Validates that a Java executable is actually runnable by executing 'java -version'.
     * Returns null if valid, or an error message describing the problem.
     */
    private static String validateJavaExecutable(Path javaExecutable) {
        if (javaExecutable == null || !Files.exists(javaExecutable)) {
            return "File does not exist";
        }
        
        if (!Files.isRegularFile(javaExecutable)) {
            return "Not a regular file";
        }
        
        if (!Files.isExecutable(javaExecutable)) {
            return "File is not executable (check permissions)";
        }
        
        // Try to actually run java -version to ensure it's compatible with the platform
        try {
            LOG.debug("Validating Java executable: {}", javaExecutable);
            ProcessBuilder pb = new ProcessBuilder(javaExecutable.toString(), "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "Process timed out after 5 seconds";
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return String.format("Process exited with code %d (may be incompatible with platform, e.g., Alpine musl vs glibc)", exitCode);
            }
            
            LOG.debug("Java executable validation succeeded");
            return null; // Success
            
        } catch (Exception e) {
            return String.format("Failed to execute: %s", e.getMessage());
        }
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
