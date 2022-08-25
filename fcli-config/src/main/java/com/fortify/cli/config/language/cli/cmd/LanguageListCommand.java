package com.fortify.cli.config.language.cli.cmd;

import com.fortify.cli.config.language.manager.LanguageConfigManager;

import jakarta.annotation.PostConstruct;
import picocli.CommandLine;


/**
 * TODO: I'd like for the implementation to be dynamic so that supported languages are automatically detected.
 */
@CommandLine.Command(
        name = "list"
)
public class LanguageListCommand extends AbstractLanguageCommand {
    @PostConstruct
    @Override
    public void run() {
        System.out.println("Below is a list of supported languages with fcli:");
        for(String lang : LanguageConfigManager.supportedLanguages){
            System.out.println(languageConfigManager.getLanguageForHelp(lang));
        }
    }

}
