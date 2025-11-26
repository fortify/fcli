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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstallationOutputDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstallationsResolver;
import com.fortify.cli.tool._common.helper.ToolInstallationsResolver.ToolInstallationRecord;
import com.fortify.cli.tool._common.helper.ToolInstallationsResolver.ToolInstallations;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;

public abstract class AbstractToolListCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    
    @Override
    public final JsonNode getJsonNode() {
        var toolName = getTool().getToolName();
        ToolInstallations installations = ToolInstallationsResolver.resolve(getTool());
        return installations.stream()
            .map(record -> createToolOutputDescriptor(toolName, record))
            .map(JsonHelper.getObjectMapper()::<ObjectNode>valueToTree)
            .collect(JsonHelper.arrayNodeCollector());
    }
    
    @Override
    public final boolean isSingular() {
        return false;
    }
    
    protected abstract Tool getTool();
    
    private ToolInstallationOutputDescriptor createToolOutputDescriptor(String toolName, ToolInstallationRecord record) {
        ToolDefinitionVersionDescriptor versionDescriptor = record.versionDescriptor();
        ToolInstallationDescriptor installationDescriptor = record.installationDescriptor();
        return new ToolInstallationOutputDescriptor(toolName, versionDescriptor, installationDescriptor, "", record.isDefault());
    }
}
