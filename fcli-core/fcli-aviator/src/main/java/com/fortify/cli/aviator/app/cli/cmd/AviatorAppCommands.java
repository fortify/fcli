package com.fortify.cli.aviator.app.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine;

@CommandLine.Command(
        name = "app",
        subcommands = {
                AviatorAppCreateCommand.class,
                AviatorAppDeleteCommand.class,
                AviatorAppGetCommand.class,
                AviatorAppListCommand.class,
                AviatorAppUpdateCommand.class
        }
)
public class AviatorAppCommands extends AbstractContainerCommand {
}