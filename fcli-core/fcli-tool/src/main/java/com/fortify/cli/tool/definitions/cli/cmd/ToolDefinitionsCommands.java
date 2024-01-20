package com.fortify.cli.tool.definitions.cli.cmd;
import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine.Command;

@Command(
        name = "definitions",
        aliases = {},
        subcommands = {
                ToolDefinitionsUpdateCommand.class
        }
)

public class ToolDefinitionsCommands extends AbstractContainerCommand {
}