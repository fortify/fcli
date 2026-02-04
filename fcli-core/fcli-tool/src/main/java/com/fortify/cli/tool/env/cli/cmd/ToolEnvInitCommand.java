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
package com.fortify.cli.tool.env.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.exception.FcliCommandExecutionException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.StreamingObjectNodeProducer;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.OutputHelper;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;
import com.fortify.cli.tool.env.cli.mixin.ToolEnvInitMixin;
import com.fortify.cli.tool.env.cli.mixin.ToolEnvInitMixin.ToolSetupSpec;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "init")
public class ToolEnvInitCommand extends AbstractOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin
    private OutputHelperMixins.TableNoQuery outputHelper;
    
    @Mixin @Getter
    private ToolEnvInitMixin toolsMixin;
    
    @Mixin
    private ProgressWriterFactoryMixin progressWriterFactory;
    
    // Class to hold setup result information
    @Reflectable
    @Data
    @Builder
    private static class ToolSetupResult {
        private final String name;
        private final String version;
        private final String binDir;
        private final String installDir;
        private final String __action__;
    }
    
    // Class to hold registration result
    @Reflectable
    @Data
    @Builder
    private static class RegistrationResult {
        private final String version;
        private final String binDir;
        private final String installDir;
        private final boolean success;
    }
    
    // Class to hold install result
    @Reflectable
    @Data
    @Builder
    private static class InstallResult {
        private final String binDir;
        private final String installDir;
        private final String action;
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
    
    @Override
    public String getActionCommandResult() {
        return "INITIALIZED";
    }
    
    @Override
    protected IObjectNodeProducer getObjectNodeProducer() {
        toolsMixin.validateOptions();
        
        List<ToolSetupSpec> specs = toolsMixin.getToolSetupSpecs();
        
        return StreamingObjectNodeProducer.builder()
                .streamSupplier(() -> setupTools(specs).stream()
                    .map(result -> JsonHelper.getObjectMapper().<ObjectNode>valueToTree(result)))
                .build();
    }
    
    private List<ToolSetupResult> setupTools(List<ToolSetupSpec> specs) {
        List<ToolSetupResult> results = new ArrayList<>();
        
        try (var progressWriter = progressWriterFactory.create()) {
            // Update tool definitions if not in preinstalled mode and not all tools have explicit paths
            if (!toolsMixin.isPreinstalledMode() && !toolsMixin.allToolsHavePaths()) {
                updateToolDefinitions(progressWriter);
            }
            
            for (ToolSetupSpec spec : specs) {
                results.add(setupTool(spec, progressWriter));
            }
        }
        
        return results;
    }
    
    private void updateToolDefinitions(IProgressWriterI18n progressWriter) {
        // Note: ToolDefinitionsHelper.updateToolDefinitions doesn't support progress writer yet
        ToolDefinitionsHelper.updateToolDefinitions(toolsMixin.getToolDefinitions(), false, null);
    }
    
    private ToolSetupResult setupTool(ToolSetupSpec spec, IProgressWriterI18n progressWriter) {
        Tool tool = spec.tool();
        String toolName = tool.getToolName();
        String requestedVersion = spec.getEffectiveVersion();
        String versionInfo = spec.hasPath() ? " at " + spec.getEffectivePath() : (requestedVersion != null ? " version '" + requestedVersion + "'" : "");
        
        progressWriter.writeProgress("Setting up " + toolName + versionInfo + "...");
        
        // Try to register first
        RegistrationResult regResult = tryRegisterTool(spec);
        if (regResult.isSuccess()) {
            progressWriter.writeProgress("Tool " + toolName + " registered successfully");
            String displayVersion = spec.hasPath() ? "preinstalled" : regResult.getVersion();
            return ToolSetupResult.builder()
                    .name(toolName)
                    .version(displayVersion)
                    .binDir(regResult.getBinDir())
                    .installDir(regResult.getInstallDir())
                    .__action__("REGISTERED")
                    .build();
        }
        
        // If registration failed and a path was specified, fail immediately
        if (spec.hasPath()) {
            throw new FcliSimpleException("Tool " + toolName + " not found at specified path: " + spec.getEffectivePath());
        }
        
        // If registration failed and not in preinstalled mode, try to install
        if (!toolsMixin.isPreinstalledMode()) {
            InstallResult installResult = installTool(spec, progressWriter);
            progressWriter.writeProgress("Tool " + toolName + " " + installResult.getAction() + " successfully");
            return ToolSetupResult.builder()
                    .name(toolName)
                    .version(spec.getEffectiveVersion())
                    .binDir(installResult.getBinDir())
                    .installDir(installResult.getInstallDir())
                    .__action__(installResult.getAction())
                    .build();
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
        AtomicReference<String> binDirRef = new AtomicReference<>();
        AtomicReference<String> installDirRef = new AtomicReference<>();
        AtomicReference<String> binaryNameRef = new AtomicReference<>();
        Consumer<ObjectNode> recordConsumer = record -> {
            versionRef.set(extractTextField(record, "version", null));
            binDirRef.set(extractTextField(record, "binDir", null));
            installDirRef.set(extractTextField(record, "installDir", null));
            binaryNameRef.set(spec.tool().getDefaultBinaryName());
        };
        
        var result = executeFcliCommandWithRecordConsumer(cmd, recordConsumer, true);
        if (result != null && result.getExitCode() == 0) {
            return RegistrationResult.builder()
                    .version(versionRef.get())
                    .binDir(binDirRef.get())
                    .installDir(installDirRef.get())
                    .success(true)
                    .build();
        }
        
        // Registration failed
        return RegistrationResult.builder()
                .success(false)
                .build();
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
                    .stderrOutputType(suppressErrors ? OutputType.suppress : OutputType.show)
                    .recordConsumer(recordConsumer)
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
    
    private InstallResult installTool(ToolSetupSpec spec, IProgressWriterI18n progressWriter) {
        String toolName = spec.toolName();
        progressWriter.writeProgress("Tool " + toolName + " version '" + spec.getEffectiveVersion() + "' not found, proceeding with installation");
        
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
        
        AtomicReference<String> actionRef = new AtomicReference<>("INSTALLED");
        AtomicReference<String> binDirRef = new AtomicReference<>();
        AtomicReference<String> installDirRef = new AtomicReference<>();
        AtomicReference<String> binaryNameRef = new AtomicReference<>();
        Consumer<ObjectNode> recordConsumer = record -> {
            String action = extractTextField(record, "__action__", null);
            if (action != null) {
                actionRef.set(action);
            }
            binDirRef.set(extractTextField(record, "binDir", null));
            installDirRef.set(extractTextField(record, "installDir", null));
            binaryNameRef.set(spec.tool().getDefaultBinaryName());
        };
        
        try {
            executeFcliCommandWithRecordConsumer(cmd, recordConsumer, false);
            return InstallResult.builder()
                    .binDir(binDirRef.get())
                    .installDir(installDirRef.get())
                    .action(actionRef.get())
                    .build();
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
}