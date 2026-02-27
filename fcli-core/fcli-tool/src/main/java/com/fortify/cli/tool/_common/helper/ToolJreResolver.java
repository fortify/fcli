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
import com.fortify.cli.common.util.JreHelper;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor.JreSource;

import lombok.Builder;
import lombok.Data;

/**
 * Helper class for resolving JRE location at runtime based on installation configuration.
 * Provides smart fallback logic and handles JRE upgrades for ENV_VAR sources.
 */
public class ToolJreResolver {
    private static final Logger LOG = LoggerFactory.getLogger(ToolJreResolver.class);
    
    /**
     * Resolves Java executable path for running a tool.
     * 
     * Strategy:
     * - EMBEDDED: Always use embedded JRE only
     * - EXPLICIT: Use stored path only, error if missing
     * - ENV_VAR or null: Check env vars first (handles upgrades), then stored path
     * 
     * @param config Configuration including descriptor, env prefixes, compatible versions
     * @return Resolution result with java command path and detected JAVA_HOME
     */
    public static JreResolveResult resolveJavaCommand(JreResolveConfig config) {
        JreSource source = config.descriptor.getJreSource();
        LOG.debug("Resolving Java command with JRE source: {}", source);
        
        // 1. EMBEDDED: Always use embedded JRE
        if (source == JreSource.EMBEDDED) {
            return resolveEmbeddedJre(config);
        }
        
        // 2. EXPLICIT: Use stored path only, error if missing
        if (source == JreSource.EXPLICIT) {
            return resolveExplicitJre(config);
        }
        
        // 3. ENV_VAR or legacy (null): Check env vars first, then stored path
        return resolveWithEnvFallback(config);
    }
    
    private static JreResolveResult resolveEmbeddedJre(JreResolveConfig config) {
        Path embeddedJre = config.descriptor.getInstallPath().resolve("jre");
        LOG.debug("Checking for embedded JRE at: {}", embeddedJre);
        Path javaExec = resolveJavaExecutable(embeddedJre, config.javaExecutableName);
        
        if (javaExec != null && Files.exists(javaExec)) {
            LOG.debug("Found embedded Java executable: {}", javaExec);
            return JreResolveResult.builder()
                .javaCommand(javaExec.toString())
                .javaHome(embeddedJre.toString())
                .build();
        }
        
        // Embedded JRE missing - fallback to PATH
        LOG.debug("Embedded JRE not found, falling back to PATH");
        return JreResolveResult.builder()
            .javaCommand(config.javaExecutableName)
            .build();
    }
    
    private static JreResolveResult resolveExplicitJre(JreResolveConfig config) {
        String storedPath = config.descriptor.getJreHome();
        LOG.debug("Using explicit JRE path: {}", storedPath);
        
        if (StringUtils.isBlank(storedPath)) {
            throw new FcliSimpleException(
                "JRE configuration is missing. Please reinstall with --jre option."
            );
        }
        
        Path javaExec = resolveJavaExecutable(Path.of(storedPath), config.javaExecutableName);
        
        if (javaExec == null || !Files.exists(javaExec)) {
            throw new FcliSimpleException(String.format(
                "JRE explicitly configured at %s no longer exists. " +
                "Please reinstall with updated --jre option or without --jre to enable auto-detection.",
                storedPath
            ));
        }
        
        LOG.debug("Found explicit Java executable: {}", javaExec);
        return JreResolveResult.builder()
            .javaCommand(javaExec.toString())
            .javaHome(storedPath)
            .build();
    }
    
    private static JreResolveResult resolveWithEnvFallback(JreResolveConfig config) {
        LOG.debug("Resolving JRE with environment variable fallback (stored path: {}, env var: {})", 
            config.descriptor.getJreHome(), config.descriptor.getJreEnvVar());
        
        // First try environment variables (handles upgrades)
        var detectionResult = JreHelper.findJavaHomeWithFallbacks(
            config.envVarPrefixes,
            config.compatibleVersions,
            config.includeGenericJavaHome
        );
        
        if (detectionResult != null) {
            LOG.debug("Environment variable check found: {}={}", 
                detectionResult.getEnvVarName(), detectionResult.getJavaHome());
            Path javaExec = resolveJavaExecutable(
                Path.of(detectionResult.getJavaHome()), 
                config.javaExecutableName
            );
            
            if (javaExec != null && Files.exists(javaExec)) {
                LOG.debug("Using Java from environment: {}", javaExec);
                return JreResolveResult.builder()
                    .javaCommand(javaExec.toString())
                    .javaHome(detectionResult.getJavaHome())
                    .build();
            } else {
                LOG.debug("Java executable not found at detected path, trying stored path");
            }
        }
        
        // Fallback to stored path (if env vars didn't work)
        String storedPath = config.descriptor.getJreHome();
        if (StringUtils.isNotBlank(storedPath)) {
            LOG.debug("Checking stored path: {}", storedPath);
            Path javaExec = resolveJavaExecutable(Path.of(storedPath), config.javaExecutableName);
            
            if (javaExec != null && Files.exists(javaExec)) {
                LOG.debug("Using Java from stored path: {}", javaExec);
                return JreResolveResult.builder()
                    .javaCommand(javaExec.toString())
                    .javaHome(storedPath)
                    .build();
            } else {
                LOG.debug("Java executable not found at stored path, falling back to PATH");
            }
        }
        
        // Final fallback to PATH
        LOG.debug("Using java from PATH: {}", config.javaExecutableName);
        return JreResolveResult.builder()
            .javaCommand(config.javaExecutableName)
            .build();
    }
    
    private static Path resolveJavaExecutable(Path javaHome, String executableName) {
        if (javaHome == null) return null;
        
        // Check bin subdirectory first
        Path javaExec = javaHome.resolve("bin").resolve(executableName);
        if (Files.exists(javaExec)) {
            return javaExec;
        }
        
        // Check jre/bin subdirectory (older JDK layouts)
        Path jreBinExec = javaHome.resolve("jre").resolve("bin").resolve(executableName);
        if (Files.exists(jreBinExec)) {
            return jreBinExec;
        }
        
        return null;
    }
    
    /**
     * Configuration for JRE resolution at runtime.
     */
    @Data @Builder
    public static class JreResolveConfig {
        private final ToolInstallationDescriptor descriptor;
        private final String[] envVarPrefixes;
        private final String[] compatibleVersions;
        private final String javaExecutableName;
        @Builder.Default
        private final boolean includeGenericJavaHome = true;
    }
    
    /**
     * Result of JRE resolution containing java command and home paths.
     */
    @Data @Builder
    public static class JreResolveResult {
        private final String javaCommand;
        private final String javaHome;
    }
}
