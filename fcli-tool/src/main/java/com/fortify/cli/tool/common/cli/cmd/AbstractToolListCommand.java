package com.fortify.cli.tool.common.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.tool.common.helper.ToolHelper;
import com.fortify.cli.tool.common.helper.ToolInstallDescriptor;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public abstract class AbstractToolListCommand extends AbstractBasicOutputCommand {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Getter @Mixin private BasicOutputHelperMixins.List outputHelper;
    
    @Override
    protected final JsonNode getJsonNode() {
        String toolName = getToolName();
        ToolInstallDescriptor descriptor = ToolHelper.getToolInstallDescriptor(toolName);
        return descriptor.getVersionsStream()
                .map(objectMapper::<ObjectNode>valueToTree)
                .map(o->o.put("name", toolName)
                         .put("installed", "Unknown"))
                .collect(JsonHelper.arrayNodeCollector());
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
    
    protected abstract String getToolName();
}
