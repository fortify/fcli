package com.fortify.cli.tools.picocli.command.mixin;

import picocli.CommandLine;

public class DownloadUrlMixin {

    @CommandLine.Option(
            names = {"-u","--downloadUrl"},
            description = "Override the default URL that FCLI will download from."
    )
    public String DownloadUrl = null;
}
