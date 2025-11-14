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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstallationOutputDescriptor;
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
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Parameters(index = "0", descriptionKey = "fcli.tool.get.version")
    private String requestedVersion;
    
    @Override
    public final JsonNode getJsonNode() {
        var toolName = getToolName();
        var toolDefinition = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName);
        
        // Resolve version (handles aliases like 'latest')
        var versionDescriptor = toolDefinition.getVersion(requestedVersion);
        
        // Load installation descriptor if tool is installed
        var installationDescriptor = ToolInstallationDescriptor.load(toolName, versionDescriptor);
        
        // Create output descriptor
        var outputDescriptor = new ToolInstallationOutputDescriptor(
            toolName,
            versionDescriptor,
            installationDescriptor,
            ""
        );
        
        return objectMapper.valueToTree(outputDescriptor);
    }
    
    @Override
    public final boolean isSingular() {
        return true;
    }
    
    /**
     * @return Tool name identifier (e.g., "sc-client", "fcli", "fod-uploader")
     */
    protected abstract String getToolName();
}
