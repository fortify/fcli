package com.fortify.cli.tools.picocli.command.mixin;

import picocli.CommandLine;

public class InstallPathMixin {
    @CommandLine.Mixin public DownloadUrlMixin DownloadUrlOpt;

    @CommandLine.Option(
            names = {"-i","--installPath"},
            description = "The location where you want to install to."
    )
    public String InstallPath = "";
}
