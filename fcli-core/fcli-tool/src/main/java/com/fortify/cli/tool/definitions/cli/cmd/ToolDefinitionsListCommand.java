package com.fortify.cli.tool.definitions.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name=OutputHelperMixins.ListNoQuery.CMD_NAME)
public class ToolDefinitionsListCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Mixin @Getter private OutputHelperMixins.ListNoQuery outputHelper;
    
    @Override
    public JsonNode getJsonNode() {
        return JsonHelper.getObjectMapper().valueToTree(ToolDefinitionsHelper.getOutputDescriptors());
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
