package com.fortify.cli.ssc.entity.plugin.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

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
public class SSCPluginCommands extends AbstractFortifyCLICommand {
}
