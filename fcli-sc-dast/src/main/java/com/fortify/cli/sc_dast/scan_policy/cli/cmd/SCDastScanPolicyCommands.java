package com.fortify.cli.sc_dast.scan_policy.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;

import picocli.CommandLine.Command;

@Command(
        name = "scan-policy",
        subcommands = {
                SCDastScanPolicyGetCommand.class,
                SCDastScanPolicyListCommand.class
        }
)
@DefaultVariablePropertyName("id")
public class SCDastScanPolicyCommands extends AbstractFortifyCLICommand {
}
