package com.fortify.cli.tool.picocli.command.mixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Option;

@ReflectiveAccess
public class DownloadUrlMixin {

    @Option(
            names = {"-u","--downloadUrl"},
            description = "Override the default URL that FCLI will download from."
    )
    public String DownloadUrl = null;
}
