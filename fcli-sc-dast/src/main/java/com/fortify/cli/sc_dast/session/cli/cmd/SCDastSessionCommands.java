package com.fortify.cli.sc_dast.session.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "session",
        subcommands = {
                SCDastSessionListCommand.class,
                SCDastSessionLoginCommand.class,
                SCDastSessionLogoutCommand.class 
        }
)
public class SCDastSessionCommands extends AbstractFortifyCLICommand {
}
