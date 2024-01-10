package com.fortify.cli.tool.config.cli.cmd;
import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine.Command;

@Command(
        name = "config",
        aliases = {},
        subcommands = {
                ToolConfigUpdateCommand.class
        }
)

public class ToolConfigCommands extends AbstractContainerCommand {
}