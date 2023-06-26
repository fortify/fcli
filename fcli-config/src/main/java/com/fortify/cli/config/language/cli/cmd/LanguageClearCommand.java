package com.fortify.cli.config.language.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.i18n.helper.LanguageHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Clear.CMD_NAME)
public class LanguageClearCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Mixin @Getter private OutputHelperMixins.Clear outputHelper;
    
    @Override
    public JsonNode getJsonNode() {
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
