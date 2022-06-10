package com.fortify.cli.tool.picocli.command.mixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine;

@ReflectiveAccess
public class DownloadPathMixin {
    @CommandLine.Mixin public DownloadUrlMixin DownloadUrlOpt;

    @CommandLine.Option(
            names = {"-d","--downloadPath"},
            description = "The location where you want to download to."
    )
    public String DownloadPath = "";
}
