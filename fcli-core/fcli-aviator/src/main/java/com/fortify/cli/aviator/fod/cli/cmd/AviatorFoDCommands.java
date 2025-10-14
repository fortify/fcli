package com.fortify.cli.aviator.fod.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import picocli.CommandLine;

@CommandLine.Command(
        name = "fod",
        hidden = true,
        subcommands = {
                AviatorFoDApplyRemediationsCommand.class
        }
)

public class AviatorFoDCommands extends AbstractContainerCommand {
}
