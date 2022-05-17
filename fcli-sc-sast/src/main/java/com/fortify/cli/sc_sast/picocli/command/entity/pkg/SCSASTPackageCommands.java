package com.fortify.cli.sc_sast.picocli.command.entity.pkg;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine.Command;

@Command(
        name = "package",
        description = "Commands creating zip packages of a specified project.",
        subcommands = {
                DummyCommand.class
        }
)
public class SCSASTPackageCommands {
}
