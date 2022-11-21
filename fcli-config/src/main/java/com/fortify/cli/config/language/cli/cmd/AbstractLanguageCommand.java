package com.fortify.cli.config.language.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.config.language.util.LanguagePropertiesManager;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import lombok.Getter;

@ReflectiveAccess @FixInjection
public abstract class AbstractLanguageCommand extends AbstractBasicOutputCommand {
    @Inject @Getter private LanguagePropertiesManager languageConfigManager;
}
