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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand.LogLevel;
import com.fortify.cli.common.util.EnvHelper;

class CiLogHelperTest {
    
    @AfterEach
    void clearTestEnvVars() {
        // Clear any test environment variables set via system properties
        System.clearProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_DEBUG));
        System.clearProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_LOG_LEVEL));
        System.clearProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_LOG_FILE));
        System.clearProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_LOG_DIR));
        System.clearProperty(EnvHelper.envSystemPropertyName("ACTIONS_STEP_DEBUG"));
        System.clearProperty(EnvHelper.envSystemPropertyName("RUNNER_DEBUG"));
        System.clearProperty(EnvHelper.envSystemPropertyName("CI_DEBUG_TRACE"));
        System.clearProperty(EnvHelper.envSystemPropertyName("SYSTEM_DEBUG"));
        System.clearProperty(EnvHelper.envSystemPropertyName("BITBUCKET_PIPELINES_DEBUG_MODE"));
    }
    
    @Test
    @DisplayName("isDebugEnabledFromEnv returns false when no debug env vars set")
    void testNoDebugEnvVars() {
        assertFalse(CiLogHelper.isDebugEnabledFromEnv());
    }
    
    @Test
    @DisplayName("isDebugEnabledFromEnv detects FCLI_DEBUG=true")
    void testFcliDebugTrue() {
        System.setProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_DEBUG), "true");
        assertTrue(CiLogHelper.isDebugEnabledFromEnv());
    }
    
    @Test
    @DisplayName("isDebugEnabledFromEnv detects FCLI_DEBUG=1")
    void testFcliDebugOne() {
        System.setProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_DEBUG), "1");
        assertTrue(CiLogHelper.isDebugEnabledFromEnv());
    }
    
    @Test
    @DisplayName("isDebugEnabledFromEnv detects GitHub ACTIONS_STEP_DEBUG")
    void testGitHubActionsStepDebug() {
        System.setProperty(EnvHelper.envSystemPropertyName("ACTIONS_STEP_DEBUG"), "true");
        assertTrue(CiLogHelper.isDebugEnabledFromEnv());
        assertTrue(CiLogHelper.isCiDebugEnabled());
    }
    
    @Test
    @DisplayName("isDebugEnabledFromEnv detects GitHub RUNNER_DEBUG")
    void testGitHubRunnerDebug() {
        System.setProperty(EnvHelper.envSystemPropertyName("RUNNER_DEBUG"), "1");
        assertTrue(CiLogHelper.isDebugEnabledFromEnv());
        assertTrue(CiLogHelper.isCiDebugEnabled());
    }
    
    @Test
    @DisplayName("isDebugEnabledFromEnv detects GitLab CI_DEBUG_TRACE")
    void testGitLabDebugTrace() {
        System.setProperty(EnvHelper.envSystemPropertyName("CI_DEBUG_TRACE"), "true");
        assertTrue(CiLogHelper.isDebugEnabledFromEnv());
        assertTrue(CiLogHelper.isCiDebugEnabled());
    }
    
    @Test
    @DisplayName("isDebugEnabledFromEnv detects Azure DevOps SYSTEM_DEBUG")
    void testAdoSystemDebug() {
        System.setProperty(EnvHelper.envSystemPropertyName("SYSTEM_DEBUG"), "true");
        assertTrue(CiLogHelper.isDebugEnabledFromEnv());
        assertTrue(CiLogHelper.isCiDebugEnabled());
    }
    
    @Test
    @DisplayName("isDebugEnabledFromEnv detects Bitbucket BITBUCKET_PIPELINES_DEBUG_MODE")
    void testBitbucketDebugMode() {
        System.setProperty(EnvHelper.envSystemPropertyName("BITBUCKET_PIPELINES_DEBUG_MODE"), "1");
        assertTrue(CiLogHelper.isDebugEnabledFromEnv());
        assertTrue(CiLogHelper.isCiDebugEnabled());
    }
    
    @Test
    @DisplayName("getLogLevelFromEnv returns null when not set")
    void testLogLevelNotSet() {
        assertNull(CiLogHelper.getLogLevelFromEnv());
    }
    
    @Test
    @DisplayName("getLogLevelFromEnv parses valid log levels")
    void testLogLevelParsing() {
        System.setProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_LOG_LEVEL), "TRACE");
        assertEquals(LogLevel.TRACE, CiLogHelper.getLogLevelFromEnv());
        
        System.setProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_LOG_LEVEL), "debug");
        assertEquals(LogLevel.DEBUG, CiLogHelper.getLogLevelFromEnv());
        
        System.setProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_LOG_LEVEL), "INFO");
        assertEquals(LogLevel.INFO, CiLogHelper.getLogLevelFromEnv());
    }
    
    @Test
    @DisplayName("getLogLevelFromEnv handles invalid log levels")
    void testInvalidLogLevel() {
        System.setProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_LOG_LEVEL), "INVALID");
        assertNull(CiLogHelper.getLogLevelFromEnv());
    }
    
    @Test
    @DisplayName("resolveLogFile returns explicit file when provided")
    void testResolveExplicitLogFile() {
        File explicitFile = new File("/path/to/custom.log");
        assertEquals(explicitFile, CiLogHelper.resolveLogFile(explicitFile));
    }
    
    @Test
    @DisplayName("resolveLogFile uses FCLI_LOG_FILE when no explicit file")
    void testResolveFcliLogFile() {
        System.setProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_LOG_FILE), "/logs/custom.log");
        File resolved = CiLogHelper.resolveLogFile(null);
        assertEquals(new File("/logs/custom.log"), resolved);
    }
    
    @Test
    @DisplayName("resolveLogFile uses FCLI_LOG_DIR with default filename")
    void testResolveFcliLogDir() {
        System.setProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_LOG_DIR), "/var/logs");
        File resolved = CiLogHelper.resolveLogFile(null);
        assertEquals(new File("/var/logs/fcli.log"), resolved);
    }
    
    @Test
    @DisplayName("resolveLogFile defaults to current directory")
    void testResolveDefaultLogFile() {
        File resolved = CiLogHelper.resolveLogFile(null);
        assertEquals(new File("fcli.log"), resolved);
    }
    
    @Test
    @DisplayName("getLogDir returns FCLI_LOG_DIR when set")
    void testGetLogDirFromEnv() {
        System.setProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_LOG_DIR), "/var/logs");
        assertEquals("/var/logs", CiLogHelper.getLogDirFromEnv());
        assertTrue(CiLogHelper.getLogDir().toString().endsWith("/var/logs"));
    }
    
    @Test
    @DisplayName("getLogDir returns current directory when not set")
    void testGetLogDirDefault() {
        assertNull(CiLogHelper.getLogDirFromEnv());
        assertNotNull(CiLogHelper.getLogDir());
    }
    
    @Test
    @DisplayName("getDebugConfigSource returns appropriate source description")
    void testGetDebugConfigSource() {
        // Default case
        assertEquals("none (default)", CiLogHelper.getDebugConfigSource());
        
        // FCLI_DEBUG
        System.setProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_DEBUG), "true");
        assertEquals("FCLI_DEBUG environment variable", CiLogHelper.getDebugConfigSource());
        System.clearProperty(EnvHelper.envSystemPropertyName(CiLogHelper.ENV_FCLI_DEBUG));
        
        // GitHub Actions
        System.setProperty(EnvHelper.envSystemPropertyName("ACTIONS_STEP_DEBUG"), "true");
        assertEquals("GitHub Actions ACTIONS_STEP_DEBUG", CiLogHelper.getDebugConfigSource());
        System.clearProperty(EnvHelper.envSystemPropertyName("ACTIONS_STEP_DEBUG"));
        
        // GitLab
        System.setProperty(EnvHelper.envSystemPropertyName("CI_DEBUG_TRACE"), "true");
        assertEquals("GitLab CI_DEBUG_TRACE", CiLogHelper.getDebugConfigSource());
    }
}
