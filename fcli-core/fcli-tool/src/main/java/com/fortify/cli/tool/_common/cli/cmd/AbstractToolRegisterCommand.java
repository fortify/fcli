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
package com.fortify.cli.tool._common.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.util.SemVer;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolRegistrationHelper;
import com.fortify.cli.tool._common.helper.ToolVersionDetector;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Abstract base class for tool register commands. Provides two modes:
 * 1. Auto-detect: Search for tool in environment variables, PATH, and fcli installation status
 * 2. Explicit path: Register tool from user-specified location
 * 
 * Subclasses must implement:
 * - getToolName(): Return the tool identifier
 * - getDefaultBinaryName(): Return platform-specific binary name
 * - getToolEnvVarName(): Return env var name for direct binary path (or null)
 * - getToolHomeEnvVarName(): Return env var name for install directory (or null)
 * 
 * @author Ruud Senden
 */
@Command(name = OutputHelperMixins.Register.CMD_NAME)
public abstract class AbstractToolRegisterCommand extends AbstractOutputCommand 
        implements IJsonNodeSupplier, IActionCommandResultSupplier {
    
    @Getter @picocli.CommandLine.Mixin 
    private OutputHelperMixins.Register outputHelper;
    
    @ArgGroup(exclusive = true, multiplicity = "1")
    private RegisterModeArgGroup registerMode;
    
    @Option(names = {"-v", "--version"}, required = false, descriptionKey = "fcli.tool.register.version")
    private String requestedVersion = "any";
    
    @Option(names = {"--any-matching"}, required = false, descriptionKey = "fcli.tool.register.any-matching")
    private boolean anyMatching = false;
    
    private static final class RegisterModeArgGroup {
        @Option(names = {"--auto-detect"}, required = true, descriptionKey = "fcli.tool.register.auto-detect")
        private boolean autoDetect;
        
        @Option(names = {"-p", "--path"}, required = true, descriptionKey = "fcli.tool.register.path")
        private File explicitPath;
    }
    
    @Override
    public final String getActionCommandResult() {
        return "REGISTERED";
    }
    
    @Override
    public final boolean isSingular() {
        return true;
    }
    
    protected abstract String getToolName();
    protected abstract String getDefaultBinaryName();
    
    /**
     * Get environment variable prefixes for auto-detecting tool location.
     * Each prefix will be used to check for {PREFIX}_CMD (pointing to executable/jar)
     * and {PREFIX}_HOME (pointing to installation directory).
     * 
     * @return Array of environment variable prefixes (e.g., ["FCLI"], ["SCANCENTRAL", "SC_CLIENT"])
     */
    protected abstract String[] getToolEnvVarPrefixes();
    
    @Override
    @SneakyThrows
    public ObjectNode getJsonNode() {
        File toolBinary;
        ToolDefinitionVersionDescriptor versionDescriptor;
        
        if (registerMode.autoDetect && !"any".equals(requestedVersion)) {
            // When version filter is specified, search all candidates for matching version
            var candidates = ToolRegistrationHelper.findAllToolBinaryCandidates(getToolName(), getDefaultBinaryName(), getToolEnvVarPrefixes());
            toolBinary = findMatchingCandidate(candidates);
            if (toolBinary == null) {
                throw new FcliSimpleException(
                    String.format("%s version matching %s not found. Please specify --path or ensure matching version is in PATH", 
                        getToolName(), requestedVersion))
                    .exitCode(ExitCode.TOOL_NOT_FOUND.getCode());
            }
            // Detect version from the matched binary
            File installDir = ToolRegistrationHelper.resolveInstallDir(toolBinary);
            String versionFromDescriptor = ToolVersionDetector.detectVersionFromDescriptor(toolBinary);
            String detectedVersion = versionFromDescriptor != null 
                ? versionFromDescriptor 
                : detectVersion(toolBinary, installDir);
            versionDescriptor = resolveVersionDescriptor(detectedVersion);
        } else {
            // No version filter or explicit path - use first found
            toolBinary = registerMode.autoDetect 
                ? ToolRegistrationHelper.autoDetectToolBinary(getToolName(), getDefaultBinaryName(), getToolEnvVarPrefixes())
                : ToolRegistrationHelper.resolveBinaryFromExplicitPath(registerMode.explicitPath, getDefaultBinaryName());
            
            // Validate binary is executable (or is a JAR file)
            if (!toolBinary.canExecute() && !toolBinary.getName().endsWith(".jar")) {
                throw new FcliSimpleException(
                    getToolName() + " binary found but not executable: " + toolBinary.getAbsolutePath())
                    .exitCode(ExitCode.TOOL_INVALID_OR_NOT_EXECUTABLE.getCode());
            }
            
            // Resolve install directory
            File installDir = ToolRegistrationHelper.resolveInstallDir(toolBinary);
            
            // Check for fcli-installed tool first
            String versionFromDescriptor = ToolVersionDetector.detectVersionFromDescriptor(toolBinary);
            String detectedVersion = versionFromDescriptor != null 
                ? versionFromDescriptor 
                : detectVersion(toolBinary, installDir);
            
            // Find matching version descriptor (this also normalizes the version)
            versionDescriptor = resolveVersionDescriptor(detectedVersion);
            
            // Validate version matches requested version (if not 'any')
            // Note: "unknown" versions are handled gracefully - they will only match if requestedVersion is "any" or "unknown"
            if (!"any".equals(requestedVersion) && !versionMatches(versionDescriptor.getVersion(), requestedVersion)) {
                throw new FcliSimpleException(
                    String.format("Detected %s version %s does not match requested version %s", 
                        getToolName(), versionDescriptor.getVersion(), requestedVersion))
                    .exitCode(ExitCode.VERSION_MISMATCH.getCode());
            }
        }
        
        // By default, verify that detected version is the latest matching the requested pattern
        // unless --any-matching is specified
        if (!anyMatching && !"any".equals(requestedVersion) && !"preinstalled".equals(requestedVersion)) {
            String latestMatchingVersion = findLatestMatchingVersion(requestedVersion);
            if (latestMatchingVersion != null && !versionDescriptor.getVersion().equals(latestMatchingVersion)) {
                throw new FcliSimpleException(
                    String.format("Detected %s version %s matches requested version %s but is not the latest available (%s). Use --any-matching to accept any matching version", 
                        getToolName(), versionDescriptor.getVersion(), requestedVersion, latestMatchingVersion))
                    .exitCode(ExitCode.VERSION_NOT_LATEST.getCode());
            }
        }
        
        // Create and save installation descriptor
        File installDir = ToolRegistrationHelper.resolveInstallDir(toolBinary);
        ToolInstallationDescriptor installation = new ToolInstallationDescriptor(
            installDir.toPath(), 
            toolBinary.getParentFile().toPath(),
            null
        );
        installation.save(getToolName(), versionDescriptor);
        
        // Use normalized version from descriptor for output
        return createOutputNode(installation, versionDescriptor.getVersion());
    }
    
    /**
     * Detect tool version. Subclasses must implement this to provide tool-specific version detection.
     * Common strategies include:
     * - Execute tool binary with version arguments (use {@link ToolVersionDetector#tryExecute})
     * - Scan installation directory for version-specific files (use {@link ToolVersionDetector#extractVersionFromFilePattern})
     * 
     * If version cannot be detected, return "unknown".
     * 
     * @param toolBinary The tool binary file
     * @param installDir The resolved installation directory
     * @return Detected version string, or "unknown" if detection fails
     */
    protected abstract String detectVersion(File toolBinary, File installDir);
    
    /**
     * Find the first candidate binary that matches the requested version.
     * 
     * @param candidates List of candidate binary files
     * @return Matching binary file or null if no match
     */
    private File findMatchingCandidate(java.util.List<File> candidates) {
        for (File candidate : candidates) {
            if (!candidate.canExecute() && !candidate.getName().endsWith(".jar")) {
                continue;
            }
            
            try {
                File installDir = ToolRegistrationHelper.resolveInstallDir(candidate);
                String versionFromDescriptor = ToolVersionDetector.detectVersionFromDescriptor(candidate);
                String detectedVersion = versionFromDescriptor != null 
                    ? versionFromDescriptor 
                    : detectVersion(candidate, installDir);
                
                ToolDefinitionVersionDescriptor versionDesc = resolveVersionDescriptor(detectedVersion);
                
                if (versionMatches(versionDesc.getVersion(), requestedVersion)) {
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
        var toolDefinition = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(getToolName());
        
        // Normalize version format to match tool definitions (e.g., 24.2.0.0050 -> 24.2.0)
        String normalizedVersion = toolDefinition.normalizeVersionFormat(detectedVersion);
        
        // Try to find matching version in tool definitions using normalized version
        try {
            return toolDefinition.getVersion(normalizedVersion);
        } catch (IllegalArgumentException e) {
            // Version not found in definitions, create synthetic descriptor with normalized version
            ToolDefinitionVersionDescriptor syntheticDescriptor = new ToolDefinitionVersionDescriptor();
            syntheticDescriptor.setVersion(normalizedVersion);
            syntheticDescriptor.setStable(false);  // External installations default to not stable
            return syntheticDescriptor;
        }
    }
    
    private String findLatestMatchingVersion(String requestedPattern) {
        try {
            var toolDefinition = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(getToolName());
            
            // Normalize pattern by removing 'v' prefix
            String normalizedPattern = requestedPattern.startsWith("v") ? requestedPattern.substring(1) : requestedPattern;
            
            // Get all versions and find the latest that matches the pattern
            return java.util.Arrays.stream(toolDefinition.getVersions())
                .map(ToolDefinitionVersionDescriptor::getVersion)
                .filter(v -> versionMatches(v, normalizedPattern))
                .max((v1, v2) -> {
                    try {
                        return new SemVer(v1).compareTo(new SemVer(v2));
                    } catch (Exception e) {
                        return v1.compareTo(v2);
                    }
                })
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    private ObjectNode createOutputNode(ToolInstallationDescriptor installation, String version) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("name", getToolName());
        result.put("version", version);
        result.put("installDir", installation.getInstallDir());
        result.put("binDir", installation.getBinDir());
        result.put("action", "REGISTERED");
        return result;
    }
    
    /**
     * Check if a version matches the requested version pattern.
     * Supports semantic versioning: "2" matches "2.x.y", "2.1" matches "2.1.x", etc.
     * Both "v2" and "2" formats are supported.
     * 
     * Special case: "unknown" versions only match if the requested pattern is exactly "unknown".
     * 
     * @param actualVersion The actual detected version (e.g., "24.4.0", "unknown")
     * @param requestedPattern The requested version pattern (e.g., "24", "v24", "24.4")
     * @return true if the version matches the pattern
     */
    private boolean versionMatches(String actualVersion, String requestedPattern) {
        // Special handling for "unknown" versions - only match exact "unknown" request
        if ("unknown".equals(actualVersion)) {
            return "unknown".equals(requestedPattern);
        }
        
        // Normalize by removing 'v' prefix
        String normalizedActual = actualVersion.startsWith("v") ? actualVersion.substring(1) : actualVersion;
        String normalizedRequested = requestedPattern.startsWith("v") ? requestedPattern.substring(1) : requestedPattern;
        
        try {
            SemVer actual = new SemVer(normalizedActual);
            
            // If not a proper semver, fall back to prefix matching
            if (!actual.isProperSemver()) {
                return normalizedActual.startsWith(normalizedRequested);
            }
            
            // Split requested pattern by dots
            String[] requestedParts = normalizedRequested.split("\\.");
            
            // Check major version
            if (requestedParts.length >= 1 && !requestedParts[0].isEmpty()) {
                if (!actual.getMajor().map(m -> m.equals(requestedParts[0])).orElse(false)) {
                    return false;
                }
            }
            
            // Check minor version if specified
            if (requestedParts.length >= 2 && !requestedParts[1].isEmpty()) {
                if (!actual.getMinor().map(m -> m.equals(requestedParts[1])).orElse(false)) {
                    return false;
                }
            }
            
            // Check patch version if specified
            if (requestedParts.length >= 3 && !requestedParts[2].isEmpty()) {
                if (!actual.getPatch().map(p -> p.equals(requestedParts[2])).orElse(false)) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            // If parsing fails, fall back to simple string prefix match
            return normalizedActual.startsWith(normalizedRequested);
        }
    }
    
    @RequiredArgsConstructor
    @Getter
    public static enum ExitCode {
        SUCCESS(0),
        TOOL_NOT_FOUND(1),
        INVALID_PATH(2),
        TOOL_INVALID_OR_NOT_EXECUTABLE(3),
        VERSION_MISMATCH(4),
        VERSION_NOT_LATEST(5);
        
        private final int code;
    }
}
