package com.fortify.cli.config.picocli.command.language;

import com.fortify.cli.common.config.LanguageConfig;

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
        for(String lang : LanguageConfig.supportedLanguages){
            System.out.println(languageConfig.getLanguageForHelp(lang));
        }
    }

}
