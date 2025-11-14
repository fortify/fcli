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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Option;

/**
 * Abstract base class for tool env commands. Outputs environment variable
 * definitions in various formats suitable for sourcing in shells or setting
 * through CI/CD systems.
 *
 * @author Ruud Senden
 */
public abstract class AbstractToolEnvCommand extends AbstractRunnableCommand {
    
    @Option(names = {"-v", "--version"}, required = false, descriptionKey = "fcli.tool.env.version")
    private String version;
    
    @Option(names = {"--path"}, required = false, descriptionKey = "fcli.tool.env.path")
    private EnvVarMode pathMode = EnvVarMode.auto;
    
    @Option(names = {"--cmd-var"}, required = false, descriptionKey = "fcli.tool.env.cmd-var")
    private EnvVarMode cmdVarMode = EnvVarMode.auto;
    
    @Option(names = {"--home-var"}, required = false, descriptionKey = "fcli.tool.env.home-var")
    private EnvVarMode homeVarMode = EnvVarMode.auto;
    
    @Option(names = {"--cmd-var-name"}, required = false, descriptionKey = "fcli.tool.env.cmd-var-name")
    private String cmdVarName;
    
    @Option(names = {"--home-var-name"}, required = false, descriptionKey = "fcli.tool.env.home-var-name")
    private String homeVarName;
    
    @Option(names = {"--format"}, required = false, descriptionKey = "fcli.tool.env.format")
    private EnvFormat format = EnvFormat.shell;
    
    @Override
    public final Integer call() {
        var descriptor = getToolInstallationDescriptor();
        
        // Determine effective modes
        boolean includePathUpdate = shouldIncludePathUpdate(descriptor);
        boolean includeCmdVar = shouldIncludeCmdVar();
        boolean includeHomeVar = shouldIncludeHomeVar();
        
        var envVars = buildEnvironmentVariables(descriptor, includeCmdVar, includeHomeVar);
        
        if (format == EnvFormat.github) {
            outputGitHubFormat(descriptor, envVars, includePathUpdate);
        } else if (format == EnvFormat.azure) {
            outputAzureFormat(descriptor, envVars, includePathUpdate);
        } else if (format == EnvFormat.gitlab) {
            outputGitLabFormat(descriptor, envVars, includePathUpdate);
        } else if (format == EnvFormat.shell) {
            outputShellFormat(descriptor, envVars, includePathUpdate);
        } else if (format == EnvFormat.powershell) {
            outputPowerShellFormat(descriptor, envVars, includePathUpdate);
        } else if (format == EnvFormat.cmd) {
            outputCmdFormat(descriptor, envVars, includePathUpdate);
        }
        
        return 0;
    }
    
    private boolean shouldIncludePathUpdate(ToolInstallationDescriptor descriptor) {
        if (pathMode == EnvVarMode.include) {
            return true;
        } else if (pathMode == EnvVarMode.exclude) {
            return false;
        } else { // auto
            if (descriptor.getBinDir() == null) {
                return false;
            }
            String binDir = descriptor.getBinPath().toString();
            String pathEnv = EnvHelper.env("PATH");
            if (pathEnv == null) {
                return true; // PATH not set, so include it
            }
            // Check if bin directory is already in PATH
            String[] pathEntries = pathEnv.split(File.pathSeparator);
            for (String entry : pathEntries) {
                if (new File(entry).getAbsolutePath().equals(new File(binDir).getAbsolutePath())) {
                    return false; // Already in PATH
                }
            }
            return true; // Not in PATH, so include it
        }
    }
    
    private boolean shouldIncludeCmdVar() {
        if (cmdVarMode == EnvVarMode.include) {
            return true;
        } else if (cmdVarMode == EnvVarMode.exclude) {
            return false;
        } else { // auto
            // Auto: include by default
            return true;
        }
    }
    
    private boolean shouldIncludeHomeVar() {
        if (homeVarMode == EnvVarMode.include) {
            return true;
        } else if (homeVarMode == EnvVarMode.exclude) {
            return false;
        } else { // auto
            // Auto: include by default
            return true;
        }
    }
    
    private ToolInstallationDescriptor getToolInstallationDescriptor() {
        var toolName = getToolName();
        if (StringUtils.isBlank(version)) {
            return checkNotNull(
                ToolInstallationDescriptor.loadLastModified(toolName),
                "No tool installations detected");
        } else {
            var versionDescriptor = ToolDefinitionsHelper
                .getToolDefinitionRootDescriptor(toolName)
                .getVersion(version);
            return checkNotNull(
                ToolInstallationDescriptor.load(toolName, versionDescriptor),
                "No tool installation detected for version " + version);
        }
    }
    
    private ToolInstallationDescriptor checkNotNull(ToolInstallationDescriptor descriptor, String msg) {
        if (descriptor == null) {
            throw new FcliSimpleException(msg);
        }
        return descriptor;
    }
    
    private Map<String, String> buildEnvironmentVariables(ToolInstallationDescriptor descriptor, 
                                                           boolean includeCmdVar, boolean includeHomeVar) {
        Map<String, String> envVars = new LinkedHashMap<>();
        
        String[] prefixes = getToolEnvVarPrefixes();
        if (prefixes == null || prefixes.length == 0) {
            return envVars;
        }
        
        // Determine variable names to use
        String effectiveCmdVarName = cmdVarName != null ? cmdVarName : prefixes[0] + "_CMD";
        String effectiveHomeVarName = homeVarName != null ? homeVarName : prefixes[0] + "_HOME";
        
        // Add *_CMD variable pointing to binary (for tools with single binary)
        if (includeCmdVar && descriptor.getBinDir() != null) {
            String binaryPath = getBinaryPath(descriptor);
            if (binaryPath != null) {
                envVars.put(effectiveCmdVarName, binaryPath);
            }
        }
        
        // Add *_HOME variable pointing to installation directory
        if (includeHomeVar && descriptor.getInstallDir() != null) {
            envVars.put(effectiveHomeVarName, descriptor.getInstallPath().toString());
        }
        
        return envVars;
    }
    
