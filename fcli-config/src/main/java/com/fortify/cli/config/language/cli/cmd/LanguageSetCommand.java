package com.fortify.cli.config.language.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.config.language.helper.LanguageConfigManager;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = BasicOutputHelperMixins.Set.CMD_NAME)
public class LanguageSetCommand extends AbstractLanguageCommand {
    @Mixin @Getter private BasicOutputHelperMixins.Set outputHelper;
    @Parameters(index = "0", descriptionKey = "fcli.config.language.set.language")
    private String language;
    
    @Override
    protected JsonNode getJsonNode() {
        LanguageConfigManager languageConfigManager = getLanguageConfigManager();
        languageConfigManager.setLanguage(language);
        return languageConfigManager.getCurrentLanguageDescriptor().asObjectNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
