package com.fortify.cli.ssc.picocli.command.plugin;

import picocli.CommandLine.Command;

@Command(
        name = "plugin",
        aliases = {},
        subcommands = {
        		SSCPluginDeleteCommand.class,
                SSCPluginListCommand.class
        }
)
public class SSCPluginCommands {
}
