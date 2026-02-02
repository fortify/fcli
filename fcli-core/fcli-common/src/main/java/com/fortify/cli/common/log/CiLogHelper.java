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
package com.fortify.cli.common.log;

import java.io.File;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand.LogLevel;
import com.fortify.cli.common.util.EnvHelper;

/**
 * Helper class for CI-aware logging configuration.
 * Provides automatic detection of CI debug settings and standardized log directory management.
 * 
 * <p>This class supports the following environment variables:
 * <ul>
 *   <li>FCLI_DEBUG - Enable debug logging (true/1)</li>
 *   <li>FCLI_LOG_LEVEL - Set log level (TRACE/DEBUG/INFO/WARN/ERROR/NONE)</li>
 *   <li>FCLI_LOG_FILE - Set explicit log file path</li>
 *   <li>FCLI_LOG_DIR - Set directory for log files (default: current directory)</li>
 * </ul>
 * 
 * <p>CI-specific debug detection:
 * <ul>
 *   <li>GitHub Actions: ACTIONS_STEP_DEBUG, RUNNER_DEBUG</li>
 *   <li>GitLab CI: CI_DEBUG_TRACE</li>
 *   <li>Azure DevOps: SYSTEM_DEBUG</li>
 *   <li>Bitbucket Pipelines: BITBUCKET_PIPELINES_DEBUG_MODE</li>
 * </ul>
 * 
 * @author Ruud Senden
 */
public final class CiLogHelper {
    // Environment variable names for fcli logging
    public static final String ENV_FCLI_DEBUG = "FCLI_DEBUG";
    public static final String ENV_FCLI_LOG_LEVEL = "FCLI_LOG_LEVEL";
    public static final String ENV_FCLI_LOG_FILE = "FCLI_LOG_FILE";
    public static final String ENV_FCLI_LOG_DIR = "FCLI_LOG_DIR";
    
    // CI-specific debug environment variables
    private static final String ENV_GITHUB_ACTIONS_STEP_DEBUG = "ACTIONS_STEP_DEBUG";
    private static final String ENV_GITHUB_RUNNER_DEBUG = "RUNNER_DEBUG";
    private static final String ENV_GITLAB_DEBUG_TRACE = "CI_DEBUG_TRACE";
    private static final String ENV_ADO_SYSTEM_DEBUG = "SYSTEM_DEBUG";
    private static final String ENV_BITBUCKET_DEBUG_MODE = "BITBUCKET_PIPELINES_DEBUG_MODE";
    
    // Default log file name
    private static final String DEFAULT_LOG_FILE_NAME = "fcli.log";
    
    private CiLogHelper() {}
    
    /**
     * Check if debug mode should be enabled based on environment variables.
     * Checks both FCLI_DEBUG and CI-specific debug variables.
     * 
     * @return true if debug mode should be enabled
     */
    public static boolean isDebugEnabledFromEnv() {
        // Check explicit FCLI_DEBUG first
        String fcliDebug = EnvHelper.env(ENV_FCLI_DEBUG);
        if (fcliDebug != null) {
            return EnvHelper.asBoolean(fcliDebug);
        }
        
        // Check CI-specific debug variables
        return isCiDebugEnabled();
    }
    
    /**
     * Check if any CI-specific debug mode is enabled.
     * 
     * @return true if any CI debug mode is detected
     */
    public static boolean isCiDebugEnabled() {
        return EnvHelper.asBoolean(EnvHelper.env(ENV_GITHUB_ACTIONS_STEP_DEBUG))
            || EnvHelper.asBoolean(EnvHelper.env(ENV_GITHUB_RUNNER_DEBUG))
            || EnvHelper.asBoolean(EnvHelper.env(ENV_GITLAB_DEBUG_TRACE))
            || EnvHelper.asBoolean(EnvHelper.env(ENV_ADO_SYSTEM_DEBUG))
            || EnvHelper.asBoolean(EnvHelper.env(ENV_BITBUCKET_DEBUG_MODE));
    }
    
    /**
     * Get the log level from environment variable if set.
     * 
     * @return LogLevel from FCLI_LOG_LEVEL environment variable, or null if not set
     */
    public static LogLevel getLogLevelFromEnv() {
        String logLevel = EnvHelper.env(ENV_FCLI_LOG_LEVEL);
        if (StringUtils.isBlank(logLevel)) {
            return null;
        }
        
        try {
            return LogLevel.valueOf(logLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Invalid log level - return null and let default handling occur
            return null;
        }
    }
    
    /**
     * Get the log file path from environment or apply defaults.
     * Priority order:
     * 1. FCLI_LOG_FILE (if absolute path or explicitly set)
     * 2. FCLI_LOG_DIR + default filename
     * 3. Current directory + default filename
     * 
     * @param explicitLogFile Log file specified via command-line option (may be null)
     * @return Resolved log file path
     */
    public static File resolveLogFile(File explicitLogFile) {
        // If log file explicitly provided via command line, use it
        if (explicitLogFile != null) {
            return explicitLogFile;
        }
        
        // Check FCLI_LOG_FILE environment variable
        String envLogFile = EnvHelper.env(ENV_FCLI_LOG_FILE);
        if (StringUtils.isNotBlank(envLogFile)) {
            return new File(envLogFile);
        }
        
        // Check FCLI_LOG_DIR and construct path
        String logDir = getLogDirFromEnv();
        if (logDir != null) {
            return new File(logDir, DEFAULT_LOG_FILE_NAME);
        }
        
        // Default to current directory
        return new File(DEFAULT_LOG_FILE_NAME);
    }
    
    /**
     * Get the log directory path from FCLI_LOG_DIR environment variable.
     * 
     * @return Log directory path or null if not set
     */
    public static String getLogDirFromEnv() {
        return EnvHelper.env(ENV_FCLI_LOG_DIR);
    }
    
    /**
     * Get the resolved log directory path for CI artifact collection.
     * This provides a standardized location for all fcli logs and related artifacts
     * that CI systems can easily archive.
     * 
     * @return Absolute path to the log directory
     */
    public static Path getLogDir() {
        String logDir = getLogDirFromEnv();
        if (logDir != null) {
            return Path.of(logDir).toAbsolutePath();
        }
        return Path.of(".").toAbsolutePath();
    }
    
    /**
     * Get information about the current CI debug configuration.
     * Useful for diagnostic output.
     * 
     * @return Human-readable description of debug configuration source
     */
    public static String getDebugConfigSource() {
        if (EnvHelper.env(ENV_FCLI_DEBUG) != null) {
            return "FCLI_DEBUG environment variable";
        }
        
        if (EnvHelper.asBoolean(EnvHelper.env(ENV_GITHUB_ACTIONS_STEP_DEBUG))) {
            return "GitHub Actions ACTIONS_STEP_DEBUG";
        }
        
        if (EnvHelper.asBoolean(EnvHelper.env(ENV_GITHUB_RUNNER_DEBUG))) {
            return "GitHub Actions RUNNER_DEBUG";
        }
        
        if (EnvHelper.asBoolean(EnvHelper.env(ENV_GITLAB_DEBUG_TRACE))) {
            return "GitLab CI_DEBUG_TRACE";
        }
        
        if (EnvHelper.asBoolean(EnvHelper.env(ENV_ADO_SYSTEM_DEBUG))) {
            return "Azure DevOps SYSTEM_DEBUG";
        }
        
        if (EnvHelper.asBoolean(EnvHelper.env(ENV_BITBUCKET_DEBUG_MODE))) {
            return "Bitbucket BITBUCKET_PIPELINES_DEBUG_MODE";
        }
        
        return "none (default)";
    }
}
