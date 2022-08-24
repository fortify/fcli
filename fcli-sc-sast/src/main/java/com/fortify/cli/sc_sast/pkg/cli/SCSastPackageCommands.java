package com.fortify.cli.sc_sast.pkg.cli;

import com.fortify.cli.common.dummy.cli.DummyCommand;

import picocli.CommandLine.Command;

@Command(
        name = "package",
        subcommands = {
                DummyCommand.class
        }
)
public class SCSastPackageCommands {
}
