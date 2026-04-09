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
package com.fortify.cli.tool._common.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstallationOutputDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import picocli.CommandLine.Parameters;

/**
 * Abstract base class for tool 'get' commands that retrieve information about
 * a specific tool version. Similar to AbstractToolListCommand but returns a
 * single record instead of a list.
 * 
 * Subclasses must implement:
 * - getToolName(): Return the tool identifier
 * 
 * @author Ruud Senden
 */
public abstract class AbstractToolGetCommand extends AbstractOutputCommand implements IJsonNodeSupplier {

    @Parameters(index = "0", descriptionKey = "fcli.tool.get.version")
    private String requestedVersion;

    @Override
    public final JsonNode getJsonNode() {
        var tool = getTool();
        var toolName = tool.getToolName();
        var optDefinition = ToolDefinitionsHelper.tryGetToolDefinitionRootDescriptor(toolName);

        if (!tool.requiresToolDefinitions() && optDefinition.isEmpty()) {
            return getJsonNodeWithoutDefinitions(toolName);
        }

        var toolDefinition = optDefinition.orElseGet(
                () -> ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName));

        // Resolve version (handles aliases like 'latest')
        var versionDescriptor = toolDefinition.getVersion(requestedVersion);

        // Load installation descriptor if tool is installed
        var installationDescriptor = ToolInstallationDescriptor.load(toolName, versionDescriptor);

        // Check if this is the default (last installed) version
        var lastInstalledDescriptor = ToolInstallationDescriptor.loadLastModified(toolName);
        boolean isDefault = isDefaultVersion(installationDescriptor, lastInstalledDescriptor);

        // Create output descriptor
        var outputDescriptor = new ToolInstallationOutputDescriptor(
                toolName,
                versionDescriptor,
                installationDescriptor,
                "",
                isDefault);

        return JsonHelper.getObjectMapper().valueToTree(outputDescriptor);
    }

    private JsonNode getJsonNodeWithoutDefinitions(String toolName) {
        ToolDefinitionVersionDescriptor versionDescriptor = new ToolDefinitionVersionDescriptor();
        versionDescriptor.setVersion(requestedVersion);
        versionDescriptor.setStable(true);
        versionDescriptor.setAliases(new String[0]);

        var installationDescriptor = ToolInstallationDescriptor.load(toolName, versionDescriptor);
        var lastInstalledDescriptor = ToolInstallationDescriptor.loadLastModified(toolName);
        boolean isDefault = isDefaultVersion(installationDescriptor, lastInstalledDescriptor);

        var outputDescriptor = new ToolInstallationOutputDescriptor(
                toolName,
                versionDescriptor,
                installationDescriptor,
                "",
                isDefault);
        return JsonHelper.getObjectMapper().valueToTree(outputDescriptor);
    }

    @Override
    public final boolean isSingular() {
        return true;
    }

    private boolean isDefaultVersion(ToolInstallationDescriptor installationDescriptor,
            ToolInstallationDescriptor lastInstalledDescriptor) {
        if (installationDescriptor == null || lastInstalledDescriptor == null) {
            return false;
        }
        return installationDescriptor.getInstallDir() != null
                && installationDescriptor.getInstallDir().equals(lastInstalledDescriptor.getInstallDir());
    }

    /**
     * @return Tool enum entry for this tool
     */
    protected abstract Tool getTool();
}
