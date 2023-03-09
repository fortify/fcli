package com.fortify.cli.config.language.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.i18n.helper.LanguageHelper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = BasicOutputHelperMixins.Clear.CMD_NAME)
public class LanguageClearCommand extends AbstractBasicOutputCommand implements IActionCommandResultSupplier {
    @Mixin @Getter private BasicOutputHelperMixins.Clear outputHelper;
    
    @Override
    protected JsonNode getJsonNode() {
        JsonNode result = LanguageHelper.getConfiguredLanguageDescriptor().asObjectNode();
        LanguageHelper.clearLanguageConfig();
        return result;
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override
    public String getActionCommandResult() {
        return "CLEARED";
    }
}
