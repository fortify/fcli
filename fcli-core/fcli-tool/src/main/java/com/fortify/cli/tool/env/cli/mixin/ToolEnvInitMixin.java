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
package com.fortify.cli.tool.env.cli.mixin;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.tool._common.helper.Tool;

import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Mixin for the --tools option in tool setup commands.
 * Parses comma-separated list of tool specifications in the format:
 * tool[:version|path]
 * 
 * Examples:
 * --tools sc-client,fcli:v3,debricked-cli:/opt/debricked
 */
public class ToolEnvInitMixin {
    
    @Option(names = "--preinstalled")
    @Getter private Boolean preinstalled;
    
    @Option(names = "--tool-definitions")
    @Getter private String toolDefinitions;
    
    @Option(names = "--base-dir")
    @Getter private String baseDir;
    
    @Option(names = "--self")
    @Getter private String self;
    
    @Option(names = "--install-dir-pattern")
    @Getter private String installDirPattern;
    
    @Option(names = "--tools", split = ",", required = false)
    @Getter private List<String> toolSpecs;
    
    /**
     * Check if preinstalled mode is enabled via option or environment variable.
     */
    public boolean isPreinstalledMode() {
        if (preinstalled != null && preinstalled) {
            return true;
        }
        return EnvHelper.asBoolean(EnvHelper.env("PREINSTALLED"));
    }
    
    /**
     * Check if all tools have explicit paths configured (via argument or _HOME env var).
     */
    public boolean allToolsHavePaths() {
        return getToolSetupSpecs().stream().allMatch(ToolSetupSpec::hasPath);
    }
    
    /**
     * Validate options and populate tools from environment if --tools not specified.
     */
    public void validateOptions() {
        if (baseDir != null && installDirPattern != null) {
            throw new FcliSimpleException("--base-dir and --install-dir-pattern are mutually exclusive");
        }
        
        // If --tools not specified, auto-detect from environment variables
        if (toolSpecs == null || toolSpecs.isEmpty()) {
            toolSpecs = autoDetectToolsFromEnvironment();
            if (toolSpecs.isEmpty()) {
                throw new FcliSimpleException("No tools specified via --tools option and no tool environment variables found. " +
                    "Either specify --tools or set environment variables like SC_CLIENT_VERSION, FCLI_VERSION, etc.");
            }
        }
    }
    
    /**
     * Get the effective install directory pattern, auto-detecting if not specified.
     */
    public String getEffectiveInstallDirPattern() {
        if (installDirPattern != null) {
            return installDirPattern;
        }
        if (baseDir != null) {
            return null; // Use base-dir instead
        }
        
        // Auto-detect CI tool cache
        String runnerToolCache = EnvHelper.env("RUNNER_TOOL_CACHE");
        if (runnerToolCache != null && !runnerToolCache.isEmpty()) {
            String arch = EnvHelper.env("RUNNER_ARCH");
            if (arch == null || arch.isEmpty()) {
                arch = System.getProperty("os.arch", "x64").toUpperCase();
            }
            return runnerToolCache + "/{tool}/{version}/" + arch;
        }
        
        String agentToolsDir = EnvHelper.env("AGENT_TOOLSDIRECTORY");
        if (agentToolsDir != null && !agentToolsDir.isEmpty()) {
            return agentToolsDir + "/fortify/{tool}/{version}/x64";
        }
        
        return null; // No pattern
    }
    
    /**
     * Auto-detect tools from environment variables.
     * Scans for <TOOL>_VERSION or <TOOL>_HOME environment variables for all supported tools.
     */
    private List<String> autoDetectToolsFromEnvironment() {
        return java.util.Arrays.stream(Tool.values())
            .filter(tool -> {
                String versionVar = tool.getDefaultEnvPrefix() + "_VERSION";
                String homeVar = tool.getDefaultEnvPrefix() + "_HOME";
                return EnvHelper.env(versionVar) != null || EnvHelper.env(homeVar) != null;
            })
            .map(tool -> tool.getToolName() + ":auto")
            .toList();
    }
    
    /**
     * Get the list of parsed tool setup specifications.
     * @return list of ToolSetupSpec
     */
    public List<ToolSetupSpec> getToolSetupSpecs() {
        if (toolSpecs == null || toolSpecs.isEmpty()) {
            return List.of();
        }
        return toolSpecs.stream()
                .map(this::parseToolSpec)
                .toList();
    }
    
