package com.fortify.cli.tool.picocli.command.mixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine;

@ReflectiveAccess
public class PackageVersionMixin {

    @CommandLine.Option(
            names = {"-v","--packageVersion"},
            description = "The version of the package that should be downloaded and/or installed."
    )
    public String DownloadPackageVersion = null;
}
