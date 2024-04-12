/**
 * Copyright 2023 Open Text.
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
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public abstract class AbstractToolListPlatformsCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper; 
    @Getter @Option(names={"-v", "--version"}, required = true, descriptionKey="fcli.tool.list-platforms.version", defaultValue = "latest") 
    private String version;
    
    @Override
    public final JsonNode getJsonNode() {
        return ToolDefinitionsHelper.getToolDefinitionRootDescriptor(getToolName())
                .getVersion(version).getBinaries().keySet().stream()
                .map(this::createObjectNode)
                .collect(JsonHelper.arrayNodeCollector());
    }
    
    @Override
    public final boolean isSingular() {
        return false;
    }
    
    protected abstract String getToolName();
    
    private final ObjectNode createObjectNode(String platform) {
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("platform", platform);
    }
}
