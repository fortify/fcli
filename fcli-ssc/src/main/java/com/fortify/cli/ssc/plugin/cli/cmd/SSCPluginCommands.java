package com.fortify.cli.ssc.plugin.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "plugin",
        aliases = {},
        subcommands = {
                SSCPluginInstallCommand.class,
        		SSCPluginUninstallCommand.class,
                SSCPluginEnableCommand.class,
                SSCPluginDisableCommand.class,
                SSCPluginGetCommand.class,
                SSCPluginListCommand.class
        }

)
public class SSCPluginCommands {
}
