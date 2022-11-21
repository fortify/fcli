package com.fortify.cli.config.language.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.config.language.helper.LanguageConfigManager.LanguageDescriptor;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name=BasicOutputHelperMixins.List.CMD_NAME)
public class LanguageListCommand extends AbstractLanguageCommand {
    @Mixin @Getter private BasicOutputHelperMixins.List outputHelper;
    
    @Override
    protected JsonNode getJsonNode() {
        return getLanguageConfigManager().getSupportLanguageDescriptorsStream()
            .map(LanguageDescriptor::asObjectNode)
            .collect(JsonHelper.arrayNodeCollector());
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
