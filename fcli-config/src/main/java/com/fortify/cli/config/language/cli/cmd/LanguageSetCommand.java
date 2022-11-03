package com.fortify.cli.config.language.cli.cmd;

import jakarta.annotation.PostConstruct;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

@CommandLine.Command(
        name = "set"
)
public class LanguageSetCommand extends AbstractLanguageCommand {
    // TODO: Need to internationalize paramLabel at some point.
    @Parameters(index = "0", descriptionKey = "fcli.config.language.set.language")
    private String language;

    @Override
    public void run() {
        languageConfigManager.setLanguage(language);
        // TODO Write some output using StandardOutputWriterFactory mixin;
    }
}
