package com.fortify.cli.sc_sast.pkg.cli.cmd;

import com.fortify.cli.common.dummy.cli.cmd.DummyCommand;

import picocli.CommandLine.Command;

@Command(
        name = "package",
        subcommands = {
                DummyCommand.class
        }
)
public class SCSastPackageCommands {
}
