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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Helper class for tool registration operations, providing auto-detection and path resolution.
 * Can be used as static utility or instantiated to track registration state.
 * 
 * @author Ruud Senden
 */
public class ToolRegistrationHelper {
    
    /**
     * Find all potential tool binary candidates from fcli installed versions and provided paths.
     * Used when version filtering is needed - returns all candidates for version matching.
     * 
     * @param toolName Tool identifier
     * @param binaryName Platform-specific binary name
     * @param paths Array of paths to search (each can be file, bin dir, or install dir)
     * @return List of all candidate binaries (may be empty)
     */
    public static List<File> findAllToolBinariesInPaths(String toolName, String binaryName, String[] paths) {
        List<File> candidates = new ArrayList<>();
        
        // Collect from fcli installed versions
        candidates.addAll(findAllBinariesFromInstalledVersions(toolName, binaryName));
        
        // Collect from provided paths
        for (String pathStr : paths) {
            File path = new File(pathStr.trim());
            if (path.isFile()) {
                // Accept both executable binaries and JAR files
                if (path.canExecute() || path.getName().endsWith(".jar")) {
                    candidates.add(path);
                }
            } else if (path.isDirectory()) {
                // Try binary in directory
                File bin = new File(path, binaryName);
                if (bin.exists() && (bin.canExecute() || bin.getName().endsWith(".jar"))) {
                    candidates.add(bin);
                }
                // Try bin/ subdirectory
                File binDir = new File(path, "bin");
                File binInBin = new File(binDir, binaryName);
                if (binInBin.exists() && (binInBin.canExecute() || binInBin.getName().endsWith(".jar"))) {
                    candidates.add(binInBin);
                }
            }
        }
        
        return candidates;
    }
    
