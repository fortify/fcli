package com.fortify.cli.aviator.entitlement.cli.cmd;


import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine.Command;

@Command(name = "entitlement",
        subcommands = {
            AviatorEntitlementListCommand.class
        }
)
public class AviatorEntitlementCommands extends AbstractContainerCommand {
}