    private String getBinaryPath(ToolInstallationDescriptor descriptor) {
        String binaryName = getDefaultBinaryName();
        if (binaryName == null) {
            return null;
        }
        File binary = new File(descriptor.getBinPath().toFile(), binaryName);
        return binary.exists() ? binary.getAbsolutePath() : null;
    }
    
    private void outputShellFormat(ToolInstallationDescriptor descriptor, Map<String, String> envVars, 
                                    boolean includePathUpdate) {
        List<String> lines = new ArrayList<>();
        lines.add("# " + getToolName() + " environment setup");
        lines.add("# Usage: source <(fcli tool " + getToolName() + " env)");
        lines.add("");
        
        if (includePathUpdate && descriptor.getBinDir() != null) {
            lines.add("export PATH=\"" + descriptor.getBinPath() + ":$PATH\"");
        }
        
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            lines.add("export " + entry.getKey() + "=\"" + entry.getValue() + "\"");
        }
        
        lines.forEach(System.out::println);
    }
    
    private void outputPowerShellFormat(ToolInstallationDescriptor descriptor, Map<String, String> envVars, 
                                         boolean includePathUpdate) {
        List<String> lines = new ArrayList<>();
        lines.add("# " + getToolName() + " environment setup");
        lines.add("# Usage: fcli tool " + getToolName() + " env --format powershell | Invoke-Expression");
        lines.add("");
        
        if (includePathUpdate && descriptor.getBinDir() != null) {
            lines.add("$env:PATH = \"" + descriptor.getBinPath() + ";$env:PATH\"");
        }
        
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            lines.add("$env:" + entry.getKey() + " = \"" + entry.getValue() + "\"");
        }
        
        lines.forEach(System.out::println);
    }
    
    private void outputCmdFormat(ToolInstallationDescriptor descriptor, Map<String, String> envVars, 
                                  boolean includePathUpdate) {
        List<String> lines = new ArrayList<>();
        lines.add("@REM " + getToolName() + " environment setup");
        lines.add("@REM Usage: fcli tool " + getToolName() + " env --format cmd > env.bat && env.bat");
        lines.add("");
        
        if (includePathUpdate && descriptor.getBinDir() != null) {
            lines.add("set PATH=" + descriptor.getBinPath() + ";%PATH%");
        }
        
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            lines.add("set " + entry.getKey() + "=" + entry.getValue());
        }
        
        lines.forEach(System.out::println);
    }
    
    private void outputGitHubFormat(ToolInstallationDescriptor descriptor, Map<String, String> envVars, 
                                     boolean includePathUpdate) {
        List<String> lines = new ArrayList<>();
        lines.add("#!/bin/bash");
        lines.add("# " + getToolName() + " environment setup for GitHub Actions");
        lines.add("# Usage: source <(fcli tool " + getToolName() + " env --format github)");
        lines.add("");
        
        if (includePathUpdate && descriptor.getBinDir() != null) {
            lines.add("echo \"" + descriptor.getBinPath() + "\" >> $GITHUB_PATH");
        }
        
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            lines.add("echo \"" + entry.getKey() + "=" + entry.getValue() + "\" >> $GITHUB_ENV");
        }
        
        lines.forEach(System.out::println);
    }
    
    private void outputAzureFormat(ToolInstallationDescriptor descriptor, Map<String, String> envVars, 
                                    boolean includePathUpdate) {
        List<String> lines = new ArrayList<>();
        lines.add("# " + getToolName() + " environment setup for Azure Pipelines");
        lines.add("# Usage: Copy output and paste into Azure Pipeline script task");
        lines.add("");
        
        if (includePathUpdate && descriptor.getBinDir() != null) {
            lines.add("##vso[task.prependpath]" + descriptor.getBinPath());
        }
        
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            lines.add("##vso[task.setvariable variable=" + entry.getKey() + "]" + entry.getValue());
        }
        
        lines.forEach(System.out::println);
    }
    
    private void outputGitLabFormat(ToolInstallationDescriptor descriptor, Map<String, String> envVars, 
                                     boolean includePathUpdate) {
        List<String> lines = new ArrayList<>();
        lines.add("# " + getToolName() + " environment setup for GitLab CI");
        lines.add("# Usage: source <(fcli tool " + getToolName() + " env --format gitlab)");
        lines.add("");
        
        if (includePathUpdate && descriptor.getBinDir() != null) {
            lines.add("export PATH=\"" + descriptor.getBinPath() + ":$PATH\"");
        }
        
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            lines.add("export " + entry.getKey() + "=\"" + entry.getValue() + "\"");
        }
        
        lines.forEach(System.out::println);
    }
    
    protected abstract String getToolName();
    protected abstract String[] getToolEnvVarPrefixes();
    
    /**
     * Get the default binary name for this tool (platform-specific).
     * Return null if the tool doesn't have a single primary binary.
     */
    protected abstract String getDefaultBinaryName();
    
    @RequiredArgsConstructor
    @Getter
    public enum EnvFormat {
        shell("Shell script (bash/zsh)"),
        powershell("PowerShell"),
        cmd("Windows Command Prompt"),
        github("GitHub Actions"),
        azure("Azure Pipelines"),
        gitlab("GitLab CI");
        
        private final String description;
    }
    
    public enum EnvVarMode {
        include,  // Always include
        exclude,  // Never include
        auto      // Auto-detect (default)
    }
}
