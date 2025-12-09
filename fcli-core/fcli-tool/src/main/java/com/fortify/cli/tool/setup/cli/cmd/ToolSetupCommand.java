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
package com.fortify.cli.tool.setup.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.exception.FcliCommandExecutionException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.JreHelper;
import com.fortify.cli.common.util.OutputHelper;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;
import com.fortify.cli.tool.setup.cli.mixin.ToolSetupToolsMixin;
import com.fortify.cli.tool.setup.cli.mixin.ToolSetupToolsMixin.ToolSetupSpec;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

// TODO Replace all fcli invocations (at least those for data retrieval; maybe consider keeping
//      register and install commands) with direct API calls (partially or all done; need to check)
// TODO Both here and in env commands, support tool name aliases (debricked-cli/dcli, ...) on --tools
@Command(name = "setup")
public class ToolSetupCommand extends AbstractRunnableCommand {
    @Mixin @Getter
    private ToolSetupToolsMixin toolsMixin;
    
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
        
        // Update tool definitions if not air-gapped
        if (!toolsMixin.isAirGapped()) {
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
        String source = toolsMixin.getToolDefinitions();
        ToolDefinitionsHelper.updateToolDefinitions(source);
    }
    
    private ToolSetupResult setupTool(ToolSetupSpec spec) {
        Tool tool = spec.tool();
        String toolName = tool.getToolName();
        
        System.out.println("Setting up " + toolName + "...");
        
        // Try to register first
        RegistrationResult regResult = tryRegisterTool(spec);
        if (regResult.success()) {
            System.out.println("✓ " + toolName + " registered successfully");
            String displayVersion = spec.hasPath() ? "preinstalled" : regResult.version();
            return new ToolSetupResult(toolName, "registered", displayVersion, regResult.installDir());
        }
        
        // If registration failed and a path was specified, fail immediately
        if (spec.hasPath()) {
            throw new FcliSimpleException("Tool " + toolName + " not found at specified path: " + spec.getEffectivePath());
        }
        
        // If registration failed and not air-gapped, try to install
        if (!toolsMixin.isAirGapped()) {
            InstallResult installResult = installTool(spec);
            System.out.println("✓ " + toolName + " " + installResult.action() + " successfully");
            return new ToolSetupResult(toolName, installResult.action(), spec.getEffectiveVersion(), installResult.installDir());
        } else {
            throw new FcliSimpleException("Tool " + toolName + " not found and air-gapped mode prevents installation");
        }
    }
    
    private RegistrationResult tryRegisterTool(ToolSetupSpec spec) {
        String toolName = spec.toolName();
        String cmd = "tool " + toolName + " register";
        
        // Handle path-based registration (from <tool>:<path> or <TOOL>_HOME)
        if (spec.hasPath()) {
            cmd += " --path \"" + spec.getEffectivePath() + "\"";
        } else {
            // Handle version-based registration (from <tool>:<version> or <TOOL>_VERSION or default)
            cmd += " --path \"" + EnvHelper.env("PATH") + "\"";
            if (spec.hasSpecificVersion()) {
                cmd += " --version " + spec.getEffectiveVersion();
            }
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
        
        // Registration failed, but don't throw - just log progress
        System.out.println("Tool " + toolName + " not found in PATH, will proceed with installation");
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
        String version = spec.getEffectiveVersion();
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
            cmd += " --install-dir \"" + toolsMixin.getBaseDir() + "\"";
        }
        
        if (spec.tool() == Tool.SC_CLIENT) {
            // Handle JRE for sc-client: try to find existing JRE, otherwise install with --with-jre
            String jrePath = findExistingJreForScClient(version);
            if (jrePath != null) {
                cmd += " --jre \"" + jrePath + "\"";
            } else {
                cmd += " --with-jre";
            }
        }
        
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
    
    private String findExistingJreForScClient(String version) {
        // First check SCANCENTRAL_JAVA_HOME environment variable
        String scanCentralJavaHome = EnvHelper.env("SCANCENTRAL_JAVA_HOME");
        if (scanCentralJavaHome != null && !scanCentralJavaHome.isEmpty()) {
            return scanCentralJavaHome;
        }
        
        // Check generic JAVA_HOME_<major-version> patterns
        // For sc-client, we need Java 8 or higher, try in order: 21, 17, 11, 8
        String[] javaVersions = {"21", "17", "11", "8"};
        String osArch = System.getProperty("os.arch", "").toUpperCase();
        
        for (String javaVersion : javaVersions) {
            // Try JAVA_HOME_<version>_<arch>
            if (!osArch.isEmpty()) {
                String envVar = "JAVA_HOME_" + javaVersion + "_" + osArch;
                String javaHome = EnvHelper.env(envVar);
                if (javaHome != null && !javaHome.isEmpty()) {
                    return javaHome;
                }
            }
            
            // Try JAVA_HOME_<version>
            String envVar = "JAVA_HOME_" + javaVersion;
            String javaHome = EnvHelper.env(envVar);
            if (javaHome != null && !javaHome.isEmpty()) {
                return javaHome;
            }
        }
        
        // Try to find a suitable JRE using JreHelper
        try {
            return JreHelper.findJavaHome("8");
        } catch (Exception e) {
            // If no suitable JRE found, return null to trigger --with-jre
            return null;
        }
    }
    
    private void printSummary(List<ToolSetupResult> results) {
        System.out.println();
        System.out.println("Fortify tools setup complete. " + results.size() + " tool(s) processed.");
        
        for (ToolSetupResult result : results) {
            System.out.println("  ✓ " + result.toolName + ": " + result.version + " (" + result.status + ") at " + result.binDir);
        }
    }
    

}