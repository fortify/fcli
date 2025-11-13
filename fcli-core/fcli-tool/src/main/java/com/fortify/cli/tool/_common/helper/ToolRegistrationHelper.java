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
     * Auto-detect tool binary location using multiple strategies in priority order:
     * 1. fcli installation descriptors
     * 2. {PREFIX}_CMD environment variables (direct binary/jar path)
     * 3. {PREFIX}_HOME environment variables (install directory)
     * 4. PATH entries
     * 
     * @param toolName Tool identifier
     * @param binaryName Platform-specific binary name
     * @param envPrefixes Environment variable prefixes to check (e.g., ["FCLI"], ["SCANCENTRAL", "SC_CLIENT"])
     * @return Detected tool binary file
     * @throws FcliSimpleException if tool not found
     */
    public static File autoDetectToolBinary(String toolName, String binaryName, String[] envPrefixes) {
        // Priority 1: Check fcli installation status
        ToolInstallationDescriptor existing = ToolInstallationDescriptor.loadLastModified(toolName);
        if (existing != null && existing.getBinDir() != null) {
            File binary = new File(existing.getBinPath().resolve(binaryName).toString());
            if (binary.exists() && (binary.canExecute() || binary.getName().endsWith(".jar"))) {
                return binary;
            }
        }
        
        // Priority 2: Check {PREFIX}_CMD env vars (direct binary/jar path)
        for (String prefix : envPrefixes) {
            String cmdEnvVar = prefix + "_CMD";
            String toolPath = EnvHelper.env(cmdEnvVar);
            if (StringUtils.isNotBlank(toolPath)) {
                File binary = new File(toolPath);
                if (binary.exists() && (binary.canExecute() || binary.getName().endsWith(".jar"))) {
                    return binary;
                }
            }
        }
        
        // Priority 3: Check {PREFIX}_HOME env vars (install directory)
        for (String prefix : envPrefixes) {
            String homeEnvVar = prefix + "_HOME";
            String toolHomePath = EnvHelper.env(homeEnvVar);
            if (StringUtils.isNotBlank(toolHomePath)) {
                File toolHome = new File(toolHomePath);
                // Try bin subdirectory first
                File binaryInBin = new File(toolHome, "bin" + File.separator + binaryName);
                if (binaryInBin.exists() && (binaryInBin.canExecute() || binaryInBin.getName().endsWith(".jar"))) {
                    return binaryInBin;
                }
                // Try root directory for JAR files
                File binaryInRoot = new File(toolHome, binaryName);
                if (binaryInRoot.exists() && binaryInRoot.getName().endsWith(".jar")) {
                    return binaryInRoot;
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
