package com.fortify.cli.config.language.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.i18n.helper.LanguageHelper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = BasicOutputHelperMixins.Get.CMD_NAME)
public class LanguageGetCommand extends AbstractBasicOutputCommand {
    @Mixin @Getter private BasicOutputHelperMixins.Get outputHelper;
    
    @Override
    protected JsonNode getJsonNode() {
        return LanguageHelper.getConfiguredLanguageDescriptor().asObjectNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
