package com.fortify.cli.aviator.entitlement.cli.cmd;


import picocli.CommandLine.Command;

@Command(name = "entitlement",
        subcommands = {
            AviatorEntitlementListCommand.class
        }
)
public class AviatorEntitlementCommands {
}
