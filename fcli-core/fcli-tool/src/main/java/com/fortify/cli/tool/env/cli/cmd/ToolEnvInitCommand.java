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
package com.fortify.cli.tool.env.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.exception.FcliCommandExecutionException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.OutputHelper;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.common.util.PlatformHelper;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;
import com.fortify.cli.tool.env.cli.mixin.ToolEnvInitMixin;
import com.fortify.cli.tool.env.cli.mixin.ToolEnvInitMixin.ToolSetupSpec;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

// TODO Replace all fcli invocations (at least those for data retrieval; maybe consider keeping
//      register and install commands) with direct API calls (partially or all done; need to check)
@Command(name = "init")
public class ToolEnvInitCommand extends AbstractRunnableCommand {
    // Platform-aware success marker: checkmark on Unix/Linux, [OK] on Windows
    private static final String SUCCESS_MARKER = PlatformHelper.isWindows() ? "[OK]" : "✓";
    
    @Mixin @Getter
    private ToolEnvInitMixin toolsMixin;
    
    // Consumer to handle fcli command failures by printing error output
    private final Consumer<OutputHelper.Result> onFail = result -> {
        if (result.getErr() != null && !result.getErr().isEmpty()) {
            System.err.println(result.getErr());
        } else if (result.getOut() != null && !result.getOut().isEmpty()) {
            System.err.println(result.getOut());
        }
        throw new FcliCommandExecutionException(result);
    };
    
    // Record to hold setup result information
    private record ToolSetupResult(String toolName, String status, String version, String binDir) {}
    
    // Record to hold registration result
    private record RegistrationResult(boolean success, String version, String installDir) {}
    
    // Record to hold install result
    private record InstallResult(String action, String installDir) {}
    
    @Override
    public Integer call() {
        toolsMixin.validateOptions();
        
        List<ToolSetupSpec> specs = toolsMixin.getToolSetupSpecs();
        List<ToolSetupResult> results = new ArrayList<>();
        
        // Update tool definitions if not in preinstalled mode and not all tools have explicit paths
        if (!toolsMixin.isPreinstalledMode() && !toolsMixin.allToolsHavePaths()) {
            updateToolDefinitions();
        }
        
        for (ToolSetupSpec spec : specs) {
            results.add(setupTool(spec));
        }
        
        // Print detailed summary
        printSummary(results);
        return 0;
    }
    
    private void updateToolDefinitions() {
        ToolDefinitionsHelper.updateToolDefinitions(toolsMixin.getToolDefinitions(), false, null);
    }
    
    private ToolSetupResult setupTool(ToolSetupSpec spec) {
        Tool tool = spec.tool();
        String toolName = tool.getToolName();
        String requestedVersion = spec.getEffectiveVersion();
        String versionInfo = spec.hasPath() ? " at " + spec.getEffectivePath() : (requestedVersion != null ? " version '" + requestedVersion + "'" : "");
        
        System.out.println("Setting up " + toolName + versionInfo + "...");
        
        // Try to register first
        RegistrationResult regResult = tryRegisterTool(spec);
        if (regResult.success()) {
            System.out.println(SUCCESS_MARKER + " " + toolName + " registered successfully");
            String displayVersion = spec.hasPath() ? "preinstalled" : regResult.version();
            return new ToolSetupResult(toolName, "registered", displayVersion, regResult.installDir());
        }
        
        // If registration failed and a path was specified, fail immediately
        if (spec.hasPath()) {
            throw new FcliSimpleException("Tool " + toolName + " not found at specified path: " + spec.getEffectivePath());
        }
        
        // If registration failed and not in preinstalled mode, try to install
        if (!toolsMixin.isPreinstalledMode()) {
            InstallResult installResult = installTool(spec);
            System.out.println(SUCCESS_MARKER + " " + toolName + " " + installResult.action() + " successfully");
            return new ToolSetupResult(toolName, installResult.action(), spec.getEffectiveVersion(), installResult.installDir());
        } else {
            throw new FcliSimpleException("Tool " + toolName + " version '" + requestedVersion + "' not found and preinstalled mode prevents installation");
        }
    }
    
    private RegistrationResult tryRegisterTool(ToolSetupSpec spec) {
        String toolName = spec.toolName();
        String cmd = "tool " + toolName + " register";
        
        // Handle path-based registration (from <tool>:<path> or <TOOL>_HOME)
        if (spec.hasPath()) {
            cmd += " --path \"" + spec.getEffectivePath() + "\"";
        } else {
            // Handle version-based registration from PATH
            cmd += " --path \"" + EnvHelper.env("PATH") + "\"";
        }

        String version = spec.getEffectiveVersion();
        // For 'auto', don't specify --version (register will find any available version)
        // For other versions including 'latest', pass to register command for resolution
        if (StringUtils.isNotBlank(version) && !"auto".equals(version)) {
            cmd += " --version " + version;
        }
        
        AtomicReference<String> versionRef = new AtomicReference<>();
        AtomicReference<String> installDirRef = new AtomicReference<>();
        Consumer<ObjectNode> recordConsumer = record -> {
            versionRef.set(extractTextField(record, "version", null));
            installDirRef.set(extractTextField(record, "installDir", "binDir"));
        };
        
        var result = executeFcliCommandWithRecordConsumer(cmd, recordConsumer, true);
        if (result != null && result.getExitCode() == 0) {
            return new RegistrationResult(true, versionRef.get(), installDirRef.get());
        }
        
        // Registration failed
        if (!toolsMixin.isPreinstalledMode()) {
            System.out.println("Tool " + toolName + " version '" + spec.getEffectiveVersion() + "' not found, will proceed with installation");
        }
        return new RegistrationResult(false, null, null);
    }
    
