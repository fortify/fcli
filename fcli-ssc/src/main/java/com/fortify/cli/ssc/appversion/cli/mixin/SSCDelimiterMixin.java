package com.fortify.cli.ssc.appversion.cli.mixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

@ReflectiveAccess
public final class SSCDelimiterMixin {
    @Option(names = {"--delim"},
            description = "Change the default delimiter character when using options that accepts " +
            "\"application:version\" as an argument or parameter.", defaultValue = ":")
    @Getter private String delimiter;
}