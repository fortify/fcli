package com.fortify.cli.config.proxy.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "proxy",
        description = "Commands for managing connectivity through proxies",
        subcommands = {
            ProxyAddCommand.class,
            ProxyClearCommand.class,
            ProxyDeleteCommand.class,
            ProxyListCommand.class,
            ProxyUpdateCommand.class
        }
)
public class ProxyCommands extends AbstractFortifyCLICommand {
}
