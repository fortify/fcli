package com.fortify.cli.sc_sast.pkg.cli.cmd.create;

import picocli.CommandLine.Command;

@Command(
        name = "create",
        subcommands = {
                SCSastPackageCreateMavenCommand.class,
                SCSastPackageCreateGradleCommand.class,
        }
)
public class SCSastPackageCreateCommands {
}
