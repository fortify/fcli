package com.fortify.cli.tools.picocli.command.mixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine;

@ReflectiveAccess
public class PrintChecksumTestInfoMixin {

    @CommandLine.Option(
            names = {"--printChecksumTest"},
            description = "Will print information about the file integrity check performed on downloaded files."
    )
    public boolean printChecksumTestInfo = false;
}
