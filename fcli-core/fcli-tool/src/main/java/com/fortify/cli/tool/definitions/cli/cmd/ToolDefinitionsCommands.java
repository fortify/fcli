package com.fortify.cli.tool.definitions.cli.cmd;
import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine.Command;

@Command(
        name = "definitions",
        aliases = {},
        subcommands = {
                ToolDefinitionsListCommand.class,
                ToolDefinitionsUpdateCommand.class,
                ToolDefinitionsResetCommand.class,
        }
)

public class ToolDefinitionsCommands extends AbstractContainerCommand {
}