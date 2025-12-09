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
package com.fortify.cli.tool.setup.cli.mixin;

import java.util.List;

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
public class ToolSetupToolsMixin {
    
    @Option(names = "--air-gapped")
    @Getter private boolean airGapped;
    
    @Option(names = "--tool-definitions")
    @Getter private String toolDefinitions;
    
    @Option(names = "--base-dir")
    @Getter private String baseDir;
    
    @Option(names = "--self")
    @Getter private String self;
    
    @Option(names = "--install-dir-pattern")
    @Getter private String installDirPattern;
    
    @Option(names = "--tools", split = ",", required = true)
    @Getter private List<String> toolSpecs;
    
    /**
     * Validate that --install-dir-pattern and --base-dir are mutually exclusive.
     */
    public void validateOptions() {
        if (baseDir != null && installDirPattern != null) {
            throw new FcliSimpleException("--base-dir and --install-dir-pattern are mutually exclusive");
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
        Tool tool = Tool.getByToolName(toolName);
        if (tool == null) {
            throw new FcliSimpleException("Unknown tool: " + toolName);
        }
        String argument = parts.length > 1 ? parts[1].trim() : null;
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
         * Check if the argument looks like a path (starts with / or \ or . or ~).
         */
        private boolean isPathArgument() {
            return argument != null && !argument.isEmpty() 
                    && (argument.startsWith("/") || argument.startsWith("\\") 
                    || argument.startsWith(".") || argument.startsWith("~"));
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
         * Returns "latest" if neither path nor version is specified. The special value "auto" is converted to "latest".
         */
        public String getEffectiveVersion() {
            // If a path is specified (via argument or HOME env var), no version
            if (getEffectivePath() != null) {
                return null;
            }
            
            // If version specified via argument, use it (convert "auto" to "latest")
            if (argument != null && !argument.isEmpty()) {
                return "auto".equals(argument) ? "latest" : argument;
            }
            
            // Check VERSION environment variable (convert "auto" to "latest")
            String versionEnvVar = tool.getDefaultEnvPrefix() + "_VERSION";
            String versionEnvValue = EnvHelper.env(versionEnvVar);
            if (versionEnvValue != null && !versionEnvValue.isEmpty()) {
                return "auto".equals(versionEnvValue) ? "latest" : versionEnvValue;
            }
            
            // Default to latest
            return "latest";
        }
        
        /**
         * Check if a specific path was provided (either via argument or HOME env var).
         */
        public boolean hasPath() {
            return getEffectivePath() != null;
        }
        
        /**
         * Check if a specific version was provided (not "latest").
         */
        public boolean hasSpecificVersion() {
            String version = getEffectiveVersion();
            return version != null && !"latest".equals(version);
        }
    }
}