package com.fortify.cli.config.language.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.i18n.helper.LanguageHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Set.CMD_NAME)
public class LanguageSetCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Mixin @Getter private OutputHelperMixins.Set outputHelper;
    @Parameters(index = "0", descriptionKey = "fcli.config.language.set.language")
    private String language;
    
    @Override
    public JsonNode getJsonNode() {
        return LanguageHelper.setConfiguredLanguage(language).asObjectNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
