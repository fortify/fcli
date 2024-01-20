package com.fortify.cli.tool.definitions.cli.cmd;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.tool._common.cli.mixin.ToolDefinitionsUpdateMixin;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name=OutputHelperMixins.Update.CMD_NAME)
public class ToolDefinitionsUpdateCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier{
    @Mixin @Getter private OutputHelperMixins.Update outputHelper;
    @Mixin private ToolDefinitionsUpdateMixin toolDefinitionsUpdateMixin;
    
    @Override
    public JsonNode getJsonNode() {
        try {
            return new ObjectMapper().<ObjectNode>valueToTree(toolDefinitionsUpdateMixin.updateToolDefinitions());
        } catch (IOException e) {
            throw new RuntimeException("Error updating tool definitions", e);
        }
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }

    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }
}
