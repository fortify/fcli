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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstallationHelper;
import com.fortify.cli.tool._common.helper.ToolInstallationOutputDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

public abstract class AbstractToolListCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public final JsonNode getJsonNode() {
        var toolName = getToolName();
        var toolDefinition = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName);
        
        // Get versions from tool definitions
        Stream<ToolInstallationOutputDescriptor> definedVersions = toolDefinition.getVersionsStream()
                .map(this::createToolOutputDescriptor);
        
        // Get versions from state directory that aren't in definitions (e.g., "unknown" versions)
        Stream<ToolInstallationOutputDescriptor> unknownVersions = getUnknownVersionsFromState(toolName, toolDefinition);
        
        // Combine both streams
        return Stream.concat(definedVersions, unknownVersions)
                .map(objectMapper::<ObjectNode>valueToTree)
                .collect(JsonHelper.arrayNodeCollector());
    }
    
    @Override
    public final boolean isSingular() {
        return false;
    }
    
    protected abstract String getToolName();
    
    private ToolInstallationOutputDescriptor createToolOutputDescriptor(ToolDefinitionVersionDescriptor versionDescriptor) {
        var toolName = getToolName();
        var installationDescriptor = ToolInstallationDescriptor.load(toolName, versionDescriptor);
        return new ToolInstallationOutputDescriptor(toolName, versionDescriptor, installationDescriptor, "");
    }
    
    private Stream<ToolInstallationOutputDescriptor> getUnknownVersionsFromState(String toolName, 
            com.fortify.cli.tool.definitions.helper.ToolDefinitionRootDescriptor toolDefinition) {
        Path stateDir = ToolInstallationHelper.getToolsStatePath().resolve(toolName);
        
        if (!Files.exists(stateDir) || !Files.isDirectory(stateDir)) {
            return Stream.empty();
        }
        
        File[] versionFiles = stateDir.toFile().listFiles(File::isFile);
        if (versionFiles == null || versionFiles.length == 0) {
            return Stream.empty();
        }
        
        return Arrays.stream(versionFiles)
                .map(File::getName)
                .filter(version -> {
                    // Exclude files that match the tool name (these are metadata, not versions)
                    if (version.equals(toolName)) {
                        return false;
                    }
                    // Only include versions not already in tool definitions
                    try {
                        toolDefinition.getVersion(version);
                        return false; // Version exists in definitions
                    } catch (IllegalArgumentException e) {
                        return true; // Version not in definitions, include it
                    }
                })
                .map(version -> {
                    // Create synthetic version descriptor for unknown version
                    var installationDescriptor = ToolInstallationDescriptor.load(toolName, 
                            createSyntheticVersionDescriptor(toolDefinition, version));
                    return new ToolInstallationOutputDescriptor(toolName, 
                            createSyntheticVersionDescriptor(toolDefinition, version), 
                            installationDescriptor, "");
                });
    }
    
    private ToolDefinitionVersionDescriptor createSyntheticVersionDescriptor(
            com.fortify.cli.tool.definitions.helper.ToolDefinitionRootDescriptor toolDefinition, 
            String version) {
        // Create a minimal synthetic descriptor for unknown versions
        ToolDefinitionVersionDescriptor descriptor = new ToolDefinitionVersionDescriptor();
        descriptor.setVersion(version);
        descriptor.setStable(false);
        descriptor.setAliases(new String[0]);
        return descriptor;
    }
}
