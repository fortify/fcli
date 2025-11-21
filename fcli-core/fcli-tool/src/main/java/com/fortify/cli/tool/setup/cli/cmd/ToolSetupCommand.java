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

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.exception.FcliCommandExecutionException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.JreHelper;
import com.fortify.cli.common.util.OutputHelper;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool.setup.cli.mixin.ToolSetupToolsMixin;
import com.fortify.cli.tool.setup.cli.mixin.ToolSetupToolsMixin.ToolSetupSpec;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "setup")
public class ToolSetupCommand extends AbstractRunnableCommand {
    @Mixin @Getter
    private ToolSetupToolsMixin toolsMixin;
    
    // Record to hold setup result information
    private record ToolSetupResult(String toolName, String status, String version, String binDir) {}
    
    // Record to hold registration result
    private record RegistrationResult(boolean success, String installDir) {}
    
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
            ToolSetupResult result = setupTool(spec);
            results.add(result);
        }
        
        // Print detailed summary
        printSummary(results);
        return 0;
    }
    
    private void updateToolDefinitions() {
        String source = toolsMixin.getToolDefinitions();
        String cmd = "tool definitions update" + (source != null ? " --source \"" + source + "\"" : "");
        executeFcliCommand(cmd);
    }
    
    private ToolSetupResult setupTool(ToolSetupSpec spec) {
        Tool tool = spec.tool();
        String toolName = tool.getToolName();
        
        System.out.println("Setting up " + toolName + "...");
        
        // Determine the effective version/path
        String effectiveVersion = determineEffectiveVersion(spec);
        
        // Try to register first
        RegistrationResult regResult = tryRegisterTool(spec, effectiveVersion);
        if (regResult.success()) {
            System.out.println("✓ " + toolName + " registered successfully");
            return new ToolSetupResult(toolName, "registered", effectiveVersion, regResult.installDir());
        }
        
        // If registration failed and not air-gapped, try to install
        if (!toolsMixin.isAirGapped()) {
            InstallResult installResult = installTool(spec, effectiveVersion);
            System.out.println("✓ " + toolName + " " + installResult.action() + " successfully");
            return new ToolSetupResult(toolName, installResult.action(), effectiveVersion, installResult.installDir());
        } else {
            throw new FcliSimpleException("Tool " + toolName + " not found and air-gapped mode prevents installation");
        }
    }
    
    private String determineEffectiveVersion(ToolSetupSpec spec) {
        if (spec.hasArgument()) {
            if (spec.isPathArgument()) {
                return "preinstalled"; // or handle path
            } else {
                return spec.getVersion();
            }
        }
        
        // Check TOOL_VERSION environment variable
        String versionEnvVar = spec.tool().getDefaultEnvPrefix() + "_VERSION";
        String versionEnvValue = System.getenv(versionEnvVar);
        if (versionEnvValue != null && !versionEnvValue.isEmpty()) {
            return versionEnvValue;
        }
        
        // Check TOOL_HOME environment variable
        String envVar = spec.tool().getDefaultEnvPrefix() + "_HOME";
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isEmpty()) {
            return "auto";
        }
        
        // Fall back to auto
        return "auto";
    }
    
    private RegistrationResult tryRegisterTool(ToolSetupSpec spec, String version) {
        String toolName = spec.toolName();
        String cmd = "tool " + toolName + " register";
        
        if (spec.isPathArgument()) {
            cmd += " --path \"" + spec.getPath() + "\"";
        } else {
            cmd += " --path $PATH";
            if (!"auto".equals(version) && !"preinstalled".equals(version)) {
                cmd += " --version " + version;
            }
        }
        
        try {
            var result = executeFcliCommand(cmd);
            if (result.getExitCode() == 0) {
                // Try to extract install directory from JSON output
                String installDir = extractInstallDirFromJsonOutput(result.getOut());
                return new RegistrationResult(true, installDir != null ? installDir : "PATH");
            }
        } catch (FcliCommandExecutionException e) {
            // Registration failed, but don't throw - just log progress
            System.out.println("Tool " + toolName + " not found in PATH, will proceed with installation");
            // Do not show stderr for expected registration failures
        } catch (Exception e) {
            // Other exceptions
            System.out.println("Tool " + toolName + " not found in PATH, will proceed with installation");
        }
        return new RegistrationResult(false, null);
    }
    
    private InstallResult installTool(ToolSetupSpec spec, String version) {
        String toolName = spec.toolName();
        String cmd = "tool " + toolName + " install --version " + ("auto".equals(version) ? "latest" : version) + " --output json";
        
        // For fcli, if --self is specified, use copy-from to avoid re-downloading
        if (spec.tool() == Tool.FCLI && toolsMixin.getSelf() != null) {
            cmd += " --copy-from \"" + toolsMixin.getSelf() + "\" --on-copy-version-mismatch skip";
        }
        
        // Handle tool cache pattern
        String effectiveInstallDirPattern = toolsMixin.getEffectiveInstallDirPattern();
        if (effectiveInstallDirPattern != null && !"preinstalled".equals(version)) {
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
        
        try {
            var result = executeFcliCommand(cmd);
            
            // Extract action and install directory from JSON output
            String action = extractActionFromJsonOutput(result.getOut());
            String installDir = extractInstallDirFromJsonOutput(result.getOut());
            
            return new InstallResult(action != null ? action : "installed", installDir != null ? installDir : "installed");
        } catch (FcliCommandExecutionException e) {
            // Show user-friendly error message
            System.err.println("Installation for " + toolName + " failed:");
            if (e.getResult().getErr() != null && !e.getResult().getErr().isEmpty()) {
                System.err.println(e.getResult().getErr());
            } else if (e.getResult().getOut() != null && !e.getResult().getOut().isEmpty()) {
                System.err.println(e.getResult().getOut());
            }
            throw new FcliSimpleException("Installation of " + toolName + " failed");
        }
    }
    
    private String extractInstallDirFromJsonOutput(String output) {
        // Parse JSON output to extract installDir
        String[] lines = output.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("\"installDir\"")) {
                // Extract installDir value from JSON like: "installDir" : "/path/to/dir",
                int colonIndex = line.indexOf(":");
                if (colonIndex != -1) {
                    String valuePart = line.substring(colonIndex + 1).trim();
                    if (valuePart.startsWith("\"") && valuePart.endsWith("\",")) {
                        return valuePart.substring(1, valuePart.length() - 2);
                    } else if (valuePart.startsWith("\"") && valuePart.endsWith("\"")) {
                        return valuePart.substring(1, valuePart.length() - 1);
                    }
                }
            }
        }
        return null;
    }
    
    private String extractActionFromJsonOutput(String output) {
        // Look for action in JSON output like: "__action__" : "SKIPPED_EXISTING",
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith("\"__action__\"")) {
                int colonIndex = line.indexOf(":");
                if (colonIndex != -1) {
                    String valuePart = line.substring(colonIndex + 1).trim();
                    if (valuePart.startsWith("\"") && valuePart.endsWith("\",")) {
                        String action = valuePart.substring(1, valuePart.length() - 2);
                        return action.equals("SKIPPED_EXISTING") ? "skipped_existing" : "installed";
                    } else if (valuePart.startsWith("\"") && valuePart.endsWith("\"")) {
                        String action = valuePart.substring(1, valuePart.length() - 1);
                        return action.equals("SKIPPED_EXISTING") ? "skipped_existing" : "installed";
                    }
                }
            }
        }
        // Default to installed
        return "installed";
    }
    
    private String resolveSemanticVersion(Tool tool, String version) {
        String versionToResolve = "auto".equals(version) ? "latest" : version;
        String cmd = "tool " + tool.getToolName() + " list -q \"version=='" + versionToResolve + "' || aliases.contains('" + versionToResolve + "')\" --output json";
        
        try {
            var result = executeFcliCommand(cmd);
            if (result.getExitCode() == 0 && !result.getOut().trim().isEmpty()) {
                // Parse JSON output to get version
                // For simplicity, extract version from the first line that contains it
                String[] lines = result.getOut().split("\n");
                for (String line : lines) {
                    if (line.contains("\"version\"")) {
                        // Simple JSON parsing - extract version value
                        int versionStart = line.indexOf("\"version\":");
                        if (versionStart != -1) {
                            int valueStart = line.indexOf("\"", versionStart + 10);
                            if (valueStart != -1) {
                                int valueEnd = line.indexOf("\"", valueStart + 1);
                                if (valueEnd != -1) {
                                    return line.substring(valueStart + 1, valueEnd);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Version resolution failed, return null
        }
        return null;
    }
    
    private String findExistingJreForScClient(String version) {
        // First check SCANCENTRAL_JAVA_HOME environment variable
        String scanCentralJavaHome = System.getenv("SCANCENTRAL_JAVA_HOME");
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
                String javaHome = System.getenv(envVar);
                if (javaHome != null && !javaHome.isEmpty()) {
                    return javaHome;
                }
            }
            
            // Try JAVA_HOME_<version>
            String envVar = "JAVA_HOME_" + javaVersion;
            String javaHome = System.getenv(envVar);
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
    
    private OutputHelper.Result executeFcliCommand(String cmd) {
        var executor = FcliCommandExecutorFactory.builder()
                .cmd(cmd)
                .stdoutOutputType(OutputType.collect)  // Collect stdout to get the JSON output
                .stderrOutputType(OutputType.collect)  // Collect stderr to show on failure
                .build()
                .create();
        return executor.execute();
    }
}