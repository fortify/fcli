package com.fortify.cli.sc_dast.scan_policy.cli.cmd;

import com.fortify.cli.common.variable.MinusVariableDefinition;

import picocli.CommandLine.Command;

@Command(
        name = "scan-policy",
        subcommands = {
                SCDastScanPolicyGetCommand.class,
                SCDastScanPolicyListCommand.class
        }
)
@MinusVariableDefinition(name = "currentScanPolicy", field = "id")
public class SCDastScanPolicyCommands {
}
