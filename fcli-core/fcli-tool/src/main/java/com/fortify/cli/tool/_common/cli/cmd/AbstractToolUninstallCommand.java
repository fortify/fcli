/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.tool._common.cli.cmd;

import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolUninstaller;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@CommandGroup("uninstall")
public abstract class AbstractToolUninstallCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Option(names={"-v", "--version"}, required = true, descriptionKey="fcli.tool.uninstall.version", defaultValue = "latest")
    private String version;
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;
    
    @Override
    public final JsonNode getJsonNode() {
        String toolName = getToolName();
        var versionDescriptor = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName).getVersionOrDefault(version);
        var installationDescriptor = getInstallationDescriptor(toolName, versionDescriptor);
        Path installPath = getInstallPath(installationDescriptor);
        requireConfirmation.checkConfirmed(installPath);
        var outputDescriptor = new ToolUninstaller(toolName).uninstall(versionDescriptor, installationDescriptor);
        return new ObjectMapper().valueToTree(outputDescriptor);
    }

    @Override
    public final String getActionCommandResult() {
        return "UNINSTALLED";
    }
    
    @Override
    public final boolean isSingular() {
        return true;
    }
    
    protected abstract String getToolName();
    
    private static final ToolInstallationDescriptor getInstallationDescriptor(String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        var installationDescriptor = ToolInstallationDescriptor.load(toolName, versionDescriptor);
        if ( installationDescriptor==null ) {
            throw new IllegalArgumentException("Tool installation not found");
        }
        return installationDescriptor;
    }
    
    private static final Path getInstallPath(ToolInstallationDescriptor installationDescriptor) {
        Path installPath = installationDescriptor.getInstallPath();
        if ( installPath==null ) {
            throw new IllegalStateException("Tool installation path not found");
        }
        return installPath;
    }
}
