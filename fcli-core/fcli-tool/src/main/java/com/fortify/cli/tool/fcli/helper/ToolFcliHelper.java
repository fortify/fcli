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
package com.fortify.cli.tool.fcli.helper;

import java.io.File;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.PlatformHelper;
import com.fortify.cli.tool._common.helper.ToolVersionDetector;

/**
 * Shared helper methods for fcli tool commands.
 * Provides common functionality used by both install and register commands.
 */
public class ToolFcliHelper {
    
    /**
     * Get the platform-specific binary name for fcli.
     * 
     * @return "fcli.exe" on Windows, "fcli" on other platforms
     */
    public static String getDefaultBinaryName() {
        return PlatformHelper.isWindows() ? "fcli.exe" : "fcli";
    }
    
    /**
     * Detect fcli version from a binary file.
     * Tries descriptor first (for fcli-installed tools), then executes --version.
     * 
     * @param toolBinary The fcli binary file
     * @return Detected version string
     * @throws FcliSimpleException if version cannot be detected
     */
    public static String detectVersion(File toolBinary) {
        // Try descriptor first (for fcli-installed tools)
        String versionFromDescriptor = ToolVersionDetector.detectVersionFromDescriptor(toolBinary);
        if (versionFromDescriptor != null) {
            return versionFromDescriptor;
        }
        
        // Try executing the tool to get version
        String output = ToolVersionDetector.tryExecute(toolBinary, "--version");
        if (output != null) {
            String version = ToolVersionDetector.extractVersionFromOutput(output);
            if (version != null) {
                return version;
            }
        }
        
        throw new FcliSimpleException(
            "Failed to detect version from fcli binary: " + toolBinary.getAbsolutePath());
    }
    
    /**
     * Detect fcli version from a binary file, returning "unknown" on failure.
     * Used by register command which tolerates unknown versions.
     * 
     * @param toolBinary The fcli binary file
     * @param installDir The installation directory (unused for fcli, kept for compatibility)
     * @return Detected version string or "unknown"
     */
    public static String detectVersionOrUnknown(File toolBinary, File installDir) {
        try {
            return detectVersion(toolBinary);
        } catch (FcliSimpleException e) {
            return "unknown";
        }
    }
    
    /**
     * Resolve and validate fcli binary from an explicit path.
     * 
     * @param explicitPath Path to binary, bin directory, or install directory
     * @return Resolved and validated binary file
     * @throws FcliSimpleException if binary cannot be resolved or is not executable
     */
    public static File resolveBinaryFromExplicitPath(File explicitPath) {
        File sourceBinary = resolveBinaryFromExplicitPath(
            explicitPath, 
            getDefaultBinaryName()
        );
        
        if ( sourceBinary == null ) {
            throw new FcliSimpleException(
                "Source fcli binary not found at specified path: " + explicitPath.getAbsolutePath());
        }
        
        if (!sourceBinary.canExecute()) {
            throw new FcliSimpleException(
                "Source fcli binary not executable: " + sourceBinary.getAbsolutePath());
        }
        
        return sourceBinary;
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
    private static final File resolveBinaryFromExplicitPath(File path, String binaryName) {
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
        return null;
    }
}
