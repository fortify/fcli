package com.fortify.cli.config.language.cli.cmd;

import jakarta.annotation.PostConstruct;
import picocli.CommandLine;

@CommandLine.Command(
        name = "get",
        description = "Get the currently set language/locale."
)
public class LanguageGetCommand extends AbstractLanguageCommand {

    @Override
    public void run() {
        System.out.println(languageConfigManager.getLanguageForHelp(languageConfigManager.getLanguage()));
    }

}
