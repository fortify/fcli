package com.fortify.cli.config.picocli.command.language;

import com.fortify.cli.common.locale.AbstractLanguageCommand;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import jakarta.annotation.PostConstruct;
import picocli.CommandLine;

@CommandLine.Command(
        name = "set"
)
public class LanguageSetCommand extends AbstractLanguageCommand {
    private String settingName = "defaultUserLanguage";

    @CommandLine.Mixin
    private OutputMixin outputMixin;

    @CommandLine.Option(
            names = {"-l", "--lang"}
    )
    private String language;

    @PostConstruct
    @Override
    public void run() {
        languageHelper.setLanguageConfig(language);
    }
}