    /**
     * Find first tool binary from fcli installed versions and provided paths.
     * 
     * @param toolName Tool identifier
     * @param binaryName Platform-specific binary name
     * @param paths Array of paths to search (each can be file, bin dir, or install dir)
     * @return First found tool binary file or null if not found
     */
    public static File findToolBinaryInPaths(String toolName, String binaryName, String[] paths) {
        // Priority 1: Check fcli installation status
        File binaryFromInstalled = findBinaryFromInstalledVersions(toolName, binaryName);
        if (binaryFromInstalled != null) {
            return binaryFromInstalled;
        }
        
        // Priority 2: Check provided paths
        for (String pathStr : paths) {
            File path = new File(pathStr.trim());
            if (path.isFile()) {
                // Accept both executable binaries and JAR files
                if (path.canExecute() || path.getName().endsWith(".jar")) {
                    return path;
                }
            } else if (path.isDirectory()) {
                // Try binary in directory
                File bin = new File(path, binaryName);
                if (bin.exists() && (bin.canExecute() || bin.getName().endsWith(".jar"))) {
                    return bin;
                }
                // Try bin/ subdirectory
                File binDir = new File(path, "bin");
                File binInBin = new File(binDir, binaryName);
                if (binInBin.exists() && (binInBin.canExecute() || binInBin.getName().endsWith(".jar"))) {
                    return binInBin;
                }
            }
        }
        
        return null;
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
    
    private static List<File> findAllBinariesFromInstalledVersions(String toolName, String binaryName) {
        List<File> binaries = new ArrayList<>();
        Path installDescriptorsDir = ToolInstallationHelper.getToolsStatePath().resolve(toolName);
        if (!installDescriptorsDir.toFile().exists()) {
            return binaries;
        }
        
        File[] descriptorFiles = installDescriptorsDir.toFile().listFiles(File::isFile);
        if (descriptorFiles == null || descriptorFiles.length == 0) {
            return binaries;
        }
        
        // Check all installed versions, newest first
        Arrays.sort(descriptorFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        
        for (File descriptorFile : descriptorFiles) {
            ToolInstallationDescriptor descriptor = FcliDataHelper.readFile(
                descriptorFile.toPath(), ToolInstallationDescriptor.class, false);
            
            if (descriptor != null && descriptor.getBinDir() != null) {
                File binary = new File(descriptor.getBinPath().resolve(binaryName).toString());
                if (binary.exists() && (binary.canExecute() || binary.getName().endsWith(".jar"))) {
                    binaries.add(binary);
                }
            }
        }
        return binaries;
    }
    
    private static File findBinaryFromInstalledVersions(String toolName, String binaryName) {
        List<File> binaries = findAllBinariesFromInstalledVersions(toolName, binaryName);
        return binaries.isEmpty() ? null : binaries.get(0);
    }
    
    /**
     * Result of a registration operation, tracking whether this was a new registration,
     * re-registration of existing installation, or error.
     */
    @RequiredArgsConstructor
    @Getter
    public static enum RegistrationAction {
        REGISTERED("New registration"),
        RE_REGISTERED("Re-registered existing installation");
        
        private final String description;
    }
    
    /**
     * Registration result including the installation descriptor and action taken.
     */
    @RequiredArgsConstructor
    @Getter
    public static class RegistrationResult {
        private final ToolInstallationDescriptor installation;
        private final ToolDefinitionVersionDescriptor versionDescriptor;
        private final RegistrationAction action;
    }
    
    /**
     * Instance helper for performing tool registration with state tracking.
     * This allows tracking whether registration created a new installation or
     * re-registered an existing one.
     */
    public static class RegistrationContext {
        private final String toolName;
        private final String defaultBinaryName;
        private final VersionDetector versionDetector;
        private RegistrationAction action;
        
        public RegistrationContext(String toolName, String defaultBinaryName, VersionDetector versionDetector) {
            this.toolName = toolName;
            this.defaultBinaryName = defaultBinaryName;
            this.versionDetector = versionDetector;
        }
        
        /**
         * Register a tool installation from the given path(s) and optional version.
         * 
         * @param pathOption Path option containing one or more paths separated by path separator
         * @param requestedVersion Requested version ("any" for no specific version)
         * @return Registration result
         */
        public RegistrationResult register(String pathOption, String requestedVersion) {
            File toolBinary = locateToolBinary(pathOption, requestedVersion);
            ToolDefinitionVersionDescriptor versionDescriptor = detectAndValidateVersion(toolBinary, requestedVersion);
            ToolInstallationDescriptor installation = createAndSaveInstallation(toolBinary, versionDescriptor);
            return new RegistrationResult(installation, versionDescriptor, action);
        }
        
        private File locateToolBinary(String pathOption, String requestedVersion) {
            String[] paths = pathOption.split(File.pathSeparator);
            
            // If single path provided, use it directly without searching other installations
            if (paths.length == 1) {
                return findToolBinaryInSinglePath(paths[0]);
            }
            
            // For multiple paths, check existing installations first, then search
            if (!"any".equals(requestedVersion)) {
                File existingInstallation = findExistingInstallation(requestedVersion);
                if (existingInstallation != null) {
                    return existingInstallation;
                }
            }
            
            return findToolBinaryInMultiplePaths(paths, requestedVersion);
        }
        
        private File findToolBinaryInSinglePath(String path) {
            File pathFile = new File(path.trim());
            File toolBinary = null;
            
            if (pathFile.isFile()) {
                // Accept both executable binaries and JAR files
                if (pathFile.canExecute() || pathFile.getName().endsWith(".jar")) {
                    toolBinary = pathFile;
                }
            } else if (pathFile.isDirectory()) {
                // Try binary in directory
                File bin = new File(pathFile, defaultBinaryName);
                if (bin.exists() && (bin.canExecute() || bin.getName().endsWith(".jar"))) {
                    toolBinary = bin;
                } else {
                    // Try bin/ subdirectory
                    File binDir = new File(pathFile, "bin");
                    File binInBin = new File(binDir, defaultBinaryName);
                    if (binInBin.exists() && (binInBin.canExecute() || binInBin.getName().endsWith(".jar"))) {
                        toolBinary = binInBin;
                    }
                }
            }
            
            if (toolBinary == null) {
                throw new FcliSimpleException(
                    toolName + " not found in specified path: " + path);
            }
            
            validateBinaryExecutable(toolBinary);
            action = RegistrationAction.REGISTERED;
            return toolBinary;
        }
        
        private File findExistingInstallation(String requestedVersion) {
            var toolDefinition = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName);
            
            // If specific version requested, look for matching installation
            if (!"any".equals(requestedVersion)) {
                try {
                    var requestedVersionDescriptor = toolDefinition.getVersionOrDefault(requestedVersion);
                    var installation = ToolInstallationDescriptor.load(toolName, requestedVersionDescriptor);
                    if (installation != null && installation.getBinPath() != null) {
                        File binDir = installation.getBinPath().toFile();
                        File toolBinary = new File(binDir, defaultBinaryName);
                        if (toolBinary.exists()) {
                            action = RegistrationAction.RE_REGISTERED;
                            return toolBinary;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Requested version not in definitions, will search paths
                }
            } else {
                // No version requested, try to find most recently registered installation
                // by checking all defined versions sorted by modification time
                var installations = toolDefinition.getVersionsStream()
                    .map(versionDesc -> ToolInstallationDescriptor.load(toolName, versionDesc))
                    .filter(inst -> inst != null && inst.getBinPath() != null)
                    .sorted((a, b) -> Long.compare(
                        b.getBinPath().toFile().lastModified(), 
                        a.getBinPath().toFile().lastModified()))
                    .toList();
                
                if (!installations.isEmpty()) {
                    var mostRecent = installations.get(0);
                    File binDir = mostRecent.getBinPath().toFile();
                    File toolBinary = new File(binDir, defaultBinaryName);
                    if (toolBinary.exists()) {
                        action = RegistrationAction.RE_REGISTERED;
                        return toolBinary;
                    }
                }
            }
            
            return null;
        }
        
        private File findToolBinaryInMultiplePaths(String[] paths, String requestedVersion) {
            if (!"any".equals(requestedVersion)) {
                var candidates = findAllToolBinariesInPaths(toolName, defaultBinaryName, paths);
                
                File toolBinary = findMatchingCandidate(candidates, requestedVersion);
                if (toolBinary == null) {
                    throw new FcliSimpleException(
                        String.format("%s version matching %s not found in specified paths", 
                            toolName, requestedVersion));
                }
                action = RegistrationAction.REGISTERED;
                return toolBinary;
            } else {
                File toolBinary = findToolBinaryInPaths(toolName, defaultBinaryName, paths);
                
                if (toolBinary == null) {
                    throw new FcliSimpleException(
                        toolName + " not found in specified paths");
                }
                
                validateBinaryExecutable(toolBinary);
                action = RegistrationAction.REGISTERED;
                return toolBinary;
            }
        }
        
        private void validateBinaryExecutable(File toolBinary) {
            if (!toolBinary.canExecute() && !toolBinary.getName().endsWith(".jar")) {
                throw new FcliSimpleException(
                    toolName + " binary found but not executable: " + toolBinary.getAbsolutePath());
            }
        }
        
        private ToolDefinitionVersionDescriptor detectAndValidateVersion(File toolBinary, String requestedVersion) {
            File installDir = resolveInstallDir(toolBinary);
            String detectedVersion = detectVersionFromBinary(toolBinary, installDir);
            ToolDefinitionVersionDescriptor versionDescriptor = resolveVersionDescriptor(detectedVersion);
            
            if (!"any".equals(requestedVersion)) {
                validateVersionMatch(versionDescriptor, requestedVersion);
            }
            
            return versionDescriptor;
        }
        
        private String detectVersionFromBinary(File toolBinary, File installDir) {
            String versionFromDescriptor = ToolVersionDetector.detectVersionFromDescriptor(toolBinary);
            return versionFromDescriptor != null 
                ? versionFromDescriptor 
                : versionDetector.detectVersion(toolBinary, installDir);
        }
        
        private void validateVersionMatch(ToolDefinitionVersionDescriptor versionDescriptor, String requestedVersion) {
            var toolDefinition = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName);
            try {
                var requestedVersionDescriptor = toolDefinition.getVersionOrDefault(requestedVersion);
                if (!versionDescriptor.getVersion().equals(requestedVersionDescriptor.getVersion())) {
                    throw new FcliSimpleException(
                        String.format("Detected %s version %s does not match requested version %s (resolves to %s)", 
                            toolName, versionDescriptor.getVersion(), requestedVersion, requestedVersionDescriptor.getVersion()));
                }
            } catch (IllegalArgumentException e) {
                throw new FcliSimpleException(
                    String.format("Requested version %s not found in tool definitions. Detected version is %s", 
                        requestedVersion, versionDescriptor.getVersion()));
            }
        }
        
        private ToolInstallationDescriptor createAndSaveInstallation(File toolBinary, ToolDefinitionVersionDescriptor versionDescriptor) {
            File installDir = resolveInstallDir(toolBinary);
            ToolInstallationDescriptor installation = new ToolInstallationDescriptor(
                installDir.toPath(), 
                toolBinary.getParentFile().toPath(),
                null
            );
            // Always save descriptor to update timestamp, making this the default version for 'tool run' commands
            installation.save(toolName, versionDescriptor);
            return installation;
        }
        
        private File findMatchingCandidate(List<File> candidates, String requestedVersion) {
            var toolDefinition = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName);
            ToolDefinitionVersionDescriptor requestedVersionDescriptor;
            try {
                requestedVersionDescriptor = toolDefinition.getVersionOrDefault(requestedVersion);
            } catch (IllegalArgumentException e) {
                throw new FcliSimpleException(
                    String.format("Requested version %s not found in tool definitions", requestedVersion));
            }
            
            for (File candidate : candidates) {
                if (!candidate.canExecute() && !candidate.getName().endsWith(".jar")) {
                    continue;
                }
                
                try {
                    File installDir = resolveInstallDir(candidate);
                    String versionFromDescriptor = ToolVersionDetector.detectVersionFromDescriptor(candidate);
                    String detectedVersion = versionFromDescriptor != null 
                        ? versionFromDescriptor 
                        : versionDetector.detectVersion(candidate, installDir);
                    
                    ToolDefinitionVersionDescriptor versionDesc = resolveVersionDescriptor(detectedVersion);
                    
                    if (versionDesc.getVersion().equals(requestedVersionDescriptor.getVersion())) {
                        return candidate;
                    }
                } catch (Exception e) {
                    // Skip candidates that fail version detection
                    continue;
                }
            }
            return null;
        }
        
        private ToolDefinitionVersionDescriptor resolveVersionDescriptor(String detectedVersion) {
            var toolDefinition = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName);
            
            // If version is unknown, create synthetic descriptor immediately without normalization
            if ("unknown".equals(detectedVersion)) {
                ToolDefinitionVersionDescriptor syntheticDescriptor = new ToolDefinitionVersionDescriptor();
                syntheticDescriptor.setVersion("unknown");
                syntheticDescriptor.setStable(true);
                syntheticDescriptor.setAliases(new String[0]);
                return syntheticDescriptor;
            }
            
            // Normalize version format to match tool definitions (e.g., 24.2.0.0050 -> 24.2.0)
            String normalizedVersion = toolDefinition.normalizeVersionFormat(detectedVersion);
            
            // Try to find matching version in tool definitions using normalized version
            try {
                return toolDefinition.getVersion(normalizedVersion);
            } catch (IllegalArgumentException e) {
                // Version not found in definitions, create synthetic descriptor with normalized version
                ToolDefinitionVersionDescriptor syntheticDescriptor = new ToolDefinitionVersionDescriptor();
                syntheticDescriptor.setVersion(normalizedVersion);
                syntheticDescriptor.setStable(true);
                syntheticDescriptor.setAliases(new String[0]);
                return syntheticDescriptor;
            }
        }
    }
    
    /**
     * Functional interface for version detection, allowing subclasses to provide
     * their own version detection logic.
     */
    @FunctionalInterface
    public static interface VersionDetector {
        /**
         * Detect the version of a tool from its binary and installation directory.
         * 
         * @param toolBinary The tool binary file
         * @param installDir The resolved installation directory
         * @return Detected version string, or "unknown" if detection fails
         */
        String detectVersion(File toolBinary, File installDir);
    }
}
