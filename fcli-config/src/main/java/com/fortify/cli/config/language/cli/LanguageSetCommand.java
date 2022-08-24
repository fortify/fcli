package com.fortify.cli.config.language.cli;

import com.fortify.cli.common.output.cli.OutputMixin;

import jakarta.annotation.PostConstruct;
import picocli.CommandLine;

@CommandLine.Command(
        name = "set"
)
public class LanguageSetCommand extends AbstractLanguageCommand {
    @CommandLine.Mixin
    private OutputMixin outputMixin;

    // TODO: Need to internationalize paramLabel at some point.
    @CommandLine.Parameters(index = "0", descriptionKey = "fcli.config.language.set.language")
    private String language;

    @PostConstruct
    @Override
    public void run() {
        languageConfigManager.setLanguage(language);
    }
}
