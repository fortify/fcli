package com.fortify.cli.aviator.fod.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import picocli.CommandLine;

@CommandLine.Command(
        name = "fod",
        subcommands = {
                AviatorFODRemediationCommand.class
        }
)

public class AviatorFODCommands extends AbstractContainerCommand {
}
