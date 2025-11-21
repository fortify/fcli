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
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool._common.helper.ToolInstallationOutputDescriptor;
import com.fortify.cli.tool._common.helper.ToolRegistrationHelper;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * Abstract base class for tool register commands. Provides registration from user-specified paths.
 * 
 * Subclasses must implement:
 * - getToolName(): Return the tool identifier
 * - getDefaultBinaryName(): Return platform-specific binary name
 * - detectVersion(File toolBinary, File installDir): Implement tool-specific version detection logic
 * 
 * @author Ruud Senden
 */
@Command(name = OutputHelperMixins.Register.CMD_NAME)
public abstract class AbstractToolRegisterCommand extends AbstractOutputCommand 
        implements IJsonNodeSupplier, IActionCommandResultSupplier {
    
    @Getter @Mixin private OutputHelperMixins.Register outputHelper;
    
    @Option(names = {"-p", "--path"}, required = true, descriptionKey = "fcli.tool.register.path")
    private String pathOption;
    
    @Option(names = {"-v", "--version"}, required = false, descriptionKey = "fcli.tool.register.version")
    private String requestedVersion = "any";
    
    @Override
    public final String getActionCommandResult() {
        return "REGISTERED";
    }
    
    @Override
    public final boolean isSingular() {
        return true;
    }
    
    /**
     * Get the tool enum entry. Subclasses must implement this to provide the tool.
     * @return Tool enum entry
     */
    protected abstract Tool getTool();
    
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
    
    @Override
    @SneakyThrows
    public ObjectNode getJsonNode() {
        var context = new ToolRegistrationHelper.RegistrationContext(
            getTool().getToolName(), 
            getTool().getDefaultBinaryName(), 
            this::detectVersion
        );
        
        var result = context.register(pathOption, requestedVersion);
        
        ToolInstallationOutputDescriptor descriptor = new ToolInstallationOutputDescriptor(
            getTool().getToolName(), 
            result.getVersionDescriptor(), 
            result.getInstallation(), 
            result.getAction().name(),
            true
        );
        
        return JsonHelper.getObjectMapper().<ObjectNode>valueToTree(descriptor);
    }
}
