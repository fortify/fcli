package com.fortify.cli.sc_sast.pkg.cli.cmd;

import com.fortify.cli.sc_sast.pkg.cli.cmd.create.SCSastPackageCreateCommands;

import picocli.CommandLine.Command;

@Command(
        name = "package",
        aliases = "pkg",
        hidden = true,
        subcommands = {
                SCSastPackageCreateCommands.class,
        }
)
public class SCSastPackageCommands {
}