    /**
     * Execute an fcli command and capture the first ObjectNode record produced.
     * 
     * @param cmd The fcli command to execute
     * @param recordConsumer Consumer to process each ObjectNode record
     * @param suppressErrors If true, catch FcliCommandExecutionException and return null on failure
     * @return The execution result, or null if suppressErrors is true and execution failed
     */
    private OutputHelper.Result executeFcliCommandWithRecordConsumer(String cmd, Consumer<ObjectNode> recordConsumer, boolean suppressErrors) {
        try {
            return FcliCommandExecutorFactory.builder()
                    .cmd(cmd)
                    .stdoutOutputType(OutputType.suppress)
                    .stderrOutputType(OutputType.collect)
                    .recordConsumer(recordConsumer)
                    .onFail(suppressErrors ? null : onFail)
                    .build()
                    .create()
                    .execute();
        } catch (FcliCommandExecutionException e) {
            if (suppressErrors) {
                return null;
            }
            throw e;
        }
    }
    
    /**
     * Extract a text field from an ObjectNode, with optional fallback to another field.
     * 
     * @param record The ObjectNode to extract from
     * @param primaryField The primary field name to extract
     * @param fallbackField Optional fallback field name if primary is null/missing
     * @return The text value, or null if not found
     */
    private String extractTextField(ObjectNode record, String primaryField, String fallbackField) {
        JsonNode node = record.get(primaryField);
        if (node != null && !node.isNull()) {
            return node.asText();
        }
        if (fallbackField != null) {
            node = record.get(fallbackField);
            if (node != null && !node.isNull()) {
                return node.asText();
            }
        }
        return null;
    }
    
    private InstallResult installTool(ToolSetupSpec spec) {
        String toolName = spec.toolName();
        // Convert 'auto' to 'latest' for installation
        String version = spec.getEffectiveVersion();
        if ("auto".equals(version)) {
            version = "latest";
        }
        String cmd = "tool " + toolName + " install --version " + version;
        
        // For fcli, if --self is specified, use copy-if-matching to avoid re-downloading
        if (spec.tool() == Tool.FCLI && toolsMixin.getSelf() != null) {
            cmd += " --copy-if-matching \"" + toolsMixin.getSelf() + "\"";
        }
        
        // Handle tool cache pattern
        String effectiveInstallDirPattern = toolsMixin.getEffectiveInstallDirPattern();
        if (effectiveInstallDirPattern != null) {
            String resolvedVersion = resolveSemanticVersion(spec.tool(), version);
            if (resolvedVersion != null) {
                String cacheDir = effectiveInstallDirPattern
                    .replace("{tool}", toolName)
                    .replace("{version}", resolvedVersion);
                cmd += " --install-dir \"" + cacheDir + "\"";
            } else {
                // Fall back to base-dir if version resolution fails
                if (toolsMixin.getBaseDir() != null) {
                    cmd += " --base-dir \"" + toolsMixin.getBaseDir() + "\"";
                }
            }
        } else if (toolsMixin.getBaseDir() != null) {
            cmd += " --base-dir \"" + toolsMixin.getBaseDir() + "\"";
        }
        
        // JRE handling for sc-client is now done automatically by the install command
        
        AtomicReference<String> actionRef = new AtomicReference<>("installed");
        AtomicReference<String> installDirRef = new AtomicReference<>();
        Consumer<ObjectNode> recordConsumer = record -> {
            String action = extractTextField(record, "__action__", null);
            if (action != null) {
                actionRef.set(action.equals("SKIPPED_EXISTING") ? "skipped_existing" : "installed");
            }
            installDirRef.set(extractTextField(record, "installDir", null));
        };
        
        try {
            executeFcliCommandWithRecordConsumer(cmd, recordConsumer, false);
            return new InstallResult(actionRef.get(), installDirRef.get());
        } catch (FcliCommandExecutionException e) {
            System.err.println("Installation for " + toolName + " failed:");
            throw new FcliSimpleException("Installation of " + toolName + " failed");
        }
    }
    

    
    private String resolveSemanticVersion(Tool tool, String version) {
        // Convert 'auto' to 'latest' for version resolution
        String versionToResolve = "auto".equals(version) ? "latest" : version;
        try {
            var definition = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(tool.getToolName());
            var versionDescriptor = definition.getVersion(versionToResolve);
            return versionDescriptor.getVersion();
        } catch (Exception e) {
            // Version resolution failed, return null
            return null;
        }
    }
    
    private void printSummary(List<ToolSetupResult> results) {
        System.out.println();
        System.out.println("Fortify tools setup complete. " + results.size() + " tool(s) processed.");
        
        for (ToolSetupResult result : results) {
            System.out.println("  " + SUCCESS_MARKER + " " + result.toolName + ": " + result.version + " (" + result.status + ") at " + result.binDir);
        }
    }

}