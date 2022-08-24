package com.fortify.cli.config.picocli.command.language;

import jakarta.annotation.PostConstruct;
import picocli.CommandLine;

@CommandLine.Command(
        name = "get",
        description = "Get the currently set language/locale."
)
public class LanguageGetCommand extends AbstractLanguageCommand {

    @PostConstruct
    @Override
    public void run() {
        System.out.println(languageConfig.getLanguageForHelp(languageConfig.getLanguage()));
    }

}
