package com.fortify.cli.common.cli.mixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

public class CommonOptionMixins {
    private CommonOptionMixins() {}
    
    @ReflectiveAccess
    public static class OptionalDestinationFile {
        @Option(names = {"-f", "--dest"}, descriptionKey = "fcli.destination-file")
        @Getter private String destination;
    }
}
