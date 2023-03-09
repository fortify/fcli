package com.fortify.cli.config.language.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.i18n.helper.LanguageHelper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = BasicOutputHelperMixins.Set.CMD_NAME)
public class LanguageSetCommand extends AbstractBasicOutputCommand {
    @Mixin @Getter private BasicOutputHelperMixins.Set outputHelper;
    @Parameters(index = "0", descriptionKey = "fcli.config.language.set.language")
    private String language;
    
    @Override
    protected JsonNode getJsonNode() {
        return LanguageHelper.setConfiguredLanguage(language).asObjectNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
