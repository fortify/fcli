/*
 * Copyright 2021-2025 Open Text.
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

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolRegisterCommand.ExitCode;

/**
 * Helper class for tool registration operations, providing auto-detection and path resolution.
 * 
 * @author Ruud Senden
 */
public class ToolRegistrationHelper {
    
    /**
     * Auto-detect tool binary by searching in priority order:
     * 1. fcli installation status
     * 2. <TOOL> environment variable (direct binary path)
     * 3. <TOOL_HOME> environment variable (install directory)
     * 4. PATH entries
     * 
     * @param toolName Tool identifier
     * @param binaryName Platform-specific binary name
     * @param toolEnvVar Environment variable for direct binary path (may be null)
     * @param toolHomeEnvVar Environment variable for install directory (may be null)
     * @return Detected tool binary file
     * @throws FcliSimpleException if tool not found
     */
    public static File autoDetectToolBinary(String toolName, String binaryName, String toolEnvVar, String toolHomeEnvVar) {
        // Priority 1: Check fcli installation status
        ToolInstallationDescriptor existing = ToolInstallationDescriptor.loadLastModified(toolName);
        if (existing != null && existing.getBinDir() != null) {
            File binary = new File(existing.getBinPath().resolve(binaryName).toString());
            if (binary.exists() && binary.canExecute()) {
                return binary;
            }
        }
        
        // Priority 2: Check <TOOL> env var (direct binary path)
        if (toolEnvVar != null) {
            String toolPath = EnvHelper.env(toolEnvVar);
            if (StringUtils.isNotBlank(toolPath)) {
                File binary = new File(toolPath);
                if (binary.exists() && binary.canExecute()) {
                    return binary;
                }
            }
        }
        
        // Priority 3: Check <TOOL_HOME> env var (install directory)
        if (toolHomeEnvVar != null) {
            String toolHomePath = EnvHelper.env(toolHomeEnvVar);
            if (StringUtils.isNotBlank(toolHomePath)) {
                File toolHome = new File(toolHomePath);
                File binary = new File(toolHome, "bin" + File.separator + binaryName);
                if (binary.exists() && binary.canExecute()) {
                    return binary;
                }
            }
        }
        
        // Priority 4: Scan PATH
        File binaryInPath = findBinaryInPath(binaryName);
        if (binaryInPath != null) {
            return binaryInPath;
        }
        
        throw new FcliSimpleException(
            toolName + " not found. Please specify --path or ensure tool is in PATH")
            .exitCode(ExitCode.TOOL_NOT_FOUND.getCode());
    }
    
    /**
     * Resolve binary from explicit path. Handles three cases:
     * 1. Direct binary file path
     * 2. Bin directory containing binary
     * 3. Install directory with bin/ subdirectory
     * 
     * @param path User-specified path
     * @param binaryName Platform-specific binary name
     * @return Resolved tool binary file
     * @throws FcliSimpleException if binary not found at specified path
     */
    public static File resolveBinaryFromExplicitPath(File path, String binaryName) {
        if (path.isFile()) {
            // Accept both executable binaries and JAR files
            if (path.canExecute() || path.getName().endsWith(".jar")) {
                return path;
            }
        }
        
        // Try as bin directory
        File binInPath = new File(path, binaryName);
        if (binInPath.exists() && (binInPath.canExecute() || binInPath.getName().endsWith(".jar"))) {
            return binInPath;
        }
        
        // Try as install directory
        File binSubdir = new File(path, "bin");
        File binInSubdir = new File(binSubdir, binaryName);
        if (binInSubdir.exists() && (binInSubdir.canExecute() || binInSubdir.getName().endsWith(".jar"))) {
            return binInSubdir;
        }
        
        throw new FcliSimpleException(
            "Tool binary not found at specified path: " + path.getAbsolutePath())
            .exitCode(ExitCode.INVALID_PATH.getCode());
    }
    
    /**
     * Resolve install directory from binary path.
     * If binary is in a bin/ directory, returns the parent directory.
     * Otherwise, returns the binary's parent directory.
     * 
     * @param toolBinary Tool binary file
     * @return Install directory
     */
    public static File resolveInstallDir(File toolBinary) {
        File binDir = toolBinary.getParentFile();
        if (binDir != null && "bin".equals(binDir.getName())) {
            File parentDir = binDir.getParentFile();
            return parentDir != null ? parentDir : binDir;
        }
        return binDir;
    }
    
    private static File findBinaryInPath(String binaryName) {
        String pathEnv = EnvHelper.env("PATH");
        if (pathEnv == null) return null;
        
        String[] pathDirs = pathEnv.split(File.pathSeparator);
        for (String pathDir : pathDirs) {
            File binary = new File(pathDir, binaryName);
            if (binary.exists() && binary.canExecute()) {
                return binary;
            }
        }
        return null;
    }
}
