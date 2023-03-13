package com.fortify.cli.tool.common.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.tool.common.helper.ToolHelper;

import io.micronaut.core.annotation.ReflectiveAccess;

@ReflectiveAccess
public abstract class AbstractToolListCommand extends AbstractBasicOutputCommand {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    protected final JsonNode getJsonNode() {
        String toolName = getToolName();
        return ToolHelper.getToolVersionCombinedDescriptorsStream(toolName)
                .map(objectMapper::<ObjectNode>valueToTree)
                .collect(JsonHelper.arrayNodeCollector());
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
    
    protected abstract String getToolName();
}