    private ToolSetupSpec parseToolSpec(String spec) {
        if (spec == null || spec.trim().isEmpty()) {
            throw new FcliSimpleException("Tool specification cannot be empty");
        }
        String[] parts = spec.trim().split(":", 2);
        String toolName = parts[0].trim();
        if (toolName.isEmpty()) {
            throw new FcliSimpleException("Tool name cannot be empty in specification: " + spec);
        }
        Tool tool = Tool.getByToolNameOrAlias(toolName);
        if (tool == null) {
            throw new FcliSimpleException("Unknown tool: " + toolName);
        }
        String argument = parts.length > 1 ? parts[1].trim() : null;
        
        // Translate fcli:self and fcli:bootstrapped to fcli:<self-path>
        if (tool == Tool.FCLI && ("self".equals(argument) || "bootstrapped".equals(argument))) {
            if (StringUtils.isBlank(self)) {
                String specName = "self".equals(argument) ? "fcli:self" : "fcli:bootstrapped";
                throw new FcliSimpleException(specName + " requires --self option to be specified");
            }
            argument = self;
        }
        
        return new ToolSetupSpec(tool, argument);
    }
    
    /**
     * Specification for a tool setup request.
     * Consolidates path and version from both command-line arguments and environment variables.
     */
    public record ToolSetupSpec(Tool tool, String argument) {
        /**
         * Get the tool name.
         */
        public String toolName() {
            return tool.getToolName();
        }
        
        /**
         * Check if the argument looks like a path.
         * Handles Unix paths (/, ~, .), Windows paths (C:\, \\), and UNC paths.
         */
        private boolean isPathArgument() {
            if (argument == null || argument.isEmpty()) {
                return false;
            }
            // Unix-style absolute paths
            if (argument.startsWith("/") || argument.startsWith("~") || argument.startsWith(".")) {
                return true;
            }
            // Windows-style paths: C:\ or C:/ or \\server
            if (argument.length() >= 3 && argument.charAt(1) == ':' 
                    && (argument.charAt(2) == '\\' || argument.charAt(2) == '/')) {
                return true;
            }
            // UNC paths: \\server\share
            if (argument.startsWith("\\\\")) {
                return true;
            }
            return false;
        }
        
        /**
         * Get the effective path from either command-line argument or <TOOL>_HOME environment variable.
         * Returns null if neither is specified or if a version was specified instead.
         * Special case: "auto" is treated the same as no argument - checks HOME environment variable.
         */
        public String getEffectivePath() {
            if (isPathArgument()) {
                return argument;
            }
            if (argument == null || argument.isEmpty() || "auto".equals(argument)) {
                String envVar = tool.getDefaultEnvPrefix() + "_HOME";
                return EnvHelper.env(envVar);
            }
            return null;
        }
        
        /**
         * Get the effective version from either command-line argument or <TOOL>_VERSION environment variable.
         * Returns "auto" if neither path nor version is specified (meaning: try any available, install latest if none found).
         * The special value "auto" is NOT converted to "latest" - that conversion happens in the init command logic.
         */
        public String getEffectiveVersion() {
            // If a path is specified (via argument or HOME env var), no version
            if (getEffectivePath() != null) {
                return null;
            }
            
            // If version specified via argument and not 'auto', use it as-is
            if (StringUtils.isNotBlank(argument) && !"auto".equals(argument)) {
                return argument;
            }
            
            // Check VERSION environment variable if no version argument or 'auto' specified
            String versionEnvVar = tool.getDefaultEnvPrefix() + "_VERSION";
            String versionEnvValue = EnvHelper.env(versionEnvVar);
            if (versionEnvValue != null && !versionEnvValue.isEmpty()) {
                return versionEnvValue;
            }
            
            // Default to auto (register any available, install latest if none found)
            return "auto";
        }
        
        /**
         * Check if a specific path was provided (either via argument or HOME env var).
         */
        public boolean hasPath() {
            return getEffectivePath() != null;
        }
        
        /**
         * Check if version is 'auto' (meaning: try any available version, install latest if none found).
         */
        public boolean isAutoVersion() {
            return "auto".equals(getEffectiveVersion());
        }
    }
}