package com.fortify.cli.config.picocli.command.language;

import com.fortify.cli.common.locale.AbstractLanguageCommand;
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
        System.out.println(languageHelper.getLanguageForHelp(languageHelper.getLanguageConfig()));
    }

}
